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


package io.github.dailytrack.data.db.dao

import androidx.room.*
import io.github.dailytrack.data.db.entity.ParkedThoughtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkedThoughtDao {

    @Query("SELECT * FROM parked_thoughts WHERE resolvedAt IS NULL ORDER BY createdAt DESC")
    fun getUnresolved(): Flow<List<ParkedThoughtEntity>>

    @Query("SELECT COUNT(*) FROM parked_thoughts WHERE resolvedAt IS NULL")
    fun getUnresolvedCount(): Flow<Int>

    @Insert
    suspend fun insert(thought: ParkedThoughtEntity): Long

    @Query("UPDATE parked_thoughts SET resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun resolve(id: Long, resolvedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM parked_thoughts WHERE id = :id")
    suspend fun delete(id: Long)
}
