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
 * Builds the short weekly progress line the app reports back to the user.
 *
 * Monitoring progress is one of the better-evidenced self-regulation
 * techniques: across 138 studies it raised goal attainment with d+ = 0.40, and
 * the effect was strongest where progress was physically recorded and reported
 * back (Harkin et al., 2015). The wording stays factual rather than
 * pressuring, because controlling feedback can undercut intrinsic motivation
 * (Deci, Koestner & Ryan, 1999).
 */
object WeeklyReview {

    /** e.g. "8h 30m" for 510 minutes; "0m" for zero or negative input. */
    fun formatDuration(minutes: Int): String {
        val safe = minutes.coerceAtLeast(0)
        val hours = safe / 60
        val mins = safe % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    /**
     * A one-line digest. Returns a gentle "quiet week" line when nothing was
     * recorded, so the user is never shown an empty or shaming summary.
     */
    fun digestLine(
        completedTasks: Int,
        productiveMinutes: Int,
        activeDays: Int
    ): String {
        val tasks = completedTasks.coerceAtLeast(0)
        val days = activeDays.coerceIn(0, 7)
        return when {
            tasks == 0 && productiveMinutes <= 0 ->
                "Quiet week so far - log a little focus time to see your trend."
            tasks == 0 ->
                "You focused ${formatDuration(productiveMinutes)} across $days ${dayWord(days)} this week."
            productiveMinutes <= 0 ->
                "You finished $tasks ${taskWord(tasks)} this week. Add some focus time to round it out."
            else ->
                "This week: $tasks ${taskWord(tasks)} done and ${formatDuration(productiveMinutes)} focused across $days ${dayWord(days)}."
        }
    }

    /** A short, non-pressuring follow-up nudge. */
    fun reviewNudge(completedTasks: Int, productiveMinutes: Int): String = when {
        completedTasks >= 5 || productiveMinutes >= 300 ->
            "Momentum is real. Pick the one thing that matters most for next week."
        completedTasks == 0 && productiveMinutes <= 0 ->
            "Name one small task for tomorrow - starting beats planning."
        else ->
            "Glance at what worked and set up tomorrow's first cue."
    }

    private fun taskWord(n: Int) = if (n == 1) "task" else "tasks"
    private fun dayWord(n: Int) = if (n == 1) "day" else "days"
}
