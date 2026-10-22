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


package io.github.dailytrack.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val todoId: Long? = null,
    val categoryId: Long? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val durationMinutes: Int = 25,
    /** Planned length of the cycle, kept when a cycle is stopped early. */
    val plannedMinutes: Int = 0,
    val type: String = "WORK",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
