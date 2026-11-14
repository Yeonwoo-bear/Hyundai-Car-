package com.way.app.sense

import android.content.Context
import android.os.Handler
import android.os.Looper
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType

/**
 * Real Pleos Vehicle SDK v2.3.2 integration.
 * No fake vehicle values are used. `getTripCurrentDrivingTime` is a *trip* measurement:
 * it must NOT be presented as medically measured fatigue or necessarily uninterrupted driving.
 *
 * The Pleos documentation publicly names the API but does not specify its numeric time unit
 * on the method page. The result is preserved raw; the caller must explicitly confirm the
 * unit before converting it to minutes. Unknown/unsupported values do not trigger alerts.
 */
data class PleosTripReading(
    val status: ReadingStatus = ReadingStatus.NOT_CONNECTED,
    val rawValue: String? = null,
    val numericValue: Double? = null,
    val readAtMillis: Long? = null,
    val detail: String = "차량 SDK 연결 전"
)

enum class ReadingStatus {
    NOT_CONNECTED, CONNECTING, READING, OK, UNAVAILABLE, ERROR
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
    private var reading = PleosTripReading()

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
        readTripTime()
    }

    fun refresh() {
        if (!closed && !pending) connectAndRead()
    }

    private fun readTripTime() {
        pending = true
        emit(PleosTripReading(status = ReadingStatus.READING, detail = "트립 주행 시간 지원 확인 중"))
        try {
            val odometer = vehicle.getOdometer()
            odometer.checkTripCurrentDrivingTimeCapabilities(
                onComplete = { config ->
                    main.post {
                        if (closed) {
                            finishRequest()
                        } else if (config.accessType == AccessType.READ ||
                                   config.accessType == AccessType.READ_WRITE) {
                            try {
                                odometer.getTripCurrentDrivingTime(
                                    onComplete = { value ->
                                        main.post {
                                            if (!closed) {
                                                // The SDK may expose an Int, Long, Float, Double,
                                                // or a non-numeric model. Preserve unknown raw values.
                                                val numeric = (value as? Number)?.toDouble()
                                                val detail = if (numeric != null) {
                                                    "차량 Vehicle Service에서 조회됨 (단위 확인 필요)"
                                                } else {
                                                    "값은 수신했으나 숫자로 해석할 수 없음"
                                                }
                                                emit(PleosTripReading(
                                                    status = if (numeric != null) ReadingStatus.OK else ReadingStatus.UNAVAILABLE,
                                                    rawValue = value?.toString(),
                                                    numericValue = numeric,
                                                    readAtMillis = System.currentTimeMillis(),
                                                    detail = detail
                                                ))
                                            }
                                            finishRequest()
                                        }
                                    },
                                    onFailed = { e ->
                                        main.post {
                                            if (!closed) emitError("주행 시간 조회 실패", e)
                                            finishRequest()
                                        }
                                    }
                                )
                            } catch (e: Exception) {
                                emitError("조회 요청 실패", e)
                                finishRequest()
                            }
                        } else {
                            emit(PleosTripReading(
                                status = ReadingStatus.UNAVAILABLE,
                                detail = "현재 에뮬레이터/차량이 트립 주행 시간 읽기를 지원하지 않음"
                            ))
                            finishRequest()
                        }
                    }
                },
                onFailed = { e ->
                    main.post {
                        if (!closed) emitError("지원 여부 확인 실패", e)
                        finishRequest()
                    }
                }
            )
        } catch (e: Exception) {
            emitError("SDK API 호출 실패", e)
            finishRequest()
        }
    }

    private fun emit(value: PleosTripReading) {
        reading = value
        if (!closed) onReading(value)
    }

    private fun emitError(label: String, e: Exception) {
        emit(PleosTripReading(
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
        try { vehicle.release() } catch (_: Exception) { /* service may already be gone */ }
        initialized = false
    }
}
