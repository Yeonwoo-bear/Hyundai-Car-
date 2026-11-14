package com.way.app.fit

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ParkingRepositoryParseTest {
    private val ok = """
        {"GetParkingInfo":{"list_total_count":3,
          "RESULT":{"CODE":"INFO-000","MESSAGE":"정상 처리되었습니다"},
          "row":[
            {"PKLT_NM":"종로 공영","LAT":"37.57","LOT":"126.99","TPKCT":"100.0","NOW_PRK_VHCL_CNT":"40","BSC_PRK_CRG":"300","PRK_STTS_YN":"1"},
            {"PKLT_NM":"좌표없음","LAT":"","LOT":"","TPKCT":"50","NOW_PRK_VHCL_CNT":"1","BSC_PRK_CRG":"300","PRK_STTS_YN":"1"},
            {"PKLT_NM":"실시간아님","LAT":"37.5","LOT":"127.0","TPKCT":"50","NOW_PRK_VHCL_CNT":"0","BSC_PRK_CRG":"300","PRK_STTS_YN":"0"}
          ]}}
    """.trimIndent()

    @Test fun parsesValidRowsOnly() {
        val page = ParkingRepository.parseResponse(ok)
        assertEquals(3, page.totalCount)
        val lot = page.lots.single()
        assertEquals("종로 공영", lot.name)
        assertEquals(100, lot.totalSpaces)
        assertEquals(40, lot.currentParked)
        assertEquals(300, lot.baseFee)
    }

    @Test(expected = IOException::class)
    fun invalidKeyThrows() {
        ParkingRepository.parseResponse("""{"RESULT":{"CODE":"INFO-100","MESSAGE":"인증키가 유효하지 않습니다."}}""")
    }

    @Test fun sampleJsonParses() {
        val json = """[{"name":"A","lat":37.5,"lon":127.0,"totalSpaces":10,"currentParked":3,"baseFee":500}]"""
        assertEquals(0.7, SampleParkingSource.parse(json).single().vacancyRate, 1e-9)
    }
}
