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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.dailytrack.data.db.entity.SyncMetaEntity

@Dao
interface SyncMetaDao {
    @Query("SELECT * FROM sync_meta")
    suspend fun getAll(): List<SyncMetaEntity>

    @Query("SELECT * FROM sync_meta WHERE entityType = :entityType")
    suspend fun getForType(entityType: String): List<SyncMetaEntity>

    @Query("SELECT * FROM sync_meta WHERE syncId = :syncId LIMIT 1")
    suspend fun getById(syncId: String): SyncMetaEntity?

    @Query("SELECT * FROM sync_meta WHERE entityType = :entityType AND localId = :localId LIMIT 1")
    suspend fun getByLocalId(entityType: String, localId: Long): SyncMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: SyncMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(metas: List<SyncMetaEntity>)

    @Query("DELETE FROM sync_meta WHERE syncId = :syncId")
    suspend fun deleteById(syncId: String)

    @Query("DELETE FROM sync_meta WHERE deleted = 1 AND updatedAt < :before")
    suspend fun pruneTombstones(before: Long)
}
