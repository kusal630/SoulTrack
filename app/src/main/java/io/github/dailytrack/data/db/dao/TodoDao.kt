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
import io.github.dailytrack.data.db.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY priority DESC, deadline ASC, createdAt DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos ORDER BY priority DESC, deadline ASC, createdAt DESC")
    suspend fun getAllTodosSync(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY priority DESC, deadline ASC, createdAt DESC")
    fun getActiveTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 AND triggerHour = :hour ORDER BY priority DESC, deadline ASC")
    suspend fun getTodosWithTriggerHour(hour: Int): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE isCompleted = 1 AND completedAt >= :start ORDER BY completedAt DESC")
    suspend fun getCompletedTodosSince(start: Long): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 AND categoryId = :categoryId ORDER BY priority DESC, deadline ASC")
    fun getActiveTodosByCategory(categoryId: Long): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 AND priority >= :minPriority ORDER BY priority DESC, deadline ASC")
    fun getActiveTodosByMinPriority(minPriority: Int): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE deadline IS NOT NULL AND deadline > :now AND isCompleted = 0 ORDER BY deadline ASC")
    fun getPendingTodosWithDeadline(now: Long): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE deadline IS NOT NULL AND deadline <= :deadline AND deadline > :now AND isCompleted = 0")
    suspend fun getTodosWithDeadlineApproaching(now: Long, deadline: Long): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: Long): TodoEntity?

    @Insert
    suspend fun insert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)

    @Delete
    suspend fun delete(todo: TodoEntity)

    @Query("UPDATE todos SET isCompleted = 1, completedAt = :completedAt WHERE id = :id")
    suspend fun completeTodo(id: Long, completedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 0")
    fun getActiveTodoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 1")
    fun getCompletedTodoCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 0 AND createdAt >= :dayStart AND createdAt < :dayEnd")
    fun getTodayActiveTodoCount(dayStart: Long, dayEnd: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 1 AND completedAt >= :dayStart AND completedAt < :dayEnd")
    fun getTodayCompletedTodoCount(dayStart: Long, dayEnd: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 0 AND priority = 3")
    fun getHighPriorityCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 0 AND deadline IS NOT NULL AND deadline < :now")
    fun getOverdueCount(now: Long): Flow<Int>

    @Query("SELECT SUM(actualMinutes) FROM todos WHERE isCompleted = 1 AND completedAt >= :start AND completedAt < :end")
    suspend fun getTotalCompletedMinutesInRange(start: Long, end: Long): Int?

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 1 AND completedAt >= :start AND completedAt < :end")
    suspend fun getCompletedCountInRange(start: Long, end: Long): Int

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 1 AND completedAt >= :start AND completedAt < :end AND priority = 3")
    suspend fun getHighPriorityCompletedInRange(start: Long, end: Long): Int

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 1 AND completedAt >= :start AND completedAt < :end AND priority = 2")
    suspend fun getMediumPriorityCompletedInRange(start: Long, end: Long): Int

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 1 AND completedAt >= :start AND completedAt < :end AND priority = 1")
    suspend fun getLowPriorityCompletedInRange(start: Long, end: Long): Int

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 0 AND categoryId = :categoryId")
    suspend fun getActiveCountByCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 1 AND categoryId = :categoryId")
    suspend fun getCompletedCountByCategory(categoryId: Long): Int

    @Query("DELETE FROM todos")
    suspend fun deleteAll()
}
