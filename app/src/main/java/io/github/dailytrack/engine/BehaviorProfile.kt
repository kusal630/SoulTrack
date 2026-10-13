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

/**
 * "At which position of the day does this person actually behave which way."
 *
 * Built entirely from the user's own sessions and task completions, so it is
 * a behavioural model of one person rather than a generic chronotype. On a
 * fresh install there is no data: [coldStart] returns a flat prior and every
 * consumer must stay correct with it (that is the out-of-the-box requirement).
 */
class BehaviorProfile private constructor(
    private val hourTypeMinutes: Map<Int, Map<String, Long>>,
    private val hourCompletions: Map<Int, Map<String, Int>>,
    private val hourIntent: Map<Int, Map<String, Int>>,
    private val typeCompletions: Map<String, Int>,
    private val typeOpen: Map<String, Int>,
    val sampleSize: Int
) {

    fun hasData(): Boolean = sampleSize > 0

    /** Share of the observed activity at [hour] that belongs to [type] (0..1). */
    fun hourAffinity(hour: Int, type: String): Double {
        val byType = hourTypeMinutes[hour]
        val total = byType?.values?.sum() ?: 0L
        if (total <= 0L) return 0.5
        val mine = byType?.get(type) ?: 0L
        val share = mine.toDouble() / total.toDouble()
        // Shrink thin hours towards the 0.5 prior so one 5 minute session
        // does not convince the model that a whole hour belongs to it.
        val confidence = (total.toDouble() / (total.toDouble() + 30.0)).coerceIn(0.0, 1.0)
        return confidence * share + (1.0 - confidence) * 0.5
    }

    /** Overall completion rate for a task type (0..1). */
    fun completionRate(type: String): Double {
        val done = typeCompletions[type] ?: 0
        val open = typeOpen[type] ?: 0
        if (done + open == 0) return 0.5
        return (done + 1.0) / (done + open + 2.0)
    }

    /** Completion rate for tasks of [type] finished around [hour] (0..1). */
    fun hourlyCompletionRate(hour: Int, type: String): Double {
        val done = hourCompletions[hour]?.get(type) ?: 0
        val started = hourIntent[hour]?.get(type) ?: 0
        if (done + started == 0) return 0.5
        return (done + 1.0) / (done + started + 2.0)
    }

    /** The hours where this person most often does [type]. */
    fun typicalHours(type: String, topN: Int = 3): List<Int> =
        hourTypeMinutes.entries
            .mapNotNull { (hour, byType) -> byType[type]?.let { hour to it } }
            .sortedByDescending { it.second }
            .take(topN)
            .map { it.first }

    /** True when the user is historically active in [type] at [hour]. */
    fun isNaturalHour(hour: Int, type: String): Boolean {
        if (!hasData()) return true
        val rank = typicalHours(type, 6)
        return rank.isEmpty() || hour in rank
    }

    fun describe(hour: Int, type: String): String? {
        if (!hasData()) return null
        val affinity = hourAffinity(hour, type)
        return when {
            affinity >= 0.66 -> "you usually do this kind of work around this hour"
            affinity <= 0.34 -> "this hour is rarely spent on this kind of work"
            else -> null
        }
    }

    companion object {
        fun coldStart(): BehaviorProfile =
            BehaviorProfile(emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), 0)

        fun of(
            sessionMinutesByHourAndType: Map<Int, Map<String, Long>>,
            completionsByHourAndType: Map<Int, Map<String, Int>>,
            intentByHourAndType: Map<Int, Map<String, Int>>,
            typeCompletions: Map<String, Int>,
            typeOpen: Map<String, Int>,
            sampleSize: Int
        ): BehaviorProfile = BehaviorProfile(
            hourTypeMinutes = sessionMinutesByHourAndType,
            hourCompletions = completionsByHourAndType,
            hourIntent = intentByHourAndType,
            typeCompletions = typeCompletions,
            typeOpen = typeOpen,
            sampleSize = sampleSize
        )

        fun hourOf(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Int =
            Instant.ofEpochMilli(epochMillis).atZone(zone).hour

        @Suppress("ReturnCount")
        fun hourOfDayOf(epochMillis: Long): Int = hourOf(epochMillis)
    }
}
