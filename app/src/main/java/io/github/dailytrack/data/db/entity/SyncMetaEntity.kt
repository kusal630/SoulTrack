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
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Device-to-device sync bookkeeping. The row is keyed by a stable [syncId]
 * that is shared by every paired device, so a record keeps the same identity
 * even though Room's auto-increment id is local to one installation.
 *
 * [hlc] is a hybrid logical clock, not wall-clock time. It gives a total,
 * monotonic order for last-writer-wins merges without depending on the two
 * devices agreeing what time it is.
 */
@Entity(
    tableName = "sync_meta",
    indices = [
        Index(value = ["entityType", "localId"]),
        Index("entityType"),
        Index("naturalKey")
    ]
)
data class SyncMetaEntity(
    @PrimaryKey val syncId: String,
    val entityType: String,
    /** Room row id on this device. May point at a deleted row for tombstones. */
    val localId: Long,
    val hlc: String,
    val payloadHash: String,
    val naturalKey: String = "",
    val deleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
