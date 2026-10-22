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
import androidx.documentfile.provider.DocumentFile
import io.github.dailytrack.service.DeviceSyncService
import io.github.dailytrack.worker.DeviceSyncWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SyncManager(
    private val context: Context,
    private val syncRepository: SyncRepository,
    private val p2pSync: P2pSyncEngine
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState

    data class SyncUiState(
        val isEnabled: Boolean = false,
        val status: SyncRepository.SyncStatus = SyncRepository.SyncStatus.DISABLED,
        val lastSyncTime: String? = null,
        val lastError: String? = null,
        val folderName: String? = null,
        val isSyncthingInstalled: Boolean = false,
        val deviceName: String = "",

        // Direct device-to-device (no cloud) sync.
        val p2pEnabled: Boolean = false,
        val p2pRunning: Boolean = false,
        val p2pSyncing: Boolean = false,
        val p2pDeviceName: String = "",
        val p2pDeviceId: String = "",
        val p2pPort: Int = 0,
        val p2pPeers: List<SyncPeerInfo> = emptyList(),
        val p2pLastSyncAt: Long? = null,
        val p2pError: String? = null,
        val p2pPairingUri: String? = null,
        val p2pPairingExpiresAt: Long? = null
    )

    init {
        scope.launch {
            syncRepository.syncStatus.collect { status ->
                _uiState.value = _uiState.value.copy(
                    status = status,
                    isEnabled = syncRepository.isSyncEnabled(),
                    lastSyncTime = syncRepository.lastSyncTime.value,
                    lastError = syncRepository.lastError.value
                )
            }
        }
        scope.launch {
            p2pSync.state.collect { p2p ->
                _uiState.value = _uiState.value.copy(
                    p2pEnabled = p2p.enabled,
                    p2pRunning = p2p.running,
                    p2pSyncing = p2p.syncing,
                    p2pDeviceName = p2p.deviceName,
                    p2pDeviceId = p2p.deviceId,
                    p2pPort = p2p.listenPort,
                    p2pPeers = p2p.peers,
                    p2pLastSyncAt = p2p.lastSyncAt,
                    p2pError = p2p.lastError,
                    p2pPairingUri = p2p.pairingUri,
                    p2pPairingExpiresAt = p2p.pairingExpiresAt
                )
            }
        }
        refreshState()
    }

    fun refreshState() {
        scope.launch {
            val folderUri = syncRepository.getSyncFolderUri()?.let { Uri.parse(it) }
            val folderName = folderUri?.let {
                try {
                    DocumentFile.fromTreeUri(context, it)?.name
                } catch (e: Exception) {
                    null
                }
            }

            _uiState.value = _uiState.value.copy(
                isEnabled = syncRepository.isSyncEnabled(),
                status = syncRepository.syncStatus.value,
                lastSyncTime = syncRepository.lastSyncTime.value,
                lastError = syncRepository.lastError.value,
                folderName = folderName,
                deviceName = syncRepository.deviceId
            )
        }
    }

    fun enableSync(folderUri: Uri) {
        syncRepository.enableSync(folderUri)
        refreshState()
        scope.launch {
            delay(500)
            syncRepository.performSync()
            refreshState()
        }
    }

    fun disableSync() {
        syncRepository.disableSync()
        refreshState()
    }

    fun manualSync() {
        scope.launch {
            syncRepository.performSync()
            refreshState()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(lastError = null)
    }

    // ---- Direct device-to-device sync API ---------------------------------
    // The UI layer can call these; no screen changes are required here.

    fun isP2pEnabled(): Boolean = p2pSync.isEnabled()

    fun enableP2pSync() {
        p2pSync.setEnabled(true)
        DeviceSyncWorker.schedule(context)
        DeviceSyncService.start(context)
        refreshState()
    }

    fun disableP2pSync() {
        p2pSync.setEnabled(false)
        DeviceSyncWorker.cancel(context)
        DeviceSyncService.stop(context)
        refreshState()
    }

    fun setP2pDeviceName(name: String) {
        p2pSync.setDeviceName(name)
        refreshState()
    }

    /**
     * Creates a one-time pairing link to render as a QR code or share as text.
     * Scanning it on the other device is all that is needed to link them.
     */
    fun createPairingLink(publicHost: String? = null): String =
        createPairingInvite(publicHost).toUri()

    fun createPairingInvite(publicHost: String? = null): PairingInvite {
        if (!p2pSync.isEnabled()) enableP2pSync()
        return p2pSync.createPairingInvite(publicHost)
    }

    /** Accepts the text/URI decoded from the other device's QR code. */
    fun pairDevice(pairingText: String): SyncPeerInfo {
        if (!p2pSync.isEnabled()) enableP2pSync()
        val peer = p2pSync.pairWithUri(pairingText)
        DeviceSyncWorker.schedule(context)
        DeviceSyncService.start(context)
        refreshState()
        return peer
    }

    fun p2pPeers(): List<SyncPeerInfo> = p2pSync.peers()

    fun removeP2pPeer(deviceId: String) {
        p2pSync.removePeer(deviceId)
        refreshState()
    }

    fun updateP2pPeerAddress(deviceId: String, host: String, port: Int) {
        p2pSync.updatePeerAddress(deviceId, host, port)
        refreshState()
    }

    fun syncP2pNow(onResult: (P2pSyncResult) -> Unit = {}) {
        scope.launch {
            val result = p2pSync.syncNow("manual")
            refreshState()
            onResult(result)
        }
    }

    fun clearP2pError() {
        p2pSync.clearError()
        refreshState()
    }

    fun onDestroy() {
        scope.cancel()
    }
}
