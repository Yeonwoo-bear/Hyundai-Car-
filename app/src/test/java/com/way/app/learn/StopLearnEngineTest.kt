package com.way.app.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StopLearnEngineTest {
    private val engine = StopLearnEngine()

    @Test fun cheaperFeedbackIncreasesPriceWeight() {
        val updated = engine.update(UserPreference(), PreferenceFeedback.CHEAPER)
        assertEquals(0.6f, updated.priceWeight, 0.0001f)
        assertEquals(0.4f, updated.distanceWeight, 0.0001f)
    }

    @Test fun closerFeedbackIncreasesDistanceWeight() {
        val updated = engine.update(UserPreference(), PreferenceFeedback.CLOSER)
        assertEquals(0.4f, updated.priceWeight, 0.0001f)
        assertEquals(0.6f, updated.distanceWeight, 0.0001f)
    }

    @Test fun clampedAtLimits() {
        var pref = UserPreference()
        repeat(20) { pref = engine.update(pref, PreferenceFeedback.CHEAPER) }
        assertEquals(0.9f, pref.priceWeight, 0.0001f)
        repeat(20) { pref = engine.update(pref, PreferenceFeedback.CLOSER) }
        assertEquals(0.1f, pref.priceWeight, 0.0001f)
    }

    @Test fun balancedFeedbackRestoresEqualWeights() {
        val updated = engine.update(UserPreference(0.8f, 0.2f), PreferenceFeedback.BALANCED)
        assertEquals(0.5f, updated.priceWeight, 0.0001f)
        assertEquals(0.5f, updated.distanceWeight, 0.0001f)
    }

    @Test fun weightsAlwaysSumToOne() {
        var pref = UserPreference()
        repeat(10) { pref = engine.update(pref, PreferenceFeedback.CHEAPER) }
        assertEquals(1f, pref.priceWeight + pref.distanceWeight, 0.0001f)
        assertTrue(engine.weightedScore(1f, 0f, pref) > engine.weightedScore(0f, 1f, pref))
    }
}
