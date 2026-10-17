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
import io.github.dailytrack.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE startTime < :dayEnd AND (endTime IS NULL OR endTime > :dayStart) ORDER BY startTime")
    fun getSessionsForDay(dayStart: Long, dayEnd: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startTime < :dayEnd AND (endTime IS NULL OR endTime > :dayStart) ORDER BY startTime")
    suspend fun getSessionsForDaySync(dayStart: Long, dayEnd: Long): List<SessionEntity>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    suspend fun getAllSessionsSync(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE isActive = 1 LIMIT 1")
    fun getActiveSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSessionSync(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE startTime < :end AND (endTime IS NULL OR endTime > :start) ORDER BY startTime")
    fun getSessionsInRange(start: Long, end: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startTime < :end AND (endTime IS NULL OR endTime > :start) ORDER BY startTime")
    suspend fun getSessionsInRangeSync(start: Long, end: Long): List<SessionEntity>

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("UPDATE sessions SET isActive = 0, endTime = :now, updatedAt = :now WHERE isActive = 1")
    suspend fun deactivateAllSessions(now: Long = System.currentTimeMillis())

    @Query("UPDATE sessions SET isActive = 0, endTime = :now, updatedAt = :now WHERE isActive = 1")
    suspend fun stopActiveSession(now: Long = System.currentTimeMillis())

    @Query("UPDATE sessions SET isActive = 0, endTime = :now, updatedAt = :now WHERE id = :sessionId AND isActive = 1")
    suspend fun stopSessionById(sessionId: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM sessions WHERE endTime IS NOT NULL AND endTime >= :start ORDER BY startTime DESC")
    suspend fun getCompletedSessionsSince(start: Long): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE startTime < :end AND (endTime IS NULL OR endTime > :start) ORDER BY startTime")
    suspend fun getSessionsBetweenSync(start: Long, end: Long): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE categoryId = :categoryId AND startTime >= :start AND startTime < :end ORDER BY startTime")
    fun getSessionsByCategoryForDay(categoryId: Long, start: Long, end: Long): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}
