package com.way.app.fit

import com.way.app.fit.ParkingRepository.Coord
import com.way.app.fit.ParkingRepository.RealtimeRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class ParkingRepositoryParseTest {
    private val ok = """
        {"GetParkingInfo":{"list_total_count":4,
          "RESULT":{"CODE":"INFO-000","MESSAGE":"정상 처리되었습니다"},
          "row":[
            {"PKLT_CD":"1001","PKLT_NM":"종로 공영","TPKCT":100.0,"NOW_PRK_VHCL_CNT":40.0,"BSC_PRK_CRG":300.0,"PRK_STTS_YN":"1"},
            {"PKLT_CD":"1002","PKLT_NM":"실시간아님","TPKCT":50.0,"NOW_PRK_VHCL_CNT":0.0,"BSC_PRK_CRG":300.0,"PRK_STTS_YN":"0"},
            {"PKLT_CD":"1003","PKLT_NM":"총면0","TPKCT":0.0,"NOW_PRK_VHCL_CNT":0.0,"BSC_PRK_CRG":300.0,"PRK_STTS_YN":"1"},
            {"PKLT_CD":"","PKLT_NM":"코드없음","TPKCT":10.0,"NOW_PRK_VHCL_CNT":1.0,"BSC_PRK_CRG":300.0,"PRK_STTS_YN":"1"}
          ]}}
    """.trimIndent()

    @Test fun parsesRealtimeRowsOnly() {
        val page = ParkingRepository.parseResponse(ok)
        assertEquals(4, page.totalCount)
        val row = page.rows.single()
        assertEquals(RealtimeRow("1001", "종로 공영", 100, 40, 300), row)
    }

    @Test(expected = IOException::class)
    fun invalidKeyThrows() {
        ParkingRepository.parseResponse("""{"RESULT":{"CODE":"INFO-100","MESSAGE":"인증키가 유효하지 않습니다."}}""")
    }

    @Test fun parseCoordsSkipsInvalidEntries() {
        val map = ParkingRepository.parseCoords(
            """[{"PKLT_CD":"A","lat":37.5,"lon":127.0},
                {"PKLT_CD":"B","lat":0.0,"lon":0.0},
                {"PKLT_CD":"C","lat":"abc","lon":127.0},
                {"PKLT_CD":"","lat":37.5,"lon":127.0}]"""
        )
        assertEquals(setOf("A"), map.keys)
        assertEquals(Coord(37.5, 127.0), map["A"])
    }

    @Test fun joinFillsCoordsAndDropsUnmatched() {
        val rows = listOf(
            RealtimeRow("A", "매칭됨", 100, 25, 500),
            RealtimeRow("Z", "좌표없음", 100, 10, 500)
        )
        val lots = ParkingRepository.join(rows, mapOf("A" to Coord(37.55, 126.98)))
        assertEquals(1, lots.size)
        val lot = lots.single()
        assertEquals("매칭됨", lot.name)
        assertEquals(37.55, lot.lat, 1e-9)
        assertEquals(126.98, lot.lon, 1e-9)
        assertEquals(0.75, lot.vacancyRate, 1e-9)
    }

    @Test fun joinWithNoMatchesIsEmpty() {
        val rows = listOf(RealtimeRow("X", "n", 10, 1, 100))
        assertTrue(ParkingRepository.join(rows, mapOf("A" to Coord(37.5, 127.0))).isEmpty())
        assertTrue(ParkingRepository.join(emptyList(), emptyMap()).isEmpty())
    }

    @Test fun bundledCoordsAssetIsValid() {
        val file = listOf("src/main/assets/parking_coords.json", "app/src/main/assets/parking_coords.json")
            .map(::File).first { it.exists() }
        val map = ParkingRepository.parseCoords(file.readText())
        assertTrue(map.isNotEmpty())
        assertTrue(map.values.all { it.lat in 37.4..37.75 && it.lon in 126.7..127.3 })
    }

    @Test fun sampleJsonParses() {
        val json = """[{"name":"A","lat":37.5,"lon":127.0,"totalSpaces":10,"currentParked":3,"baseFee":500}]"""
        assertEquals(0.7, SampleParkingSource.parse(json).single().vacancyRate, 1e-9)
    }
}
