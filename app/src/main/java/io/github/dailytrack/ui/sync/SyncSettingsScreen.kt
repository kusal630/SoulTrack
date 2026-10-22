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


package io.github.dailytrack.ui.sync

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import io.github.dailytrack.sync.PairingInvite
import io.github.dailytrack.sync.SyncManager
import io.github.dailytrack.sync.SyncPeerInfo
import io.github.dailytrack.sync.SyncRepository
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.Hairline
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.ScreenSection
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.theme.WastedColor
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun SyncSettingsScreen(
    navController: NavController,
    syncManager: SyncManager
) {
    val context = LocalContext.current
    val uiState by syncManager.uiState.collectAsState()
    var showDisableDialog by remember { mutableStateOf(false) }
    var showFolderDisableDialog by remember { mutableStateOf(false) }
    var pairCode by remember { mutableStateOf<PairingInvite?>(null) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var peerToEdit by remember { mutableStateOf<SyncPeerInfo?>(null) }
    var peerToRemove by remember { mutableStateOf<SyncPeerInfo?>(null) }

    val scannerLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { text ->
            pairWithText(context, syncManager, text) { peer ->
                Toast.makeText(context, "Paired with ${peer.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            syncManager.enableSync(uri)
        }
    }

    LaunchedEffect(Unit) {
        syncManager.refreshState()
    }

    ScreenList(
        title = "Sync & devices",
        subtitle = "Private, direct, no account",
        navController = navController,
        compact = true
    ) {
        item {
            ScreenSection(
                title = "Direct device sync",
                subtitle = "Phone to tablet, encrypted end to end",
                leadingIcon = Icons.Default.Devices
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (uiState.p2pEnabled) "On" else "Off",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            when {
                                !uiState.p2pEnabled -> "Nothing leaves your devices"
                                uiState.p2pSyncing -> "Syncing changes now"
                                uiState.p2pRunning -> "Listening on port ${uiState.p2pPort}"
                                else -> "Starting listener"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.p2pEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                syncManager.enableP2pSync()
                            } else {
                                showDisableDialog = true
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Changes travel directly over Wi-Fi, a hotspot, a VPN such as Tailscale, or a reachable internet address. There is no cloud account or relay server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (uiState.p2pEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Hairline()
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusRow(
                        icon = Icons.Default.Devices,
                        label = "Device",
                        value = uiState.p2pDeviceName.ifBlank { "This device" }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusRow(
                        icon = Icons.Default.Fingerprint,
                        label = "ID",
                        value = shortDeviceId(uiState.p2pDeviceId)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusRow(
                        icon = Icons.Default.Schedule,
                        label = "Last sync",
                        value = uiState.p2pLastSyncAt?.let(::formatTime) ?: "Never"
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusRow(
                        icon = Icons.Default.Wifi,
                        label = "Pairing",
                        value = when (uiState.p2pPeers.size) {
                            0 -> "No devices yet"
                            1 -> "1 device"
                            else -> "${uiState.p2pPeers.size} devices"
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { showNameDialog = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rename this device")
                    }
                }

                if (uiState.p2pError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ErrorCard(
                        message = uiState.p2pError!!,
                        onDismiss = { syncManager.clearP2pError() }
                    )
                }
            }
        }

        if (uiState.p2pEnabled) {
            item {
                ScreenSection(
                    title = "Pair a device",
                    subtitle = "Use the other device's code once",
                    leadingIcon = Icons.Default.QrCode
                ) {
                    Text(
                        "On the other device: turn on direct sync, tap Pair a device, then scan this code or paste the link.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PrimaryCta(
                        text = if (uiState.p2pSyncing) "Syncing..." else "Show pairing code",
                        icon = Icons.Default.QrCode,
                        enabled = !uiState.p2pSyncing,
                        onClick = {
                            runCatching { syncManager.createPairingInvite() }
                                .onSuccess { pairCode = it }
                                .onFailure {
                                    Toast.makeText(
                                        context,
                                        it.message ?: "Could not create a pairing code",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val options = ScanOptions()
                                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                .setPrompt("Scan the Soul Track pairing code")
                                .setBeepEnabled(false)
                                .setOrientationLocked(false)
                            scannerLauncher.launch(options)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan a code")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showPasteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Paste pairing link")
                    }
                }
            }

            item {
                ScreenSection(
                    title = "Paired devices",
                    subtitle = if (uiState.p2pPeers.isEmpty()) "None yet" else "${uiState.p2pPeers.size} linked",
                    leadingIcon = Icons.Default.Wifi,
                    actionLabel = if (uiState.p2pPeers.isNotEmpty()) {
                        if (uiState.p2pSyncing) "Syncing..." else "Sync now"
                    } else {
                        null
                    },
                    onAction = if (uiState.p2pPeers.isNotEmpty() && !uiState.p2pSyncing) {
                        {
                            syncManager.syncP2pNow { result ->
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        null
                    }
                ) {
                    if (uiState.p2pPeers.isEmpty()) {
                        EmptyState(
                            title = "No devices paired",
                            message = "Pair a phone or tablet to start syncing tasks, timer history and pomodoros.",
                            icon = Icons.Default.Devices,
                            actionLabel = "Pair a device",
                            onAction = {
                                runCatching { syncManager.createPairingInvite() }
                                    .onSuccess { pairCode = it }
                                    .onFailure {
                                        Toast.makeText(context, it.message ?: "Pairing failed", Toast.LENGTH_LONG)
                                            .show()
                                    }
                            }
                        )
                    } else {
                        uiState.p2pPeers.forEachIndexed { index, peer ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Hairline()
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            PeerRow(
                                peer = peer,
                                onEditAddress = { peerToEdit = peer },
                                onRemove = { peerToRemove = peer }
                            )
                        }
                    }
                }
            }
        }

        item {
            ScreenSection(
                title = "Synced data",
                leadingIcon = Icons.Default.CheckCircle,
                tint = ProductiveColor
            ) {
                Text(
                    "Tasks and subtasks, categories, timer sessions, and pomodoro history stay identical on every paired device.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Changes merge by logical time, deletions are remembered, and reconnecting after being offline is safe. Device preferences and active timers stay local.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ScreenSection(
                title = "Privacy",
                leadingIcon = Icons.Default.Security,
                tint = ProductiveColor
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = ProductiveColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Pairing exchanges a one-time key. Every sync frame is encrypted and authenticated with AES-GCM. No account, cloud, telemetry, or third-party relay is used.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            ScreenSection(
                title = "Folder sync (advanced)",
                subtitle = "Syncthing-style backup folder",
                leadingIcon = Icons.Default.Folder
            ) {
                StatusRow(
                    icon = when (uiState.status) {
                        SyncRepository.SyncStatus.ENABLED -> Icons.Default.CheckCircle
                        SyncRepository.SyncStatus.SYNCING -> Icons.Default.Sync
                        SyncRepository.SyncStatus.ERROR -> Icons.Default.Error
                        SyncRepository.SyncStatus.FOLDER_MISSING -> Icons.Default.FolderOff
                        SyncRepository.SyncStatus.PERMISSION_MISSING -> Icons.Default.Lock
                        SyncRepository.SyncStatus.DISABLED -> Icons.Default.CloudOff
                    },
                    label = "Status",
                    value = when (uiState.status) {
                        SyncRepository.SyncStatus.ENABLED -> "Enabled"
                        SyncRepository.SyncStatus.SYNCING -> "Syncing..."
                        SyncRepository.SyncStatus.ERROR -> "Error"
                        SyncRepository.SyncStatus.FOLDER_MISSING -> "Folder missing"
                        SyncRepository.SyncStatus.PERMISSION_MISSING -> "Permission lost"
                        SyncRepository.SyncStatus.DISABLED -> "Disabled"
                    },
                    color = when (uiState.status) {
                        SyncRepository.SyncStatus.ENABLED -> ProductiveColor
                        SyncRepository.SyncStatus.SYNCING -> MaterialTheme.colorScheme.primary
                        SyncRepository.SyncStatus.ERROR -> WastedColor
                        SyncRepository.SyncStatus.FOLDER_MISSING -> ExerciseColor
                        SyncRepository.SyncStatus.PERMISSION_MISSING -> ExerciseColor
                        SyncRepository.SyncStatus.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (uiState.isEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusRow(Icons.Default.Folder, "Folder", uiState.folderName ?: "Unknown")
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusRow(Icons.Default.Schedule, "Last sync", uiState.lastSyncTime ?: "Never")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { syncManager.manualSync() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync folder now")
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { showFolderDisableDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disable folder sync", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose a sync folder")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Optional. Use the same folder with Syncthing if you already have that setup. Direct device sync above does not need it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.lastError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ErrorCard(
                        message = uiState.lastError!!,
                        onDismiss = { syncManager.clearError() }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }

    if (showDisableDialog) {
        AlertDialog(
            onDismissRequest = { showDisableDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Turn off direct sync?") },
            text = {
                Text("Paired devices and local data stay on this device. Syncing stops until you turn it on again.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        syncManager.disableP2pSync()
                        showDisableDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Turn off")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showFolderDisableDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDisableDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Disable folder sync") },
            text = { Text("This stops writing to the sync folder. Existing data is preserved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        syncManager.disableSync()
                        showFolderDisableDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDisableDialog = false }) { Text("Cancel") }
            }
        )
    }

    pairCode?.let { invite ->
        PairingCodeDialog(
            invite = invite,
            deviceName = uiState.p2pDeviceName,
            onDismiss = { pairCode = null },
            onShare = { sharePairingLink(context, invite.toUri()) },
            onCopy = {
                copyPairingLink(context, invite.toUri())
                Toast.makeText(context, "Pairing link copied", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showPasteDialog) {
        PastePairingDialog(
            onDismiss = { showPasteDialog = false },
            onPair = { text, dialogDismiss ->
                pairWithText(context, syncManager, text) { peer ->
                    Toast.makeText(context, "Paired with ${peer.name}", Toast.LENGTH_SHORT).show()
                    dialogDismiss()
                    showPasteDialog = false
                }
            }
        )
    }

    if (showNameDialog) {
        DeviceNameDialog(
            currentName = uiState.p2pDeviceName,
            onDismiss = { showNameDialog = false },
            onSave = {
                syncManager.setP2pDeviceName(it)
                showNameDialog = false
            }
        )
    }

    peerToEdit?.let { peer ->
        PeerAddressDialog(
            peer = peer,
            onDismiss = { peerToEdit = null },
            onSave = { host, port ->
                runCatching { syncManager.updateP2pPeerAddress(peer.deviceId, host, port) }
                    .onSuccess {
                        Toast.makeText(context, "Address saved", Toast.LENGTH_SHORT).show()
                        peerToEdit = null
                    }
                    .onFailure {
                        Toast.makeText(context, it.message ?: "Invalid address", Toast.LENGTH_LONG).show()
                    }
            }
        )
    }

    peerToRemove?.let { peer ->
        AlertDialog(
            onDismissRequest = { peerToRemove = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Remove ${peer.name}?") },
            text = { Text("This device will stop syncing with it until you pair it again. Local data is kept on both devices.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        syncManager.removeP2pPeer(peer.deviceId)
                        peerToRemove = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { peerToRemove = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PeerRow(
    peer: SyncPeerInfo,
    onEditAddress: () -> Unit,
    onRemove: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    peer.name.ifBlank { "Linked device" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (peer.lastSeenAt > 0) "Last seen ${formatTime(peer.lastSeenAt)}" else "Ready to pair",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = ProductiveColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        StatusRow(
            icon = Icons.Default.Wifi,
            label = "Address",
            value = peer.host?.takeIf { it.isNotBlank() }?.let {
                "$it:${peer.port}"
            } ?: "Found on local network"
        )
        Spacer(modifier = Modifier.height(4.dp))
        StatusRow(
            icon = Icons.Default.Fingerprint,
            label = "Device ID",
            value = shortDeviceId(peer.deviceId)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onEditAddress) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Address")
            }
            TextButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun PairingCodeDialog(
    invite: PairingInvite,
    deviceName: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit
) {
    var now by remember(invite) { mutableStateOf(System.currentTimeMillis()) }
    val qr = remember(invite.toUri()) {
        runCatching { generateQr(invite.toUri()) }.getOrNull()
    }
    val remaining = (invite.expiresAt - now).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60

    LaunchedEffect(invite) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Pair with $deviceName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Scan this with Soul Track on the other device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (qr != null) {
                        Image(
                            bitmap = qr.asImageBitmap(),
                            contentDescription = "Pairing QR code",
                            modifier = Modifier
                                .size(252.dp)
                                .padding(10.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(252.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Could not draw the code. Copy the link instead.",
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    if (remaining > 0) {
                        "Expires in %d:%02d".format(minutes, seconds)
                    } else {
                        "This code has expired"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remaining > 0) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy link")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share link")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun PastePairingDialog(
    onDismiss: () -> Unit,
    onPair: (String, () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Link, contentDescription = null) },
        title = { Text("Paste pairing link") },
        text = {
            Column {
                Text(
                    "Paste the soultrack:// link shared by the other device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        error = null
                    },
                    label = { Text("Pairing link") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
                TextButton(
                    onClick = {
                        clipboardText(context)?.let {
                            text = it
                            error = null
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Paste from clipboard")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isBlank()) {
                        error = "Paste a pairing link first"
                    } else {
                        onPair(text.trim()) { onDismiss() }
                    }
                }
            ) {
                Text("Pair")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DeviceNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
        title = { Text("Device name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name shown to paired devices") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim().ifBlank { "Soul Track device" }) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PeerAddressDialog(
    peer: SyncPeerInfo,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var host by remember(peer.deviceId) { mutableStateOf(peer.host.orEmpty()) }
    var port by remember(peer.deviceId) { mutableStateOf(peer.port.takeIf { it > 0 }?.toString() ?: "47831") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Wifi, contentDescription = null) },
        title = { Text("Address for ${peer.name}") },
        text = {
            Column {
                Text(
                    "Automatic discovery works on the same Wi-Fi. For internet sync, enter a reachable public IP, IPv6 address, or VPN/tailnet address. A port forward may be needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = host,
                    onValueChange = {
                        host = it
                        error = null
                    },
                    label = { Text("Host or IP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        port = it.filter(Char::isDigit).take(5)
                        error = null
                    },
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedPort = port.toIntOrNull()
                    when {
                        host.isBlank() -> error = "Enter a host or IP address"
                        parsedPort == null || parsedPort !in 1..65535 -> error = "Port must be 1-65535"
                        else -> onSave(host.trim(), parsedPort)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    SurfaceCard(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        border = null,
        contentPadding = PaddingValues(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun StatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

private fun pairWithText(
    context: Context,
    syncManager: SyncManager,
    text: String,
    onSuccess: (SyncPeerInfo) -> Unit
) {
    runCatching { syncManager.pairDevice(text.trim()) }
        .onSuccess(onSuccess)
        .onFailure {
            Toast.makeText(
                context,
                it.message ?: "Could not pair with that code",
                Toast.LENGTH_LONG
            ).show()
        }
}

private fun generateQr(content: String, size: Int = 720): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8")
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        val row = y * size
        for (x in 0 until size) {
            pixels[row + x] = if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

private fun copyPairingLink(context: Context, link: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Soul Track pairing link", link))
}

private fun clipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    return clipboard.primaryClip?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)?.coerceToText(context)?.toString()
}

private fun sharePairingLink(context: Context, link: String) {
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, link)
        putExtra(Intent.EXTRA_SUBJECT, "Soul Track device pairing")
    }
    context.startActivity(Intent.createChooser(share, "Share pairing link"))
}

private fun shortDeviceId(id: String): String =
    if (id.length <= 18) id else "${id.take(10)}…${id.takeLast(6)}"

private fun formatTime(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
