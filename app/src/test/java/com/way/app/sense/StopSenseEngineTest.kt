package com.way.app.sense

import org.junit.Assert.assertEquals
import org.junit.Test

class StopSenseEngineTest {
    private val engine = StopSenseEngine()

    @Test fun normalDriving() {
        assertEquals(SenseResult.NORMAL, engine.evaluate(DrivingContext(40, 50)))
    }

    @Test fun restRecommendation() {
        assertEquals(SenseResult.REST, engine.evaluate(DrivingContext(130, 40)))
    }

    @Test fun parkingRecommendation() {
        assertEquals(SenseResult.PARKING, engine.evaluate(DrivingContext(50, 10)))
    }

    @Test fun longDrivingTakesPriority() {
        assertEquals(SenseResult.REST, engine.evaluate(DrivingContext(130, 10)))
    }

    @Test fun noPromptWhileParked() {
        assertEquals(
            SenseResult.NORMAL,
            engine.evaluate(DrivingContext(150, 10, isParkingBrakeEngaged = true))
        )
    }

    @Test fun parkingAfterRecentRest() {
        assertEquals(
            SenseResult.PARKING,
            engine.evaluate(DrivingContext(150, 10, restRecentlyCompleted = true))
        )
    }
}
