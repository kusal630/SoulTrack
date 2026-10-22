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

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket

private const val TAG = "P2pSync"
private const val MAGIC = "SLTRSYNC"
private const val PROTOCOL_VERSION = 1
private const val CHUNK_RECORDS = 200

private const val STATUS_OK = 0
private const val STATUS_REJECTED = 1
private const val STATUS_PAIRING = 2

data class HandshakeResult(
    val channel: SyncCrypto.SecureChannel,
    val remoteDeviceId: String,
    val remoteDeviceName: String,
    val remoteListenPort: Int,
    val pairedNow: Boolean
)

object P2pHandshake {
    suspend fun client(
        socket: Socket,
        clientId: String,
        clientName: String,
        clientListenPort: Int,
        peer: SyncPeer,
        pairingSecret: ByteArray? = null
    ): HandshakeResult = withContext(Dispatchers.IO) {
        socket.soTimeout = 30_000
        val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
        val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))

        val clientNonce = SyncCrypto.randomBytes(SyncCrypto.NONCE_BYTES)
        writeMagic(output)
        output.writeInt(PROTOCOL_VERSION)
        output.writeString(clientId)
        output.writeString(clientName)
        output.writeInt(clientListenPort)
        output.writeInt(clientNonce.size)
        output.write(clientNonce)
        val pair = pairingSecret ?: ByteArray(0)
        output.writeInt(pair.size)
        output.write(pair)
        output.flush()

        readMagic(input)
        val version = input.readInt()
        if (version != PROTOCOL_VERSION) throw IllegalStateException("Unsupported sync protocol")
        val status = input.readByte().toInt()
        val serverId = input.readString()
        val serverName = input.readString()
        val serverListenPort = input.readInt()
        val serverNonce = input.readBounded(input.readInt())

        if (status == STATUS_REJECTED) throw IllegalStateException("Peer rejected the connection")
        if (status != STATUS_OK && status != STATUS_PAIRING) throw IllegalStateException("Bad sync handshake")
        if (serverId != peer.deviceId) throw IllegalStateException("Unexpected peer identity")

        val psk = SyncCrypto.decode(peer.secret)
        val expectedServerProof = SyncCrypto.serverProof(psk, clientId, serverId, clientNonce, serverNonce)
        val actualServerProof = input.readExact(input.readInt())
        if (!SyncCrypto.constantTimeEquals(expectedServerProof, actualServerProof)) {
            throw IllegalStateException("Peer authentication failed")
        }

        val clientProof = SyncCrypto.clientProof(psk, clientId, serverId, clientNonce, serverNonce)
        output.writeInt(clientProof.size)
        output.write(clientProof)
        output.flush()

        val authStatus = input.readByte().toInt()
        if (authStatus != STATUS_OK) throw IllegalStateException("Peer authentication failed")

        val sessionKey = SyncCrypto.sessionKey(psk, clientId, serverId, clientNonce, serverNonce)
        val channel = SyncCrypto.SecureChannel(input, output, sessionKey, isClient = true)
        socket.soTimeout = 120_000
        HandshakeResult(
            channel = channel,
            remoteDeviceId = serverId,
            remoteDeviceName = serverName,
            remoteListenPort = serverListenPort,
            pairedNow = status == STATUS_PAIRING
        )
    }

    suspend fun server(socket: Socket, store: PeerStore): HandshakeResult? = withContext(Dispatchers.IO) {
        socket.soTimeout = 30_000
        val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
        val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))

        try {
            readMagic(input)
            val version = input.readInt()
            if (version != PROTOCOL_VERSION) return@withContext null
            val clientId = input.readString()
            val clientName = input.readString()
            val clientListenPort = input.readInt()
            val clientNonce = input.readBounded(input.readInt())
            val pairSecretLength = input.readInt()
            val pairSecret = if (pairSecretLength > 0) input.readBounded(pairSecretLength) else ByteArray(0)

            val knownPeer = store.peer(clientId)
            val invite = store.activeInvite()
            val pairingNow = knownPeer == null || !knownPeer.enabled
            val psk: ByteArray
            val status: Int

            if (!pairingNow) {
                psk = runCatching { SyncCrypto.decode(knownPeer!!.secret) }.getOrElse { return@withContext null }
                status = STATUS_OK
            } else if (
                pairSecret.isNotEmpty() &&
                invite != null &&
                SyncCrypto.constantTimeEquals(
                    SyncCrypto.decode(invite.secret),
                    pairSecret
                )
            ) {
                psk = pairSecret
                status = STATUS_PAIRING
            } else {
                writeMagic(output)
                output.writeInt(PROTOCOL_VERSION)
                output.writeByte(STATUS_REJECTED)
                output.writeString(store.deviceId)
                output.writeString(store.deviceName)
                output.writeInt(store.listenPort)
                val nonce = SyncCrypto.randomBytes(SyncCrypto.NONCE_BYTES)
                output.writeInt(nonce.size)
                output.write(nonce)
                output.writeInt(0)
                output.flush()
                return@withContext null
            }

            val serverNonce = SyncCrypto.randomBytes(SyncCrypto.NONCE_BYTES)
            writeMagic(output)
            output.writeInt(PROTOCOL_VERSION)
            output.writeByte(status)
            output.writeString(store.deviceId)
            output.writeString(store.deviceName)
            output.writeInt(store.listenPort)
            output.writeInt(serverNonce.size)
            output.write(serverNonce)
            output.writeInt(SyncCrypto.PROOF_BYTES)

            val serverProof = SyncCrypto.serverProof(
                psk, clientId, store.deviceId, clientNonce, serverNonce
            )
            output.write(serverProof)
            output.flush()

            val expectedClientProof = SyncCrypto.clientProof(
                psk, clientId, store.deviceId, clientNonce, serverNonce
            )
            val clientProof = input.readExact(input.readInt())
            val authenticated = SyncCrypto.constantTimeEquals(expectedClientProof, clientProof)
            output.writeByte(if (authenticated) STATUS_OK else STATUS_REJECTED)
            output.flush()
            if (!authenticated) return@withContext null

            if (status == STATUS_PAIRING) {
                store.upsert(
                    SyncPeer(
                        deviceId = clientId,
                        name = clientName.ifBlank { "Device" },
                        secret = SyncCrypto.encode(psk),
                        host = socket.inetAddress?.hostAddress,
                        port = clientListenPort,
                        lastSeenAt = System.currentTimeMillis(),
                        addressUpdatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                store.markSeen(clientId)
                val current = store.peer(clientId)
                if (current != null && current.host.isNullOrBlank() && clientListenPort > 0) {
                    store.updateAddress(
                        deviceId = clientId,
                        host = socket.inetAddress?.hostAddress,
                        port = clientListenPort,
                        seenNow = true
                    )
                }
            }

            val sessionKey = SyncCrypto.sessionKey(
                psk, clientId, store.deviceId, clientNonce, serverNonce
            )
            val channel = SyncCrypto.SecureChannel(input, output, sessionKey, isClient = false)
            socket.soTimeout = 120_000
            HandshakeResult(
                channel = channel,
                remoteDeviceId = clientId,
                remoteDeviceName = clientName,
                remoteListenPort = clientListenPort,
                pairedNow = status == STATUS_PAIRING
            )
        } catch (e: Exception) {
            Log.d(TAG, "Handshake rejected: ${e.javaClass.simpleName}")
            null
        }
    }

    private fun writeMagic(output: DataOutputStream) {
        output.write(MAGIC.toByteArray(Charsets.US_ASCII))
    }

    private fun readMagic(input: DataInputStream) {
        val bytes = input.readExact(MAGIC.length)
        if (bytes.toString(Charsets.US_ASCII) != MAGIC) {
            throw IllegalStateException("Not a Soul Track sync connection")
        }
    }
}

