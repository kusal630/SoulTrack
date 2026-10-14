package io.github.dailytrack.engine

import io.github.dailytrack.data.db.entity.CategoryEntity
import io.github.dailytrack.data.db.entity.SessionEntity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TimeCoverageEngineTest {
    private lateinit var engine: TimeCoverageEngine
    private val emptyCategories = emptyMap<Long, CategoryEntity>()

    @Before
    fun setup() {
        engine = TimeCoverageEngine()
    }

    @Test
    fun `empty sessions returns zero coverage`() {
        val date = java.time.LocalDate.of(2024, 1, 15)
        val zone = java.time.ZoneId.of("UTC")
        // Clock pinned to noon so the assertions do not depend on wall time.
        val now = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val result = engine.calculateCoverage(emptyList(), emptyCategories, date, zone, now = now)

        assertEquals(0L, result.trackedSeconds)
        // Only the twelve hours that already happened can be untracked.
        assertEquals(12 * 3600L, result.untrackedSeconds)
        assertEquals(12 * 3600L, result.elapsedSeconds)
        assertEquals(24 * 3600L, result.totalDaySeconds)
        assertEquals(0.0, result.trackedRatio, 0.001)
    }

    @Test
    fun `unrecorded time counts as waste, future time does not`() {
        val date = java.time.LocalDate.of(2024, 1, 15)
        val zone = java.time.ZoneId.of("UTC")
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        // Clock is 4 hours into the day; one hour of it was recorded.
        val now = dayStart + 4 * 3600_000L

        val session = SessionEntity(
            id = 1,
            type = "PRODUCTIVE",
            categoryId = null,
            startTime = dayStart + 3600_000,
            endTime = dayStart + 7200_000,
            isActive = false
        )

        val result = engine.calculateCoverage(listOf(session), emptyCategories, date, zone, now = now)

        assertEquals(4 * 3600L, result.elapsedSeconds)
        assertEquals(3600L, result.trackedSeconds)
        // Three hours of the day already happened and were never written down.
        assertEquals(3 * 3600L, result.untrackedSeconds)
        // Nothing tracked as distraction, so all of it is unrecorded waste.
        assertEquals(3 * 3600L, result.totalWastedSeconds)
        assertEquals(0.75, result.wasteRatio, 0.001)
    }

    @Test
    fun `single session calculates duration correctly`() {
        val date = java.time.LocalDate.of(2024, 1, 15)
        val zone = java.time.ZoneId.of("UTC")
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()

        val session = SessionEntity(
            id = 1,
            type = "ACTIVITY",
            categoryId = null,
            startTime = dayStart + 3600_000,
            endTime = dayStart + 7200_000,
            isActive = false
        )

        val result = engine.calculateCoverage(listOf(session), emptyCategories, date, zone)
        assertEquals(3600L, result.trackedSeconds)
    }

    @Test
    fun `sleep session counted correctly`() {
        val date = java.time.LocalDate.of(2024, 1, 15)
        val zone = java.time.ZoneId.of("UTC")
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()

        val session = SessionEntity(
            id = 1,
            type = "SLEEP",
            categoryId = null,
            startTime = dayStart,
            endTime = dayStart + 28800_000,
            isActive = false
        )

        val result = engine.calculateCoverage(listOf(session), emptyCategories, date, zone)
        assertEquals(28800L, result.sleepSeconds)
        assertEquals(28800L, result.trackedSeconds)
    }

    @Test
    fun `exercise session counted correctly`() {
        val date = java.time.LocalDate.of(2024, 1, 15)
        val zone = java.time.ZoneId.of("UTC")
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()

        val session = SessionEntity(
            id = 1,
            type = "EXERCISE",
            categoryId = null,
            startTime = dayStart + 3600_000,
            endTime = dayStart + 5400_000,
            isActive = false
        )

        val result = engine.calculateCoverage(listOf(session), emptyCategories, date, zone)
        assertEquals(1800L, result.exerciseSeconds)
    }

    @Test
    fun `category type overrides session type`() {
        val date = java.time.LocalDate.of(2024, 1, 15)
        val zone = java.time.ZoneId.of("UTC")
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()

        val category = CategoryEntity(id = 1, name = "Study", type = "LEARNING")
        val categories = mapOf(1L to category)

        val session = SessionEntity(
            id = 1,
            type = "ACTIVITY",
            categoryId = 1,
            startTime = dayStart + 3600_000,
            endTime = dayStart + 7200_000,
            isActive = false
        )

        val result = engine.calculateCoverage(listOf(session), categories, date, zone)
        assertEquals(3600L, result.learningSeconds)
        assertEquals(0L, result.productiveSeconds)
    }

    @Test
    fun `multiple sessions sum correctly`() {
        val date = java.time.LocalDate.of(2024, 1, 15)
        val zone = java.time.ZoneId.of("UTC")
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()

        val sessions = listOf(
            SessionEntity(id = 1, type = "SLEEP", categoryId = null, startTime = dayStart, endTime = dayStart + 28800_000, isActive = false),
            SessionEntity(id = 2, type = "LEARNING", categoryId = null, startTime = dayStart + 30000_000, endTime = dayStart + 33600_000, isActive = false),
            SessionEntity(id = 3, type = "EXERCISE", categoryId = null, startTime = dayStart + 36000_000, endTime = dayStart + 37800_000, isActive = false)
        )

        val result = engine.calculateCoverage(sessions, emptyCategories, date, zone)
        assertEquals(28800L, result.sleepSeconds)
        assertEquals(3600L, result.learningSeconds)
        assertEquals(1800L, result.exerciseSeconds)
        assertEquals(34200L, result.trackedSeconds)
    }
}
