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
import io.github.dailytrack.data.db.entity.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE todoId = :todoId ORDER BY sortOrder ASC, createdAt ASC")
    fun getSubtasksForTodo(todoId: Long): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE todoId = :todoId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getSubtasksForTodoSync(todoId: Long): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks")
    suspend fun getAllSubtasksSync(): List<SubtaskEntity>

    @Query("SELECT * FROM subtasks WHERE id = :id")
    suspend fun getById(id: Long): SubtaskEntity?

    @Query("SELECT COUNT(*) FROM subtasks WHERE todoId = :todoId AND isCompleted = 1")
    fun getCompletedCountForTodo(todoId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM subtasks WHERE todoId = :todoId")
    fun getTotalCountForTodo(todoId: Long): Flow<Int>

    @Insert
    suspend fun insert(subtask: SubtaskEntity): Long

    @Insert
    suspend fun insertAll(subtasks: List<SubtaskEntity>)

    @Update
    suspend fun update(subtask: SubtaskEntity)

    @Delete
    suspend fun delete(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE todoId = :todoId")
    suspend fun deleteAllForTodo(todoId: Long)

    @Query("UPDATE subtasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM subtasks")
    suspend fun deleteAll()
}
