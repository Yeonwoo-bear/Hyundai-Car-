package com.way.app.fit

import android.content.Context
import android.util.Log
import com.way.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 서울 열린데이터 광장 GetParkingInfo(시영주차장 실시간 주차대수) 조회.
 * 키가 없거나 호출/파싱이 실패하면 assets/parking_sample.json 으로 폴백한다.
 *
 * 주의: 인증키가 들어간 URL은 절대 로그에 남기지 않는다.
 */
class ParkingRepository(
    context: Context,
    private val apiKey: String = BuildConfig.SEOUL_API_KEY
) {
    private val appContext = context.applicationContext

    enum class Source { API, SAMPLE }

    data class Result(val lots: List<ParkingLot>, val source: Source)

    suspend fun load(): Result = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.w(TAG, "SEOUL_API_KEY 미설정 → 샘플 데이터 사용")
            return@withContext sample()
        }
        try {
            val lots = fetchAll()
            if (lots.isEmpty()) {
                Log.w(TAG, "API 응답에 사용 가능한 주차장 없음 → 샘플 데이터 사용")
                sample()
            } else {
                Result(lots, Source.API)
            }
        } catch (e: Exception) {
            // e.message 에 URL(=키)이 포함될 수 있으므로 예외 종류만 기록한다.
            Log.w(TAG, "API 호출 실패(${e.javaClass.simpleName}) → 샘플 데이터 사용")
            sample()
        }
    }

    private fun sample() = Result(SampleParkingSource.load(appContext), Source.SAMPLE)

    private fun fetchAll(): List<ParkingLot> {
        val all = mutableListOf<ParkingLot>()
        var start = 1
        var total = Int.MAX_VALUE
        var pages = 0
        while (start <= total && pages < MAX_PAGES) {
            val end = start + PAGE_SIZE - 1
            val page = parseResponse(get(start, end))
            total = page.totalCount
            all += page.lots
            start = end + 1
            pages++
        }
        return all.distinctBy { it.name }
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

    data class Page(val totalCount: Int, val lots: List<ParkingLot>)

    companion object {
        private const val TAG = "ParkingRepository"
        private const val PAGE_SIZE = 1000   // API 1회 최대 1000건
        private const val MAX_PAGES = 3
        private const val TIMEOUT_MS = 5_000

        /**
         * GetParkingInfo JSON 파싱 (순수 함수, 단위 테스트 대상).
         * - RESULT.CODE 가 INFO-000 이 아니면 예외
         * - 좌표가 서울 범위 밖이거나 총면수 0 인 행 제외
         * - 실시간 연계(PRK_STTS_YN=1) 가 아닌 행은 현재 대수를 신뢰할 수 없어 제외
         */
        fun parseResponse(json: String): Page {
            val root = JSONObject(json)
            val body = root.optJSONObject("GetParkingInfo")
                ?: throw IOException("API error ${root.optJSONObject("RESULT")?.optString("CODE")}")
            val resultCode = body.optJSONObject("RESULT")?.optString("CODE").orEmpty()
            if (resultCode != "INFO-000") throw IOException("API error $resultCode")

            val rows = body.optJSONArray("row") ?: return Page(0, emptyList())
            val lots = (0 until rows.length()).mapNotNull { i ->
                val r = rows.getJSONObject(i)
                val realtime = r.optString("PRK_STTS_YN", "1")
                if (realtime.isNotEmpty() && realtime != "1") return@mapNotNull null
                val lat = r.optString("LAT").toDoubleOrNull() ?: return@mapNotNull null
                val lon = r.optString("LOT").toDoubleOrNull() ?: return@mapNotNull null
                if (lat !in 37.4..37.75 || lon !in 126.7..127.3) return@mapNotNull null
                val total = r.optString("TPKCT").toDoubleOrNull()?.toInt() ?: 0
                if (total <= 0) return@mapNotNull null
                ParkingLot(
                    name = r.optString("PKLT_NM").ifBlank { return@mapNotNull null },
                    lat = lat,
                    lon = lon,
                    totalSpaces = total,
                    currentParked = r.optString("NOW_PRK_VHCL_CNT").toDoubleOrNull()?.toInt() ?: 0,
                    baseFee = r.optString("BSC_PRK_CRG").toDoubleOrNull()?.toInt() ?: 0
                )
            }
            return Page(body.optInt("list_total_count", lots.size), lots)
        }
    }
}
