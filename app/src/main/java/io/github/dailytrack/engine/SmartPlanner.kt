package io.github.dailytrack.engine

import java.time.Instant
import java.time.ZoneId
import kotlin.math.ceil

/**
 * The four Eisenhower quadrants. The number matches [io.github.dailytrack.data.db.entity.TodoEntity.quadrant]
 * so a classification can be persisted as-is.
 */
enum class EisenhowerQuadrant(val number: Int, val label: String, val action: String) {
    DO_FIRST(1, "Urgent & important", "Do first"),
    SCHEDULE(2, "Important, not urgent", "Schedule deep work"),
    DELEGATE(3, "Urgent, not important", "Batch or delegate"),
    DELETE(4, "Neither urgent nor important", "Delete");

    companion object {
        fun fromNumber(number: Int): EisenhowerQuadrant? =
            entries.firstOrNull { it.number == number }
    }
}

/** A task as the planner sees it - plain values, no database or clock. */
data class PlannerTask(
    val id: Long,
    val title: String,
    val priority: Int = 0,
    val deadline: Long? = null,
    val estimatedMinutes: Int = 0,
    /** A saved quadrant (1..4); 0 means "classify from priority + deadline". */
    val savedQuadrant: Int = 0,
    val isCompleted: Boolean = false,
    val isInbox: Boolean = false
)

data class PlannedBlock(
    val taskId: Long,
    val title: String,
    val quadrant: EisenhowerQuadrant,
    val startMillis: Long,
    val endMillis: Long,
    val pomodoroMinutes: Int,
    val cycles: Int,
    val inBestHour: Boolean
) {
    val minutes: Int get() = ((endMillis - startMillis) / 60_000L).toInt()
}

data class DailyPlan(
    val blocks: List<PlannedBlock>,
    val deleteCandidates: List<PlannerTask>,
    val unscheduled: List<PlannerTask>,
    val bestHours: List<Int>
) {
    val totalMinutes: Int get() = blocks.sumOf { it.minutes }
    val totalCycles: Int get() = blocks.sumOf { it.cycles }
    val isEmpty: Boolean get() = blocks.isEmpty() && deleteCandidates.isEmpty()
}

object SmartPlanner {
    /** Anything due inside this window counts as urgent. */
    const val URGENT_WINDOW_HOURS = 48L
    /** Important = high or medium priority. */
    const val IMPORTANT_PRIORITY = 2
    /** Deep-work blocks are capped so a single task cannot eat the whole day. */
    const val MAX_BLOCK_MINUTES = 120

    val DEFAULT_BEST_HOURS = listOf(9, 10, 14, 15)

    fun classify(
        priority: Int,
        deadline: Long?,
        now: Long,
        urgentWindowHours: Long = URGENT_WINDOW_HOURS
    ): EisenhowerQuadrant {
        val urgent = deadline != null &&
            deadline - now <= urgentWindowHours * 3_600_000L
        // Priority 0 means "not set yet" in this app, so it is treated as
        // important rather than as a reason to delete the task. Only an
        // explicitly low-priority (1) task can land in quadrant 4.
        val important = priority >= IMPORTANT_PRIORITY || priority == 0
        return when {
            urgent && important -> EisenhowerQuadrant.DO_FIRST
            important -> EisenhowerQuadrant.SCHEDULE
            urgent -> EisenhowerQuadrant.DELEGATE
            else -> EisenhowerQuadrant.DELETE
        }
    }

    fun classify(task: PlannerTask, now: Long): EisenhowerQuadrant =
        EisenhowerQuadrant.fromNumber(task.savedQuadrant)
            ?: classify(task.priority, task.deadline, now)

