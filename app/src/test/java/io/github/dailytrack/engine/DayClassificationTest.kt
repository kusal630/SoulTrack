package io.github.dailytrack.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DayClassificationTest {

    private fun snapshot(
        tracked: Long = 8 * 3600,
        distraction: Long = 0,
        productive: Long = 4 * 3600,
        planned: Long = 4 * 3600,
        assigned: Int = 5,
        completed: Int = 5
    ) = DaySnapshot(
        trackedSeconds = tracked,
        distractionSeconds = distraction,
        productiveSeconds = productive,
        plannedFocusSeconds = planned,
        assignedTasks = assigned,
        completedTasks = completed
    )

    // --- thresholds are editable constants ---------------------------------

    @Test
    fun `thresholds ship with documented values`() {
        assertEquals(0.25, DayClassificationThresholds.WASTING_TIME_DISTRACTION_RATIO, 1e-9)
        assertEquals(0.40, DayClassificationThresholds.NOT_PRODUCTIVE_PRODUCTIVE_RATIO, 1e-9)
    }

    // --- 1. wasting time ----------------------------------------------------

    @Test
    fun `distraction above 25 percent marks the day as wasting time`() {
        assertEquals(
            DayStatus.WASTING_TIME,
            classifyDay(snapshot(tracked = 4000, distraction = 1001))
        )
    }

    @Test
    fun `distraction exactly at 25 percent is not wasting time`() {
        assertEquals(
            DayStatus.DONE,
            classifyDay(snapshot(tracked = 4000, distraction = 1000))
        )
    }

    @Test
    fun `nothing tracked never counts as wasting time`() {
        val day = snapshot(tracked = 0, distraction = 0, productive = 0, planned = 0)
        assertEquals(DayStatus.DONE, classifyDay(day))
    }

    // --- 2. not productive --------------------------------------------------

    @Test
    fun `productive below 40 percent of planned focus marks not productive`() {
        assertEquals(
            DayStatus.NOT_PRODUCTIVE,
            classifyDay(
                snapshot(
                    tracked = 8 * 3600,
                    distraction = 0,
                    productive = 3599,
                    planned = 4 * 3600
                )
            )
        )
    }

    @Test
    fun `productive exactly at 40 percent passes the productivity gate`() {
        assertEquals(
            DayStatus.DONE,
            classifyDay(
                snapshot(
                    tracked = 8 * 3600,
                    distraction = 0,
                    productive = 4 * 3600 / 5 * 2,
                    planned = 4 * 3600
                )
            )
        )
    }

    @Test
    fun `wasting time wins over not productive because it is checked first`() {
        assertEquals(
            DayStatus.WASTING_TIME,
            classifyDay(
                snapshot(
                    tracked = 1000,
                    distraction = 500,
                    productive = 0,
                    planned = 8 * 3600
                )
            )
        )
    }

    @Test
    fun `no planned focus time skips the productivity gate`() {
        assertEquals(
            DayStatus.DONE,
            classifyDay(snapshot(productive = 0, planned = 0))
        )
    }

    // --- 3. tasks not met ---------------------------------------------------

    @Test
    fun `partial completion marks tasks not met`() {
        assertEquals(
            DayStatus.TASKS_NOT_MET,
            classifyDay(snapshot(assigned = 5, completed = 4))
        )
    }

    @Test
    fun `zero completion marks tasks not met`() {
        assertEquals(
            DayStatus.TASKS_NOT_MET,
            classifyDay(snapshot(assigned = 3, completed = 0))
        )
    }

    // --- 4. done ------------------------------------------------------------

    @Test
    fun `everything done is a done day`() {
        assertEquals(DayStatus.DONE, classifyDay(snapshot(assigned = 5, completed = 5)))
    }

    @Test
    fun `over-completion still clamps to done`() {
        assertEquals(DayStatus.DONE, classifyDay(snapshot(assigned = 3, completed = 4)))
    }

    // --- no tasks ------------------------------------------------------------

    @Test
    fun `a day without assigned tasks is not judged`() {
        assertEquals(
            DayStatus.NO_TASKS,
            classifyDay(snapshot(assigned = 0, completed = 0))
        )
    }

    @Test
    fun `negative assignment counts are treated as no tasks`() {
        assertEquals(
            DayStatus.NO_TASKS,
            classifyDay(snapshot(assigned = -1, completed = 0))
        )
    }

    // --- progress maths ------------------------------------------------------

    @Test
    fun `day progress is completed over assigned`() {
        assertEquals(0.6f, dayProgress(5, 3), 1e-6f)
        assertEquals(0f, dayProgress(5, 0), 1e-6f)
        assertEquals(1f, dayProgress(5, 5), 1e-6f)
    }

    @Test
    fun `day progress with no tasks is zero not nan`() {
        assertEquals(0f, dayProgress(0, 0), 1e-6f)
        assertTrue(!dayProgress(0, 0).isNaN())
    }

    @Test
    fun `day progress clamps out of range input`() {
        assertEquals(1f, dayProgress(4, 9), 1e-6f)
        assertEquals(0f, dayProgress(4, -2), 1e-6f)
    }

    @Test
    fun `goal progress is current over target and clamps`() {
        assertEquals(0.5f, goalProgress(5f, 10f), 1e-6f)
        assertEquals(1f, goalProgress(12f, 10f), 1e-6f)
        assertEquals(0f, goalProgress(0f, 10f), 1e-6f)
    }

    @Test
    fun `goal progress with no target is zero not nan`() {
        assertEquals(0f, goalProgress(5f, 0f), 1e-6f)
        assertTrue(!goalProgress(5f, 0f).isNaN())
    }

    @Test
    fun `goal gradient message highlights a small number of remaining tasks`() {
        assertTrue(goalGradientMessage(2, 3)!!.contains("1 left"))
        assertTrue(goalGradientMessage(4, 6)!!.contains("Only 2"))
        assertTrue(goalGradientMessage(3, 3)!!.contains("All done"))
    }

    @Test
    fun `goal gradient message is null when there is nothing useful to say`() {
        assertNull(goalGradientMessage(0, 0))
        assertNull(goalGradientMessage(1, 8))
    }

    @Test
    fun `weighted progress favours long estimates`() {
        val tasks = listOf(
            TaskWeight(id = 1, estimatedMinutes = 90),
            TaskWeight(id = 2, estimatedMinutes = 10)
        )
        assertEquals(0.9f, weightedDayProgress(tasks, setOf(1)), 1e-6f)
        assertEquals(0.1f, weightedDayProgress(tasks, setOf(2)), 1e-6f)
        assertEquals(1f, weightedDayProgress(tasks, setOf(1, 2)), 1e-6f)
        assertEquals(0f, weightedDayProgress(tasks, emptySet()), 1e-6f)
    }

    @Test
    fun `weighted progress falls back to the plain ratio without estimates`() {
        val tasks = listOf(
            TaskWeight(id = 1, estimatedMinutes = 0),
            TaskWeight(id = 2, estimatedMinutes = 0),
            TaskWeight(id = 3, estimatedMinutes = 0),
            TaskWeight(id = 4, estimatedMinutes = 0)
        )
        assertEquals(0.5f, weightedDayProgress(tasks, setOf(1, 2)), 1e-6f)
    }

    @Test
    fun `snapshot completion and ratios are derived correctly`() {
        val day = DaySnapshot(
            trackedSeconds = 1000,
            distractionSeconds = 100,
            productiveSeconds = 300,
            plannedFocusSeconds = 600,
            assignedTasks = 4,
            completedTasks = 1
        )
        assertEquals(0.1f, day.distractionRatio, 1e-6f)
        assertEquals(0.5f, day.productiveRatio, 1e-6f)
        assertEquals(0.25f, day.completion, 1e-6f)
    }

    // --- labels --------------------------------------------------------------

    @Test
    fun `today label reads naturally`() {
        assertEquals("Today: 3 of 5 tasks done.", todayProgressLabel(3, 5))
        assertEquals("Today: no tasks assigned yet.", todayProgressLabel(0, 0))
    }

    @Test
    fun `week label spells out every day`() {
        val label = weekStatusLabel(
            listOf(
                DayStatus.DONE,
                DayStatus.TASKS_NOT_MET,
                DayStatus.NOT_PRODUCTIVE,
                DayStatus.WASTING_TIME,
                DayStatus.NO_TASKS,
                DayStatus.IN_PROGRESS,
                null
            ),
            listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        )
        assertEquals(
            "Week: Monday all tasks done, Tuesday tasks not met, Wednesday not productive, " +
                "Thursday wasting time, Friday no tasks assigned, Saturday in progress, Sunday not recorded.",
            label
        )
    }

    // --- week ring recording -------------------------------------------------

    @Test
    fun `only finished days are recorded on the week ring`() {
        assertTrue(DayStatus.DONE.isRecorded)
        assertTrue(DayStatus.TASKS_NOT_MET.isRecorded)
        assertTrue(DayStatus.NOT_PRODUCTIVE.isRecorded)
        assertTrue(DayStatus.WASTING_TIME.isRecorded)
        assertTrue(DayStatus.NO_TASKS.isRecorded)
        assertTrue(!DayStatus.IN_PROGRESS.isRecorded)
    }
}
