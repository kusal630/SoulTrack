/*
 * Copyright 2024 Soul Track Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.github.dailytrack.engine

/**
 * The verdict written into the outer (week) ring for one day.
 *
 * Every status carries a plain-language label so the UI never has to invent
 * copy, and it is always paired with an icon in the UI layer - colour is
 * never the only signal.
 */
enum class DayStatus(val label: String) {
    /** All assigned tasks were completed (and the day was not wasted). */
    DONE("All tasks done"),

    /** Some tasks were assigned but not every one was completed. */
    TASKS_NOT_MET("Tasks not met"),

    /** Focus time fell short of what the day planned. */
    NOT_PRODUCTIVE("Not productive"),

    /** Distraction ate too much of the tracked time. */
    WASTING_TIME("Wasting time"),

    /** No tasks were assigned, so there is nothing to judge yet. */
    NO_TASKS("No tasks assigned"),

    /** The day has not ended yet - this segment is still dim. */
    IN_PROGRESS("In progress");

    /** True when this status should be painted into the week ring. */
    val isRecorded: Boolean
        get() = this != IN_PROGRESS
}

/**
 * Editable knobs for [classifyDay]. They start at the exact values the unit
 * tests assert against and can be retuned from Settings at runtime, so
 * [classifyDay] itself never has to change.
 */
object DayClassificationThresholds {
    /** Above this share of tracked time spent distracting, the day is wasted. */
    @Volatile
    var WASTING_TIME_DISTRACTION_RATIO = 0.25

    /** Below this share of the planned focus time, the day is unproductive. */
    @Volatile
    var NOT_PRODUCTIVE_PRODUCTIVE_RATIO = 0.40
}

/**
 * Everything the classifier needs about a single day. A plain value object so
 * it stays a pure function - no database, no clock, no Compose.
 */
data class DaySnapshot(
    /** Seconds of the day that were actually tracked. */
    val trackedSeconds: Long = 0L,
    /** Seconds spent on distracting categories. */
    val distractionSeconds: Long = 0L,
    /** Seconds spent on productive categories. */
    val productiveSeconds: Long = 0L,
    /** Seconds of focus the user planned for that day (0 = nothing planned). */
    val plannedFocusSeconds: Long = 0L,
    /** Tasks assigned for that day. */
    val assignedTasks: Int = 0,
    /** Tasks completed for that day. */
    val completedTasks: Int = 0
) {
    /** Fraction of assigned tasks completed, 0f..1f. */
    val completion: Float
        get() = dayProgress(assignedTasks, completedTasks)

    /** Fraction of tracked time that was distracting, 0f..1f (0 when nothing tracked). */
    val distractionRatio: Float
        get() = if (trackedSeconds <= 0L) 0f
        else distractionSeconds.toFloat().coerceAtMost(trackedSeconds.toFloat()) / trackedSeconds

    /** Fraction of the planned focus time actually spent productive, 0f..1f. */
    val productiveRatio: Float
        get() = if (plannedFocusSeconds <= 0L) 1f
        else (productiveSeconds.toFloat() / plannedFocusSeconds).coerceIn(0f, 1f)
}

/**
 * Classifies one day. Order is fixed and documented:
 *
 * 1. no assigned tasks -> [DayStatus.NO_TASKS] (nothing to judge)
 * 2. distraction > 25% of tracked time -> [DayStatus.WASTING_TIME]
 * 3. productive < 40% of planned focus -> [DayStatus.NOT_PRODUCTIVE]
 * 4. completion < 100% -> [DayStatus.TASKS_NOT_MET]
 * 5. otherwise -> [DayStatus.DONE]
 *
 * The thresholds come from [DayClassificationThresholds] and are editable
 * without changing this function.
 */
fun classifyDay(
    snapshot: DaySnapshot,
    thresholds: DayClassificationThresholds = DayClassificationThresholds
): DayStatus = when {
    snapshot.assignedTasks <= 0 -> DayStatus.NO_TASKS
    snapshot.distractionRatio > thresholds.WASTING_TIME_DISTRACTION_RATIO -> DayStatus.WASTING_TIME
    snapshot.productiveRatio < thresholds.NOT_PRODUCTIVE_PRODUCTIVE_RATIO -> DayStatus.NOT_PRODUCTIVE
    snapshot.completion < 1f -> DayStatus.TASKS_NOT_MET
    else -> DayStatus.DONE
}

