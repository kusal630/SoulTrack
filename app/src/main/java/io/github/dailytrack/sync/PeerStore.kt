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

import android.content.Context
import android.net.Uri
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SyncPeer(
    val deviceId: String,
    val name: String,
    /** Base64url-encoded 256-bit pre-shared key. Never shown in the UI. */
    val secret: String,
    val host: String? = null,
    val port: Int = 0,
    val lastSeenAt: Long = 0L,
    val addressUpdatedAt: Long = 0L,
    val enabled: Boolean = true
) {
    fun info(): SyncPeerInfo = SyncPeerInfo(
        deviceId = deviceId,
        name = name,
        host = host,
        port = port,
        lastSeenAt = lastSeenAt,
        enabled = enabled
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", deviceId)
        put("name", name)
        put("secret", secret)
        put("host", host ?: JSONObject.NULL)
        put("port", port)
        put("lastSeenAt", lastSeenAt)
        put("addressUpdatedAt", addressUpdatedAt)
        put("enabled", enabled)
    }

    companion object {
        fun fromJson(json: JSONObject): SyncPeer? {
            val id = json.optString("id", "")
            val secret = json.optString("secret", "")
            if (id.isBlank() || secret.isBlank()) return null
            return SyncPeer(
                deviceId = id,
                name = json.optString("name", "Device"),
                secret = secret,
                host = json.optString("host", "").ifBlank { null },
                port = json.optInt("port", 0),
                lastSeenAt = json.optLong("lastSeenAt", 0L),
                addressUpdatedAt = json.optLong("addressUpdatedAt", 0L),
                enabled = json.optBoolean("enabled", true)
            )
        }
    }
}

/** Safe projection of a peer for UI state; contains no key material. */
data class SyncPeerInfo(
    val deviceId: String,
    val name: String,
    val host: String?,
    val port: Int,
    val lastSeenAt: Long,
    val enabled: Boolean
)

data class PairingInvite(
    val deviceId: String,
    val deviceName: String,
    val secret: String,
    val host: String?,
    val port: Int,
    val createdAt: Long,
    val expiresAt: Long
) {
    fun toUri(): String = Uri.Builder()
        .scheme("soultrack")
        .authority("pair")
        .appendQueryParameter("v", "1")
        .appendQueryParameter("id", deviceId)
        .appendQueryParameter("name", deviceName)
        .appendQueryParameter("secret", secret)
        .apply { if (!host.isNullOrBlank()) appendQueryParameter("host", host) }
        .appendQueryParameter("port", port.toString())
        .build()
        .toString()

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", deviceId)
        put("name", deviceName)
        put("secret", secret)
        put("host", host ?: JSONObject.NULL)
        put("port", port)
        put("createdAt", createdAt)
        put("expiresAt", expiresAt)
    }

    companion object {
        fun fromJson(json: JSONObject): PairingInvite? {
            val id = json.optString("id", "")
            val secret = json.optString("secret", "")
            val expiresAt = json.optLong("expiresAt", 0L)
            if (id.isBlank() || secret.isBlank() || expiresAt <= 0L) return null
            return PairingInvite(
                deviceId = id,
                deviceName = json.optString("name", "Device"),
                secret = secret,
                host = json.optString("host", "").ifBlank { null },
                port = json.optInt("port", 0),
                createdAt = json.optLong("createdAt", 0L),
                expiresAt = expiresAt
            )
        }

        fun fromUri(text: String): PairingInvite? {
            val uri = runCatching { Uri.parse(text.trim()) }.getOrNull() ?: return null
            if (!uri.scheme.equals("soultrack", ignoreCase = true)) return null
            if (!uri.host.equals("pair", ignoreCase = true)) return null
            val id = uri.getQueryParameter("id")?.takeIf { it.isNotBlank() } ?: return null
            val secret = uri.getQueryParameter("secret")?.takeIf { it.isNotBlank() } ?: return null
            runCatching { SyncCrypto.decode(secret) }.getOrElse { return null }
            val now = System.currentTimeMillis()
            return PairingInvite(
                deviceId = id,
                deviceName = uri.getQueryParameter("name") ?: "Device",
                secret = secret,
                host = uri.getQueryParameter("host"),
                port = uri.getQueryParameter("port")?.toIntOrNull() ?: PeerStore.DEFAULT_PORT,
                createdAt = now,
                expiresAt = now + PeerStore.INVITE_LIFETIME_MS
            )
        }
    }
}

/**
 * All pairing and sync settings. This file is app-private. Secrets are never
 * written to logs, exported JSON or the old file/folder sync.
 */
class PeerStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val deviceId: String
        get() {
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (existing != null) return existing
            val created = "dev-${UUID.randomUUID()}"
            prefs.edit().putString(KEY_DEVICE_ID, created).apply()
            return created
        }

    var deviceName: String
        get() = prefs.getString(KEY_DEVICE_NAME, null)?.takeIf { it.isNotBlank() }
            ?: Build.MODEL?.takeIf { it.isNotBlank() } ?: "Soul Track device"
        set(value) {
            prefs.edit().putString(KEY_DEVICE_NAME, value.trim().ifBlank { "Soul Track device" }).apply()
        }

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        }

    var listenPort: Int
        get() = prefs.getInt(KEY_LISTEN_PORT, DEFAULT_PORT)
        set(value) {
            prefs.edit().putInt(KEY_LISTEN_PORT, value).apply()
        }

    var lastSyncAt: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_SYNC, value).apply()
        }

    var lastError: String?
        get() = prefs.getString(KEY_LAST_ERROR, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_LAST_ERROR) else putString(KEY_LAST_ERROR, value)
            }.apply()
        }

    @Synchronized
    fun peers(): List<SyncPeer> {
        val raw = prefs.getString(KEY_PEERS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    array.optJSONObject(i)?.let { SyncPeer.fromJson(it) }?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun peer(deviceId: String): SyncPeer? = peers().firstOrNull { it.deviceId == deviceId }

    @Synchronized
    fun upsert(peer: SyncPeer) {
        val updated = peers().toMutableList()
        val index = updated.indexOfFirst { it.deviceId == peer.deviceId }
        if (index >= 0) updated[index] = peer else updated.add(peer)
        savePeers(updated)
    }

    @Synchronized
    fun remove(deviceId: String) {
        savePeers(peers().filterNot { it.deviceId == deviceId })
    }

    @Synchronized
    fun updateAddress(
        deviceId: String,
        host: String?,
        port: Int,
        seenNow: Boolean = false,
        force: Boolean = false
    ) {
        val current = peer(deviceId) ?: return
        val cleanHost = host?.takeIf { it.isNotBlank() }
        val usablePort = port.takeIf { it in 1..65535 } ?: current.port
        if (cleanHost == null || usablePort <= 0) return
        // Do not replace an internet/VPN address with a local Wi-Fi address
        // discovered on the LAN. The LAN address is tried transiently instead.
        if (!force && current.host != null &&
            !isPrivateAddress(current.host) && isPrivateAddress(cleanHost)
        ) {
            return
        }
        if (current.host == cleanHost && current.port == usablePort) {
            if (seenNow) upsert(current.copy(lastSeenAt = System.currentTimeMillis()))
            return
        }
        upsert(
            current.copy(
                host = cleanHost,
                port = usablePort,
                lastSeenAt = if (seenNow) System.currentTimeMillis() else current.lastSeenAt,
                addressUpdatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun isPrivateAddress(value: String): Boolean {
        return runCatching {
            val address = java.net.InetAddress.getByName(value)
            address.isSiteLocalAddress || address.isLoopbackAddress ||
                address.isLinkLocalAddress || address.hostAddress?.startsWith("fc") == true ||
                address.hostAddress?.startsWith("fd") == true
        }.getOrDefault(false)
    }

    @Synchronized
    fun markSeen(deviceId: String) {
        val current = peer(deviceId) ?: return
        upsert(current.copy(lastSeenAt = System.currentTimeMillis()))
    }

    @Synchronized
    fun createInvite(preferredHost: String?): PairingInvite {
        val now = System.currentTimeMillis()
        val secret = SyncCrypto.encode(SyncCrypto.randomBytes(32))
        val invite = PairingInvite(
            deviceId = deviceId,
            deviceName = deviceName,
            secret = secret,
            host = preferredHost?.takeIf { it.isNotBlank() } ?: LocalAddresses.bestLanHost(),
            port = listenPort,
            createdAt = now,
            expiresAt = now + INVITE_LIFETIME_MS
        )
        prefs.edit().putString(KEY_ACTIVE_INVITE, invite.toJson().toString()).apply()
        return invite
    }

    @Synchronized
    fun activeInvite(): PairingInvite? {
        val raw = prefs.getString(KEY_ACTIVE_INVITE, null) ?: return null
        val invite = runCatching { PairingInvite.fromJson(JSONObject(raw)) }.getOrNull() ?: return null
        if (invite.expiresAt <= System.currentTimeMillis()) {
            clearInvite()
            return null
        }
        return invite
    }

    @Synchronized
    fun clearInvite() {
        prefs.edit().remove(KEY_ACTIVE_INVITE).apply()
    }

    fun hlcClock(): HlcClock = HlcClock(
        load = { prefs.getString(KEY_HLC, null) },
        save = { prefs.edit().putString(KEY_HLC, it).apply() }
    )

    private fun savePeers(peers: List<SyncPeer>) {
        val array = JSONArray()
        peers.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_PEERS, array.toString()).apply()
    }

    companion object {
        const val DEFAULT_PORT = 47831
        const val INVITE_LIFETIME_MS = 10 * 60 * 1000L
        private const val PREFS_NAME = "p2p_sync_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LISTEN_PORT = "listen_port"
        private const val KEY_LAST_SYNC = "last_sync_at"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_PEERS = "peers"
        private const val KEY_ACTIVE_INVITE = "active_invite"
        private const val KEY_HLC = "hlc"
    }
}

object LocalAddresses {
    /** Best-effort LAN address to put in a pairing QR. */
    fun bestLanHost(): String? {
        return runCatching {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces().toList()
            val addresses = interfaces
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }

            addresses.firstOrNull { it is java.net.Inet4Address && it.isSiteLocalAddress }?.hostAddress
                ?: addresses.firstOrNull { it is java.net.Inet4Address }?.hostAddress
                ?: addresses.firstOrNull { it is java.net.Inet6Address && !it.isLinkLocalAddress }?.hostAddress
        }.getOrNull()
    }
}
