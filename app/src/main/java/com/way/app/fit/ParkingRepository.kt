package com.way.app.fit

import android.content.Context
import android.util.Log
import com.way.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 서울 열린데이터 GetParkingInfo(실시간 주차대수) 조회.
 *
 * GetParkingInfo 응답에는 좌표가 없으므로, GetParkInfo에서 미리 추출한
 * assets/parking_coords.json(PKLT_CD -> lat/lon, tools/build_parking_coords.py로 생성)과
 * PKLT_CD로 조인해 좌표를 채운다. 좌표가 매칭되지 않는 항목은 제외한다.
 *
 * 키 미설정 / 호출 실패 / 조인 결과 0건이면 assets/parking_sample.json 으로 폴백한다.
 * 주의: 인증키가 들어간 URL은 절대 로그에 남기지 않는다.
 */
class ParkingRepository(
    context: Context,
    private val apiKey: String = BuildConfig.SEOUL_API_KEY
) {
    private val appContext = context.applicationContext

    enum class Source { API, SAMPLE }

    data class Result(val lots: List<ParkingLot>, val source: Source)

    /** GetParkingInfo 한 행(좌표 없음). */
    data class RealtimeRow(
        val code: String,
        val name: String,
        val totalSpaces: Int,
        val currentParked: Int,
        val baseFee: Int
    )

    data class Coord(val lat: Double, val lon: Double)

    data class Page(val totalCount: Int, val rows: List<RealtimeRow>)

    private val coords: Map<String, Coord> by lazy {
        appContext.assets.open(COORDS_FILE).bufferedReader(Charsets.UTF_8).use { parseCoords(it.readText()) }
    }

    suspend fun load(): Result = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.w(TAG, "SEOUL_API_KEY 미설정 → 샘플 데이터 사용")
            return@withContext sample()
        }
        try {
            val realtime = fetchAll()
            val joined = join(realtime, coords)
            Log.i(TAG, "realtime=${realtime.size}, joined=${joined.size}")
            if (joined.isEmpty()) {
                Log.w(TAG, "좌표 조인 결과 0건 → 샘플 데이터 사용")
                sample()
            } else {
                Result(joined, Source.API)
            }
        } catch (e: Exception) {
            // e.message 에 URL(=키)이 포함될 수 있으므로 예외 종류만 기록한다.
            Log.w(TAG, "API 호출 실패(${e.javaClass.simpleName}) → 샘플 데이터 사용")
            sample()
        }
    }

    private fun sample() = Result(SampleParkingSource.load(appContext), Source.SAMPLE)

    private fun fetchAll(): List<RealtimeRow> {
        val all = mutableListOf<RealtimeRow>()
        var start = 1
        var total = Int.MAX_VALUE
        var pages = 0
        while (start <= total && pages < MAX_PAGES) {
            val end = start + PAGE_SIZE - 1
            val page = parseResponse(get(start, end))
            total = page.totalCount
            all += page.rows
            start = end + 1
            pages++
        }
        return all.distinctBy { it.code }
    }

    private fun get(start: Int, end: Int): String {
        val key = URLEncoder.encode(apiKey, "UTF-8")
        val url = URL("http://openapi.seoul.go.kr:8088/$key/json/GetParkingInfo/$start/$end/")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val TAG = "ParkingRepository"
        private const val COORDS_FILE = "parking_coords.json"
        private const val PAGE_SIZE = 1000   // API 1회 최대 1000건
        private const val MAX_PAGES = 3
        private const val TIMEOUT_MS = 5_000

        /**
         * GetParkingInfo JSON 파싱 (순수 함수).
         * - RESULT.CODE 가 INFO-000 이 아니면 예외
         * - 실시간 연계(PRK_STTS_YN=1)가 아니거나, PKLT_CD/이름이 없거나, 총면수 0 이하인 행 제외
         */
        fun parseResponse(json: String): Page {
            val root = JSONObject(json)
            val body = root.optJSONObject("GetParkingInfo")
                ?: throw IOException("API error ${root.optJSONObject("RESULT")?.optString("CODE")}")
            val resultCode = body.optJSONObject("RESULT")?.optString("CODE").orEmpty()
            if (resultCode != "INFO-000") throw IOException("API error $resultCode")

            val arr = body.optJSONArray("row") ?: return Page(0, emptyList())
            val rows = (0 until arr.length()).mapNotNull { i ->
                val r = arr.getJSONObject(i)
                if (r.optString("PRK_STTS_YN") != "1") return@mapNotNull null
                val code = r.optString("PKLT_CD").trim().ifEmpty { return@mapNotNull null }
                val name = r.optString("PKLT_NM").trim().ifEmpty { return@mapNotNull null }
                val total = r.optString("TPKCT").toDoubleOrNull()?.toInt() ?: 0
                if (total <= 0) return@mapNotNull null
                RealtimeRow(
                    code = code,
                    name = name,
                    totalSpaces = total,
                    currentParked = r.optString("NOW_PRK_VHCL_CNT").toDoubleOrNull()?.toInt() ?: 0,
                    baseFee = r.optString("BSC_PRK_CRG").toDoubleOrNull()?.toInt() ?: 0
                )
            }
            return Page(body.optInt("list_total_count", rows.size), rows)
        }

        /** parking_coords.json 파싱: [{"PKLT_CD","lat","lon"}] -> 맵. 유효하지 않은 항목은 건너뛴다. */
        fun parseCoords(json: String): Map<String, Coord> {
            val arr = JSONArray(json)
            val map = HashMap<String, Coord>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val code = o.optString("PKLT_CD").trim()
                val lat = o.optDouble("lat")
                val lon = o.optDouble("lon")
                if (code.isEmpty() || lat.isNaN() || lon.isNaN() || lat == 0.0 || lon == 0.0) continue
                map.putIfAbsent(code, Coord(lat, lon))
            }
            return map
        }

        /** 실시간 행과 좌표를 PKLT_CD로 조인. 좌표가 없는 행은 제외. */
        fun join(rows: List<RealtimeRow>, coords: Map<String, Coord>): List<ParkingLot> =
            rows.mapNotNull { r ->
                val c = coords[r.code] ?: return@mapNotNull null
                ParkingLot(
                    name = r.name,
                    lat = c.lat,
                    lon = c.lon,
                    totalSpaces = r.totalSpaces,
                    currentParked = r.currentParked,
                    baseFee = r.baseFee
                )
            }
    }
}
