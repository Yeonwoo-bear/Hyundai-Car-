package com.way.app.fit

import kotlin.math.roundToInt

/** 주어진 주차장 목록에서 점수순 추천을 만든다. */
class ParkingRecommender(private val lots: List<ParkingLot>) {

    /**
     * @param radiusMeters 이 반경 안의 후보만 정규화/평가한다. 반경 안에 없으면 전체 후보 사용.
     */
    fun recommend(
        lat: Double,
        lon: Double,
        weights: Weights,
        limit: Int = 3,
        radiusMeters: Double = 5_000.0
    ): List<Recommendation> {
        val available = lots.filter { it.vacancyRate > 0.0 }
        val nearby = available.filter {
            ParkingScorer.haversineMeters(lat, lon, it.lat, it.lon) <= radiusMeters
        }
        return rank(nearby.ifEmpty { available }, lat, lon, weights, limit)
    }

    private fun rank(candidates: List<ParkingLot>, lat: Double, lon: Double, weights: Weights, limit: Int) =
        ParkingScorer.score(candidates, lat, lon, weights)
            .sortedByDescending { it.score }
            .take(limit)
            .map { s ->
                Recommendation(
                    name = s.lot.name,
                    lat = s.lot.lat,
                    lon = s.lot.lon,
                    score = s.score,
                    reason = reasonFor(s, weights.normalized()),
                    distanceMeters = s.distanceMeters,
                    vacancyRate = s.lot.vacancyRate,
                    baseFee = s.lot.baseFee
                )
            }

    companion object {
        /** 점수에 가장 크게 기여한 항목을 한 줄로 설명한다. */
        fun reasonFor(s: ParkingScorer.Scored, w: Weights): String {
            val contributions = listOf(
                "distance" to w.distance * s.distanceScore,
                "price" to w.price * s.priceScore,
                "vacancy" to w.vacancy * s.vacancyScore
            )
            return when (contributions.maxBy { it.second }.first) {
                "distance" -> "가까워요 · ${formatDistance(s.distanceMeters)}"
                "price" -> "저렴해요 · 기본 ${s.lot.baseFee}원"
                else -> "자리 여유 · ${(s.lot.vacancyRate * 100).roundToInt()}% 비어 있음"
            }
        }

        fun formatDistance(meters: Double): String =
            if (meters < 1000) "${meters.roundToInt()}m"
            else "%.1fkm".format(meters / 1000)
    }
}
