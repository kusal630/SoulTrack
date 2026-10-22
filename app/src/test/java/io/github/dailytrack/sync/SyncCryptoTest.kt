package io.github.dailytrack.sync

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.PipedInputStream
import java.io.PipedOutputStream

class SyncCryptoTest {
    @Test
    fun `proofs are role specific`() {
        val psk = SyncCrypto.randomBytes(32)
        val clientNonce = SyncCrypto.randomBytes(32)
        val serverNonce = SyncCrypto.randomBytes(32)
        val client = SyncCrypto.clientProof(psk, "a", "b", clientNonce, serverNonce)
        val server = SyncCrypto.serverProof(psk, "a", "b", clientNonce, serverNonce)
        assertFalse(client.contentEquals(server))
    }

    @Test
    fun `secure channel round trips in both directions`() {
        val psk = SyncCrypto.randomBytes(32)
        val clientNonce = SyncCrypto.randomBytes(32)
        val serverNonce = SyncCrypto.randomBytes(32)
        val key = SyncCrypto.sessionKey(psk, "a", "b", clientNonce, serverNonce)

        val clientToServer = PipedOutputStream()
        val serverInput = PipedInputStream(clientToServer, 1 shl 20)
        val serverToClient = PipedOutputStream()
        val clientInput = PipedInputStream(serverToClient, 1 shl 20)

        val client = SyncCrypto.SecureChannel(clientInput, clientToServer, key, isClient = true)
        val server = SyncCrypto.SecureChannel(serverInput, serverToClient, key, isClient = false)

        client.writeMessage("hello from phone".toByteArray())
        assertArrayEquals("hello from phone".toByteArray(), server.readMessage())

        server.writeMessage("hello from tablet".toByteArray())
        assertArrayEquals("hello from tablet".toByteArray(), client.readMessage())
    }
}
