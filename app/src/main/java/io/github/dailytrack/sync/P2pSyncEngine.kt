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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.util.Log
import androidx.room.InvalidationTracker
import io.github.dailytrack.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class P2pSyncState(
    val enabled: Boolean = false,
    val running: Boolean = false,
    val syncing: Boolean = false,
    val deviceId: String = "",
    val deviceName: String = "",
    val listenPort: Int = 0,
    val peers: List<SyncPeerInfo> = emptyList(),
    val lastSyncAt: Long? = null,
    val lastError: String? = null,
    val pairingUri: String? = null,
    val pairingExpiresAt: Long? = null
)

data class P2pSyncResult(
    val success: Boolean,
    val message: String
)

/**
 * Always-on peer server plus outbound peer connections. It does not use a
 * cloud, relay or account: pairing exchanges a 256-bit key over a QR/link, and
 * devices talk directly to each other over TCP.
 *
 * Local network: automatic mDNS discovery.
 * Across the internet: the paired host/port is used directly (public IPv4 with
 * port forwarding, IPv6, or a VPN/tailnet address). No traffic is relayed by a
 * third party.
 */
class P2pSyncEngine(context: Context) {
    private val appContext = context.applicationContext
    private val store = PeerStore(appContext)
    private val database = AppDatabase.getInstance(appContext)
    private val clock = store.hlcClock()
    private val syncStore = P2pSyncStore(database, clock)
    private val dbMutex = Mutex()
    private val outboundMutex = Mutex()
    private val discoveredAddresses = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Int>>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private val activeIncoming = AtomicInteger(0)
    private var serverJob: Job? = null
    private var periodicJob: Job? = null
    private var localChangeJob: Job? = null
    private var invalidationObserver: InvalidationTracker.Observer? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var discovery: LanDiscovery? = null
    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var boundPort: Int = 0

    private val _state = MutableStateFlow(P2pSyncState())
    val state: StateFlow<P2pSyncState> = _state
    @Volatile private var syncing = false

    val deviceId: String get() = store.deviceId
    val deviceName: String get() = store.deviceName

    fun isEnabled(): Boolean = store.enabled

    fun start() {
        if (!store.enabled) {
            refreshState()
            return
        }
        if (!started.compareAndSet(false, true)) return

        bindServer()
        startDiscovery()
        registerNetworkCallback()
        registerInvalidationObserver()
        startPeriodicLoop()
        refreshState()
    }

    fun stop() {
        if (!started.compareAndSet(true, false)) {
            refreshState()
            return
        }
        periodicJob?.cancel()
        periodicJob = null
        localChangeJob?.cancel()
        localChangeJob = null
        serverJob?.cancel()
        serverJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        boundPort = 0
        discovery?.stop()
        discovery = null
        unregisterNetworkCallback()
        unregisterInvalidationObserver()
        releaseMulticastLock()
        refreshState()
    }

    fun setEnabled(enabled: Boolean) {
        store.enabled = enabled
        if (enabled) start() else stop()
        refreshState()
    }

    fun setDeviceName(name: String) {
        store.deviceName = name
        refreshState()
    }

    fun createPairingInvite(preferredHost: String? = null): PairingInvite {
        if (store.enabled) start()
        val invite = store.createInvite(preferredHost)
        refreshState()
        return invite
    }

    fun pairWithUri(text: String): SyncPeerInfo {
        val invite = PairingInvite.fromUri(text)
            ?: throw IllegalArgumentException("Not a Soul Track pairing link")
        if (invite.deviceId == store.deviceId) {
            throw IllegalArgumentException("That is this device")
        }
        runCatching { SyncCrypto.decode(invite.secret) }
            .getOrElse { throw IllegalArgumentException("Invalid pairing key") }
        val peer = SyncPeer(
            deviceId = invite.deviceId,
            name = invite.deviceName,
            secret = invite.secret,
            host = invite.host,
            port = invite.port,
            lastSeenAt = 0L,
            addressUpdatedAt = System.currentTimeMillis()
        )
        store.upsert(peer)
        store.clearInvite()
        store.lastError = null
        refreshState()
        scope.launch { syncNow("paired") }
        return peer.info()
    }

