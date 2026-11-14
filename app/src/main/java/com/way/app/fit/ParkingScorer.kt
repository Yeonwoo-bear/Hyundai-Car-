package com.way.app.fit

import com.way.app.learn.UserPreference
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * STOP FIT 점수 계산. Android 의존성이 없는 순수 함수만 둔다.
 *
 * 각 항목은 후보 집합 안에서 min-max 정규화(0~1, 1이 좋음)한다.
 *  - 거리: 가까울수록 1
 *  - 요금: 저렴할수록 1
 *  - 여유율: (총면 - 현재대수) / 총면, 이미 0~1
 * 최종 점수 = Σ (정규화 가중치 × 항목 점수)
 */
object ParkingScorer {

    data class Scored(
        val lot: ParkingLot,
        val distanceMeters: Double,
        val distanceScore: Double,
        val priceScore: Double,
        val vacancyScore: Double,
        val score: Double
    )

    fun score(lots: List<ParkingLot>, lat: Double, lon: Double, weights: Weights): List<Scored> {
        if (lots.isEmpty()) return emptyList()
        val w = weights.normalized()
        val distances = lots.map { haversineMeters(lat, lon, it.lat, it.lon) }
        val dMin = distances.min()
        val dMax = distances.max()
        val pMin = lots.minOf { it.baseFee }.toDouble()
        val pMax = lots.maxOf { it.baseFee }.toDouble()

        return lots.mapIndexed { i, lot ->
            val d = invertedMinMax(distances[i], dMin, dMax)
            val p = invertedMinMax(lot.baseFee.toDouble(), pMin, pMax)
            val v = lot.vacancyRate
            Scored(
                lot = lot,
                distanceMeters = distances[i],
                distanceScore = d,
                priceScore = p,
                vacancyScore = v,
                score = w.distance * d + w.price * p + w.vacancy * v
            )
        }
    }

    /** 작을수록 좋은 값을 0~1로. 모든 값이 같으면 1. */
    fun invertedMinMax(value: Double, min: Double, max: Double): Double =
        if (max - min < 1e-9) 1.0 else ((max - value) / (max - min)).coerceIn(0.0, 1.0)

    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }
}

/**
 * STOP LEARN 선호도(가격/거리)를 STOP FIT 가중치로 변환.
 * 여유율 비중은 고정하고 나머지를 학습된 비율로 나눈다.
 */
fun UserPreference.toWeights(vacancy: Double = 0.3): Weights {
    val rest = (1.0 - vacancy).coerceAtLeast(0.0)
    return Weights(
        distance = rest * distanceWeight,
        price = rest * priceWeight,
        vacancy = vacancy
    )
}
