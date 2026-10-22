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
import io.github.dailytrack.data.db.entity.PomodoroSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroSessionDao {
    @Query("SELECT * FROM pomodoro_sessions ORDER BY startTime DESC")
    fun getAllPomodoros(): Flow<List<PomodoroSessionEntity>>

    @Query("SELECT * FROM pomodoro_sessions ORDER BY startTime DESC")
    suspend fun getAllPomodorosSync(): List<PomodoroSessionEntity>

    @Query("SELECT * FROM pomodoro_sessions WHERE startTime >= :start AND startTime < :end ORDER BY startTime")
    fun getPomodorosForDay(start: Long, end: Long): Flow<List<PomodoroSessionEntity>>

    @Query("SELECT * FROM pomodoro_sessions WHERE startTime >= :start AND startTime < :end ORDER BY startTime")
    suspend fun getPomodorosForDaySync(start: Long, end: Long): List<PomodoroSessionEntity>

    @Query("SELECT * FROM pomodoro_sessions WHERE startTime >= :start AND startTime < :end ORDER BY startTime")
    suspend fun getPomodorosBetween(start: Long, end: Long): List<PomodoroSessionEntity>

    @Query("SELECT * FROM pomodoro_sessions WHERE todoId = :todoId ORDER BY startTime DESC")
    fun getPomodorosForTodo(todoId: Long): Flow<List<PomodoroSessionEntity>>

    @Query("SELECT * FROM pomodoro_sessions WHERE id = :id")
    suspend fun getPomodoroById(id: Long): PomodoroSessionEntity?

    @Query("SELECT * FROM pomodoro_sessions WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    fun getActivePomodoro(): Flow<PomodoroSessionEntity?>

    @Query("SELECT * FROM pomodoro_sessions WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActivePomodoroSync(): PomodoroSessionEntity?

    @Query("SELECT * FROM pomodoro_sessions WHERE isCompleted = 0")
    suspend fun getAllActivePomodorosSync(): List<PomodoroSessionEntity>

    @Insert
    suspend fun insert(pomodoro: PomodoroSessionEntity): Long

    @Update
    suspend fun update(pomodoro: PomodoroSessionEntity)

    @Delete
    suspend fun delete(pomodoro: PomodoroSessionEntity)

    /**
     * Closing a pomodoro always rewrites [PomodoroSessionEntity.durationMinutes]
     * to the minutes actually spent, so stopping a 25 minute block after five
     * minutes records five minutes - never the plan.
     */
    @Query(
        "UPDATE pomodoro_sessions SET endTime = :endTime, isCompleted = :isCompleted, " +
            "durationMinutes = MAX(1, CAST((:endTime - startTime) / 60000 AS INTEGER)) WHERE id = :id"
    )
    suspend fun completePomodoro(id: Long, endTime: Long, isCompleted: Boolean = true)

    @Query(
        "UPDATE pomodoro_sessions SET endTime = :endTime, isCompleted = 1, " +
            "durationMinutes = MAX(1, CAST((:endTime - startTime) / 60000 AS INTEGER)) WHERE isCompleted = 0"
    )
    suspend fun completeActiveSession(endTime: Long = System.currentTimeMillis())

    @Query("SELECT SUM(durationMinutes) FROM pomodoro_sessions WHERE type = 'WORK' AND isCompleted = 1 AND startTime >= :start AND startTime < :end")
    suspend fun getTotalWorkMinutesInRange(start: Long, end: Long): Int?

    @Query("SELECT SUM(durationMinutes) FROM pomodoro_sessions WHERE type = 'BREAK' AND isCompleted = 1 AND startTime >= :start AND startTime < :end")
    suspend fun getTotalBreakMinutesInRange(start: Long, end: Long): Int?

    @Query("SELECT COUNT(*) FROM pomodoro_sessions WHERE type = 'WORK' AND isCompleted = 1 AND startTime >= :start AND startTime < :end")
    suspend fun getCompletedPomodoroCountInRange(start: Long, end: Long): Int

    @Query("DELETE FROM pomodoro_sessions")
    suspend fun deleteAll()
}
