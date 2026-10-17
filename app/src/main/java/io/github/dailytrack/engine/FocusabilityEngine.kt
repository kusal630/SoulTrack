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
 * How focusable the current moment is, 0-100.
 *
 * Every input is optional on purpose: with no history, no microphone and no
 * mood check-in the engine still returns a sensible curve-driven score, which
 * is what makes the layer work on a cold start.
 */
data class FocusContext(
    val hourOfDay: Int,
    val dayOfWeek: Int = 0,
    /** Minutes since the last break or finished session. Null = unknown. */
    val minutesSinceBreak: Int? = null,
    /** Minutes already spent in the currently running session. */
    val minutesInCurrentSession: Int? = null,
    /** Ambient quietness 0-100 (100 = silent). Null = mic not available. */
    val quietness: Int? = null,
    /** Self reported mood 1-5. */
    val mood: Int? = null,
    /** Self reported energy 1-5. */
    val energy: Int? = null
)

enum class FocusBand { LOW, MEDIUM, HIGH }

data class Focusability(
    val score: Int,
    val band: FocusBand,
    val reasons: List<String>
)

class FocusabilityEngine {

    fun evaluate(
        ctx: FocusContext,
        behavior: BehaviorProfile = BehaviorProfile.coldStart()
    ): Focusability {
        var score = 60.0
        val reasons = mutableListOf<String>()

        val (timeScore, timeReason) = timeOfDayScore(ctx.hourOfDay, behavior)
        score += timeScore
        if (timeReason != null) reasons.add(timeReason)

        ctx.minutesSinceBreak?.let { since ->
            when {
                since < 10 -> { score += 8.0; reasons.add("fresh from a break") }
                since > 120 -> { score -= 15.0; reasons.add("no break in over two hours") }
                since > 90 -> { score -= 8.0; reasons.add("long stretch without a break") }
                else -> { score += 4.0; reasons.add("good rhythm since your last break") }
            }
        }

        ctx.minutesInCurrentSession?.let { running ->
            when {
                running > 150 -> { score -= 18.0; reasons.add("deep session running long") }
                running > 90 -> { score -= 8.0; reasons.add("focus fades after 90 minutes") }
                running > 0 -> { score += 5.0; reasons.add("already in the flow") }
            }
        }

        ctx.quietness?.let { quiet ->
            when {
                quiet >= 78 -> { score += 15.0; reasons.add("very quiet environment") }
                quiet >= 55 -> { score += 5.0; reasons.add("environment sounds manageable") }
                quiet >= 35 -> { score -= 8.0; reasons.add("some background noise") }
                else -> { score -= 22.0; reasons.add("noisy environment") }
            }
        }

        val mood = ctx.mood
        val energy = ctx.energy
        when {
            energy != null && energy >= 4 -> { score += 10.0; reasons.add("high energy right now") }
            energy != null && energy <= 2 -> { score -= 14.0; reasons.add("energy is low right now") }
            mood != null && mood >= 4 -> { score += 6.0; reasons.add("you reported feeling good") }
            mood != null && mood <= 2 -> { score -= 8.0; reasons.add("you reported feeling low") }
        }

        // Weekday rhythm: the same hour lands differently across the week.
        // `dayOfWeek` follows ISO (1 = Monday); 0 means "unknown" and is ignored.
        when (ctx.dayOfWeek) {
            1 -> { score -= 3.0; reasons.add("easing into the week") }
            in 2..5 -> { score += 3.0 }
            6, 7 -> { score -= 6.0; reasons.add("weekend pace") }
            else -> Unit
        }

        val clamped = score.coerceIn(0.0, 100.0).toInt()
        return Focusability(
            score = clamped,
            band = when {
                clamped < 40 -> FocusBand.LOW
                clamped < 70 -> FocusBand.MEDIUM
                else -> FocusBand.HIGH
            },
            reasons = if (reasons.isEmpty()) listOf("baseline estimate") else reasons
        )
    }

    /**
     * Circadian curve: strong late morning and early evening, weak in the
     * small hours and after lunch. Works with zero personal history.
     */
    /**
     * The generic circadian curve, re-weighted once the user has their own
     * history: how often they actually did productive work at this hour wins
     * over the population curve, and they only get a reason they recognise.
     */
    private fun timeOfDayScore(
        hour: Int,
        behavior: BehaviorProfile = BehaviorProfile.coldStart()
    ): Pair<Double, String?> {
        val generic = genericTimeOfDayScore(hour)
        if (!behavior.hasData()) return generic

        val affinity = listOf("PRODUCTIVE", "LEARNING", "ACTIVITY")
            .maxOf { behavior.hourAffinity(hour, it) }
        val personal = (affinity - 0.5) * 70.0
        val blended = generic.first * 0.4 + personal * 0.6
        return blended to (behavior.describe(hour, "PRODUCTIVE") ?: generic.second)
    }

    private fun genericTimeOfDayScore(hour: Int): Pair<Double, String?> = when (hour) {
        in 5..6 -> -10.0 to "very early hours"
        in 7..8 -> 5.0 to "warming up for the day"
        in 9..11 -> 18.0 to "peak morning focus"
        12 -> 8.0 to "still a strong hour"
        in 13..14 -> -8.0 to "post-lunch dip"
        in 15..16 -> 4.0 to "focus rebuilding"
        in 17..19 -> 12.0 to "second wind this evening"
        in 20..21 -> 0.0 to null
        in 22..23 -> -12.0 to "winding down for the day"
        else -> -20.0 to "middle of the night"
    }
}
