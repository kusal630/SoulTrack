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

/**
 * A distractor the user "parked" instead of chasing it.
 *
 * Writing an unfulfilled goal or intrusive thought down frees the cognitive
 * resources it was holding, so attention returns to the task at hand
 * (Masicampo & Baumeister, 2011). This table is that parking lot.
 */
@Entity(tableName = "parked_thoughts")
data class ParkedThoughtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)
