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

import java.time.Instant
import java.time.ZoneId
import kotlin.math.max

data class TaskCandidate(
    val id: Long,
    val title: String,
    val priority: Int = 0,
    val deadline: Long? = null,
    val estimatedMinutes: Int = 0,
    val categoryType: String = "ACTIVITY",
    val categoryId: Long? = null,
    val createdAt: Long = 0L
) {
    /** High focus work: important, long, or in a deep-work category. */
    val requiresDeepFocus: Boolean
        get() = priority >= 3 ||
            (estimatedMinutes >= 30) ||
            categoryType == "LEARNING" ||
            categoryType == "PRODUCTIVE"
}

data class BrainInputs(
    val now: Long,
    val candidates: List<TaskCandidate>,
    val focusability: Focusability,
    val behavior: BehaviorProfile,
    val mood: Int? = null,
    val energy: Int? = null,
    val zone: ZoneId = ZoneId.systemDefault()
) {
    val hourOfDay: Int get() = Instant.ofEpochMilli(now).atZone(zone).hour
    val dayOfWeek: Int get() = Instant.ofEpochMilli(now).atZone(zone).dayOfWeek.value
}

data class Recommendation(
    val task: TaskCandidate,
    val score: Double,
    val reason: String,
    val deepFocus: Boolean
)

data class BrainResult(
    val primary: Recommendation?,
    val alternatives: List<Recommendation>,
    val focusability: Focusability,
    val totalConsidered: Int
) {
    companion object {
        fun empty(focusability: Focusability) =
            BrainResult(null, emptyList(), focusability, 0)
    }
}

/**
 * The decision layer: picks the one task to do right now.
 *
 * Design rules that come straight from the product spec:
 *  - it has to be useful with **zero** training history (cold start), so every
 *    signal falls back to a sane prior instead of returning "no data";
 *  - priority and deadline are hard weights, the model can never bury an
 *    urgent high priority task under a clever but irrelevant pick;
 *  - the answer always carries a human readable reason, never a verdict.
 */
class BrainLayer {

    fun recommend(inputs: BrainInputs): BrainResult {
        val focusability = inputs.focusability
        if (inputs.candidates.isEmpty()) return BrainResult.empty(focusability)

        val hour = inputs.hourOfDay
        val ranked = inputs.candidates
            .map { task -> score(task, inputs, hour) }
            .sortedByDescending { it.score }

        val primary = ranked.firstOrNull()
        return BrainResult(
            primary = primary,
            alternatives = ranked.drop(1).take(3),
            focusability = focusability,
            totalConsidered = ranked.size
        )
    }

    /** Exposed for tests: the same numbers the UI shows. */
    fun score(task: TaskCandidate, inputs: BrainInputs, hour: Int): Recommendation {
        val urgencyScore = urgencyScore(task, inputs.now)
        val priorityScore = priorityScore(task)
        val focusFitScore = focusFitScore(task, inputs.focusability)
        val behaviorScore = behaviorScore(task, inputs.behavior, hour)
        val historyScore = historyScore(task, inputs.behavior, hour)
        val effortScore = effortScore(task, inputs.focusability)

        var total = W_URGENCY * urgencyScore +
            W_PRIORITY * priorityScore +
            W_FOCUS_FIT * focusFitScore +
            W_BEHAVIOR * behaviorScore +
            W_HISTORY * historyScore +
            W_EFFORT * effortScore

        // Hard weight: a deadline that is close is never buried by the model.
        val hoursLeft = task.deadline?.let { (it - inputs.now) / 3_600_000.0 }
        if (hoursLeft != null && hoursLeft <= 24.0) {
            total = max(total, urgencyScore)
        }
        if (task.priority >= 3 && hoursLeft != null && hoursLeft <= 24.0) {
            total = max(total, 95.0)
        }

        return Recommendation(
            task = task,
            score = total.coerceIn(0.0, 100.0),
            reason = explain(task, inputs, hour, urgencyScore, focusFitScore, behaviorScore),
            deepFocus = task.requiresDeepFocus && inputs.focusability.band != FocusBand.LOW
        )
    }

