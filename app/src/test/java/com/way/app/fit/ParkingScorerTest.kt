package com.way.app.fit

import com.way.app.learn.UserPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkingScorerTest {
    private val here = 37.5665 to 126.9780 // 서울시청

    private val near = ParkingLot("가까움", 37.5670, 126.9785, 100, 90, 1500)   // 여유 10%, 비쌈
    private val cheap = ParkingLot("저렴", 37.5900, 127.0100, 100, 90, 300)     // 멂, 쌈
    private val empty = ParkingLot("여유", 37.5800, 126.9900, 100, 5, 1000)     // 중간, 여유 95%
    private val full = ParkingLot("만차", 37.5666, 126.9781, 100, 100, 100)

    private val lots = listOf(near, cheap, empty)

    @Test fun scoresAreBetweenZeroAndOne() {
        ParkingScorer.score(lots, here.first, here.second, Weights()).forEach {
            assertTrue(it.score in 0.0..1.0)
            assertTrue(it.distanceScore in 0.0..1.0)
            assertTrue(it.priceScore in 0.0..1.0)
        }
    }

    @Test fun distanceWeightPicksNearest() {
        val top = ParkingRecommender(lots).recommend(here.first, here.second, Weights(1.0, 0.0, 0.0)).first()
        assertEquals("가까움", top.name)
        assertTrue(top.reason.startsWith("가까워요"))
    }

    @Test fun priceWeightPicksCheapest() {
        val top = ParkingRecommender(lots).recommend(here.first, here.second, Weights(0.0, 1.0, 0.0)).first()
        assertEquals("저렴", top.name)
    }

    @Test fun vacancyWeightPicksEmptiest() {
        val top = ParkingRecommender(lots).recommend(here.first, here.second, Weights(0.0, 0.0, 1.0)).first()
        assertEquals("여유", top.name)
    }

    @Test fun fullLotIsExcluded() {
        val recs = ParkingRecommender(lots + full).recommend(here.first, here.second, Weights(), limit = 10)
        assertTrue(recs.none { it.name == "만차" })
    }

    @Test fun weightsAreNormalized() {
        val w = Weights(2.0, 1.0, 1.0).normalized()
        assertEquals(0.5, w.distance, 1e-9)
        assertEquals(1.0, w.distance + w.price + w.vacancy, 1e-9)
    }

    @Test fun singleCandidateGetsFullRelativeScores() {
        val s = ParkingScorer.score(listOf(near), here.first, here.second, Weights()).single()
        assertEquals(1.0, s.distanceScore, 1e-9)
        assertEquals(1.0, s.priceScore, 1e-9)
    }

    @Test fun userPreferenceMapsToWeights() {
        val w = UserPreference(priceWeight = 0.7f, distanceWeight = 0.3f).toWeights(vacancy = 0.3)
        assertEquals(0.49, w.price, 1e-6)
        assertEquals(0.21, w.distance, 1e-6)
    }

    @Test fun haversineSanity() {
        // 서울시청 ↔ 강남역 약 8.8km
        val d = ParkingScorer.haversineMeters(37.5665, 126.9780, 37.4979, 127.0276)
        assertTrue(d in 8_000.0..9_500.0)
    }
}
