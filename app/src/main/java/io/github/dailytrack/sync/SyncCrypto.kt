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

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SyncCrypto {
    const val NONCE_BYTES = 32
    const val PROOF_BYTES = 32
    private const val MAX_FRAME_BYTES = 32 * 1024 * 1024

    private val random = SecureRandom()

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)

    fun encode(value: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value)

    fun decode(value: String): ByteArray =
        Base64.getUrlDecoder().decode(value)

    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean =
        MessageDigest.isEqual(a, b)

    fun authKey(psk: ByteArray): ByteArray =
        hmacSha256(psk, "soultrack-auth-v1".toByteArray(Charsets.UTF_8))

    fun clientProof(
        psk: ByteArray,
        clientId: String,
        serverId: String,
        clientNonce: ByteArray,
        serverNonce: ByteArray
    ): ByteArray = proof(psk, "client", clientId, serverId, clientNonce, serverNonce)

    fun serverProof(
        psk: ByteArray,
        clientId: String,
        serverId: String,
        clientNonce: ByteArray,
        serverNonce: ByteArray
    ): ByteArray = proof(psk, "server", clientId, serverId, clientNonce, serverNonce)

    private fun proof(
        psk: ByteArray,
        role: String,
        clientId: String,
        serverId: String,
        clientNonce: ByteArray,
        serverNonce: ByteArray
    ): ByteArray {
        val data = buildString {
            append(role)
            append('|')
            append(clientId)
            append('|')
            append(serverId)
            append('|')
            append(encode(clientNonce))
            append('|')
            append(encode(serverNonce))
        }.toByteArray(Charsets.UTF_8)
        return hmacSha256(authKey(psk), data)
    }

    fun sessionKey(
        psk: ByteArray,
        clientId: String,
        serverId: String,
        clientNonce: ByteArray,
        serverNonce: ByteArray
    ): ByteArray {
        val salt = buildString {
            append(clientId)
            append('|')
            append(serverId)
            append('|')
            append(encode(clientNonce))
            append('|')
            append(encode(serverNonce))
        }.toByteArray(Charsets.UTF_8)
        return hmacSha256(psk, "soultrack-msg-v1".toByteArray(Charsets.UTF_8) + salt)
    }

    /**
     * Authenticated, encrypted, replay-resistant message channel.
     * Each direction has its own monotonic sequence number; AES-GCM protects
     * both confidentiality and integrity and the sequence is authenticated as
     * additional data.
     */
    class SecureChannel(
        private val input: InputStream,
        private val output: OutputStream,
        key: ByteArray,
        private val isClient: Boolean
    ) {
        private val dataIn = DataInputStream(input)
        private val dataOut = DataOutputStream(output)
        private val keySpec = SecretKeySpec(key.copyOf(32), "AES")
        private val direction: Byte = if (isClient) 1 else 2
        private val sendLock = Any()
        private val receiveLock = Any()
        private var sendSequence = 0L
        private var receiveSequence = 0L

        fun writeMessage(message: ByteArray) {
            synchronized(sendLock) {
                val sequence = sendSequence++
                val nonce = ByteArray(12)
                nonce[0] = direction
                for (i in 0..7) {
                    nonce[1 + i] = ((sequence ushr (8 * (7 - i))) and 0xFF).toByte()
                }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(128, nonce))
                val aad = byteArrayOf(direction) + longBytes(sequence)
                cipher.updateAAD(aad)
                val encrypted = cipher.doFinal(message)
                dataOut.writeInt(encrypted.size)
                dataOut.writeLong(sequence)
                dataOut.write(encrypted)
                dataOut.flush()
            }
        }

        fun readMessage(): ByteArray {
            synchronized(receiveLock) {
                val expected = receiveSequence
                val length = dataIn.readInt()
                if (length <= 0 || length > MAX_FRAME_BYTES) {
                    throw IllegalStateException("Invalid sync frame size")
                }
                val sequence = dataIn.readLong()
                if (sequence != expected) {
                    throw IllegalStateException("Replayed or out-of-order sync frame")
                }
                val encrypted = ByteArray(length)
                dataIn.readFully(encrypted)
                receiveSequence = expected + 1

                val nonce = ByteArray(12)
                nonce[0] = if (isClient) 2 else 1
                for (i in 0..7) {
                    nonce[1 + i] = ((sequence ushr (8 * (7 - i))) and 0xFF).toByte()
                }
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(128, nonce))
                val aad = byteArrayOf(if (isClient) 2 else 1) + longBytes(sequence)
                cipher.updateAAD(aad)
                return cipher.doFinal(encrypted)
            }
        }

        fun close() {
            runCatching { input.close() }
            runCatching { output.close() }
        }

        private fun longBytes(value: Long): ByteArray = ByteArray(8) { index ->
            ((value ushr (8 * (7 - index))) and 0xFF).toByte()
        }
    }
}

internal fun DataInputStream.readExact(size: Int): ByteArray {
    val bytes = ByteArray(size)
    readFully(bytes)
    return bytes
}

internal fun DataInputStream.readBounded(size: Int): ByteArray {
    if (size <= 0) return ByteArray(0)
    if (size > SyncCrypto.NONCE_BYTES * 1024) throw IllegalStateException("Field too large")
    return readExact(size)
}

internal fun DataInputStream.readString(): String {
    val size = readUnsignedShort()
    return readExact(size).toString(Charsets.UTF_8)
}

internal fun DataOutputStream.writeString(value: String) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    writeShort(bytes.size)
    write(bytes)
}

internal fun DataInputStream.discardOrThrow() {
    if (read() == -1) throw EOFException("Unexpected end of stream")
}
