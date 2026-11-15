package com.way.app.sense.ui

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.way.app.sense.DrivingContext
import com.way.app.sense.PleosTripReading
import com.way.app.sense.PleosVehicleReader
import com.way.app.sense.ReadingStatus
import com.way.app.sense.SenseDecision
import com.way.app.sense.StopSenseEngine
import com.way.app.sense.TripTimeUnit

/**
 * 홈 / STOP SENSE 화면이 함께 쓰는 UI 상태.
 * 차량 값 읽기와 판단 로직은 기존 PleosVehicleReader / StopSenseEngine 을 그대로 사용하고,
 * 여기서는 화면에 무엇을 보여줄지만 정한다.
 */
class SenseController(context: Context) {
    var reading by mutableStateOf(PleosTripReading())
        private set

    /** 차량 값을 분으로 바꿀 단위. 사용자가 확인하기 전에는 UNCONFIRMED. */
    var unit by mutableStateOf(TripTimeUnit.UNCONFIRMED)

    // ---- 데모 설정 (차량 값이 없을 때만 사용) ----
    var demoMinutesText by mutableStateOf("")
    var etaText by mutableStateOf("")
    var treatParkAsDriving by mutableStateOf(false)

    private var lastLogged: String? = null

    val reader = PleosVehicleReader(context) { r ->
        reading = r
        // 원문 SDK 에러(ERROR_NOT_SUPPORTED_PROPERTY 등)는 화면 대신 logcat 에만 남긴다.
        if ((r.status == ReadingStatus.ERROR || r.status == ReadingStatus.UNAVAILABLE) && r.detail != lastLogged) {
            Log.w(TAG, "vehicle ${r.status}: ${r.detail}")
            lastLogged = r.detail
        }
    }

    /** 차량이 실제로 준 주행 시간(분). 단위 미확인이면 null. */
    val vehicleMinutes: Int?
        get() = if (reading.status == ReadingStatus.OK) unit.minutesOrNull(reading.numericValue) else null

    val vehicleProvidesTripTime: Boolean get() = reading.status == ReadingStatus.OK

    val demoMinutes: Int? get() = demoMinutesText.toIntOrNull()?.takeIf { it >= 0 }

    /** 판단에 쓰는 주행 시간과 그것이 데모 값인지 여부. */
    val minutes: Int? get() = vehicleMinutes ?: demoMinutes
    val minutesAreDemo: Boolean get() = vehicleMinutes == null && demoMinutes != null

    /** 목적지까지 남은 시간: 내비 연동 전이므로 항상 데모 입력. */
    val etaMinutes: Int? get() = etaText.toIntOrNull()?.takeIf { it >= 0 }

    val parkedByGear: Boolean get() = reading.gear?.isParked == true
    val parkOverridden: Boolean get() = parkedByGear && treatParkAsDriving

    /** 데모 값이 하나라도 판단에 들어갔는지. */
    val decisionUsesDemo: Boolean
        get() = minutesAreDemo || etaMinutes != null || parkOverridden

    val decision: SenseDecision?
        get() {
            val m = minutes ?: return null
            val moving = !(parkedByGear && !treatParkAsDriving)
            return StopSenseEngine().assess(
                DrivingContext(drivingMinutes = m, minutesToDestination = etaMinutes, isVehicleMoving = moving)
            ).copy(isDemoDecision = decisionUsesDemo)
        }

    /** 화면에 보여줄 차량 연결 상태 (원문 에러 대신 쉬운 문장). */
    val friendlyStatus: String
        get() = when (reading.status) {
            ReadingStatus.NOT_CONNECTED -> "차량 연결 전"
            ReadingStatus.CONNECTING, ReadingStatus.READING -> "차량 데이터 확인 중…"
            ReadingStatus.OK -> if (vehicleMinutes == null) "차량 주행 시간 수신 · 데모 설정에서 단위를 확인해 주세요"
                                else "차량 데이터 연결됨"
            ReadingStatus.UNAVAILABLE -> "이 차량은 주행 시간 정보를 제공하지 않아요"
            ReadingStatus.ERROR -> "차량 데이터를 불러오지 못했어요"
        }

    companion object {
        const val TAG = "WAY-Sense"
    }
}

@Composable
fun rememberSenseController(): SenseController {
    val context = LocalContext.current.applicationContext
    val controller = remember(context) { SenseController(context) }
    DisposableEffect(controller) {
        controller.reader.connectAndRead()
        onDispose { controller.reader.close() }
    }
    return controller
}

fun formatMinutes(min: Int): String = when {
    min < 60 -> "${min}분"
    min % 60 == 0 -> "${min / 60}시간"
    else -> "${min / 60}시간 ${min % 60}분"
}
