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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.Hairline
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.ScreenSection
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.theme.WastedColor
import io.github.dailytrack.sync.SyncManager
import io.github.dailytrack.sync.SyncRepository

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

    ScreenList(
        title = "Offline Device Sync",
        subtitle = "Syncthing, on your own network",
        navController = navController
    ) {
        item {
            ScreenSection(title = "How Sync Works", leadingIcon = Icons.Default.Info) {
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

        item {
            SurfaceCard {
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
                        SyncRepository.SyncStatus.ENABLED -> ProductiveColor
                        SyncRepository.SyncStatus.SYNCING -> MaterialTheme.colorScheme.primary
                        SyncRepository.SyncStatus.ERROR -> WastedColor
                        SyncRepository.SyncStatus.FOLDER_MISSING -> ExerciseColor
                        SyncRepository.SyncStatus.PERMISSION_MISSING -> ExerciseColor
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
                    SurfaceCard(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        border = null,
                        contentPadding = PaddingValues(8.dp)
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
                                uiState.lastError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { syncManager.clearError() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.lastSyncTime == null) {
            item {
                EmptyState(
                    title = "No sync history yet",
                    message = if (uiState.isEnabled) {
                        "Run your first sync and the last sync time will show up here."
                    } else {
                        "Turn on sync to mirror your data between your own devices."
                    },
                    icon = Icons.Default.History,
                    actionLabel = if (uiState.isEnabled) "Sync now" else "Enable sync",
                    onAction = {
                        if (uiState.isEnabled) {
                            syncManager.manualSync()
                        } else {
                            folderPickerLauncher.launch(null)
                        }
                    }
                )
            }
        } else {
            item {
                ScreenSection(title = "Sync Actions", leadingIcon = Icons.Default.Sync) {
                    if (uiState.isEnabled) {
                        PrimaryCta(
                            text = "Sync Now",
                            icon = Icons.Default.Sync,
                            onClick = { syncManager.manualSync() }
                        )
                    } else {
                        PrimaryCta(
                            text = "Enable Sync",
                            icon = Icons.Default.Folder,
                            onClick = { folderPickerLauncher.launch(null) }
                        )
                    }
                }
            }
        }

        item {
            ScreenSection(title = "Syncthing Setup", leadingIcon = Icons.Default.Build) {
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

        item {
            ScreenSection(
                title = "Privacy & Security",
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
                        "All data stays on your devices. No internet permission, no cloud, no accounts. Syncthing handles encrypted local network transfer.",
                        style = MaterialTheme.typography.bodySmall
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
            title = { Text("Disable Sync") },
            text = {
                Text("This will stop writing to the sync folder. Existing data on both devices is preserved. You can re-enable sync anytime.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        syncManager.disableSync()
                        showDisableDialog = false
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Disable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableDialog = false }, shape = RoundedCornerShape(50)) {
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
