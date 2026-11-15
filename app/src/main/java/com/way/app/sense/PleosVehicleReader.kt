package com.way.app.sense

import android.content.Context
import android.os.Handler
import android.os.Looper
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.Odometer
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener

/**
 * Real Pleos Vehicle SDK v2.3.2 integration.
 * No fake vehicle values are used. Trip driving time is a *trip* measurement:
 * it must NOT be presented as medically measured fatigue or necessarily uninterrupted driving.
 *
 * The Pleos documentation does not specify the numeric time unit on the method page.
 * The result is preserved raw; the caller must explicitly confirm the unit before
 * converting it to minutes. Unknown/unsupported values do not trigger alerts.
 */
data class PleosTripReading(
    val status: ReadingStatus = ReadingStatus.NOT_CONNECTED,
    val rawValue: String? = null,
    val numericValue: Double? = null,
    val readAtMillis: Long? = null,
    val detail: String = "차량 SDK 연결 전",
    /** 실제로 값을 읽어 온 트립 시간 종류 (지원되는 첫 번째 항목). */
    val source: TripTimeSource? = null,
    /** 현재 기어. 읽지 못했으면 null. */
    val gear: GearState? = null
)

enum class ReadingStatus {
    NOT_CONNECTED, CONNECTING, READING, OK, UNAVAILABLE, ERROR
}

enum class TripTimeSource(val caption: String) {
    CURRENT("현재 트립 주행 시간"),
    CUMULATIVE("누적 트립 주행 시간"),
    SINCE_REFILL("주유/충전 후 주행 시간")
}

enum class GearState(val caption: String) {
    PARK("P (주차)"), REVERSE("R (후진)"), NEUTRAL("N (중립)"), DRIVE("D (주행)"), UNKNOWN("알 수 없음");

    /** P 일 때만 '정차'로 본다. 알 수 없으면 null (판단에 사용하지 않음). */
    val isParked: Boolean? get() = when (this) {
        PARK -> true
        UNKNOWN -> null
        else -> false
    }

    companion object {
        fun from(gear: VehicleGear?): GearState = when (gear) {
            VehicleGear.GEAR_PARK -> PARK
            VehicleGear.GEAR_REVERSE -> REVERSE
            VehicleGear.GEAR_NEUTRAL -> NEUTRAL
            null, VehicleGear.GEAR_UNKNOWN -> UNKNOWN
            else -> DRIVE // GEAR_DRIVE, GEAR_1..9
        }
    }
}

/** SDK / service may change its unit across supported vehicles. Never guess. */
enum class TripTimeUnit(val caption: String) {
    UNCONFIRMED("단위 미확인"),
    MILLISECONDS("밀리초(ms)"),
    SECONDS("초(s)"),
    MINUTES("분(min)");

    fun minutesOrNull(raw: Double?): Int? {
        if (this == UNCONFIRMED || raw == null || !raw.isFinite() || raw < 0.0) return null
        val value = when (this) {
            MILLISECONDS -> raw / 60_000.0
            SECONDS -> raw / 60.0
            MINUTES -> raw
            UNCONFIRMED -> return null
        }
        // Reject absurd/unrepresentable values rather than overflowing Int.
        if (!value.isFinite() || value > 100_000.0) return null
        return value.toInt()
    }
}

/**
 * Owns exactly one Vehicle session. Public methods must be called from the Android main thread.
 * Pleos IPC callbacks may arrive on background threads and are dispatched to the UI main thread.
 * Only one outstanding read is allowed. Closing waits for the callback before release().
 *
 * 차량/에뮬레이터마다 지원하는 트립 시간 속성이 다르므로
 * CURRENT → CUMULATIVE → SINCE_REFILL 순서로 지원 여부를 확인하고 처음 지원되는 값을 쓴다.
 * (Pleos 에뮬레이터는 TRIP_CURRENT_DRIVING_TIME 을 ERROR_NOT_SUPPORTED_PROPERTY 로 거부한다.)
 */
