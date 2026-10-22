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


package io.github.dailytrack.sync

import io.github.dailytrack.data.db.AppDatabase
import io.github.dailytrack.data.db.entity.CategoryEntity
import io.github.dailytrack.data.db.entity.PomodoroSessionEntity
import io.github.dailytrack.data.db.entity.SessionEntity
import io.github.dailytrack.data.db.entity.SubtaskEntity
import io.github.dailytrack.data.db.entity.SyncMetaEntity
import io.github.dailytrack.data.db.entity.TodoEntity
import org.json.JSONObject

/**
 * Snapshot-based, conflict-free (last-writer-wins by hybrid clock) store for
 * the data the user actually edits on more than one device.
 *
 * Local changes are detected by diffing a canonical serialisation of each row
 * against the last hash we recorded. That means no DAO call has to remember to
 * touch sync fields, and deletions are detected too. Tombstones are retained
 * so a deletion can never be undone by an offline peer replaying old state.
 */
class P2pSyncStore(
    private val database: AppDatabase,
    private val clock: HlcClock
) {
    private val metaDao = database.syncMetaDao()

    private data class LocalRow(
        val localId: Long,
        val payload: JSONObject,
        val naturalKey: String
    )

    private data class RefMaps(
        val categoryBySyncId: Map<String, Long>,
        val categoryByLocalId: Map<Long, String>,
        val todoBySyncId: Map<String, Long>,
        val todoByLocalId: Map<Long, String>
    )

    suspend fun reconcileLocal() {
        val metas = metaDao.getAll().associateBy { it.syncId }.toMutableMap()

        reconcileType(SyncTypes.CATEGORY, collectCategories(), metas)
        var refs = refMaps(metas)
        reconcileType(SyncTypes.TODO, collectTodos(refs.categoryByLocalId), metas)

        refs = refMaps(metas)
        reconcileType(SyncTypes.SUBTASK, collectSubtasks(refs.todoByLocalId), metas)
        reconcileType(SyncTypes.SESSION, collectSessions(refs.categoryByLocalId, refs.todoByLocalId), metas)
        reconcileType(SyncTypes.POMODORO, collectPomodoros(refs.categoryByLocalId, refs.todoByLocalId), metas)
    }

    suspend fun snapshot(): List<SyncRecord> {
        val metas = metaDao.getAll()
        val refs = refMaps(metas.associateBy { it.syncId })
        val records = ArrayList<SyncRecord>(metas.size)

        for (type in SyncTypes.APPLY_ORDER) {
            for (meta in metas.filter { it.entityType == type }) {
                if (meta.deleted) {
                    records.add(
                        SyncRecord(
                            syncId = meta.syncId,
                            entityType = type,
                            hlc = meta.hlc,
                            naturalKey = meta.naturalKey,
                            deleted = true,
                            payload = null
                        )
                    )
                    continue
                }

                val payload = payloadFor(meta, refs)
                if (payload == null) {
                    val tombstone = meta.copy(
                        deleted = true,
                        payloadHash = "",
                        hlc = clock.next().encode(),
                        updatedAt = System.currentTimeMillis()
                    )
                    metaDao.upsert(tombstone)
                    records.add(
                        SyncRecord(
                            syncId = tombstone.syncId,
                            entityType = type,
                            hlc = tombstone.hlc,
                            naturalKey = tombstone.naturalKey,
                            deleted = true,
                            payload = null
                        )
                    )
                } else {
                    records.add(
                        SyncRecord(
                            syncId = meta.syncId,
                            entityType = type,
                            hlc = meta.hlc,
                            naturalKey = meta.naturalKey,
                            deleted = false,
                            payload = payload
                        )
                    )
                }
            }
        }
        return records
    }

    /**
     * Merge a peer snapshot into the local database. Both devices run the same
     * deterministic ordering, so an exchange in either direction converges.
     * Newly paired devices that hold identical pre-existing rows are matched
     * by natural key and adopt the lexicographically smaller sync id, which
     * avoids duplicate tasks after the first sync.
     */
    suspend fun mergeRemote(remote: List<SyncRecord>) {
        if (remote.isEmpty()) return

        val allowed = remote.filter { it.entityType in SyncTypes.ALL }
        if (allowed.isEmpty()) return

        val localSnapshot = snapshot()
        val metas = metaDao.getAll().associateBy { it.syncId }.toMutableMap()
        val naturalKeys = HashMap<String, String>()
        for (record in localSnapshot) {
            // Tombstones must not block a row that was legitimately recreated
            // with the same natural key after the original was deleted.
            if (record.deleted || record.naturalKey.isBlank()) continue
            naturalKeys[key(record.entityType, record.naturalKey)] = record.syncId
        }

        for (type in SyncTypes.APPLY_ORDER) {
            val typeRecords = allowed.filter { it.entityType == type }.sortedBy { it.syncId }
            for (record in typeRecords) {
                clock.observe(Hlc.parse(record.hlc))
                val naturalKey = key(record.entityType, record.naturalKey)
                var localMeta = metas[record.syncId]

                if (localMeta == null && record.naturalKey.isNotBlank()) {
                    val candidateId = naturalKeys[naturalKey]
                    val candidate = candidateId?.let { metas[it] }
                    if (candidate != null && candidate.syncId != record.syncId) {
                        if (record.syncId < candidate.syncId) {
                            // Same logical row, different local ids: keep the
                            // smaller stable id on both devices.
                            metaDao.deleteById(candidate.syncId)
                            metas.remove(candidate.syncId)
                            localMeta = candidate.copy(syncId = record.syncId)
                            metaDao.upsert(localMeta)
                            metas[record.syncId] = localMeta
                            naturalKeys[naturalKey] = record.syncId
                        } else {
                            // Our id is smaller. The peer will adopt it when it
                            // merges the snapshot we send back.
                            continue
                        }
                    }
                }

                if (localMeta == null) {
                    if (record.deleted) {
                        val tombstone = SyncMetaEntity(
                            syncId = record.syncId,
                            entityType = record.entityType,
                            localId = 0,
                            hlc = record.hlc,
                            payloadHash = "",
                            naturalKey = record.naturalKey,
                            deleted = true,
                            updatedAt = System.currentTimeMillis()
                        )
                        metaDao.upsert(tombstone)
                        metas[tombstone.syncId] = tombstone
                        if (record.naturalKey.isNotBlank()) naturalKeys[naturalKey] = tombstone.syncId
                    } else {
                        val localId = insertRecord(record, refMaps(metas))
                        if (localId == null) continue
                        val meta = SyncMetaEntity(
                            syncId = record.syncId,
                            entityType = record.entityType,
                            localId = localId,
                            hlc = record.hlc,
                            payloadHash = record.payloadHash,
                            naturalKey = record.naturalKey,
                            deleted = false,
                            updatedAt = System.currentTimeMillis()
                        )
                        metaDao.upsert(meta)
                        metas[meta.syncId] = meta
                        if (record.naturalKey.isNotBlank()) naturalKeys[naturalKey] = meta.syncId
                    }
                    continue
                }

                val remoteWins = SyncRecordOrder.remoteWins(
                    remote = record,
                    localHlc = localMeta.hlc,
                    localHash = localMeta.payloadHash,
                    localId = localMeta.syncId
                )
                if (!remoteWins) continue

                if (record.deleted) {
                    deleteLocal(record.entityType, localMeta.localId)
                    val tombstone = localMeta.copy(
                        hlc = record.hlc,
                        payloadHash = "",
                        naturalKey = record.naturalKey.ifBlank { localMeta.naturalKey },
                        deleted = true,
                        updatedAt = System.currentTimeMillis()
                    )
                    metaDao.upsert(tombstone)
                    metas[tombstone.syncId] = tombstone
                } else {
                    val localId = upsertRecord(record, localMeta.localId, refMaps(metas))
                    if (localId == null) continue
                    val meta = localMeta.copy(
                        localId = localId,
                        hlc = record.hlc,
                        payloadHash = record.payloadHash,
                        naturalKey = record.naturalKey.ifBlank { localMeta.naturalKey },
                        deleted = false,
                        updatedAt = System.currentTimeMillis()
                    )
                    metaDao.upsert(meta)
                    metas[meta.syncId] = meta
                    if (meta.naturalKey.isNotBlank()) {
                        naturalKeys[key(meta.entityType, meta.naturalKey)] = meta.syncId
                    }
                }
            }
        }
    }

    private suspend fun reconcileType(
        type: String,
        rows: List<LocalRow>,
        metas: MutableMap<String, SyncMetaEntity>
    ) {
        val byLocalId = rows.associateBy { it.localId }
        val existingByLocalId = metas.values.filter { it.entityType == type }.associateBy { it.localId }
        val now = System.currentTimeMillis()

        for (row in rows) {
            val hash = CanonicalJson.sha256(row.payload)
            val existing = existingByLocalId[row.localId]
            if (existing == null) {
                val meta = SyncMetaEntity(
                    syncId = java.util.UUID.randomUUID().toString(),
                    entityType = type,
                    localId = row.localId,
                    hlc = clock.next().encode(),
                    payloadHash = hash,
                    naturalKey = row.naturalKey,
                    deleted = false,
                    updatedAt = now
                )
                metaDao.upsert(meta)
                metas[meta.syncId] = meta
            } else if (existing.deleted || existing.payloadHash != hash || existing.naturalKey != row.naturalKey) {
                val meta = existing.copy(
                    hlc = clock.next().encode(),
                    payloadHash = hash,
                    naturalKey = row.naturalKey,
                    deleted = false,
                    updatedAt = now
                )
                metaDao.upsert(meta)
                metas[meta.syncId] = meta
            }
        }

        // Anything we used to know about but which no longer exists locally is
        // a deletion that must be replicated to every peer.
        for (meta in metas.values.filter { it.entityType == type && !it.deleted }) {
            if (byLocalId.containsKey(meta.localId)) continue
            val tombstone = meta.copy(
                hlc = clock.next().encode(),
                payloadHash = "",
                deleted = true,
                updatedAt = now
            )
            metaDao.upsert(tombstone)
            metas[tombstone.syncId] = tombstone
        }
    }

    private suspend fun collectCategories(): List<LocalRow> =
        database.categoryDao().getAllCategoriesSync().map { category ->
            LocalRow(
                localId = category.id,
                payload = JSONObject().apply {
                    put("name", category.name)
                    put("type", category.type)
                    put("growthContribution", category.growthContribution)
                    put("comfortRisk", category.comfortRisk)
                    put("color", category.color)
                    put("icon", category.icon)
                    put("archived", category.archived)
                    put("createdAt", category.createdAt)
                    put("updatedAt", category.updatedAt)
                },
                naturalKey = "${category.name.trim().lowercase()}|${category.type.lowercase()}"
            )
        }

    private suspend fun collectTodos(categoryByLocalId: Map<Long, String>): List<LocalRow> =
        database.todoDao().getAllTodosSync().map { todo ->
            LocalRow(
                localId = todo.id,
                payload = JSONObject().apply {
                    put("title", todo.title)
                    put("description", todo.description)
                    put("categoryId", todo.categoryId?.let { categoryByLocalId[it] } ?: JSONObject.NULL)
                    put("deadline", todo.deadline ?: JSONObject.NULL)
                    put("completed", todo.isCompleted)
                    put("completedAt", todo.completedAt ?: JSONObject.NULL)
                    put("estimatedMinutes", todo.estimatedMinutes)
                    put("actualMinutes", todo.actualMinutes)
                    put("pomodoroCount", todo.pomodoroCount)
                    put("priority", todo.priority)
                    put("triggerText", todo.triggerText)
                    put("triggerHour", todo.triggerHour)
                    put("obstacleText", todo.obstacleText)
                    put("outcomeText", todo.outcomeText)
                    put("createdAt", todo.createdAt)
                    put("updatedAt", todo.updatedAt)
                },
                naturalKey = "${todo.title.trim().lowercase()}|${todo.description.trim().lowercase()}|${todo.createdAt}"
            )
        }

    private suspend fun collectSubtasks(todoByLocalId: Map<Long, String>): List<LocalRow> =
        database.subtaskDao().getAllSubtasksSync().mapNotNull { subtask ->
            val todoSyncId = todoByLocalId[subtask.todoId] ?: return@mapNotNull null
            LocalRow(
                localId = subtask.id,
                payload = JSONObject().apply {
                    put("todoId", todoSyncId)
                    put("title", subtask.title)
                    put("completed", subtask.isCompleted)
                    put("sortOrder", subtask.sortOrder)
                    put("createdAt", subtask.createdAt)
                },
                naturalKey = "$todoSyncId|${subtask.title.trim().lowercase()}|${subtask.sortOrder}|${subtask.createdAt}"
            )
        }

    private suspend fun collectSessions(
        categoryByLocalId: Map<Long, String>,
        todoByLocalId: Map<Long, String>
    ): List<LocalRow> =
        database.sessionDao().getAllSessionsSync()
            .filterNot { it.isActive || it.endTime == null }
            .map { session ->
                LocalRow(
                    localId = session.id,
                    payload = JSONObject().apply {
                        put("title", session.title)
                        put("categoryId", session.categoryId?.let { categoryByLocalId[it] } ?: JSONObject.NULL)
                        put("todoId", session.todoId?.let { todoByLocalId[it] } ?: JSONObject.NULL)
                        put("type", session.type)
                        put("startTime", session.startTime)
                        put("endTime", session.endTime ?: JSONObject.NULL)
                        put("notes", session.notes)
                        put("tags", session.tags)
                        put("source", session.source)
                        put("timezoneId", session.timezoneId)
                        put("isActive", session.isActive)
                        put("createdAt", session.createdAt)
                        put("updatedAt", session.updatedAt)
                    },
                    naturalKey = "${session.startTime}|${session.title.trim().lowercase()}|${session.type}|${session.createdAt}"
                )
            }

    private suspend fun collectPomodoros(
        categoryByLocalId: Map<Long, String>,
        todoByLocalId: Map<Long, String>
    ): List<LocalRow> =
        database.pomodoroSessionDao().getAllPomodorosSync()
            .filter { it.isCompleted }
            .map { pomodoro ->
                LocalRow(
                    localId = pomodoro.id,
                    payload = JSONObject().apply {
                        put("todoId", pomodoro.todoId?.let { todoByLocalId[it] } ?: JSONObject.NULL)
                        put("categoryId", pomodoro.categoryId?.let { categoryByLocalId[it] } ?: JSONObject.NULL)
                        put("startTime", pomodoro.startTime)
                        put("endTime", pomodoro.endTime ?: JSONObject.NULL)
                        put("durationMinutes", pomodoro.durationMinutes)
                        put("type", pomodoro.type)
                        put("completed", pomodoro.isCompleted)
                        put("createdAt", pomodoro.createdAt)
                    },
                    naturalKey = "${pomodoro.startTime}|${pomodoro.type}|${pomodoro.createdAt}"
                )
            }

    private suspend fun payloadFor(meta: SyncMetaEntity, refs: RefMaps): JSONObject? {
        return when (meta.entityType) {
            SyncTypes.CATEGORY -> database.categoryDao().getCategoryById(meta.localId)?.let { it.toPayload() }
            SyncTypes.TODO -> database.todoDao().getTodoById(meta.localId)?.let {
                it.toPayload(refs.categoryByLocalId)
            }
            SyncTypes.SUBTASK -> database.subtaskDao().getById(meta.localId)?.let { subtask ->
                val todoSyncId = refs.todoByLocalId[subtask.todoId] ?: return@let null
                subtask.toPayload(todoSyncId)
            }
            SyncTypes.SESSION -> database.sessionDao().getSessionById(meta.localId)?.let {
                it.toPayload(refs.categoryByLocalId, refs.todoByLocalId)
            }
            SyncTypes.POMODORO -> database.pomodoroSessionDao().getPomodoroById(meta.localId)?.let {
                it.toPayload(refs.categoryByLocalId, refs.todoByLocalId)
            }
            else -> null
        }
    }

    private suspend fun insertRecord(record: SyncRecord, refs: RefMaps): Long? {
        val payload = record.payload ?: return null
        return when (record.entityType) {
            SyncTypes.CATEGORY -> database.categoryDao().insert(
                CategoryEntity(
                    name = payload.optString("name", ""),
                    type = payload.optString("type", "PRODUCTIVE"),
                    growthContribution = payload.optString("growthContribution", "LOW"),
                    comfortRisk = payload.optString("comfortRisk", "LOW"),
                    color = payload.optLong("color", 0xFF6200EE),
                    icon = payload.optString("icon", "default"),
                    archived = payload.optBoolean("archived", false),
                    createdAt = payload.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = payload.optLong("updatedAt", System.currentTimeMillis())
                )
            )
            SyncTypes.TODO -> database.todoDao().insert(
                TodoEntity(
                    title = payload.optString("title", ""),
                    description = payload.optString("description", ""),
                    categoryId = payload.optRef("categoryId")?.let { refs.categoryBySyncId[it] },
                    deadline = payload.optLongOrNull("deadline"),
                    isCompleted = payload.optBoolean("completed", false),
                    completedAt = payload.optLongOrNull("completedAt"),
                    estimatedMinutes = payload.optInt("estimatedMinutes", 0),
                    actualMinutes = payload.optInt("actualMinutes", 0),
                    pomodoroCount = payload.optInt("pomodoroCount", 0),
                    priority = payload.optInt("priority", 0),
                    triggerText = payload.optString("triggerText", ""),
                    triggerHour = payload.optInt("triggerHour", -1),
                    obstacleText = payload.optString("obstacleText", ""),
                    outcomeText = payload.optString("outcomeText", ""),
                    createdAt = payload.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = payload.optLong("updatedAt", System.currentTimeMillis())
                )
            )
            SyncTypes.SUBTASK -> {
                val todoId = payload.optRef("todoId")
                    ?.let { refs.todoBySyncId[it] } ?: return null
                database.subtaskDao().insert(
                    SubtaskEntity(
                        todoId = todoId,
                        title = payload.optString("title", ""),
                        isCompleted = payload.optBoolean("completed", false),
                        sortOrder = payload.optInt("sortOrder", 0),
                        createdAt = payload.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            SyncTypes.SESSION -> database.sessionDao().insert(
                SessionEntity(
                    title = payload.optString("title", ""),
                    categoryId = payload.optRef("categoryId")?.let { refs.categoryBySyncId[it] },
                    todoId = payload.optRef("todoId")?.let { refs.todoBySyncId[it] },
                    type = payload.optString("type", "ACTIVITY"),
                    startTime = payload.optLong("startTime", System.currentTimeMillis()),
                    endTime = payload.optLongOrNull("endTime"),
                    notes = payload.optString("notes", ""),
                    tags = payload.optString("tags", ""),
                    source = payload.optString("source", "TIMER"),
                    timezoneId = payload.optString("timezoneId", ""),
                    isActive = payload.optBoolean("isActive", false),
                    createdAt = payload.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = payload.optLong("updatedAt", System.currentTimeMillis())
                )
            )
            SyncTypes.POMODORO -> database.pomodoroSessionDao().insert(
                PomodoroSessionEntity(
                    todoId = payload.optRef("todoId")?.let { refs.todoBySyncId[it] },
                    categoryId = payload.optRef("categoryId")?.let { refs.categoryBySyncId[it] },
                    startTime = payload.optLong("startTime", System.currentTimeMillis()),
                    endTime = payload.optLongOrNull("endTime"),
                    durationMinutes = payload.optInt("durationMinutes", 25),
                    type = payload.optString("type", "WORK"),
                    isCompleted = payload.optBoolean("completed", false),
                    createdAt = payload.optLong("createdAt", System.currentTimeMillis())
                )
            )
            else -> null
        }
    }

    private suspend fun upsertRecord(record: SyncRecord, localIdHint: Long, refs: RefMaps): Long? {
        val payload = record.payload ?: return null
        return when (record.entityType) {
            SyncTypes.CATEGORY -> {
                val existing = database.categoryDao().getCategoryById(localIdHint)
                if (existing == null) return insertRecord(record, refs)
                database.categoryDao().update(
                    existing.copy(
                        name = payload.optString("name", existing.name),
                        type = payload.optString("type", existing.type),
                        growthContribution = payload.optString("growthContribution", existing.growthContribution),
                        comfortRisk = payload.optString("comfortRisk", existing.comfortRisk),
                        color = payload.optLong("color", existing.color),
                        icon = payload.optString("icon", existing.icon),
                        archived = payload.optBoolean("archived", existing.archived),
                        createdAt = payload.optLong("createdAt", existing.createdAt),
                        updatedAt = payload.optLong("updatedAt", existing.updatedAt)
                    )
                )
                localIdHint
            }
            SyncTypes.TODO -> {
                val existing = database.todoDao().getTodoById(localIdHint)
                if (existing == null) return insertRecord(record, refs)
                database.todoDao().update(
                    existing.copy(
                        title = payload.optString("title", existing.title),
                        description = payload.optString("description", existing.description),
                        categoryId = payload.optRef("categoryId")?.let { refs.categoryBySyncId[it] },
                        deadline = payload.optLongOrNull("deadline"),
                        isCompleted = payload.optBoolean("completed", existing.isCompleted),
                        completedAt = payload.optLongOrNull("completedAt"),
                        estimatedMinutes = payload.optInt("estimatedMinutes", existing.estimatedMinutes),
                        actualMinutes = payload.optInt("actualMinutes", existing.actualMinutes),
                        pomodoroCount = payload.optInt("pomodoroCount", existing.pomodoroCount),
                        priority = payload.optInt("priority", existing.priority),
                        triggerText = payload.optString("triggerText", existing.triggerText),
                        triggerHour = payload.optInt("triggerHour", existing.triggerHour),
                        obstacleText = payload.optString("obstacleText", existing.obstacleText),
                        outcomeText = payload.optString("outcomeText", existing.outcomeText),
                        createdAt = payload.optLong("createdAt", existing.createdAt),
                        updatedAt = payload.optLong("updatedAt", existing.updatedAt)
                    )
                )
                localIdHint
            }
            SyncTypes.SUBTASK -> {
                val existing = database.subtaskDao().getById(localIdHint)
                if (existing == null) return insertRecord(record, refs)
                val todoId = payload.optRef("todoId")
                    ?.let { refs.todoBySyncId[it] } ?: return null
                database.subtaskDao().update(
                    existing.copy(
                        todoId = todoId,
                        title = payload.optString("title", existing.title),
                        isCompleted = payload.optBoolean("completed", existing.isCompleted),
                        sortOrder = payload.optInt("sortOrder", existing.sortOrder),
                        createdAt = payload.optLong("createdAt", existing.createdAt)
                    )
                )
                localIdHint
            }
            SyncTypes.SESSION -> {
                val existing = database.sessionDao().getSessionById(localIdHint)
                if (existing == null) return insertRecord(record, refs)
                database.sessionDao().update(
                    existing.copy(
                        title = payload.optString("title", existing.title),
                        categoryId = payload.optRef("categoryId")?.let { refs.categoryBySyncId[it] },
                        todoId = payload.optRef("todoId")?.let { refs.todoBySyncId[it] },
                        type = payload.optString("type", existing.type),
                        startTime = payload.optLong("startTime", existing.startTime),
                        endTime = payload.optLongOrNull("endTime"),
                        notes = payload.optString("notes", existing.notes),
                        tags = payload.optString("tags", existing.tags),
                        source = payload.optString("source", existing.source),
                        timezoneId = payload.optString("timezoneId", existing.timezoneId),
                        isActive = payload.optBoolean("isActive", existing.isActive),
                        createdAt = payload.optLong("createdAt", existing.createdAt),
                        updatedAt = payload.optLong("updatedAt", existing.updatedAt)
                    )
                )
                localIdHint
            }
            SyncTypes.POMODORO -> {
                val existing = database.pomodoroSessionDao().getPomodoroById(localIdHint)
                if (existing == null) return insertRecord(record, refs)
                database.pomodoroSessionDao().update(
                    existing.copy(
                        todoId = payload.optRef("todoId")?.let { refs.todoBySyncId[it] },
                        categoryId = payload.optRef("categoryId")?.let { refs.categoryBySyncId[it] },
                        startTime = payload.optLong("startTime", existing.startTime),
                        endTime = payload.optLongOrNull("endTime"),
                        durationMinutes = payload.optInt("durationMinutes", existing.durationMinutes),
                        type = payload.optString("type", existing.type),
                        isCompleted = payload.optBoolean("completed", existing.isCompleted),
                        createdAt = payload.optLong("createdAt", existing.createdAt)
                    )
                )
                localIdHint
            }
            else -> null
        }
    }

    private suspend fun deleteLocal(type: String, localId: Long) {
        when (type) {
            SyncTypes.CATEGORY -> {
                database.todoDao().getAllTodosSync().forEach { todo ->
                    if (todo.categoryId == localId) database.todoDao().update(todo.copy(categoryId = null))
                }
                database.sessionDao().getAllSessionsSync().forEach { session ->
                    if (session.categoryId == localId) database.sessionDao().update(session.copy(categoryId = null))
                }
                database.pomodoroSessionDao().getAllPomodorosSync().forEach { pomodoro ->
                    if (pomodoro.categoryId == localId) {
                        database.pomodoroSessionDao().update(pomodoro.copy(categoryId = null))
                    }
                }
                database.categoryDao().getCategoryById(localId)?.let { database.categoryDao().delete(it) }
            }
            SyncTypes.TODO -> {
                database.subtaskDao().deleteAllForTodo(localId)
                database.sessionDao().getAllSessionsSync().forEach { session ->
                    if (session.todoId == localId) database.sessionDao().update(session.copy(todoId = null))
                }
                database.pomodoroSessionDao().getAllPomodorosSync().forEach { pomodoro ->
                    if (pomodoro.todoId == localId) database.pomodoroSessionDao().update(pomodoro.copy(todoId = null))
                }
                database.todoDao().getTodoById(localId)?.let { database.todoDao().delete(it) }
            }
            SyncTypes.SUBTASK -> {
                database.subtaskDao().getById(localId)?.let { database.subtaskDao().delete(it) }
            }
            SyncTypes.SESSION -> {
                database.sessionDao().getSessionById(localId)?.let { database.sessionDao().delete(it) }
            }
            SyncTypes.POMODORO -> {
                database.pomodoroSessionDao().getPomodoroById(localId)?.let { database.pomodoroSessionDao().delete(it) }
            }
        }
    }

    private fun refMaps(metas: Map<String, SyncMetaEntity>): RefMaps {
        val categoryBySync = HashMap<String, Long>()
        val categoryByLocal = HashMap<Long, String>()
        val todoBySync = HashMap<String, Long>()
        val todoByLocal = HashMap<Long, String>()
        for (meta in metas.values) {
            if (meta.deleted) continue
            when (meta.entityType) {
                SyncTypes.CATEGORY -> {
                    categoryBySync[meta.syncId] = meta.localId
                    categoryByLocal[meta.localId] = meta.syncId
                }
                SyncTypes.TODO -> {
                    todoBySync[meta.syncId] = meta.localId
                    todoByLocal[meta.localId] = meta.syncId
                }
            }
        }
        return RefMaps(categoryBySync, categoryByLocal, todoBySync, todoByLocal)
    }

    private fun key(type: String, naturalKey: String): String = "$type|$naturalKey"

    private fun CategoryEntity.toPayload(): JSONObject = JSONObject().apply {
        put("name", name)
        put("type", type)
        put("growthContribution", growthContribution)
        put("comfortRisk", comfortRisk)
        put("color", color)
        put("icon", icon)
        put("archived", archived)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun TodoEntity.toPayload(categoryByLocalId: Map<Long, String>): JSONObject = JSONObject().apply {
        put("title", title)
        put("description", description)
        put("categoryId", categoryId?.let { categoryByLocalId[it] } ?: JSONObject.NULL)
        put("deadline", deadline ?: JSONObject.NULL)
        put("completed", isCompleted)
        put("completedAt", completedAt ?: JSONObject.NULL)
        put("estimatedMinutes", estimatedMinutes)
        put("actualMinutes", actualMinutes)
        put("pomodoroCount", pomodoroCount)
        put("priority", priority)
        put("triggerText", triggerText)
        put("triggerHour", triggerHour)
        put("obstacleText", obstacleText)
        put("outcomeText", outcomeText)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun SubtaskEntity.toPayload(todoSyncId: String): JSONObject = JSONObject().apply {
        put("todoId", todoSyncId)
        put("title", title)
        put("completed", isCompleted)
        put("sortOrder", sortOrder)
        put("createdAt", createdAt)
    }

    private fun SessionEntity.toPayload(
        categoryByLocalId: Map<Long, String>,
        todoByLocalId: Map<Long, String>
    ): JSONObject = JSONObject().apply {
        put("title", title)
        put("categoryId", categoryId?.let { categoryByLocalId[it] } ?: JSONObject.NULL)
        put("todoId", todoId?.let { todoByLocalId[it] } ?: JSONObject.NULL)
        put("type", type)
        put("startTime", startTime)
        put("endTime", endTime ?: JSONObject.NULL)
        put("notes", notes)
        put("tags", tags)
        put("source", source)
        put("timezoneId", timezoneId)
        put("isActive", isActive)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun PomodoroSessionEntity.toPayload(
        categoryByLocalId: Map<Long, String>,
        todoByLocalId: Map<Long, String>
    ): JSONObject = JSONObject().apply {
        put("todoId", todoId?.let { todoByLocalId[it] } ?: JSONObject.NULL)
        put("categoryId", categoryId?.let { categoryByLocalId[it] } ?: JSONObject.NULL)
        put("startTime", startTime)
        put("endTime", endTime ?: JSONObject.NULL)
        put("durationMinutes", durationMinutes)
        put("type", type)
        put("completed", isCompleted)
        put("createdAt", createdAt)
    }
}

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (isNull(key) || !has(key)) null else optLong(key)

private fun JSONObject.optRef(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key, "").takeIf { it.isNotBlank() }
}
