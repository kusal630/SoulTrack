package io.github.dailytrack.engine

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow

data class CompoundPoint(val dayIndex: Int, val value: Double)

data class CompoundStreak(
    val current: Int,
    val best: Int,
    val missedDays: Int,
    val lastLogged: LocalDate?
)

data class CompoundProgress(
    val currentValue: Double,
    val changePercent: Double,
    val daysLogged: Int,
    val streak: CompoundStreak,
    val curve: List<CompoundPoint>
) {
    val isOnTrack: Boolean get() = streak.missedDays == 0
    val message: String get() = when {
        streak.missedDays <= 1 && streak.current > 0 ->
            "One small step today keeps the curve alive. Miss a day and you simply start again tomorrow."
        streak.missedDays > 1 ->
            "You missed ${streak.missedDays} days - the curve is not zeroed. Back on track tomorrow."
        else ->
            "Log one tiny increment today to start the curve."
    }
}

object Compounding {
    /** Value after [days] of compounding at [dailyPercent] per day. */
    fun compoundValue(startValue: Double, dailyPercent: Double, days: Int): Double {
        if (days <= 0) return startValue
        val rate = dailyPercent / 100.0
        return startValue * (1.0 + rate).pow(days)
    }

    /**
     * The growth curve for a tractable window (a year is 365 points). Values
     * are compounded, not linear, so the shape shows why tiny daily gains add up.
     */
    fun curve(
        startValue: Double,
        dailyPercent: Double,
        days: Int = 365,
        points: Int = 30
    ): List<CompoundPoint> {
        val safeDays = days.coerceAtLeast(1)
        val step = (safeDays / points.coerceAtLeast(1)).coerceAtLeast(1)
        val result = mutableListOf(CompoundPoint(0, startValue))
        var day = step
        while (day < safeDays) {
            result += CompoundPoint(day, compoundValue(startValue, dailyPercent, day))
            day += step
        }
        result += CompoundPoint(safeDays, compoundValue(startValue, dailyPercent, safeDays))
        return result
    }

    /**
     * Today's progress from the days actually logged. A gap lowers the current
     * streak but never wipes the best streak or the accumulated value, which is
     * the point of the 1% rule: one slip should not end the habit.
     */
    fun progress(
        startValue: Double,
        dailyIncrement: Double,
        dailyPercent: Double,
        logs: List<Pair<LocalDate, Double>>,
        today: LocalDate
    ): CompoundProgress {
        val unique = logs
            .groupBy { it.first }
            .map { (date, entries) -> date to entries.sumOf { it.second } }
            .sortedBy { it.first }

        val totalAdded = unique.sumOf { it.second }
        val current = (startValue + totalAdded).coerceAtLeast(0.0)
        val daysLogged = unique.size

        var currentStreak = 0
        var cursor = today
        val loggedDates = unique.map { it.first }.toSet()
        if (today !in loggedDates) {
            // Yesterday still counts as a live streak; today is not over yet.
            cursor = today.minusDays(1)
            if (cursor !in loggedDates) currentStreak = 0
        }
        if (cursor in loggedDates) {
            while (cursor in loggedDates) {
                currentStreak++
                cursor = cursor.minusDays(1)
            }
        }

        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (date in unique.map { it.first }) {
            run = if (previous != null && ChronoUnit.DAYS.between(previous, date) == 1L) run + 1 else 1
            best = maxOf(best, run)
            previous = date
        }
        best = maxOf(best, currentStreak)

        val lastLogged = unique.lastOrNull()?.first
        val missed = lastLogged?.let {
            ChronoUnit.DAYS.between(it, today).toInt().coerceAtLeast(0) - 1
        } ?: 0

        val projected = compoundValue(current, dailyPercent, 30)
        val change = if (current > 0.0) (projected - current) / current * 100.0 else 0.0

        return CompoundProgress(
            currentValue = current,
            changePercent = change,
            daysLogged = daysLogged,
            streak = CompoundStreak(currentStreak, best, missed.coerceAtLeast(0), lastLogged),
            curve = curve(startValue, dailyPercent, days = 30)
        )
    }

    /** Value of a daily increment habit without compounding (for the UI copy). */
    fun simpleProjection(startValue: Double, dailyIncrement: Double, days: Int): Double =
        startValue + dailyIncrement.coerceAtLeast(0.0) * days.coerceAtLeast(0)
}
