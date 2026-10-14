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
 * Detection of "starting another task in the middle of the current one" -
 * the attention drift this product calls a hallucination.
 *
 * The research behind the numbers (Leroy's attention residue, Mark's
 * interruption studies, the CHI context-switch literature) says the same
 * three things every time:
 *
 *  1. A switch is only costly when it happens *while the previous work is
 *     still unfinished* - so a switch is defined as a change of work context
 *     that resumes within [SwitchCostThresholds.MAX_GAP_MINUTES] of the
 *     previous block stopping, or that cuts the previous block short.
 *  2. The damage is in the **recovery**, not the switch itself: focus is
 *     measured in the minutes right after the switch and compared against
 *     that same day's baseline.
 *  3. People who abandon work rarely come back to it - the *resumption delay*
 *     and the number of never-resumed tasks are the honest damage report.
 *
 * Everything is derived from the user's own sessions. When there is not
 * enough data to claim something, the field is `null` rather than a number
 * borrowed from a paper.
 */
object SwitchCostThresholds {
    /** Gap between two blocks still counts as the same work period. */
    @Volatile
    var MAX_GAP_MINUTES = 5.0

    /** Switches per hour that already score full marks on the frequency axis. */
    @Volatile
    var SWITCHES_PER_HOUR_FOR_MAX = 2.0

    /** Window used to compare focus after a switch against the day's baseline. */
    @Volatile
    var RECOVERY_WINDOW_MINUTES = 15.0

    /** Switches needed before a recovery figure is quoted at all. */
    @Volatile
    var MIN_SWITCHES_FOR_RECOVERY = 5

    /** Share of the index coming from frequency vs. abandoned work. */
    const val WEIGHT_FREQUENCY = 0.65
    const val WEIGHT_ABANDONED = 0.35
}

/** One uninterrupted stretch of a single context (timer session or pomodoro). */
data class SwitchBlock(
    /** The task being worked on, when the block is task-linked. */
    val todoId: Long?,
    /** What makes this block "this work": task, else category, else title. */
    val contextKey: String,
    val startMillis: Long,
    val endMillis: Long,
    /** Category type (PRODUCTIVE, WASTED, ...) or the session type. */
    val type: String
) {
    val durationMillis: Long get() = (endMillis - startMillis).coerceAtLeast(0L)

    val isFocused: Boolean
        get() = type == "PRODUCTIVE" || type == "LEARNING" || type == "ACTIVITY"
}

/** A detected jump from one context to another. */
data class SwitchEvent(
    val atMillis: Long,
    val fromKey: String,
    val toKey: String,
    val fromTodoId: Long?,
    val toTodoId: Long?,
    /** How long after the previous block the new one started (can be negative when it cut in). */
    val gapMillis: Long,
    /** When the abandoned task was picked up again, or null if it never was. */
    val resumedAtMillis: Long?
)

/** The day's switch report - every number comes from the user's own sessions. */
data class SwitchReport(
    val switchCount: Int,
    /** Switches per tracked hour, 0f when nothing was tracked. */
    val switchesPerHour: Float,
    /** Tasks that were dropped mid-flight (and, by definition, unfinished). */
    val abandonedTaskCount: Int,
    val abandonedTodoIds: Set<Long>,
    /** Mean delay before an abandoned task was touched again, null if none came back. */
    val averageResumeMinutes: Float?,
    /** Focus share right after a switch; null until enough switches happened. */
    val postSwitchProductivity: Float?,
    /** The same day's focus share away from switch windows; null alongside the above. */
    val baselineProductivity: Float?,
    /** 0..100 attention-drift index (the "hallucination" score). */
    val hallucinationIndex: Int
) {
    companion object {
        val EMPTY = SwitchReport(
            switchCount = 0,
            switchesPerHour = 0f,
            abandonedTaskCount = 0,
            abandonedTodoIds = emptySet(),
            averageResumeMinutes = null,
            postSwitchProductivity = null,
            baselineProductivity = null,
            hallucinationIndex = 0
        )
    }
}

class SwitchCostEngine(private val thresholds: SwitchCostThresholds = SwitchCostThresholds) {

    /** Identity of a stretch of work: task first, then category, then raw title. */
    fun contextKeyOf(todoId: Long?, categoryId: Long?, title: String): String = when {
        todoId != null -> "todo:$todoId"
        categoryId != null -> "cat:$categoryId"
        else -> "title:${title.trim().lowercase()}"
    }

    /**
     * Finds every jump between contexts inside one day.
     *
     * Two blocks belong to the same context only when [SwitchBlock.contextKey]
     * matches. A pair counts as a switch when the new block starts no more than
     * [SwitchCostThresholds.MAX_GAP_MINUTES] after the old one ended (the user
     * came straight back and did something else) or when it starts before the
     * old one has finished (an outright cut-in).
     */
    fun detectSwitches(blocks: List<SwitchBlock>): List<SwitchEvent> {
        val ordered = blocks.sortedBy { it.startMillis }
        if (ordered.size < 2) return emptyList()

        val maxGap = (thresholds.MAX_GAP_MINUTES * 60_000.0).toLong()
        val events = mutableListOf<SwitchEvent>()

        for (i in 1 until ordered.size) {
            val previous = ordered[i - 1]
            val current = ordered[i]
            if (previous.contextKey == current.contextKey) continue

            val gap = current.startMillis - previous.endMillis
            if (gap <= maxGap) {
                events += SwitchEvent(
                    atMillis = current.startMillis,
                    fromKey = previous.contextKey,
                    toKey = current.contextKey,
                    fromTodoId = previous.todoId,
                    toTodoId = current.todoId,
                    gapMillis = gap,
                    resumedAtMillis = null
                )
            }
        }
        return events
    }

