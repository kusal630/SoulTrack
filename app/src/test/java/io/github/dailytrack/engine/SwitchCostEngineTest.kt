package io.github.dailytrack.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwitchCostEngineTest {
    private val engine = SwitchCostEngine()
    private val minute = 60_000L
    private val dayStart = 1_700_000_000_000L

    private fun block(
        todoId: Long?,
        contextKey: String,
        startMinutes: Long,
        endMinutes: Long,
        type: String = "PRODUCTIVE"
    ) = SwitchBlock(
        todoId = todoId,
        contextKey = contextKey,
        startMillis = dayStart + startMinutes * minute,
        endMillis = dayStart + endMinutes * minute,
        type = type
    )

    @Test
    fun `a single block is never a switch`() {
        val events = engine.detectSwitches(listOf(block(1L, "todo:1", 0, 60)))
        assertTrue(events.isEmpty())
    }

    @Test
    fun `staying on the same task is not a switch`() {
        val events = engine.detectSwitches(
            listOf(
                block(1L, "todo:1", 0, 30),
                block(1L, "todo:1", 32, 60)
            )
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `starting another task right after the current one counts as a switch`() {
        val events = engine.detectSwitches(
            listOf(
                block(1L, "todo:1", 0, 30),
                block(2L, "todo:2", 32, 60)
            )
        )
        assertEquals(1, events.size)
        assertEquals("todo:1", events[0].fromKey)
        assertEquals("todo:2", events[0].toKey)
        assertEquals(2 * minute, events[0].gapMillis)
    }

    @Test
    fun `a fresh session hours later starts a new work period, not a switch`() {
        val events = engine.detectSwitches(
            listOf(
                block(1L, "todo:1", 0, 30),
                block(2L, "todo:2", 120, 180)
            )
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `cutting into a running block counts as a switch`() {
        val events = engine.detectSwitches(
            listOf(
                block(1L, "todo:1", 0, 60),
                block(2L, "todo:2", 30, 45)
            )
        )
        assertEquals(1, events.size)
        assertTrue(events[0].gapMillis < 0)
    }

    @Test
    fun `dropping a task and coming back records the resumption delay`() {
        val blocks = listOf(
            block(1L, "todo:1", 0, 30),
            block(2L, "todo:2", 32, 45),
            block(1L, "todo:1", 60, 90)
        )
        val events = engine.withResumeTimes(engine.detectSwitches(blocks), blocks)

        assertEquals(1, events.size)
        assertNotNull(events[0].resumedAtMillis)
        assertEquals(dayStart + 60 * minute, events[0].resumedAtMillis)
    }

    @Test
    fun `a task that never comes back keeps a null resumption`() {
        val blocks = listOf(
            block(1L, "todo:1", 0, 30),
            block(2L, "todo:2", 32, 60)
        )
        val events = engine.withResumeTimes(engine.detectSwitches(blocks), blocks)

        assertEquals(1, events.size)
        assertNull(events[0].resumedAtMillis)
    }

    @Test
    fun `the day before anything happened reports the empty report`() {
        val report = engine.summarize(
            events = emptyList(),
            blocks = listOf(block(1L, "todo:1", 0, 60)),
            trackedMillis = 60 * minute,
            completedTodoIds = emptySet(),
            dayStartMillis = dayStart,
            dayEndMillis = dayStart + 60 * minute
        )
        assertEquals(SwitchReport.EMPTY, report)
    }

    @Test
    fun `the index climbs as the day fragments`() {
        val calm = reportFor(
            blocks = listOf(
                block(1L, "todo:1", 0, 45),
                block(2L, "todo:2", 47, 90)
            )
        )
        val frantic = reportFor(
            blocks = listOf(
                block(1L, "todo:1", 0, 20),
                block(2L, "todo:2", 22, 40),
                block(3L, "todo:3", 42, 60),
                block(4L, "todo:4", 62, 80),
                block(5L, "todo:5", 82, 100)
            )
        )

        assertTrue(frantic.hallucinationIndex > calm.hallucinationIndex)
        assertTrue(frantic.abandonedTaskCount > calm.abandonedTaskCount)
        assertTrue(frantic.hallucinationIndex in 0..100)
    }

    @Test
    fun `recovery numbers stay quiet until there are enough switches`() {
        val few = reportFor(
            blocks = listOf(
                block(1L, "todo:1", 0, 30),
                block(2L, "todo:2", 32, 60),
                block(1L, "todo:1", 62, 90)
            )
        )
        assertNull(few.postSwitchProductivity)
        assertNull(few.baselineProductivity)

        val many = reportFor(
            blocks = listOf(
                block(1L, "todo:1", 0, 10),
                block(2L, "todo:2", 11, 20),
                block(1L, "todo:1", 21, 30),
                block(2L, "todo:2", 31, 40),
                block(1L, "todo:1", 41, 50),
                block(2L, "todo:2", 51, 60)
            )
        )
        assertEquals(5, many.switchCount)
        assertNotNull(many.postSwitchProductivity)
        assertNotNull(many.baselineProductivity)
    }

    @Test
    fun `context key prefers the task, then the category, then the title`() {
        assertEquals("todo:7", engine.contextKeyOf(todoId = 7L, categoryId = 3L, title = "Write"))
        assertEquals("cat:3", engine.contextKeyOf(todoId = null, categoryId = 3L, title = "Write"))
        assertEquals("title:write", engine.contextKeyOf(todoId = null, categoryId = null, title = "Write"))
    }

    @Test
    fun `non focused blocks drag the post switch focus share down`() {
        val report = reportFor(
            blocks = listOf(
                block(1L, "todo:1", 0, 10, type = "PRODUCTIVE"),
                block(9L, "cat:9", 11, 20, type = "WASTED"),
                block(1L, "todo:1", 21, 30, type = "PRODUCTIVE"),
                block(9L, "cat:9", 31, 40, type = "WASTED"),
                block(1L, "todo:1", 41, 50, type = "PRODUCTIVE"),
                block(9L, "cat:9", 51, 60, type = "WASTED")
            )
        )
        val post = report.postSwitchProductivity
        val base = report.baselineProductivity
        assertNotNull(post)
        assertNotNull(base)
        // The jump always lands in the wasted block, so focus right after a
        // switch must be below the rest of the day.
        assertTrue(post!! < base!!)
    }

    private fun reportFor(blocks: List<SwitchBlock>): SwitchReport {
        val events = engine.withResumeTimes(engine.detectSwitches(blocks), blocks)
        return engine.summarize(
            events = events,
            blocks = blocks,
            trackedMillis = blocks.sumOf { it.durationMillis },
            completedTodoIds = emptySet(),
            dayStartMillis = dayStart,
            dayEndMillis = blocks.maxOf { it.endMillis }
        )
    }
}
