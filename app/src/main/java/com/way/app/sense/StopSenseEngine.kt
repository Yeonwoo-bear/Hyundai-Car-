package com.way.app.sense

/**
 * STOP SENSE에 필요한 입력값.
 * 현재 버전은 UI / 데모 데이터 기반으로 동작한다.
 * 추후 Pleos Vehicle, Navi SDK 값으로 각 필드를 채우면 된다.
 */
data class DrivingContext(
    val drivingMinutes: Int,
    val minutesToDestination: Int?,
    val isVehicleMoving: Boolean = true,
    val isParkingBrakeEngaged: Boolean = false,
    val restRecentlyCompleted: Boolean = false
)

enum class SenseResult {
    NORMAL,
    REST,
    PARKING
}

data class SenseDecision(
    val result: SenseResult,
    val title: String,
    val message: String,
    val isDemoDecision: Boolean = true
)

data class SenseRules(
    val restAfterMinutes: Int = 120,
    val parkingWithinMinutes: Int = 15
) {
    init {
        require(restAfterMinutes > 0) { "restAfterMinutes must be positive" }
        require(parkingWithinMinutes >= 0) { "parkingWithinMinutes must be non-negative" }
    }
}

/**
 * 순수 Kotlin 규칙 엔진. Android SDK나 화면 코드에 의존하지 않는다.
 * 120분/15분은 데모용 임의 기준이며 실제 졸음 또는 건강 상태를 진단하지 않는다.
 */
class StopSenseEngine(private val rules: SenseRules = SenseRules()) {

    /** 기존 화면에서 간편하게 사용할 수 있는 호환 API. */
    fun evaluate(context: DrivingContext): SenseResult = assess(context).result

    /** UI에 설명과 함께 전달할 상세 판단 결과. */
    fun assess(context: DrivingContext): SenseDecision {
        require(context.drivingMinutes >= 0) { "drivingMinutes must be non-negative" }
        require(context.minutesToDestination == null || context.minutesToDestination >= 0) {
            "minutesToDestination must be non-negative"
        }

        // 정차 중에는 운전자에게 신규 정차 알림을 띄우지 않는다.
        if (!context.isVehicleMoving || context.isParkingBrakeEngaged) {
            return SenseDecision(
                result = SenseResult.NORMAL,
                title = "정차 중",
                message = "차량이 정차 중이므로 새 정차 알림을 표시하지 않아요."
            )
        }

        // 장시간 주행은 주차 추천보다 먼저 안내한다.
        if (!context.restRecentlyCompleted && context.drivingMinutes >= rules.restAfterMinutes) {
            return SenseDecision(
                result = SenseResult.REST,
                title = "휴식 권장",
                message = "연속 주행 ${context.drivingMinutes}분이에요. 가까운 휴식 장소를 확인해 보세요."
            )
        }

        if (context.minutesToDestination != null &&
            context.minutesToDestination <= rules.parkingWithinMinutes
        ) {
            return SenseDecision(
                result = SenseResult.PARKING,
                title = "주차장 추천",
                message = "목적지까지 약 ${context.minutesToDestination}분 남았어요. 주변 주차장을 비교해 보세요."
            )
        }

        return SenseDecision(
            result = SenseResult.NORMAL,
            title = "정상 주행",
            message = "현재는 추가 정차 안내가 필요하지 않아요."
        )
    }
}
