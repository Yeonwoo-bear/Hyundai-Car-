package com.way.app.fit

/** 주차장 원본 데이터 (샘플 JSON / 서울 열린데이터 공통 형태). */
data class ParkingLot(
    val name: String,
    val lat: Double,
    val lon: Double,
    val totalSpaces: Int,
    val currentParked: Int,
    /** 기본요금(원). */
    val baseFee: Int
) {
    /** 여유율 0~1. 총면수가 0 이하이면 0. */
    val vacancyRate: Double
        get() = if (totalSpaces <= 0) 0.0
        else ((totalSpaces - currentParked).toDouble() / totalSpaces).coerceIn(0.0, 1.0)
}

/** 추천 가중치. 합이 1이 아니어도 계산 시 자동으로 정규화된다. */
data class Weights(
    val distance: Double = 0.4,
    val price: Double = 0.3,
    val vacancy: Double = 0.3
) {
    init {
        require(distance >= 0 && price >= 0 && vacancy >= 0) { "weights must be >= 0" }
    }

    fun normalized(): Weights {
        val sum = distance + price + vacancy
        return if (sum <= 0.0) Weights(1.0 / 3, 1.0 / 3, 1.0 / 3)
        else Weights(distance / sum, price / sum, vacancy / sum)
    }
}

data class Recommendation(
    val name: String,
    val lat: Double,
    val lon: Double,
    /** 0~1 종합 점수. */
    val score: Double,
    /** 운전자에게 보여줄 짧은 추천 이유. */
    val reason: String,
    val distanceMeters: Double = 0.0,
    val vacancyRate: Double = 0.0,
    val baseFee: Int = 0
)