    /** Fills in when (if ever) the dropped task came back. */
    fun withResumeTimes(events: List<SwitchEvent>, blocks: List<SwitchBlock>): List<SwitchEvent> {
        if (events.isEmpty()) return events
        val ordered = blocks.sortedBy { it.startMillis }
        return events.map { event ->
            val target = event.fromTodoId ?: return@map event
            val resumed = ordered.firstOrNull {
                it.todoId == target && it.startMillis > event.atMillis
            } ?: return@map event
            event.copy(resumedAtMillis = resumed.startMillis)
        }
    }

    /**
     * Aggregates the day. [trackedMillis] is how much time was tracked that
     * day, [completedTodoIds] the tasks closed that day - both let the index
     * say something about unfinished work instead of just counting jumps.
     */
    fun summarize(
        events: List<SwitchEvent>,
        blocks: List<SwitchBlock>,
        trackedMillis: Long,
        completedTodoIds: Set<Long>,
        dayStartMillis: Long,
        dayEndMillis: Long
    ): SwitchReport {
        if (events.isEmpty()) return SwitchReport.EMPTY

        val touchedTodos = blocks.mapNotNull { it.todoId }.toSet()
        val abandoned = events.mapNotNull { it.fromTodoId }
            .filter { it !in completedTodoIds }
            .toSet()

        val resumeDelays = events.mapNotNull { event ->
            val resumed = event.resumedAtMillis ?: return@mapNotNull null
            (resumed - event.atMillis).takeIf { it >= 0L }
        }
        val averageResume = resumeDelays
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.let { it / 60_000.0 }
            ?.toFloat()

        val switchesPerHour = if (trackedMillis > 0L) {
            events.size.toFloat() / (trackedMillis / 3_600_000.0).toFloat()
        } else 0f

        val frequency = (
            switchesPerHour /
                thresholds.SWITCHES_PER_HOUR_FOR_MAX.toFloat().coerceAtLeast(0.1f)
            ).coerceIn(0f, 1f)
        val abandonedShare = if (touchedTodos.size >= 2) {
            (abandoned.size.toFloat() / touchedTodos.size.toFloat()).coerceIn(0f, 1f)
        } else if (touchedTodos.isEmpty()) {
            // Contexts without tasks: fall back to how much of the day was left unfinished.
            if (events.size >= 3) 0.5f else 0f
        } else 0f

        val index = (
            100.0f * (
                SwitchCostThresholds.WEIGHT_FREQUENCY.toFloat() * frequency +
                    SwitchCostThresholds.WEIGHT_ABANDONED.toFloat() * abandonedShare
                )
            ).toInt().coerceIn(0, 100)

        val (postSwitch, baseline) = productivityAroundSwitches(
            events = events,
            blocks = blocks,
            dayStartMillis = dayStartMillis,
            dayEndMillis = dayEndMillis
        )

        return SwitchReport(
            switchCount = events.size,
            switchesPerHour = switchesPerHour,
            abandonedTaskCount = abandoned.size,
            abandonedTodoIds = abandoned,
            averageResumeMinutes = averageResume,
            postSwitchProductivity = postSwitch,
            baselineProductivity = baseline,
            hallucinationIndex = index
        )
    }

    /**
     * Focus share in the minutes after each switch versus the same day away
     * from those windows. Both sides are null until there are enough switches
     * to make the comparison worth saying out loud.
     */
    private fun productivityAroundSwitches(
        events: List<SwitchEvent>,
        blocks: List<SwitchBlock>,
        dayStartMillis: Long,
        dayEndMillis: Long
    ): Pair<Float?, Float?> {
        if (events.size < thresholds.MIN_SWITCHES_FOR_RECOVERY) return null to null

        val windowMillis = (thresholds.RECOVERY_WINDOW_MINUTES * 60_000.0).toLong()
        val windows = mergeRanges(
            events.map {
                maxOf(dayStartMillis, it.atMillis) until minOf(dayEndMillis, it.atMillis + windowMillis)
            }.filter { !it.isEmpty() }
        )

        var windowTracked = 0L
        var windowFocused = 0L
        var outsideTracked = 0L
        var outsideFocused = 0L

        for (block in blocks) {
            val blockStart = maxOf(dayStartMillis, block.startMillis)
            val blockEnd = minOf(dayEndMillis, block.endMillis)
            val blockLength = (blockEnd - blockStart).coerceAtLeast(0L)
            if (blockLength == 0L) continue

            val inside = windows.sumOf { overlap ->
                val from = maxOf(overlap.first, blockStart)
                val to = minOf(overlap.last + 1, blockEnd)
                (to - from).coerceAtLeast(0L)
            }
            val outside = blockLength - inside

            windowTracked += inside
            if (block.isFocused) windowFocused += inside
            outsideTracked += outside
            if (block.isFocused) outsideFocused += outside
        }

        val post = if (windowTracked > 0L) windowFocused.toFloat() / windowTracked else null
        val base = if (outsideTracked > 0L) outsideFocused.toFloat() / outsideTracked else null
        return post to base
    }

    /** Folds overlapping ranges so a minute is never counted twice. */
    private fun mergeRanges(ranges: List<LongRange>): List<LongRange> {
        if (ranges.isEmpty()) return emptyList()
        val sorted = ranges.sortedBy { it.first }
        val merged = mutableListOf<LongRange>()
        var current = sorted.first()
        for (range in sorted.drop(1)) {
            current = if (range.first <= current.last + 1) {
                current.first..maxOf(current.last, range.last)
            } else {
                merged += current
                range
            }
        }
        merged += current
        return merged
    }
}
