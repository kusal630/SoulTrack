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

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val categoryId: Long? = null,
    val deadline: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val estimatedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val pomodoroCount: Int = 0,
    val priority: Int = 0,
    /**
     * Implementation intention ("if-then plan"): the situation/cue that should
     * trigger this task, e.g. "after I open my laptop at my desk". Empty when
     * the user has not planned it. Cue + response planning has strong evidence
     * for closing the intention-action gap (Gollwitzer & Sheeran, d = .65).
     */
    val triggerText: String = "",
    /** Optional hour of day (0-23) to nudge the cue reminder; -1 = none. */
    val triggerHour: Int = -1,
    /**
     * Coping plan ("if I can't start, I'll ..."). Naming a fallback for the
     * moment a plan meets friction is the part of implementation-intention
     * research that keeps intentions alive under difficulty (Gollwitzer, 1999;
     * Gollwitzer & Sheeran, 2006).
     */
    val obstacleText: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