    fun removePeer(deviceId: String) {
        store.remove(deviceId)
        discoveredAddresses.remove(deviceId)
        refreshState()
    }

    fun updatePeerAddress(deviceId: String, host: String, port: Int) {
        val cleanHost = host.trim().removePrefix("[").removeSuffix("]")
        require(cleanHost.isNotBlank()) { "Address is required" }
        require(port in 1..65535) { "Port must be between 1 and 65535" }
        store.updateAddress(deviceId, cleanHost, port, force = true)
        refreshState()
        scope.launch { syncNow("address") }
    }

    fun peers(): List<SyncPeerInfo> = store.peers().map { it.info() }

    fun clearError() {
        store.lastError = null
        refreshState()
    }

    fun refreshState() {
        val invite = store.activeInvite()
        _state.value = P2pSyncState(
            enabled = store.enabled,
            running = started.get(),
            syncing = syncing,
            deviceId = store.deviceId,
            deviceName = store.deviceName,
            listenPort = if (boundPort > 0) boundPort else store.listenPort,
            peers = store.peers().map { it.info() },
            lastSyncAt = store.lastSyncAt.takeIf { it > 0 },
            lastError = store.lastError,
            pairingUri = invite?.toUri(),
            pairingExpiresAt = invite?.expiresAt
        )
    }

    suspend fun syncNow(reason: String = "manual"): P2pSyncResult {
        if (!store.enabled) return P2pSyncResult(false, "Sync is off")
        if (!started.get()) start()

        val peers = store.peers().filter {
            it.enabled && (!it.host.isNullOrBlank() || discoveredAddresses.containsKey(it.deviceId))
        }
        if (peers.isEmpty()) {
            refreshState()
            return P2pSyncResult(false, "No paired device with a reachable address")
        }

        if (!outboundMutex.tryLock()) return P2pSyncResult(false, "Sync already running")
        syncing = true
        refreshState()
        return try {
            val results = coroutineScope {
                peers.map { peer -> async { syncWithPeer(peer) } }.awaitAll()
            }
            val successes = results.count { it }
            if (successes > 0) {
                store.lastSyncAt = System.currentTimeMillis()
                store.lastError = null
                P2pSyncResult(true, "Synced with $successes device${if (successes == 1) "" else "s"}")
            } else {
                store.lastError = "No paired device reachable"
                P2pSyncResult(false, "No paired device reachable")
            }
        } catch (e: Exception) {
            store.lastError = e.message ?: "Sync failed"
            P2pSyncResult(false, store.lastError ?: "Sync failed")
        } finally {
            syncing = false
            outboundMutex.unlock()
            refreshState()
        }
    }

