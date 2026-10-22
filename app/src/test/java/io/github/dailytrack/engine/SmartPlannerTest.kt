package io.github.dailytrack.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class SmartPlannerTest {
    private val zone = ZoneId.of("UTC")
    private val now = LocalDateTime.of(2026, 1, 5, 9, 7).atZone(zone).toInstant().toEpochMilli()
    private val hour = 3_600_000L

    @Test
    fun classifies_the_four_quadrants() {
        assertEquals(EisenhowerQuadrant.DO_FIRST, SmartPlanner.classify(3, now + hour, now))
        assertEquals(EisenhowerQuadrant.SCHEDULE, SmartPlanner.classify(3, null, now))
        assertEquals(EisenhowerQuadrant.DELEGATE, SmartPlanner.classify(1, now + hour, now))
        assertEquals(EisenhowerQuadrant.DELETE, SmartPlanner.classify(1, null, now))
        // An unset priority is not evidence that the task is unimportant.
        assertEquals(EisenhowerQuadrant.SCHEDULE, SmartPlanner.classify(0, null, now))
    }

    @Test
    fun saved_quadrant_wins_over_the_estimate() {
        val task = PlannerTask(id = 1, title = "Chosen", priority = 3, savedQuadrant = 4)
        assertEquals(EisenhowerQuadrant.DELETE, SmartPlanner.classify(task, now))
    }

    @Test
    fun day_starts_with_q1_then_batches_q3_then_uses_best_hours_for_q2() {
        val tasks = listOf(
            PlannerTask(1, "Urgent important", priority = 3, deadline = now + hour, estimatedMinutes = 50),
            PlannerTask(2, "Important", priority = 3, estimatedMinutes = 60),
            PlannerTask(3, "Urgent low", priority = 1, deadline = now + 2 * hour, estimatedMinutes = 20),
            PlannerTask(4, "Delete me", priority = 1),
            PlannerTask(5, "Inbox item", priority = 3, isInbox = true)
        )

        val plan = SmartPlanner.buildDay(tasks, now, bestHours = listOf(9, 10, 14), zone = zone)

        assertEquals(3, plan.blocks.size)
        assertEquals(listOf(1L, 3L, 2L), plan.blocks.map { it.taskId })
        assertEquals(9 * 60 + 10, minutesOfDay(plan.blocks[0].startMillis))
        assertEquals(50, plan.blocks[0].minutes)
        assertEquals(2, plan.blocks[0].cycles)
        assertEquals(10 * 60, minutesOfDay(plan.blocks[1].startMillis))
        assertEquals(25, plan.blocks[1].minutes)
        // 14:00 is the next preferred hour still ahead of the cursor.
        assertEquals(14 * 60, minutesOfDay(plan.blocks[2].startMillis))
        assertTrue(plan.blocks[2].inBestHour)
        assertEquals(3, plan.blocks[2].cycles)

        assertEquals(listOf(4L), plan.deleteCandidates.map { it.id })
        assertEquals(listOf(5L), plan.unscheduled.map { it.id })
        assertEquals(135, plan.totalMinutes)
        assertEquals(6, plan.totalCycles)
    }

    @Test
    fun deep_work_blocks_are_capped() {
        val task = PlannerTask(1, "Huge", priority = 3, estimatedMinutes = 400)
        val plan = SmartPlanner.buildDay(listOf(task), now, bestHours = listOf(10), zone = zone)
        assertEquals(SmartPlanner.MAX_BLOCK_MINUTES, plan.blocks.single().minutes)
        assertEquals(5, plan.blocks.single().cycles)
    }

    @Test
    fun completed_tasks_are_ignored() {
        val task = PlannerTask(1, "Done", priority = 3, isCompleted = true)
        val plan = SmartPlanner.buildDay(listOf(task), now, zone = zone)
        assertTrue(plan.isEmpty)
    }

    @Test
    fun without_history_the_default_best_hours_are_used() {
        val plan = SmartPlanner.buildDay(emptyList(), now, bestHours = emptyList(), zone = zone)
        assertEquals(SmartPlanner.DEFAULT_BEST_HOURS, plan.bestHours)
        assertFalse(plan.bestHours.isEmpty())
    }

    private fun minutesOfDay(millis: Long): Int {
        val time = java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
        return time.hour * 60 + time.minute
    }
}
