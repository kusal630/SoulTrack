package io.github.dailytrack.engine

import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/** Focused minutes and completed cycles recorded in one hour of the day. */
data class HourSample(
    val hour: Int,
    val focusedMinutes: Int,
    val completedCycles: Int = 0
)

data class FocusHour(
    val hour: Int,
    val focusedMinutes: Int,
    val completedCycles: Int,
    val score: Int
)

/** One task's plan against what actually happened. */
data class TaskTime(
    val taskId: Long,
    val title: String,
    val quadrant: Int,
    val estimatedMinutes: Int,
    val actualMinutes: Int
)

data class TaskTimeComparison(
    val taskId: Long,
    val title: String,
    val estimatedMinutes: Int,
    val actualMinutes: Int
) {
    val deltaMinutes: Int get() = actualMinutes - estimatedMinutes
    val label: String get() = when {
        deltaMinutes >= 15 -> "took ${deltaMinutes}m longer than planned"
        deltaMinutes <= -15 -> "finished ${(-deltaMinutes)}m early"
        else -> "matched the estimate"
    }
}

data class QuadrantSlice(
    val quadrant: EisenhowerQuadrant,
    val minutes: Int
) {
    val shareOfTotal: Float get() = 0f
}

data class QuadrantSplit(
    val slices: List<QuadrantSlice>,
    val totalMinutes: Int
) {
    fun share(quadrant: EisenhowerQuadrant): Float {
        if (totalMinutes <= 0) return 0f
        val minutes = slices.firstOrNull { it.quadrant == quadrant }?.minutes ?: 0
        return minutes.toFloat() / totalMinutes
    }

    val dominant: EisenhowerQuadrant? get() = slices.maxByOrNull { it.minutes }?.quadrant
}

/** A completed (or abandoned) focus cycle. */
data class CycleRecord(
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val completed: Boolean
)

data class DistractionPattern(
    val totalCycles: Int,
    val earlyQuits: Int,
    val earlyQuitRate: Float,
    val averageQuitAtMinutes: Int?
) {
    val hasEnoughData: Boolean get() = totalCycles >= 3
    val label: String get() {
        if (totalCycles <= 0) return "No focus cycles recorded yet"
        if (!hasEnoughData) return "A few more cycles will reveal your pattern"
        return when {
            earlyQuitRate >= 0.40f -> "You stop early in ${(earlyQuitRate * 100).roundToInt()}% of cycles - try a shorter block"
            earlyQuitRate >= 0.20f -> "You stop early in ${(earlyQuitRate * 100).roundToInt()}% of cycles"
            else -> "You finish ${(100 - earlyQuitRate * 100).roundToInt()}% of cycles - your block length fits"
        }
    }
}

data class FocusInsightReport(
    val bestHours: List<FocusHour>,
    val comparisons: List<TaskTimeComparison>,
    val averageErrorMinutes: Int?,
    val quadrantSplit: QuadrantSplit,
    val distraction: DistractionPattern
) {
    val hasFocusData: Boolean get() = bestHours.isNotEmpty()
    val bestHourLabel: String?
        get() = bestHours.firstOrNull()?.let { "%02d:00".format(it.hour) }
}

object FocusInsights {
    /** Focus minutes plus a small bonus per finished cycle, best first. */
    fun bestHours(samples: List<HourSample>, limit: Int = 3): List<FocusHour> {
        if (samples.isEmpty()) return emptyList()
        return samples
            .filter { it.hour in 0..23 && it.focusedMinutes > 0 }
            .groupBy { it.hour }
            .map { (hour, rows) ->
                val minutes = rows.sumOf { it.focusedMinutes }
                val cycles = rows.sumOf { it.completedCycles }
                FocusHour(hour, minutes, cycles, minutes + cycles * 10)
            }
            .sortedWith(compareByDescending<FocusHour> { it.score }.thenBy { it.hour })
            .take(limit.coerceAtLeast(1))
    }

    /** The hours the smart planner should reserve for deep work. */
    fun bestHourNumbers(samples: List<HourSample>, limit: Int = 4): List<Int> =
        bestHours(samples, limit).map { it.hour }.ifEmpty { SmartPlanner.DEFAULT_BEST_HOURS }

    fun plannedVsActual(tasks: List<TaskTime>): List<TaskTimeComparison> =
        tasks.filter { it.estimatedMinutes > 0 && it.actualMinutes > 0 }
            .map { TaskTimeComparison(it.taskId, it.title, it.estimatedMinutes, it.actualMinutes) }
            .sortedByDescending { it.deltaMinutes.absoluteValue }

    fun averageErrorMinutes(comparisons: List<TaskTimeComparison>): Int? {
        if (comparisons.isEmpty()) return null
        return comparisons.map { it.deltaMinutes }.average().roundToInt()
    }

    fun quadrantSplit(tasks: List<TaskTime>): QuadrantSplit {
        val perQuadrant = EisenhowerQuadrant.entries.map { quadrant ->
            val minutes = tasks
                .filter { it.quadrant == quadrant.number }
                .sumOf { it.actualMinutes.coerceAtLeast(0) }
            QuadrantSlice(quadrant, minutes)
        }.filter { it.minutes > 0 }
        return QuadrantSplit(perQuadrant, perQuadrant.sumOf { it.minutes })
    }

    fun distractionPattern(cycles: List<CycleRecord>): DistractionPattern {
        val valid = cycles.filter { it.plannedMinutes > 0 && it.actualMinutes >= 0 }
        if (valid.isEmpty()) return DistractionPattern(0, 0, 0f, null)
        val quits = valid.filter { !it.completed || it.actualMinutes < it.plannedMinutes * 0.8f }
        return DistractionPattern(
            totalCycles = valid.size,
            earlyQuits = quits.size,
            earlyQuitRate = quits.size.toFloat() / valid.size,
            averageQuitAtMinutes = quits.takeIf { it.isNotEmpty() }
                ?.map { it.actualMinutes }
                ?.average()
                ?.roundToInt()
        )
    }

    fun report(
        hourSamples: List<HourSample>,
        tasks: List<TaskTime>,
        cycles: List<CycleRecord>
    ): FocusInsightReport {
        val comparisons = plannedVsActual(tasks)
        return FocusInsightReport(
            bestHours = bestHours(hourSamples),
            comparisons = comparisons,
            averageErrorMinutes = averageErrorMinutes(comparisons),
            quadrantSplit = quadrantSplit(tasks),
            distraction = distractionPattern(cycles)
        )
    }
}
