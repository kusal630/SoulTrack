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
import io.github.dailytrack.data.db.entity.InsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightDao {
    @Query("SELECT * FROM insights ORDER BY createdAt DESC")
    fun getAllInsights(): Flow<List<InsightEntity>>

    /**
     * Live, non-dismissed insights that have not cooled down yet. The cooldown
     * doubles as the expiry, so a card can never sit on the dashboard with
     * copy written for a day that is long gone.
     */
    @Query("SELECT * FROM insights WHERE dismissed = 0 AND cooldownUntil > :now ORDER BY createdAt DESC")
    fun getActiveInsights(now: Long = System.currentTimeMillis()): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insights WHERE title = :title AND dismissed = 0 LIMIT 1")
    suspend fun getUndismissedByTitle(title: String): InsightEntity?

    @Query("SELECT * FROM insights WHERE dismissed = 0 AND severity IN ('WARNING', 'CRITICAL') ORDER BY createdAt DESC")
    fun getWarnings(): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insights WHERE category = :category AND dismissed = 0 ORDER BY createdAt DESC")
    fun getInsightsByCategory(category: String): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insights WHERE title = :title AND cooldownUntil > :now AND dismissed = 0")
    suspend fun getActiveInsightByTitle(title: String, now: Long): InsightEntity?

    @Insert
    suspend fun insert(insight: InsightEntity): Long

    @Update
    suspend fun update(insight: InsightEntity)

    @Query("UPDATE insights SET dismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: Long)

    @Query("DELETE FROM insights WHERE dismissed = 1")
    suspend fun deleteDismissed()

    @Query("DELETE FROM insights")
    suspend fun deleteAll()
}
