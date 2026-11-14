package com.way.app.learn

/** 가중치 합계는 항상 1.0으로 유지한다. */
data class UserPreference(
    val priceWeight: Float = 0.5f,
    val distanceWeight: Float = 0.5f
)

enum class PreferenceFeedback {
    CHEAPER,    // 사용자가 낮은 요금을 중요하게 생각함
    CLOSER,     // 사용자가 짧은 도보 거리를 중요하게 생각함
    BALANCED    // 가격과 거리의 균형을 원함
}

enum class StopCategory {
    PARKING,
    REST
}

enum class RecommendationAction {
    SHOWN,         // 추천이 화면에 노출됨
    SELECTED,      // 사용자가 장소를 선택함 (실제 방문/주차 성공을 뜻하지 않음)
    DISMISSED,     // 사용자가 추천을 거절함
    USER_CONFIRMED,// 사용자가 직접 방문/이용을 확인함
    FEEDBACK       // 사용자가 선호도를 명시적으로 선택함
}

data class RecommendationEvent(
    val placeId: String,
    val placeName: String,
    val category: StopCategory,
    val action: RecommendationAction,
    val timestampMillis: Long,
    val feedback: PreferenceFeedback? = null
)
