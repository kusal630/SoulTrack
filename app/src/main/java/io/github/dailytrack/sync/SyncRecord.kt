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

import org.json.JSONArray
import org.json.JSONObject

object SyncTypes {
    const val CATEGORY = "category"
    const val TODO = "todo"
    const val SUBTASK = "subtask"
    const val SESSION = "session"
    const val POMODORO = "pomodoro"

    val ALL = listOf(CATEGORY, TODO, SUBTASK, SESSION, POMODORO)
    val APPLY_ORDER = listOf(CATEGORY, TODO, SUBTASK, SESSION, POMODORO)
}

/**
 * One synchronisable row, including tombstones. [naturalKey] is transported
 * even for deletions so two devices that independently held the same row
 * before pairing can recognise it and converge on a single identity.
 */
data class SyncRecord(
    val syncId: String,
    val entityType: String,
    val hlc: String,
    val naturalKey: String,
    val deleted: Boolean,
    val payload: JSONObject?
) {
    val payloadHash: String by lazy {
        if (deleted) "" else payload?.let { CanonicalJson.sha256(it) } ?: ""
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", syncId)
        put("type", entityType)
        put("hlc", hlc)
        put("key", naturalKey)
        put("deleted", deleted)
        put("payload", payload ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(json: JSONObject): SyncRecord = SyncRecord(
            syncId = json.getString("id"),
            entityType = json.getString("type"),
            hlc = json.optString("hlc", Hlc.ZERO.encode()),
            naturalKey = json.optString("key", ""),
            deleted = json.optBoolean("deleted", false),
            payload = if (json.isNull("payload")) null else json.optJSONObject("payload")
        )

        fun listToJson(records: List<SyncRecord>): JSONArray =
            JSONArray().also { array -> records.forEach { array.put(it.toJson()) } }

        fun listFromJson(array: JSONArray): List<SyncRecord> = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                runCatching { add(fromJson(item)) }
            }
        }
    }
}

/**
 * Total order used by the merge. Higher hybrid clocks win. If two devices
 * somehow produce the same clock, the payload hash decides deterministically,
 * then the stable id, so every device reaches the same conclusion.
 */
object SyncRecordOrder {
    fun remoteWins(remote: SyncRecord, localHlc: String, localHash: String, localId: String): Boolean {
        val clockCompare = Hlc.parse(remote.hlc).compareTo(Hlc.parse(localHlc))
        if (clockCompare != 0) return clockCompare > 0
        val hashCompare = remote.payloadHash.compareTo(localHash)
        if (hashCompare != 0) return hashCompare > 0
        return remote.syncId > localId
    }
}
