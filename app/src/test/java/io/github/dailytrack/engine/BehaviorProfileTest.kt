package io.github.dailytrack.engine

import io.github.dailytrack.data.db.entity.CategoryEntity
import io.github.dailytrack.data.db.entity.SessionEntity
import io.github.dailytrack.data.db.entity.TodoEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class BehaviorProfileTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun at(hour: Int): Long =
        LocalDateTime.of(2026, 1, 5, hour, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `cold start reports no data and neutral affinities`() {
        val profile = BehaviorProfile.coldStart()
        assertFalse(profile.hasData())
        assertEquals(0, profile.sampleSize)
        assertEquals(0.5, profile.hourAffinity(10, "PRODUCTIVE"), 0.0001)
        assertEquals(0.5, profile.completionRate("PRODUCTIVE"), 0.0001)
        assertEquals(0.5, profile.hourlyCompletionRate(10, "PRODUCTIVE"), 0.0001)
        assertTrue(profile.isNaturalHour(3, "PRODUCTIVE"))
        assertNull(profile.describe(10, "PRODUCTIVE"))
    }

    @Test
    fun `learns which hours belong to which activity`() {
        val sessions = (8..11).flatMap { hour ->
            (0 until 6).map {
                SessionEntity(
                    title = "learn",
                    type = "LEARNING",
                    startTime = at(hour),
                    endTime = at(hour) + 3_600_000L,
                    isActive = false
                )
            }
        } + (20..22).flatMap { hour ->
            (0 until 6).map {
                SessionEntity(
                    title = "code",
                    type = "PRODUCTIVE",
                    startTime = at(hour),
                    endTime = at(hour) + 3_600_000L,
                    isActive = false
                )
            }
        }

        val profile = BehaviorProfiler.build(
            sessions = sessions,
            todos = emptyList(),
            categories = emptyMap(),
            now = at(23),
            zone = zone
        )

        assertTrue(profile.hasData())
        assertTrue(profile.hourAffinity(9, "LEARNING") > profile.hourAffinity(9, "PRODUCTIVE"))
        assertTrue(profile.hourAffinity(21, "PRODUCTIVE") > profile.hourAffinity(21, "LEARNING"))
        assertEquals(listOf(8, 9, 10, 11), profile.typicalHours("LEARNING", 4))
        assertEquals("you usually do this kind of work around this hour", profile.describe(9, "LEARNING"))
    }

    @Test
    fun `empty inputs fall back to the cold start profile`() {
        val profile = BehaviorProfiler.build(emptyList(), emptyList(), emptyMap(), at(12), zone)
        assertFalse(profile.hasData())
        assertEquals(0.5, profile.hourAffinity(12, "ACTIVITY"), 0.0001)
    }

    @Test
    fun `completion rate sits between zero and one`() {
        val profile = BehaviorProfile.of(
            sessionMinutesByHourAndType = emptyMap(),
            completionsByHourAndType = mapOf(10 to mapOf("PRODUCTIVE" to 4)),
            intentByHourAndType = mapOf(10 to mapOf("PRODUCTIVE" to 4)),
            typeCompletions = mapOf("PRODUCTIVE" to 4),
            typeOpen = mapOf("PRODUCTIVE" to 1),
            sampleSize = 9
        )
        val rate = profile.completionRate("PRODUCTIVE")
        assertTrue(rate > 0.0 && rate < 1.0)
        assertEquals(9, profile.sampleSize)
    }

    @Test
    fun `completed todos are counted by completion hour`() {
        val categories = mapOf(
            1L to CategoryEntity(id = 1L, name = "Deep Work", type = "PRODUCTIVE")
        )
        val todos = listOf(
            TodoEntity(title = "Done 1", categoryId = 1L, isCompleted = true, completedAt = at(14), createdAt = at(9)),
            TodoEntity(title = "Done 2", categoryId = 1L, isCompleted = true, completedAt = at(14), createdAt = at(9)),
            TodoEntity(title = "Done 3", categoryId = 1L, isCompleted = true, completedAt = at(14), createdAt = at(9)),
            TodoEntity(title = "Open", categoryId = 1L, isCompleted = false, createdAt = at(14))
        )

        val profile = BehaviorProfiler.build(emptyList(), todos, categories, at(20), zone)

        assertTrue(profile.hasData())
        assertTrue(profile.hourlyCompletionRate(14, "PRODUCTIVE") != 0.5)
        assertTrue(profile.completionRate("PRODUCTIVE") < 1.0)
    }

    @Test
    fun `session type falls back to the category type when present`() {
        val categories = mapOf(
            7L to CategoryEntity(id = 7L, name = "Study", type = "LEARNING")
        )
        val sessions = listOf(
            SessionEntity(
                title = "Study",
                categoryId = 7L,
                type = "ACTIVITY",
                startTime = at(10),
                endTime = at(10) + 3_600_000L,
                isActive = false
            )
        )

        val profile = BehaviorProfiler.build(sessions, emptyList(), categories, at(23), zone)

        assertTrue(profile.hourAffinity(10, "LEARNING") > 0.7)
    }
}
