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
import io.github.dailytrack.data.db.entity.TodoEntity
import java.time.Instant
import java.time.ZoneId

/**
 * Turns raw sessions and todos into the per-person behavioural model the
 * [BrainLayer] reasons with.
 *
 * Everything is derived locally on device; nothing leaves the phone.
 */
object BehaviorProfiler {

    private const val HISTORY_DAYS = 45

    fun build(
        sessions: List<SessionEntity>,
        todos: List<TodoEntity>,
        categories: Map<Long, CategoryEntity>,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): BehaviorProfile {
        if (sessions.isEmpty() && todos.isEmpty()) return BehaviorProfile.coldStart()

        val cutoff = now - HISTORY_DAYS * 24L * 60L * 60L * 1000L

        val hourTypeMinutes = mutableMapOf<Int, MutableMap<String, Long>>()
        var sessionSamples = 0

        sessions.forEach { session ->
            val start = session.startTime
            if (start < cutoff) return@forEach
            val end = session.endTime ?: now
            val minutes = ((end - start) / 60_000L).coerceAtLeast(1L)
            val hour = Instant.ofEpochMilli(start).atZone(zone).hour
            val type = resolveType(session, categories)
            hourTypeMinutes.getOrPut(hour) { mutableMapOf() }
                .merge(type, minutes, Long::plus)
            sessionSamples++
        }

        val hourCompletions = mutableMapOf<Int, MutableMap<String, Int>>()
        val hourIntent = mutableMapOf<Int, MutableMap<String, Int>>()
        val typeCompletions = mutableMapOf<String, Int>()
        val typeOpen = mutableMapOf<String, Int>()
        var todoSamples = 0

        todos.forEach { todo ->
            val type = resolveType(todo.categoryId, categories)
            if (todo.isCompleted && todo.completedAt != null && todo.completedAt >= cutoff) {
                val hour = Instant.ofEpochMilli(todo.completedAt).atZone(zone).hour
                hourCompletions.getOrPut(hour) { mutableMapOf() }
                    .merge(type, 1, Int::plus)
                typeCompletions.merge(type, 1, Int::plus)
                todoSamples++
            } else if (!todo.isCompleted && todo.createdAt >= cutoff) {
                val hour = Instant.ofEpochMilli(todo.createdAt).atZone(zone).hour
                hourIntent.getOrPut(hour) { mutableMapOf() }
                    .merge(type, 1, Int::plus)
                typeOpen.merge(type, 1, Int::plus)
                todoSamples++
            }
        }

        if (sessionSamples == 0 && todoSamples == 0) return BehaviorProfile.coldStart()

        return BehaviorProfile.of(
            sessionMinutesByHourAndType = hourTypeMinutes.mapValues { it.value.toMap() },
            completionsByHourAndType = hourCompletions.mapValues { it.value.toMap() },
            intentByHourAndType = hourIntent.mapValues { it.value.toMap() },
            typeCompletions = typeCompletions,
            typeOpen = typeOpen,
            sampleSize = sessionSamples + todoSamples
        )
    }

    private fun resolveType(session: SessionEntity, categories: Map<Long, CategoryEntity>): String {
        val fromCategory = session.categoryId?.let { categories[it]?.type }
        return normalize(fromCategory ?: session.type)
    }

    private fun resolveType(categoryId: Long?, categories: Map<Long, CategoryEntity>): String =
        normalize(categoryId?.let { categories[it]?.type } ?: "ACTIVITY")

    private fun normalize(type: String?): String = when (type) {
        null, "", "ACTIVITY" -> "ACTIVITY"
        "REST" -> "RECOVERY"
        else -> type
    }
}