    private fun urgencyScore(task: TaskCandidate, now: Long): Double {
        val deadline = task.deadline ?: return 25.0
        val hoursLeft = (deadline - now) / 3_600_000.0
        return when {
            hoursLeft < 0 -> 100.0
            hoursLeft <= 1 -> 96.0
            hoursLeft <= 6 -> 90.0
            hoursLeft <= 24 -> 82.0
            hoursLeft <= 72 -> 66.0
            hoursLeft <= 168 -> 50.0
            else -> 34.0
        }
    }

    private fun priorityScore(task: TaskCandidate): Double =
        (task.priority.coerceIn(0, 3) / 3.0) * 100.0

    private fun focusFitScore(task: TaskCandidate, focus: Focusability): Double {
        val deep = task.requiresDeepFocus
        return when (focus.band) {
            FocusBand.HIGH -> if (deep) 100.0 else 55.0
            FocusBand.MEDIUM -> if (deep) 78.0 else 72.0
            FocusBand.LOW -> if (deep) 25.0 else 100.0
        }
    }

    private fun behaviorScore(task: TaskCandidate, behavior: BehaviorProfile, hour: Int): Double {
        val affinity = behavior.hourAffinity(hour, task.categoryType)
        val natural = behavior.isNaturalHour(hour, task.categoryType)
        return (affinity * 70.0) + (if (natural) 30.0 else 0.0)
    }

    private fun historyScore(task: TaskCandidate, behavior: BehaviorProfile, hour: Int): Double {
        val overall = behavior.completionRate(task.categoryType)
        val hourly = behavior.hourlyCompletionRate(hour, task.categoryType)
        return ((overall + hourly) / 2.0) * 100.0
    }

    private fun effortScore(task: TaskCandidate, focus: Focusability): Double {
        val minutes = task.estimatedMinutes
        if (minutes <= 0) return 60.0
        return when (focus.band) {
            FocusBand.HIGH -> when {
                minutes >= 30 -> 100.0
                minutes >= 15 -> 80.0
                else -> 65.0
            }
            FocusBand.MEDIUM -> if (minutes in 15..60) 100.0 else 70.0
            FocusBand.LOW -> when {
                minutes <= 15 -> 100.0
                minutes <= 30 -> 70.0
                else -> 35.0
            }
        }
    }

    private fun explain(
        task: TaskCandidate,
        inputs: BrainInputs,
        hour: Int,
        urgency: Double,
        focusFit: Double,
        behavior: Double
    ): String {
        val parts = mutableListOf<String>()
        val hoursLeft = task.deadline?.let { (it - inputs.now) / 3_600_000.0 }
        when {
            hoursLeft != null && hoursLeft < 0 -> parts.add("overdue")
            hoursLeft != null && hoursLeft <= 24 ->
                parts.add(if (hoursLeft <= 1) "due within the hour" else "due in ${hoursLeft.toInt()}h")
            task.priority >= 3 -> parts.add("high priority")
        }

        when (inputs.focusability.band) {
            FocusBand.HIGH -> parts.add("quiet, focused moment - fits deep work")
            FocusBand.LOW -> parts.add("low focus right now - this one is light")
            FocusBand.MEDIUM -> Unit
        }

        inputs.behavior.describe(hour, task.categoryType)?.let { parts.add(it) }
        inputs.focusability.reasons.firstOrNull()?.let { parts.add(it) }

        if (parts.isEmpty()) parts.add("solid next step")
        return parts.take(3).joinToString(", ").replaceFirstChar { it.uppercase() }
    }

    companion object {
        private const val W_URGENCY = 0.30
        private const val W_PRIORITY = 0.25
        private const val W_FOCUS_FIT = 0.20
        private const val W_BEHAVIOR = 0.10
        private const val W_HISTORY = 0.10
        private const val W_EFFORT = 0.05
    }
}