class PleosVehicleReader(
    context: Context,
    private val onReading: (PleosTripReading) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private val vehicle = Vehicle(context.applicationContext)
    private var initialized = false
    private var closed = false
    private var pending = false
    private var gearListenerRegistered = false
    private var reading = PleosTripReading()

    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            main.post { if (!closed) emit(reading.copy(gear = GearState.from(gear))) }
        }

        override fun onFailed(e: Exception) {
            main.post { if (!closed) emit(reading.copy(gear = GearState.UNKNOWN)) }
        }
    }

    private class TripApi(
        val source: TripTimeSource,
        val check: (Odometer, (CapabilityConfig) -> Unit, (Exception) -> Unit) -> Unit,
        val get: (Odometer, (Int?) -> Unit, (Exception) -> Unit) -> Unit
    )

    private val tripApis = listOf(
        TripApi(TripTimeSource.CURRENT,
            { o, ok, fail -> o.checkTripCurrentDrivingTimeCapabilities(ok, fail) },
            { o, ok, fail -> o.getTripCurrentDrivingTime(ok, fail) }),
        TripApi(TripTimeSource.CUMULATIVE,
            { o, ok, fail -> o.checkTripCumulativeDrivingTimeCapabilities(ok, fail) },
            { o, ok, fail -> o.getTripCumulativeDrivingTime(ok, fail) }),
        TripApi(TripTimeSource.SINCE_REFILL,
            { o, ok, fail -> o.checkTripSinceRefillDrivingTimeCapabilities(ok, fail) },
            { o, ok, fail -> o.getTripSinceRefillDrivingTime(ok, fail) })
    )

    fun connectAndRead() {
        if (closed || pending) return
        if (!initialized) {
            emit(PleosTripReading(status = ReadingStatus.CONNECTING, detail = "Vehicle.initialize() 진행"))
            try {
                vehicle.initialize()
                initialized = true
            } catch (e: Exception) {
                emitError("Vehicle 초기화 실패", e)
                return
            }
        }
        registerGearListener()
        readGear()
        readTripTime()
    }

    fun refresh() {
        if (!closed && !pending) connectAndRead()
    }

    private fun registerGearListener() {
        if (gearListenerRegistered) return
        try {
            vehicle.getGear().registerGearSelection(gearListener)
            gearListenerRegistered = true
        } catch (_: Exception) { /* gear is optional */ }
    }

    private fun readGear() {
        try {
            vehicle.getGear().getCurrentGearSelectionState(
                { gear -> main.post { if (!closed) emit(reading.copy(gear = GearState.from(gear))) } },
                { _ -> main.post { if (!closed) emit(reading.copy(gear = GearState.UNKNOWN)) } }
            )
        } catch (_: Exception) {
            emit(reading.copy(gear = GearState.UNKNOWN))
        }
    }

    private fun readTripTime() {
        pending = true
        emit(reading.copy(status = ReadingStatus.READING, detail = "트립 주행 시간 지원 확인 중"))
        val odometer = try {
            vehicle.getOdometer()
        } catch (e: Exception) {
            emitError("SDK API 호출 실패", e)
            finishRequest()
            return
        }
        tryTripApi(odometer, 0, mutableListOf())
    }

    /** index 번째 API를 시도하고, 지원되지 않으면 다음 API로 넘어간다. */
    private fun tryTripApi(odometer: Odometer, index: Int, failures: MutableList<String>) {
        if (closed) { finishRequest(); return }
        if (index >= tripApis.size) {
            emit(reading.copy(
                status = ReadingStatus.UNAVAILABLE,
                rawValue = null, numericValue = null, source = null,
                detail = "이 차량/에뮬레이터는 트립 주행 시간을 제공하지 않음 (${failures.joinToString()})"
            ))
            finishRequest()
            return
        }
        val api = tripApis[index]
        val next = { reason: String ->
            main.post {
                failures += "${api.source.name}: $reason"
                tryTripApi(odometer, index + 1, failures)
            }
        }
        try {
            api.check(odometer, { config ->
                if (config.accessType == AccessType.READ || config.accessType == AccessType.READ_WRITE) {
                    main.post { readValue(odometer, api, index, failures) }
                } else {
                    next("읽기 미지원")
                }
            }, { e -> next(e.message ?: e.javaClass.simpleName) })
        } catch (e: Exception) {
            next(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun readValue(odometer: Odometer, api: TripApi, index: Int, failures: MutableList<String>) {
        if (closed) { finishRequest(); return }
        try {
            api.get(odometer, { value ->
                main.post {
                    if (!closed) {
                        val numeric = value?.toDouble()
                        emit(reading.copy(
                            status = if (numeric != null) ReadingStatus.OK else ReadingStatus.UNAVAILABLE,
                            rawValue = value?.toString(),
                            numericValue = numeric,
                            readAtMillis = System.currentTimeMillis(),
                            source = api.source,
                            detail = if (numeric != null) "${api.source.caption} 조회됨 (단위 확인 필요)"
                                     else "값이 비어 있음"
                        ))
                    }
                    finishRequest()
                }
            }, { e ->
                main.post {
                    failures += "${api.source.name}: ${e.message ?: e.javaClass.simpleName}"
                    tryTripApi(odometer, index + 1, failures)
                }
            })
        } catch (e: Exception) {
            failures += "${api.source.name}: ${e.message ?: e.javaClass.simpleName}"
            tryTripApi(odometer, index + 1, failures)
        }
    }

    private fun emit(value: PleosTripReading) {
        reading = value
        if (!closed) onReading(value)
    }

    private fun emitError(label: String, e: Exception) {
        emit(reading.copy(
            status = ReadingStatus.ERROR,
            detail = "$label: ${e.javaClass.simpleName} / ${e.message ?: "상세 메시지 없음"}"
        ))
    }

    private fun finishRequest() {
        pending = false
        if (closed && initialized) releaseNow()
    }

    fun close() {
        if (closed) return
        closed = true
        if (!pending && initialized) releaseNow()
        // If an IPC request never responds, the session cannot safely be released early.
        // In that rare case the app process lifecycle will reclaim it.
    }

    private fun releaseNow() {
        if (gearListenerRegistered) {
            try { vehicle.getGear().unregisterGearSelection(gearListener) } catch (_: Exception) { }
            gearListenerRegistered = false
        }
        try { vehicle.release() } catch (_: Exception) { /* service may already be gone */ }
        initialized = false
    }
}
