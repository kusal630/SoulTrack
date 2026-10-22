package io.github.dailytrack.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CompoundingTest {
    private val today = LocalDate.of(2026, 3, 10)

    @Test
    fun one_percent_a_day_compounds() {
        val value = Compounding.compoundValue(100.0, 1.0, 365)
        assertEquals(3778.34, value, 0.5)
        assertEquals(100.0, Compounding.compoundValue(100.0, 1.0, 0), 0.001)
    }

    @Test
    fun curve_ends_at_the_requested_horizon() {
        val curve = Compounding.curve(100.0, 1.0, days = 365, points = 30)
        assertEquals(0, curve.first().dayIndex)
        assertEquals(365, curve.last().dayIndex)
        assertTrue(curve.size in 2..32)
        assertTrue(curve.last().value > curve.first().value)
    }

    @Test
    fun logged_days_add_up_and_keep_the_streak() {
        val logs = listOf(
            today.minusDays(2) to 1.0,
            today.minusDays(1) to 1.0,
            today to 2.0
        )
        val progress = Compounding.progress(10.0, 1.0, 1.0, logs, today)
        assertEquals(14.0, progress.currentValue, 0.001)
        assertEquals(3, progress.streak.current)
        assertEquals(3, progress.streak.best)
        assertEquals(0, progress.streak.missedDays)
        assertEquals(3, progress.daysLogged)
        assertTrue(progress.isOnTrack)
    }

    @Test
    fun a_slip_does_not_zero_the_value_or_the_best_streak() {
        val logs = listOf(
            today.minusDays(6) to 1.0,
            today.minusDays(5) to 1.0,
            today.minusDays(4) to 1.0
        )
        val progress = Compounding.progress(10.0, 1.0, 1.0, logs, today)
        assertEquals(13.0, progress.currentValue, 0.001)
        assertEquals(0, progress.streak.current)
        assertEquals(3, progress.streak.best)
        assertEquals(3, progress.streak.missedDays)
        assertFalse(progress.isOnTrack)
        assertTrue(progress.message.contains("Back on track tomorrow"))
    }

    @Test
    fun logging_yesterday_keeps_the_streak_alive_until_today_ends() {
        val logs = listOf(
            today.minusDays(2) to 1.0,
            today.minusDays(1) to 1.0
        )
        val progress = Compounding.progress(10.0, 1.0, 1.0, logs, today)
        assertEquals(2, progress.streak.current)
        assertEquals(0, progress.streak.missedDays)
    }

    @Test
    fun duplicate_days_count_once() {
        val logs = listOf(
            today to 1.0,
            today to 2.0,
            today.minusDays(1) to 1.0
        )
        val progress = Compounding.progress(0.0, 1.0, 1.0, logs, today)
        assertEquals(4.0, progress.currentValue, 0.001)
        assertEquals(2, progress.daysLogged)
    }
}
