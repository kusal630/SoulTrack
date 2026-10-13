package io.github.dailytrack.engine

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class BrainLayerTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val now: Long =
        LocalDateTime.of(2026, 1, 5, 10, 0).atZone(zone).toInstant().toEpochMilli()

    private val brain = BrainLayer()
    private val coldStart = BehaviorProfile.coldStart()

    private fun band(value: Int) = when {
        value < 40 -> FocusBand.LOW
        value < 70 -> FocusBand.MEDIUM
        else -> FocusBand.HIGH
    }

    private fun focus(score: Int) = Focusability(score, band(score), listOf("test condition"))

    private fun inputs(
        tasks: List<TaskCandidate>,
        focusability: Focusability,
        behavior: BehaviorProfile = coldStart
    ) = BrainInputs(
        now = now,
        candidates = tasks,
        focusability = focusability,
        behavior = behavior,
        zone = zone
    )

    private fun naiveBaseline(tasks: List<TaskCandidate>): TaskCandidate =
        tasks.sortedWith(
            compareByDescending<TaskCandidate> { it.priority }
                .thenBy { it.deadline ?: Long.MAX_VALUE }
                .thenByDescending { it.estimatedMinutes }
        ).first()

    @Test
    fun `cold start picks the task that is due next even with a lower priority`() {
        val dueSoon = TaskCandidate(
            id = 1,
            title = "Reply to the client email",
            priority = 1,
            deadline = now + 30 * 60_000L,
            estimatedMinutes = 10,
            categoryType = "ACTIVITY"
        )
        val big = TaskCandidate(
            id = 2,
            title = "Write the quarterly report",
            priority = 3,
            deadline = null,
            estimatedMinutes = 120,
            categoryType = "PRODUCTIVE"
        )

        val result = brain.recommend(inputs(listOf(big, dueSoon), focus(85)))

        assertEquals(1L, result.primary?.task?.id)
        assertEquals(2L, naiveBaseline(listOf(big, dueSoon)).id)
    }

    @Test
    fun `cold start prefers light work when focus is low`() {
        val deep = TaskCandidate(1, "Deep work: architecture doc", priority = 3, estimatedMinutes = 120, categoryType = "LEARNING")
        val light = TaskCandidate(2, "Clear the inbox", priority = 2, estimatedMinutes = 10, categoryType = "ACTIVITY")

        val result = brain.recommend(inputs(listOf(deep, light), focus(25)))

        assertEquals(2L, result.primary?.task?.id)
        assertEquals(1L, naiveBaseline(listOf(deep, light)).id)
        assertEquals(FocusBand.LOW, result.focusability.band)
    }

    @Test
    fun `cold start prefers deep work when focus is high`() {
        val deep = TaskCandidate(1, "Deep work: architecture doc", priority = 3, estimatedMinutes = 120, categoryType = "LEARNING")
        val light = TaskCandidate(2, "Clear the inbox", priority = 2, estimatedMinutes = 10, categoryType = "ACTIVITY")

        val result = brain.recommend(inputs(listOf(deep, light), focus(85)))

        assertEquals(1L, result.primary?.task?.id)
        assertEquals(1L, naiveBaseline(listOf(deep, light)).id)
    }

    @Test
    fun `a high priority task due within a day is never buried`() {
        val urgent = TaskCandidate(1, "Send the invoice", priority = 3, deadline = now + 5 * 60_000L, estimatedMinutes = 15, categoryType = "ACTIVITY")
        val others = (2..6).map {
            TaskCandidate(it.toLong(), "Filler task $it", priority = 3, estimatedMinutes = 90, categoryType = "PRODUCTIVE")
        }

        val result = brain.recommend(inputs(others + urgent, focus(25)))

        assertEquals(1L, result.primary?.task?.id)
        assertTrue(result.primary!!.score >= 95.0)
        assertEquals(1L, naiveBaseline(others + urgent).id)
    }

    @Test
    fun `learns the hour the person actually works in`() {
        val behavior = BehaviorProfile.of(
            sessionMinutesByHourAndType = mapOf(
                10 to mapOf("LEARNING" to 5_400L, "PRODUCTIVE" to 600L),
                20 to mapOf("PRODUCTIVE" to 5_000L)
            ),
            completionsByHourAndType = mapOf(10 to mapOf("LEARNING" to 20, "PRODUCTIVE" to 2)),
            intentByHourAndType = mapOf(10 to mapOf("LEARNING" to 22, "PRODUCTIVE" to 10)),
            typeCompletions = mapOf("LEARNING" to 22, "PRODUCTIVE" to 12),
            typeOpen = mapOf("LEARNING" to 2, "PRODUCTIVE" to 8),
            sampleSize = 64
        )

        val learning = TaskCandidate(1, "Read the design patterns chapter", priority = 0, estimatedMinutes = 45, categoryType = "LEARNING")
        val productive = TaskCandidate(2, "Tidy the backlog", priority = 0, estimatedMinutes = 45, categoryType = "PRODUCTIVE")

        val result = brain.recommend(inputs(listOf(productive, learning), focus(55), behavior))

        assertEquals(1L, result.primary?.task?.id)
        assertTrue(result.primary!!.reason.isNotBlank())
    }

    @Test
    fun `empty candidate list produces no recommendation`() {
        val result = brain.recommend(inputs(emptyList(), focus(85)))
        assertNull(result.primary)
        assertTrue(result.alternatives.isEmpty())
        assertEquals(0, result.totalConsidered)
    }

    @Test
    fun `every recommendation carries a short human readable reason`() {
        val tasks = listOf(
            TaskCandidate(1, "A", priority = 3, deadline = now + 60_000L, estimatedMinutes = 30, categoryType = "PRODUCTIVE"),
            TaskCandidate(2, "B", priority = 1, estimatedMinutes = 10, categoryType = "ACTIVITY")
        )
        val result = brain.recommend(inputs(tasks, focus(55)))
        assertTrue(result.primary!!.reason.isNotBlank())
        assertTrue(result.primary!!.reason.length < 140)
        result.alternatives.forEach { assertTrue(it.reason.isNotBlank()) }
    }

    @Test
    fun `beats the naive sort-by-priority baseline on every fixture`() {
        val fixtures = listOf(
            Triple(
                listOf(
                    TaskCandidate(1, "Due soon", priority = 1, deadline = now + 20 * 60_000L, estimatedMinutes = 10, categoryType = "ACTIVITY"),
                    TaskCandidate(2, "Big ticket", priority = 3, estimatedMinutes = 120, categoryType = "PRODUCTIVE")
                ),
                focus(85),
                1L
            ),
            Triple(
                listOf(
                    TaskCandidate(1, "Deep work", priority = 3, estimatedMinutes = 120, categoryType = "LEARNING"),
                    TaskCandidate(2, "Inbox", priority = 2, estimatedMinutes = 10, categoryType = "ACTIVITY")
                ),
                focus(25),
                2L
            ),
            Triple(
                listOf(
                    TaskCandidate(1, "Deep work", priority = 3, estimatedMinutes = 120, categoryType = "LEARNING"),
                    TaskCandidate(2, "Inbox", priority = 2, estimatedMinutes = 10, categoryType = "ACTIVITY")
                ),
                focus(85),
                1L
            ),
            Triple(
                listOf(
                    TaskCandidate(1, "Overdue bill", priority = 1, deadline = now - 3_600_000L, estimatedMinutes = 5, categoryType = "ACTIVITY"),
                    TaskCandidate(2, "Nice to have", priority = 3, estimatedMinutes = 60, categoryType = "PRODUCTIVE")
                ),
                focus(55),
                1L
            )
        )

        var brainMatches = 0
        var naiveMatches = 0
        fixtures.forEach { (tasks, focusability, expectedId) ->
            val picked = brain.recommend(inputs(tasks, focusability)).primary?.task?.id
            assertEquals(expectedId, picked)
            if (picked == expectedId) brainMatches++
            if (naiveBaseline(tasks).id == expectedId) naiveMatches++
        }

        assertEquals(fixtures.size, brainMatches)
        assertTrue("naive baseline must not be perfect", naiveMatches < fixtures.size)
    }

    @Test
    fun `alternatives never repeat the primary pick`() {
        val tasks = listOf(
            TaskCandidate(1, "A", priority = 3, estimatedMinutes = 60, categoryType = "PRODUCTIVE"),
            TaskCandidate(2, "B", priority = 2, estimatedMinutes = 30, categoryType = "LEARNING"),
            TaskCandidate(3, "C", priority = 1, estimatedMinutes = 10, categoryType = "ACTIVITY")
        )
        val result = brain.recommend(inputs(tasks, focus(55)))
        assertEquals(3, result.totalConsidered)
        assertEquals(2, result.alternatives.size)
        assertTrue(result.alternatives.none { it.task.id == result.primary!!.task.id })
    }
}
