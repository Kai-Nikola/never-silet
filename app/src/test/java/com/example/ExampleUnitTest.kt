package com.example

import com.example.data.InterceptionLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testInterceptionLogCreation() {
        val now = System.currentTimeMillis()
        val log = InterceptionLog(
            id = "log-1",
            timestamp = now,
            triggerReason = "Silent mode detected",
            timeToRestoreSeconds = 2.8f,
            restoredVolumePercent = 100
        )

        assertEquals("log-1", log.id)
        assertEquals(now, log.timestamp)
        assertEquals("Silent mode detected", log.triggerReason)
        assertEquals(2.8f, log.timeToRestoreSeconds, 0.01f)
        assertEquals(100, log.restoredVolumePercent)
        assertTrue(log.timeToRestoreSeconds <= 3.0f)
    }

    @Test
    fun testRestorationTimeCalculation() {
        val startTime = 1000L

        // After 2850ms (0.15s remaining before 3.0s limit)
        val intermediateTime = 3850L
        val elapsedIntermediate = intermediateTime - startTime
        val remainingIntermediate = ((3000L - elapsedIntermediate) / 1000f).coerceAtLeast(0f)
        assertEquals(0.15f, remainingIntermediate, 0.001f)

        // After 3200ms (exceeds 3.0s, countdown clamped to 0)
        val overTime = 4200L
        val elapsedOver = overTime - startTime
        val remainingOver = ((3000L - elapsedOver) / 1000f).coerceAtLeast(0f)
        assertEquals(0.0f, remainingOver, 0.001f)

        // Halfway at 1200ms elapsed (1.8s remaining)
        val halfTime = 2200L
        val remainingHalf = ((3000L - (halfTime - startTime)) / 1000f)
        assertEquals(1.8f, remainingHalf, 0.01f)
    }
}
