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

import io.github.dailytrack.data.db.entity.CategoryEntity
import io.github.dailytrack.data.db.entity.SessionEntity
import java.time.LocalDate
import java.time.ZoneId

data class TimeCoverage(
    val date: LocalDate,
    val trackedSeconds: Long,
    val untrackedSeconds: Long,
    val productiveSeconds: Long,
    val wastedSeconds: Long,
    val neutralSeconds: Long,
    val learningSeconds: Long,
    val exerciseSeconds: Long,
    val sleepSeconds: Long,
    val socialSeconds: Long,
    val recoverySeconds: Long,
    val totalDaySeconds: Long,
    /** Seconds from day start to now (clamped to the day end) - the part of the day that actually happened. */
    val elapsedSeconds: Long = totalDaySeconds
) {
    val trackedRatio: Double
        get() = if (elapsedSeconds > 0) (trackedSeconds.toDouble() / elapsedSeconds).coerceIn(0.0, 1.0) else 0.0

    val productivityRatio: Double
        get() {
            val awakeSeconds = (elapsedSeconds - sleepSeconds).coerceAtLeast(0)
            return if (awakeSeconds > 0) (productiveSeconds.toDouble() / awakeSeconds).coerceIn(0.0, 1.0) else 0.0
        }

    val totalProductiveSeconds: Long
        get() = productiveSeconds + learningSeconds

    /**
     * Waste the day is accountable for: explicitly tracked distraction plus
     * every elapsed minute nobody recorded. Unrecorded time is never free -
     * if it is not written down it did not go to work.
     */
    val totalWastedSeconds: Long
        get() = wastedSeconds + awakeUntrackedSeconds

    /** Elapsed time nobody accounted for (sleep counts until it is tracked). */
    val awakeUntrackedSeconds: Long
        get() = untrackedSeconds.coerceAtLeast(0)

    /** Share of the elapsed day that is accounted waste, 0f..1f. */
    val wasteRatio: Double
        get() = if (elapsedSeconds <= 0) 0.0
        else (totalWastedSeconds.toDouble() / elapsedSeconds).coerceIn(0.0, 1.0)
}

class TimeCoverageEngine {
    fun calculateCoverage(
        sessions: List<SessionEntity>,
        categories: Map<Long, CategoryEntity>,
        date: LocalDate,
        zone: ZoneId,
        dayStartHour: Int = 0,
        now: Long = System.currentTimeMillis()
    ): TimeCoverage {
        val dayStart = date.atStartOfDay(zone).plusHours(dayStartHour.toLong()).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).plusHours(dayStartHour.toLong()).toInstant().toEpochMilli()
        val totalDaySeconds = (dayEnd - dayStart) / 1000
        val elapsedSeconds = ((minOf(now, dayEnd) - dayStart) / 1000).coerceIn(0L, totalDaySeconds)

        val intervals = mutableListOf<Triple<Long, Long, String>>()

        for (session in sessions) {
            val rawEnd = if (session.isActive) now else session.endTime
            if (rawEnd == null) continue

            val sessionStart = maxOf(session.startTime, dayStart)
            val sessionEnd = minOf(rawEnd, dayEnd)
            if (sessionEnd <= sessionStart) continue

            val category = session.categoryId?.let { categories[it] }
            val categoryType = category?.type ?: session.type
            intervals.add(Triple(sessionStart, sessionEnd, categoryType))
        }

        intervals.sortBy { it.first }

        var trackedSeconds = 0L
        var productiveSeconds = 0L
        var wastedSeconds = 0L
        var neutralSeconds = 0L
        var learningSeconds = 0L
        var exerciseSeconds = 0L
        var sleepSeconds = 0L
        var socialSeconds = 0L
        var recoverySeconds = 0L

        var mergedStart = -1L
        var mergedEnd = -1L
        var mergedType = ""

        fun flush() {
            if (mergedStart < 0 || mergedEnd <= mergedStart) return
            val duration = (mergedEnd - mergedStart) / 1000
            trackedSeconds += duration
            when (mergedType) {
                "SLEEP" -> sleepSeconds += duration
                "EXERCISE" -> exerciseSeconds += duration
                "LEARNING" -> learningSeconds += duration
                "SOCIAL" -> socialSeconds += duration
                "RECOVERY", "REST" -> recoverySeconds += duration
                "PRODUCTIVE", "ACTIVITY" -> productiveSeconds += duration
                "WASTED" -> wastedSeconds += duration
                else -> neutralSeconds += duration
            }
        }

        for ((start, end, type) in intervals) {
            if (mergedStart < 0) {
                mergedStart = start
                mergedEnd = end
                mergedType = type
                continue
            }
            if (start <= mergedEnd) {
                val previousLength = mergedEnd - mergedStart
                val newLength = end - start
                if (newLength > previousLength) mergedType = type
                mergedEnd = maxOf(mergedEnd, end)
            } else {
                flush()
                mergedStart = start
                mergedEnd = end
                mergedType = type
            }
        }
        flush()

        // Only the part of the day that already happened can be untracked.
        // The rest of tonight is not "missing", it just has not arrived yet.
        val untrackedSeconds = (elapsedSeconds - trackedSeconds).coerceAtLeast(0)

        return TimeCoverage(
            date = date,
            trackedSeconds = trackedSeconds,
            untrackedSeconds = untrackedSeconds,
            productiveSeconds = productiveSeconds,
            wastedSeconds = wastedSeconds,
            neutralSeconds = neutralSeconds,
            learningSeconds = learningSeconds,
            exerciseSeconds = exerciseSeconds,
            sleepSeconds = sleepSeconds,
            socialSeconds = socialSeconds,
            recoverySeconds = recoverySeconds,
            totalDaySeconds = totalDaySeconds,
            elapsedSeconds = elapsedSeconds
        )
    }
}
