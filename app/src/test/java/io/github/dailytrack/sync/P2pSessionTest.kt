package io.github.dailytrack.sync

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.dailytrack.data.db.AppDatabase
import io.github.dailytrack.data.db.entity.TodoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class P2pSessionTest {
    private lateinit var dbA: AppDatabase
    private lateinit var dbB: AppDatabase
    private lateinit var storeA: P2pSyncStore
    private lateinit var storeB: P2pSyncStore
    private lateinit var peerStore: PeerStore

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun clock(startWall: Long, saved: Array<String?>): HlcClock = HlcClock(
        load = { saved[0] },
        save = { saved[0] = it },
        wallClock = { startWall }
    )

    @Before
    fun setUp() {
        context().getSharedPreferences("p2p_sync_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        dbA = Room.inMemoryDatabaseBuilder(context(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dbB = Room.inMemoryDatabaseBuilder(context(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storeA = P2pSyncStore(dbA, clock(1_000L, arrayOfNulls(1)))
        storeB = P2pSyncStore(dbB, clock(1_000L, arrayOfNulls(1)))
        peerStore = PeerStore(context())
    }

    @After
    fun tearDown() {
        dbA.close()
        dbB.close()
    }

    @Test
    fun `pairing handshake and session converge both databases`() = runBlocking {
        dbA.todoDao().insert(TodoEntity(title = "From phone", createdAt = 100L))
        dbB.todoDao().insert(TodoEntity(title = "From tablet", createdAt = 200L))
        storeA.reconcileLocal()
        storeB.reconcileLocal()

        ServerSocket().use { server ->
            server.reuseAddress = true
            server.bind(InetSocketAddress("127.0.0.1", 0))
            peerStore.listenPort = server.localPort
            val invite = peerStore.createInvite("127.0.0.1")

            val serverJob = async(Dispatchers.IO) {
                server.accept().use { socket ->
                    val handshake = P2pHandshake.server(socket, peerStore)
                    assertNotNull(handshake)
                    handshake!!
                    P2pSession(storeB, peerStore.deviceId, "Tablet", Mutex())
                        .runServer(handshake.channel)
                    handshake.channel.close()
                }
            }

            val clientId = "test-client-device"
            Socket("127.0.0.1", server.localPort).use { socket ->
                socket.tcpNoDelay = true
                val handshake = P2pHandshake.client(
                    socket = socket,
                    clientId = clientId,
                    clientName = "Phone",
                    clientListenPort = 47831,
                    peer = SyncPeer(
                        deviceId = invite.deviceId,
                        name = invite.deviceName,
                        secret = invite.secret,
                        host = "127.0.0.1",
                        port = invite.port
                    ),
                    pairingSecret = SyncCrypto.decode(invite.secret)
                )
                P2pSession(storeA, clientId, "Phone", Mutex())
                    .runClient(handshake.channel, invite.deviceId)
                handshake.channel.close()
            }

            serverJob.await()
        }

        assertEquals(
            setOf("From phone", "From tablet"),
            dbA.todoDao().getAllTodosSync().map { it.title }.toSet()
        )
        assertEquals(
            setOf("From phone", "From tablet"),
            dbB.todoDao().getAllTodosSync().map { it.title }.toSet()
        )
        val pairs = storeB.snapshot().filter { it.entityType == SyncTypes.TODO }
            .associateBy { it.title() }
        val pairsA = storeA.snapshot().filter { it.entityType == SyncTypes.TODO }
            .associateBy { it.title() }
        assertEquals(pairsA.keys, pairs.keys)
        pairsA.forEach { (title, record) ->
            assertEquals(title, record.syncId, pairs.getValue(title).syncId)
        }
        assertNotNull(peerStore.peer("test-client-device"))
    }

    private fun SyncRecord.title(): String = payload?.optString("title").orEmpty()
}