/**
 * How much of today's focus goal has been *recorded*, 0f..1f.
 *
 * The ring has to answer "did this day go to work?", and the only honest
 * measure of that is time the user actually wrote down. A goal of 0 means no
 * target is known yet (nothing set, no history), so the focus part simply
 * stays silent rather than inventing a number.
 */
fun focusProgress(focusedSeconds: Long, goalSeconds: Long): Float {
    if (goalSeconds <= 0L) return 0f
    return (focusedSeconds.toFloat() / goalSeconds).coerceIn(0f, 1f)
}

/**
 * The day ring: finished tasks and recorded focus time both earn progress.
 *
 * Tasks cover the planned side of the day; every timer, pomodoro and backfilled
 * work block covers the recorded side. The ring fills with whichever is
 * further along, so work that was tracked but never written as a task still
 * moves it - and a task-heavy day still fills it without a single timer.
 */
fun recordedDayProgress(taskProgress: Float, focusProgress: Float): Float =
    maxOf(taskProgress.coerceIn(0f, 1f), focusProgress.coerceIn(0f, 1f))

/**
 * Day progress = completed / assigned, clamped to 0f..1f.
 *
 * Adding the very first task makes this move immediately (1 assigned, 0 done
 * still shows an empty ring, but a single completion snaps it to 1.0 for a
 * one-task day), and an empty day reads as 0 rather than NaN.
 */
fun dayProgress(assignedTasks: Int, completedTasks: Int): Float {
    if (assignedTasks <= 0) return 0f
    return (completedTasks.toFloat() / assignedTasks).coerceIn(0f, 1f)
}

/**
 * Progress toward a goal, 0f..1f. A target of 0 (or less) means "no target
 * set", so the progress is unknown and reported as 0 rather than dividing by
 * zero. Monitoring and reporting goal progress is a proven way to raise
 * attainment (Harkin et al., 2015, 138 studies, d+ = 0.40).
 */
fun goalProgress(currentHours: Float, targetHours: Float): Float {
    if (targetHours <= 0f) return 0f
    return (currentHours / targetHours).coerceIn(0f, 1f)
}

/**
 * Optional minute-weighted progress: a 90 minute task moves the ring more
 * than a 5 minute one. Falls back to the plain ratio when no estimate exists.
 */
fun weightedDayProgress(
    tasks: List<TaskWeight>,
    completedTaskIds: Set<Long> = emptySet()
): Float {
    val total = tasks.sumOf { it.estimatedMinutes.coerceAtLeast(0) }
    if (total <= 0) return dayProgress(tasks.size, tasks.count { it.id in completedTaskIds })
    val done = tasks.filter { it.id in completedTaskIds }.sumOf { it.estimatedMinutes.coerceAtLeast(0) }
    return (done.toFloat() / total).coerceIn(0f, 1f)
}

/** Minimal task shape the progress maths needs - keeps the function pure. */
data class TaskWeight(val id: Long, val estimatedMinutes: Int)

/**
 * Human sentence for TalkBack: "Today: 3 of 5 tasks done."
 */
fun todayProgressLabel(completed: Int, assigned: Int): String = when {
    assigned <= 0 -> "Today: no tasks assigned yet."
    else -> "Today: $completed of $assigned tasks done."
}

/**
 * TalkBack sentence for a whole week, e.g.
 * "Week: Monday all tasks done, Tuesday tasks not met, Wednesday in progress."
 */
fun weekStatusLabel(statuses: List<DayStatus?>, dayNames: List<String>): String {
    if (statuses.isEmpty()) return "Week: no days recorded yet."
    val parts = statuses.mapIndexedNotNull { index, status ->
        val name = dayNames.getOrNull(index) ?: return@mapIndexedNotNull null
        when (status) {
            null -> "$name not recorded"
            DayStatus.IN_PROGRESS -> "$name in progress"
            else -> "$name ${status.label.lowercase()}"
        }
    }
    return "Week: ${parts.joinToString(", ")}."
}
