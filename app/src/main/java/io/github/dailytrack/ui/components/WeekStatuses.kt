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


package io.github.dailytrack.ui.components

import io.github.dailytrack.data.db.entity.TodoEntity
import io.github.dailytrack.engine.DaySnapshot
import io.github.dailytrack.engine.DayStatus
import io.github.dailytrack.engine.TimeCoverage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The seven day names the ring and TalkBack both use. */
val WeekDayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** Short labels under the mini rings. */
val WeekDayShortNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Index (0 = Monday) of [date] inside its ISO week. */
fun mondayIndexFor(date: LocalDate): Int = date.dayOfWeek.value - 1

/** The Monday that starts the week containing [date]. */
fun weekStartFor(date: LocalDate): LocalDate = date.minusDays(mondayIndexFor(date).toLong())

/**
 * Builds the seven outer-ring statuses for the week that contains [today].
 *
 * Finished days are classified with [io.github.dailytrack.engine.classifyDay]
 * using the tracked time for that day plus the tasks that were created and
 * completed on it - no new data layer, just the data the app already keeps.
 * Days that have not happened yet read as `null`, today reads as in progress.
 */
fun weekStatusesFor(
    today: LocalDate,
    weeklyReport: Map<LocalDate, TimeCoverage>,
    todos: List<TodoEntity>,
    zone: ZoneId = ZoneId.systemDefault()
): List<DayStatus?> {
    val monday = weekStartFor(today)
    return (0 until 7).map { offset ->
        val day = monday.plusDays(offset.toLong())
        when {
            day.isAfter(today) -> null
            day == today -> DayStatus.IN_PROGRESS
            else -> {
                val snapshot = daySnapshotFor(day, weeklyReport[day], todos, zone)
                io.github.dailytrack.engine.classifyDay(snapshot)
            }
        }
    }
}

/** Day progress (completed / assigned) for one calendar day. */
fun dayProgressFor(
    date: LocalDate,
    todos: List<TodoEntity>,
    zone: ZoneId = ZoneId.systemDefault()
): Float {
    val (assigned, completed) = taskCountsFor(date, todos, zone)
    return io.github.dailytrack.engine.dayProgress(assigned, completed)
}

/** Snapshot feeding [io.github.dailytrack.engine.classifyDay] for one day. */
fun daySnapshotFor(
    date: LocalDate,
    coverage: TimeCoverage?,
    todos: List<TodoEntity>,
    zone: ZoneId = ZoneId.systemDefault()
): DaySnapshot {
    val (assigned, completed) = taskCountsFor(date, todos, zone)
    val plannedFocusSeconds = todos.filter { it.isAssignedOn(date, zone) }
        .sumOf { it.estimatedMinutes.coerceAtLeast(0) } * 60L

    return DaySnapshot(
        trackedSeconds = coverage?.trackedSeconds ?: 0L,
        distractionSeconds = coverage?.wastedSeconds ?: 0L,
        productiveSeconds = coverage?.totalProductiveSeconds ?: 0L,
        plannedFocusSeconds = plannedFocusSeconds,
        assignedTasks = assigned,
        completedTasks = completed
    )
}

private fun taskCountsFor(
    date: LocalDate,
    todos: List<TodoEntity>,
    zone: ZoneId
): Pair<Int, Int> {
    val assigned = todos.count { it.isAssignedOn(date, zone) }
    val completed = todos.count { todo ->
        todo.completedAt != null &&
            LocalDate.ofInstant(Instant.ofEpochMilli(todo.completedAt!!), zone) == date
    }
    return assigned to completed
}

private fun TodoEntity.isAssignedOn(date: LocalDate, zone: ZoneId): Boolean {
    val created = LocalDate.ofInstant(Instant.ofEpochMilli(createdAt), zone)
    val deadlineDate = deadline?.let { LocalDate.ofInstant(Instant.ofEpochMilli(it), zone) }
    return created == date || deadlineDate == date
}
