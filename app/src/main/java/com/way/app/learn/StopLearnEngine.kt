package com.way.app.learn

/**
 * 명시적인 사용자 피드백으로 가중치를 조정한다.
 * 머신러닝 모델을 학습하는 것이 아니라 규칙 기반으로 개인화하는 프로토타입이다.
 */
class StopLearnEngine(private val adjustmentStep: Float = 0.1f) {
    init {
        require(adjustmentStep > 0f && adjustmentStep <= 0.5f)
    }

    fun update(current: UserPreference, feedback: PreferenceFeedback): UserPreference {
        val currentPrice = current.priceWeight.coerceIn(0.1f, 0.9f)
        val nextPrice = when (feedback) {
            PreferenceFeedback.CHEAPER -> (currentPrice + adjustmentStep).coerceAtMost(0.9f)
            PreferenceFeedback.CLOSER -> (currentPrice - adjustmentStep).coerceAtLeast(0.1f)
            PreferenceFeedback.BALANCED -> 0.5f
        }
        return UserPreference(
            priceWeight = nextPrice,
            distanceWeight = 1f - nextPrice
        )
    }

    /**
     * STOP FIT 연동을 위한 선택적 점수 도우미.
     * 두 점수 모두 0.0~1.0 범위여야 하며, 클수록 좋은 값이다.
     * STOP FIT 담당자는 자체 후보 정규화 및 점수화 로직을 사용할 수 있다.
     */
    fun weightedScore(
        normalizedAffordability: Float,
        normalizedCloseness: Float,
        preference: UserPreference
    ): Float {
        require(normalizedAffordability in 0f..1f)
        require(normalizedCloseness in 0f..1f)
        return normalizedAffordability * preference.priceWeight +
            normalizedCloseness * preference.distanceWeight
    }
}
