package io.github.dailytrack.engine

import org.junit.Assert.*
import org.junit.Test

class FocusabilityEngineTest {

    private val engine = FocusabilityEngine()

    @Test
    fun `cold start with no signals still returns a usable score`() {
        val result = engine.evaluate(FocusContext(hourOfDay = 10))
        assertTrue(result.score in 0..100)
        assertTrue(result.reasons.isNotEmpty())
        assertTrue(result.reasons.all { it.isNotBlank() })
    }

    @Test
    fun `late morning scores higher than the middle of the night`() {
        val morning = engine.evaluate(FocusContext(hourOfDay = 10))
        val night = engine.evaluate(FocusContext(hourOfDay = 3))
        assertTrue(morning.score > night.score)
    }

    @Test
    fun `post lunch dip scores below the morning peak`() {
        val morning = engine.evaluate(FocusContext(hourOfDay = 10))
        val afterLunch = engine.evaluate(FocusContext(hourOfDay = 13))
        assertTrue(afterLunch.score < morning.score)
    }

    @Test
    fun `a quiet room beats a noisy one`() {
        val quiet = engine.evaluate(FocusContext(hourOfDay = 10, quietness = 95))
        val noisy = engine.evaluate(FocusContext(hourOfDay = 10, quietness = 10))
        assertTrue(quiet.score > noisy.score)
        assertTrue(quiet.reasons.any { it.contains("quiet", ignoreCase = true) })
        assertTrue(noisy.reasons.any { it.contains("noisy", ignoreCase = true) })
    }

    @Test
    fun `low energy drags the score down`() {
        val rested = engine.evaluate(FocusContext(hourOfDay = 10, energy = 5))
        val tired = engine.evaluate(FocusContext(hourOfDay = 10, energy = 1))
        assertTrue(rested.score > tired.score)
    }

    @Test
    fun `a long session without a break reduces focusability`() {
        val fresh = engine.evaluate(FocusContext(hourOfDay = 10, minutesSinceBreak = 5))
        val stale = engine.evaluate(FocusContext(hourOfDay = 10, minutesSinceBreak = 150))
        assertTrue(fresh.score > stale.score)
    }

    @Test
    fun `bands follow the score`() {
        assertEquals(FocusBand.LOW, engine.evaluate(FocusContext(hourOfDay = 3, quietness = 5, energy = 1)).band)
        assertEquals(FocusBand.HIGH, engine.evaluate(FocusContext(hourOfDay = 10, quietness = 98, energy = 5, minutesSinceBreak = 5)).band)
    }

    @Test
    fun `short sleep last night lowers focusability`() {
        val rested = engine.evaluate(FocusContext(hourOfDay = 10, sleepHoursLastNight = 7.5))
        val short = engine.evaluate(FocusContext(hourOfDay = 10, sleepHoursLastNight = 5.5))
        val veryShort = engine.evaluate(FocusContext(hourOfDay = 10, sleepHoursLastNight = 4.0))
        assertTrue(rested.score > short.score)
        assertTrue(short.score > veryShort.score)
        assertTrue(short.reasons.any { it.contains("sleep", ignoreCase = true) })
        assertTrue(rested.reasons.any { it.contains("rested", ignoreCase = true) })
    }

    @Test
    fun `score never leaves the 0 to 100 range`() {        val worst = engine.evaluate(
            FocusContext(hourOfDay = 3, quietness = 0, energy = 1, mood = 1, minutesSinceBreak = 300, minutesInCurrentSession = 400)
        )
        val best = engine.evaluate(
            FocusContext(hourOfDay = 10, quietness = 100, energy = 5, mood = 5, minutesSinceBreak = 3, minutesInCurrentSession = 20)
        )
        assertTrue(worst.score >= 0)
        assertTrue(best.score <= 100)
    }
}
