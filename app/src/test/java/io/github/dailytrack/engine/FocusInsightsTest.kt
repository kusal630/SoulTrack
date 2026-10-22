package io.github.dailytrack.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusInsightsTest {

    @Test
    fun best_hours_rank_focus_minutes_and_completed_cycles() {
        val samples = listOf(
            HourSample(9, focusedMinutes = 50, completedCycles = 2),
            HourSample(9, focusedMinutes = 25, completedCycles = 1),
            HourSample(10, focusedMinutes = 100, completedCycles = 0),
            HourSample(14, focusedMinutes = 10, completedCycles = 5),
            HourSample(3, focusedMinutes = 0)
        )
        val best = FocusInsights.bestHours(samples)
        assertEquals(listOf(9, 10, 14), best.map { it.hour })
        assertEquals(105, best.first().score)
    }

    @Test
    fun no_samples_means_no_invented_best_hour() {
        assertTrue(FocusInsights.bestHours(emptyList()).isEmpty())
        assertEquals(
            SmartPlanner.DEFAULT_BEST_HOURS,
            FocusInsights.bestHourNumbers(emptyList())
        )
    }

    @Test
    fun planned_vs_actual_is_sorted_by_the_biggest_miss() {
        val tasks = listOf(
            TaskTime(1, "On time", 1, 30, 32),
            TaskTime(2, "Late", 1, 30, 90),
            TaskTime(3, "No estimate", 2, 0, 40)
        )
        val comparisons = FocusInsights.plannedVsActual(tasks)
        assertEquals(listOf(2L, 1L), comparisons.map { it.taskId })
        assertEquals(60, comparisons.first().deltaMinutes)
        assertEquals(31, FocusInsights.averageErrorMinutes(comparisons))
    }

    @Test
    fun quadrant_split_ignores_empty_quadrants() {
        val tasks = listOf(
            TaskTime(1, "Q1", 1, 30, 60),
            TaskTime(2, "Q2", 2, 30, 30),
            TaskTime(3, "Q2 again", 2, 30, 10)
        )
        val split = FocusInsights.quadrantSplit(tasks)
        assertEquals(100, split.totalMinutes)
        assertEquals(0.6f, split.share(EisenhowerQuadrant.DO_FIRST), 0.001f)
        assertEquals(0.4f, split.share(EisenhowerQuadrant.SCHEDULE), 0.001f)
        assertEquals(EisenhowerQuadrant.DO_FIRST, split.dominant)
    }

    @Test
    fun early_quits_are_counted_from_planned_and_actual_minutes() {
        val cycles = listOf(
            CycleRecord(25, 25, completed = true),
            CycleRecord(25, 20, completed = true),
            CycleRecord(25, 8, completed = false),
            CycleRecord(25, 25, completed = true)
        )
        val pattern = FocusInsights.distractionPattern(cycles)
        assertEquals(4, pattern.totalCycles)
        assertEquals(1, pattern.earlyQuits)
        assertEquals(0.25f, pattern.earlyQuitRate, 0.001f)
        assertEquals(8, pattern.averageQuitAtMinutes)
        assertTrue(pattern.label.contains("25%"))
    }

    @Test
    fun empty_data_is_reported_as_unknown_not_as_failure() {
        val report = FocusInsights.report(emptyList(), emptyList(), emptyList())
        assertNull(report.bestHourLabel)
        assertNull(report.averageErrorMinutes)
        assertNotNull(report.distraction.label)
        assertEquals(0, report.quadrantSplit.totalMinutes)
    }

    @Test
    fun report_combines_all_four_answers() {
        val report = FocusInsights.report(
            hourSamples = listOf(HourSample(9, 120, 4)),
            tasks = listOf(TaskTime(1, "Deep work", 1, 60, 75)),
            cycles = listOf(CycleRecord(25, 25, true))
        )
        assertEquals("09:00", report.bestHourLabel)
        assertEquals(15, report.averageErrorMinutes)
        assertEquals(1, report.distraction.totalCycles)
        assertTrue(report.hasFocusData)
    }
}
