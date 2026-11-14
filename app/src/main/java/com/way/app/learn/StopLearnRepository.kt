package com.way.app.learn

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Android 기본 SharedPreferences만 사용하는 영구 저장소.
 * 추가 Gradle 라이브러리가 필요하지 않다.
 * 서버 동기화 없이 이 앱 설치 공간에만 데이터가 보관된다.
 */
class StopLearnRepository(
    context: Context,
    private val engine: StopLearnEngine = StopLearnEngine()
) {
    private val storage: SharedPreferences = context.applicationContext.getSharedPreferences(
        "way_stop_learn_v1", Context.MODE_PRIVATE
    )

    fun getPreference(): UserPreference {
        val price = storage.getFloat(KEY_PRICE, 0.5f).coerceIn(0.1f, 0.9f)
        return UserPreference(priceWeight = price, distanceWeight = 1f - price)
    }

    /** UI의 명시적인 선호도 응답으로만 가중치를 변경한다. */
    @Synchronized
    fun submitFeedback(
        feedback: PreferenceFeedback,
        placeId: String = "demo-parking",
        placeName: String = "추천 주차장",
        timestampMillis: Long = System.currentTimeMillis()
    ): UserPreference {
        val next = engine.update(getPreference(), feedback)
        val events = readAllHistory().toMutableList()
        events.add(
            RecommendationEvent(
                placeId = placeId,
                placeName = placeName,
                category = StopCategory.PARKING,
                action = RecommendationAction.FEEDBACK,
                timestampMillis = timestampMillis,
                feedback = feedback
            )
        )
        storage.edit()
            .putFloat(KEY_PRICE, next.priceWeight)
            .putString(KEY_HISTORY, encode(events.takeLast(MAX_EVENTS)))
            .apply()
        return next
    }

    /** 추천 노출/선택/거절/사용자 직접 완료 확인을 기록한다. */
    @Synchronized
    fun recordEvent(event: RecommendationEvent) {
        require(event.placeId.isNotBlank()) { "placeId cannot be empty" }
        val events = readAllHistory().toMutableList()
        events.add(event)
        storage.edit().putString(KEY_HISTORY, encode(events.takeLast(MAX_EVENTS))).apply()
    }

    /** 최근 기록부터 반환한다. */
    fun getHistory(limit: Int = 20): List<RecommendationEvent> {
        require(limit >= 0) { "limit must be >= 0" }
        return readAllHistory().takeLast(limit).asReversed()
    }

    @Synchronized
    fun clearAll() {
        storage.edit().remove(KEY_PRICE).remove(KEY_HISTORY).apply()
    }

    private fun readAllHistory(): List<RecommendationEvent> {
        val encoded = storage.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(encoded)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val category = enumValueOrNull<StopCategory>(item.optString("category"))
                        ?: continue
                    val action = enumValueOrNull<RecommendationAction>(item.optString("action"))
                        ?: continue
                    val feedback = enumValueOrNull<PreferenceFeedback>(item.optString("feedback"))
                    val placeId = item.optString("placeId")
                    if (placeId.isBlank()) continue
                    add(
                        RecommendationEvent(
                            placeId = placeId,
                            placeName = item.optString("placeName"),
                            category = category,
                            action = action,
                            timestampMillis = item.optLong("timestampMillis", 0L),
                            feedback = feedback
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encode(events: List<RecommendationEvent>): String {
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject().apply {
                    put("placeId", event.placeId)
                    put("placeName", event.placeName)
                    put("category", event.category.name)
                    put("action", event.action.name)
                    put("timestampMillis", event.timestampMillis)
                    if (event.feedback != null) put("feedback", event.feedback.name)
                }
            )
        }
        return array.toString()
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
        enumValues<T>().firstOrNull { it.name == value }

    private companion object {
        const val KEY_PRICE = "price_weight"
        const val KEY_HISTORY = "recommendation_history"
        const val MAX_EVENTS = 100
    }
}