    private suspend fun syncWithPeer(peer: SyncPeer): Boolean {
        val addresses = LinkedHashSet<Pair<String, Int>>()
        discoveredAddresses[peer.deviceId]?.let { addresses.add(it) }
        peer.host?.takeIf { it.isNotBlank() }?.let { host ->
            if (peer.port in 1..65535) addresses.add(host to peer.port)
        }
        if (addresses.isEmpty()) return false

        for ((host, port) in addresses) {
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                socket.tcpNoDelay = true
                socket.keepAlive = true
                val handshake = P2pHandshake.client(
                    socket = socket,
                    clientId = store.deviceId,
                    clientName = store.deviceName,
                    clientListenPort = store.listenPort,
                    peer = peer
                )
                try {
                    P2pSession(syncStore, store.deviceId, store.deviceName, dbMutex)
                        .runClient(handshake.channel, peer.deviceId)
                } finally {
                    handshake.channel.close()
                }
                store.markSeen(peer.deviceId)
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Outbound sync to ${peer.deviceId.take(8)} failed: ${e.javaClass.simpleName}")
            } finally {
                runCatching { socket.close() }
            }
        }
        return false
    }

    private fun bindServer() {
        val preferred = store.listenPort.takeIf { it in 1..65535 } ?: PeerStore.DEFAULT_PORT
        var socket: ServerSocket? = null
        var port = 0
        for (candidate in preferred..minOf(preferred + 9, 65535)) {
            socket = runCatching {
                ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(candidate), 50)
                }
            }.getOrNull()
            if (socket != null) {
                port = candidate
                break
            }
        }
        if (socket == null) {
            store.lastError = "Could not open the sync port"
            refreshState()
            return
        }
        serverSocket = socket
        boundPort = port
        store.listenPort = port

        serverJob = scope.launch {
            while (isActive) {
                val accepted = runCatching { socket.accept() }.getOrElse { break }
                if (activeIncoming.incrementAndGet() > MAX_INCOMING) {
                    activeIncoming.decrementAndGet()
                    runCatching { accepted.close() }
                    continue
                }
                launch {
                    try {
                        handleIncoming(accepted)
                    } finally {
                        activeIncoming.decrementAndGet()
                    }
                }
            }
        }
    }

    private suspend fun handleIncoming(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            val handshake = P2pHandshake.server(socket, store) ?: return
            val session = P2pSession(syncStore, store.deviceId, store.deviceName, dbMutex)
            try {
                session.runServer(handshake.channel)
            } finally {
                handshake.channel.close()
            }
            store.markSeen(handshake.remoteDeviceId)
            store.lastSyncAt = System.currentTimeMillis()
            store.lastError = null
            if (handshake.pairedNow) store.clearInvite()
            refreshState()
        } catch (e: Exception) {
            Log.d(TAG, "Inbound sync failed: ${e.javaClass.simpleName}")
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun startDiscovery() {
        if (boundPort <= 0) return
        val lan = LanDiscovery(appContext, store) { deviceId, host, port ->
            if (store.peer(deviceId) != null) {
                discoveredAddresses[deviceId] = host to port
                refreshState()
                scope.launch { syncNow("discovered") }
            }
        }
        discovery = lan
        acquireMulticastLock()
        lan.start(boundPort)
    }

    private fun startPeriodicLoop() {
        periodicJob?.cancel()
        periodicJob = scope.launch {
            while (isActive) {
                delay(PERIODIC_INTERVAL_MS)
                runCatching { syncNow("periodic") }
            }
        }
    }

    private fun registerInvalidationObserver() {
        if (invalidationObserver != null) return
        val observer = object : InvalidationTracker.Observer(
            "categories", "todos", "subtasks", "sessions", "pomodoro_sessions"
        ) {
            override fun onInvalidated(tables: Set<String>) {
                localChangeJob?.cancel()
                localChangeJob = scope.launch {
                    delay(LOCAL_CHANGE_DEBOUNCE_MS)
                    if (store.enabled) runCatching { syncNow("local-change") }
                }
            }
        }
        invalidationObserver = observer
        database.invalidationTracker.addObserver(observer)
    }

    private fun unregisterInvalidationObserver() {
        invalidationObserver?.let { database.invalidationTracker.removeObserver(it) }
        invalidationObserver = null
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch { runCatching { syncNow("network") } }
            }
        }
        networkCallback = callback
        runCatching { manager.registerNetworkCallback(request, callback) }
            .onFailure { networkCallback = null }
    }

    private fun unregisterNetworkCallback() {
        val callback = networkCallback ?: return
        val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        runCatching { manager.unregisterNetworkCallback(callback) }
        networkCallback = null
    }

    private fun acquireMulticastLock() {
        if (multicastLock != null) return
        runCatching {
            val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lock = wifi.createMulticastLock("soultrack-device-sync")
            lock.setReferenceCounted(false)
            lock.acquire()
            multicastLock = lock
        }
    }

    private fun releaseMulticastLock() {
        runCatching { multicastLock?.release() }
        multicastLock = null
    }

    companion object {
        private const val TAG = "P2pSync"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val PERIODIC_INTERVAL_MS = 60_000L
        private const val LOCAL_CHANGE_DEBOUNCE_MS = 1_500L
        private const val MAX_INCOMING = 4
    }
}