    /**
     * Builds the day: quadrant 1 first, quadrant 2 into the user's best hours,
     * quadrant 3 batched afterwards, quadrant 4 handed back for deletion.
     * Every block is pre-split into whole pomodoro cycles.
     */
    fun buildDay(
        tasks: List<PlannerTask>,
        now: Long,
        bestHours: List<Int> = emptyList(),
        pomodoroMinutes: Int = 25,
        zone: ZoneId = ZoneId.systemDefault()
    ): DailyPlan {
        val cycle = pomodoroMinutes.coerceIn(5, 120)
        val active = tasks.filter { !it.isCompleted && !it.isInbox }
        val quadrants = active.associateWith { classify(it, now) }
        val hours = bestHours.filter { it in 0..23 }.ifEmpty { DEFAULT_BEST_HOURS }

        val q1 = active.filter { quadrants[it] == EisenhowerQuadrant.DO_FIRST }
            .sortedWith(compareBy({ it.deadline ?: Long.MAX_VALUE }, { -it.priority }, { it.id }))
        val q2 = active.filter { quadrants[it] == EisenhowerQuadrant.SCHEDULE }
            .sortedWith(compareBy({ -it.priority }, { it.deadline ?: Long.MAX_VALUE }, { it.id }))
        val q3 = active.filter { quadrants[it] == EisenhowerQuadrant.DELEGATE }
            .sortedWith(compareBy({ it.deadline ?: Long.MAX_VALUE }, { it.id }))
        val q4 = active.filter { quadrants[it] == EisenhowerQuadrant.DELETE }

        val blocks = mutableListOf<PlannedBlock>()
        var cursor = roundUpToFiveMinutes(now)

        fun schedule(task: PlannerTask, quadrant: EisenhowerQuadrant, start: Long, best: Boolean) {
            val duration = task.estimatedMinutes
                .takeIf { it > 0 }
                ?.coerceIn(cycle, MAX_BLOCK_MINUTES)
                ?: cycle
            val end = start + duration * 60_000L
            blocks += PlannedBlock(
                taskId = task.id,
                title = task.title,
                quadrant = quadrant,
                startMillis = start,
                endMillis = end,
                pomodoroMinutes = cycle,
                cycles = ceil(duration.toDouble() / cycle).toInt().coerceAtLeast(1),
                inBestHour = best
            )
            cursor = maxOf(cursor, end)
        }

        // Q1: urgent and important, back to back, first thing.
        q1.forEach { schedule(it, EisenhowerQuadrant.DO_FIRST, cursor, inBestHour(cursor, hours, zone)) }

        // Q3: urgent but not important - batch it in the gap that Q2 leaves
        // open when its best hour is later in the day.
        q3.forEach { schedule(it, EisenhowerQuadrant.DELEGATE, cursor, inBestHour(cursor, hours, zone)) }

        // Q2: important work gets the best hours. When a preferred hour is
        // still ahead today, start there; otherwise start as soon as possible.
        q2.forEach { task ->
            val preferred = preferredStart(cursor, hours, zone)
            schedule(task, EisenhowerQuadrant.SCHEDULE, preferred, inBestHour(preferred, hours, zone))
        }

        return DailyPlan(
            blocks = blocks.sortedBy { it.startMillis },
            deleteCandidates = q4,
            unscheduled = tasks.filter { !it.isCompleted && it.isInbox },
            bestHours = hours
        )
    }

    fun roundUpToFiveMinutes(millis: Long): Long {
        val step = 5 * 60_000L
        return ((millis + step - 1) / step) * step
    }

    private fun inBestHour(start: Long, bestHours: List<Int>, zone: ZoneId): Boolean =
        Instant.ofEpochMilli(start).atZone(zone).hour in bestHours

    /**
     * The next occurrence of [hour] that is at or after [notBefore]. If the
     * hour already passed today, the work starts now rather than being pushed
     * to tomorrow - today's plan must stay executable today.
     */
    private fun preferredStart(notBefore: Long, bestHours: List<Int>, zone: ZoneId): Long {
        val today = Instant.ofEpochMilli(notBefore).atZone(zone)
        val next = bestHours
            .map { hour -> today.withHour(hour).withMinute(0).withSecond(0).withNano(0) }
            .filter { it.toInstant().toEpochMilli() >= notBefore }
            .minOrNull()
        return roundUpToFiveMinutes(next?.toInstant()?.toEpochMilli() ?: notBefore)
    }

    /** The three-hour pomodoro cadence label shown on a planned block. */
    fun cycleLabel(cycles: Int, pomodoroMinutes: Int): String =
        if (cycles <= 1) "$pomodoroMinutes min focus"
        else "$cycles x $pomodoroMinutes min cycles"
}