class P2pSession(
    private val store: P2pSyncStore,
    private val deviceId: String,
    private val deviceName: String,
    private val dbMutex: Mutex
) {
    suspend fun runClient(channel: SyncCrypto.SecureChannel, expectedPeerId: String) {
        sendJson(
            channel,
            JSONObject().apply {
                put("type", "hello")
                put("v", PROTOCOL_VERSION)
                put("id", deviceId)
                put("name", deviceName)
            }
        )
        val hello = receiveJson(channel)
        if (hello.optString("type") != "hello") throw IllegalStateException("Bad peer greeting")
        if (hello.optString("id") != expectedPeerId) throw IllegalStateException("Unexpected peer identity")

        val snapshot = dbMutex.withLock {
            store.reconcileLocal()
            store.snapshot()
        }
        sendSnapshot(channel, snapshot)
        val remote = receiveSnapshot(channel)
        val merged = dbMutex.withLock {
            store.mergeRemote(remote)
            store.reconcileLocal()
            store.snapshot()
        }
        // A second exchange closes the only asymmetric case: our row won a
        // conflict under an id the server had not seen yet. After this the
        // server has everything we have, and we already have its full state.
        sendSnapshot(channel, merged)
        val finalAck = receiveJson(channel)
        if (finalAck.optString("type") != "final_ack") {
            throw IllegalStateException("Peer did not confirm the sync")
        }
    }

    suspend fun runServer(channel: SyncCrypto.SecureChannel) {
        val hello = receiveJson(channel)
        if (hello.optString("type") != "hello") throw IllegalStateException("Bad peer greeting")
        sendJson(
            channel,
            JSONObject().apply {
                put("type", "hello")
                put("v", PROTOCOL_VERSION)
                put("id", deviceId)
                put("name", deviceName)
            }
        )

        val remote = receiveSnapshot(channel)
        val snapshot = dbMutex.withLock {
            store.mergeRemote(remote)
            store.reconcileLocal()
            store.snapshot()
        }
        sendSnapshot(channel, snapshot)

        // Receive the client's post-merge state and apply anything that was
        // newer on its side after identity adoption, then confirm convergence.
        val updates = receiveSnapshot(channel)
        dbMutex.withLock {
            store.mergeRemote(updates)
            store.reconcileLocal()
        }
        sendJson(channel, JSONObject().apply { put("type", "final_ack") })
    }

    private suspend fun sendSnapshot(channel: SyncCrypto.SecureChannel, records: List<SyncRecord>) {
        sendJson(
            channel,
            JSONObject().apply {
                put("type", "state_begin")
                put("total", records.size)
            }
        )
        var index = 0
        while (index < records.size) {
            val end = minOf(index + CHUNK_RECORDS, records.size)
            val chunk = records.subList(index, end)
            sendJson(
                channel,
                JSONObject().apply {
                    put("type", "state")
                    put("records", SyncRecord.listToJson(chunk))
                }
            )
            index = end
        }
        sendJson(channel, JSONObject().apply { put("type", "state_end") })
    }

    private suspend fun receiveSnapshot(channel: SyncCrypto.SecureChannel): List<SyncRecord> {
        val records = ArrayList<SyncRecord>()
        var started = false
        while (true) {
            val message = receiveJson(channel)
            when (message.optString("type")) {
                "state_begin" -> {
                    started = true
                    val expected = message.optInt("total", 0)
                    if (expected < 0 || expected > 2_000_000) {
                        throw IllegalStateException("Invalid sync state size")
                    }
                }
                "state" -> {
                    if (!started) throw IllegalStateException("Sync state out of order")
                    records.addAll(SyncRecord.listFromJson(message.optJSONArray("records") ?: JSONArray()))
                }
                "state_end" -> return records
                "error" -> throw IllegalStateException(message.optString("message", "Peer sync error"))
                else -> throw IllegalStateException("Unexpected sync message")
            }
        }
    }

    private suspend fun sendJson(channel: SyncCrypto.SecureChannel, json: JSONObject) {
        withContext(Dispatchers.IO) {
            channel.writeMessage(json.toString().toByteArray(Charsets.UTF_8))
        }
    }

    private suspend fun receiveJson(channel: SyncCrypto.SecureChannel): JSONObject = withContext(Dispatchers.IO) {
        val bytes = channel.readMessage()
        JSONObject(bytes.toString(Charsets.UTF_8))
    }
}
