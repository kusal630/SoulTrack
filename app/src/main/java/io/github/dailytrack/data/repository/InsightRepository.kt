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


package io.github.dailytrack.data.repository

import io.github.dailytrack.data.db.dao.InsightDao
import io.github.dailytrack.data.db.entity.InsightEntity
import kotlinx.coroutines.flow.Flow

class InsightRepository(private val insightDao: InsightDao) {
    fun getAllInsights(): Flow<List<InsightEntity>> = insightDao.getAllInsights()
    fun getActiveInsights(): Flow<List<InsightEntity>> =
        insightDao.getActiveInsights(System.currentTimeMillis())
    fun getWarnings(): Flow<List<InsightEntity>> = insightDao.getWarnings()
    fun getInsightsByCategory(category: String): Flow<List<InsightEntity>> =
        insightDao.getInsightsByCategory(category)

    suspend fun getActiveInsightByTitle(title: String, now: Long): InsightEntity? =
        insightDao.getActiveInsightByTitle(title, now)

    suspend fun insert(insight: InsightEntity): Long = insightDao.insert(insight)
    suspend fun update(insight: InsightEntity) = insightDao.update(insight)
    suspend fun dismiss(id: Long) = insightDao.dismiss(id)
    suspend fun deleteDismissed() = insightDao.deleteDismissed()

    /**
     * Keeps one row per insight title and always stores the newest wording.
     * Cooling down only suppresses a rewrite while the live card is still on
     * screen - once it expires, today's version replaces it instead of
     * leaving yesterday's copy frozen forever.
     */
    suspend fun insertIfNotCoolingDown(insight: InsightEntity): Long {
        val now = System.currentTimeMillis()
        val live = insightDao.getActiveInsightByTitle(insight.title, now)
        if (live != null) return live.id
        val sameTitle = insightDao.getUndismissedByTitle(insight.title)
        return if (sameTitle == null) {
            insightDao.insert(insight)
        } else {
            insightDao.update(
                sameTitle.copy(
                    severity = insight.severity,
                    category = insight.category,
                    message = insight.message,
                    dataEvidence = insight.dataEvidence,
                    actionLabel = insight.actionLabel,
                    timestamp = insight.timestamp,
                    createdAt = insight.createdAt,
                    cooldownUntil = insight.cooldownUntil
                )
            )
            sameTitle.id
        }
    }
}
