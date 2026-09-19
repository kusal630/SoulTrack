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

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import io.github.dailytrack.sync.SyncManager
import io.github.dailytrack.sync.SyncRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    navController: NavController,
    syncManager: SyncManager
) {
    val context = LocalContext.current
    val uiState by syncManager.uiState.collectAsState()
    var showDisableDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Device Sync") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "How Sync Works",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sync is completely optional and works offline through Syncthing on your local network. No internet, no cloud, no accounts needed.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "1. Enable sync and choose a folder\n2. Install Syncthing on your other device\n3. Add the same folder in Syncthing\n4. Both devices sync automatically when on the same network",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Sync Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                uiState.deviceName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    folderPickerLauncher.launch(null)
                                } else {
                                    showDisableDialog = true
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                            SyncRepository.SyncStatus.ENABLED -> Color(0xFF2E7D32)
                            SyncRepository.SyncStatus.SYNCING -> MaterialTheme.colorScheme.primary
                            SyncRepository.SyncStatus.ERROR -> Color(0xFFC62828)
                            SyncRepository.SyncStatus.FOLDER_MISSING -> Color(0xFFF57C00)
                            SyncRepository.SyncStatus.PERMISSION_MISSING -> Color(0xFFF57C00)
                            SyncRepository.SyncStatus.DISABLED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if (uiState.isEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusRow(
                            icon = Icons.Default.Folder,
                            label = "Folder",
                            value = uiState.folderName ?: "Unknown"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusRow(
                            icon = Icons.Default.Schedule,
                            label = "Last sync",
                            value = uiState.lastSyncTime ?: "Never"
                        )
                    }

                    if (uiState.lastError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
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
                                    uiState.lastError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { syncManager.clearError() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isEnabled) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Sync Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { syncManager.manualSync() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync Now")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Syncthing Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "1. Install Syncthing from F-Droid or syncthing.net",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "2. On both devices, add the same shared folder",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "3. Accept the folder invite on the other device",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "4. Ensure both devices are on the same local network",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Sync happens automatically in the background when Syncthing is running. No internet required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Privacy & Security",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "All data stays on your devices. No internet permission, no cloud, no accounts. Syncthing handles encrypted local network transfer.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    if (showDisableDialog) {
        AlertDialog(
            onDismissRequest = { showDisableDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Disable Sync") },
            text = {
                Text("This will stop writing to the sync folder. Existing data on both devices is preserved. You can re-enable sync anytime.")
            },
            confirmButton = {
                TextButton(onClick = {
                    syncManager.disableSync()
                    showDisableDialog = false
                }) {
                    Text("Disable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
