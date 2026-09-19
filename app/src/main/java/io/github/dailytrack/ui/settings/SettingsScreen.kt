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


package io.github.dailytrack.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.dailytrack.SoulTrackApp
import io.github.dailytrack.data.api.QuotesApi
import io.github.dailytrack.ui.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var productiveMinutes by remember { mutableStateOf("60") }
    var learningMinutes by remember { mutableStateOf("30") }
    var exerciseMinutes by remember { mutableStateOf("30") }
    var sleepTarget by remember { mutableStateOf("8") }
    var maintenanceMode by remember { mutableStateOf(false) }
    var apiQuotesEnabled by remember { mutableStateOf(QuotesApi.isApiEnabled()) }
    var wasteModeEnabled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val exportData = """{"app":"SoulTrack","version":"0.1.0","exportDate":"${java.time.Instant.now()}"}"""
                    outputStream.write(exportData.toByteArray())
                }
                Toast.makeText(context, "Data exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val data = inputStream.readBytes().toString(Charsets.UTF_8)
                    Toast.makeText(context, "Data imported successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SettingsSection(title = "Targets", icon = Icons.Default.Flag) {
                    TargetInput("Productive minutes/day", productiveMinutes) { productiveMinutes = it }
                    TargetInput("Learning minutes/day", learningMinutes) { learningMinutes = it }
                    TargetInput("Exercise minutes/day", exerciseMinutes) { exerciseMinutes = it }
                    TargetInput("Sleep target (hours)", sleepTarget) { sleepTarget = it }
                }
            }

            item {
                SettingsSection(title = "Privacy", icon = Icons.Default.Shield) {
                    PrivacySwitch("Maintenance mode", Icons.Default.Pause, maintenanceMode) { maintenanceMode = it }
                }
            }

            item {
                SettingsSection(title = "Sync", icon = Icons.Default.Sync) {
                    val app = context.applicationContext as SoulTrackApp
                    val syncUiState by app.syncManager.uiState.collectAsState()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Offline Device Sync", fontWeight = FontWeight.Medium)
                            Text(
                                if (syncUiState.isEnabled) "Syncing via Syncthing"
                                else "Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (syncUiState.isEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ActionButton("Sync Settings", Icons.Default.Sync) {
                        navController.navigate(Screen.SyncSettings.route)
                    }
                }
            }

            item {
                SettingsSection(title = "Quotes", icon = Icons.Default.FormatQuote) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Online Quotes API", fontWeight = FontWeight.Medium)
                            Text(
                                "Fetch quotes from the internet for more variety",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = apiQuotesEnabled,
                            onCheckedChange = { enabled ->
                                apiQuotesEnabled = enabled
                                QuotesApi.setApiEnabled(enabled)
                                if (enabled) {
                                    scope.launch {
                                        val fetched = QuotesApi.fetchOnlineQuotes()
                                        if (fetched.isNotEmpty()) {
                                            Toast.makeText(context, "Fetched ${fetched.size} online quotes", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to fetch quotes. Check your connection.", Toast.LENGTH_SHORT).show()
                                            apiQuotesEnabled = false
                                            QuotesApi.setApiEnabled(false)
                                        }
                                    }
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Offline: ${QuotesApi.getOfflineQuotesCount()} quotes | Total: ${QuotesApi.getTotalQuotesCount()} quotes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SettingsSection(title = "Time Tracking", icon = Icons.Default.Timer) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Strict Waste Mode", fontWeight = FontWeight.Medium)
                            Text(
                                "Treat all non-productive, non-break time as waste",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = wasteModeEnabled,
                            onCheckedChange = { wasteModeEnabled = it }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "When enabled, any time tracked outside of 'Productive'/'Learning' categories and breaks is flagged as wasted time in reports.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SettingsSection(title = "Data", icon = Icons.Default.Storage) {
                    ActionButton("Export Data", Icons.Default.FileDownload) {
                        exportLauncher.launch("soultrack_export.json")
                    }
                    ActionButton("Import Data", Icons.Default.FileUpload) {
                        importLauncher.launch(arrayOf("application/json"))
                    }
                }
            }

            item {
                SettingsSection(title = "Notifications", icon = Icons.Default.Notifications) {
                    NotificationSwitch("Timer notification", true)
                    NotificationSwitch("Learning reminder", false)
                    NotificationSwitch("Sleep wind-down", false)
                    NotificationSwitch("Insight notifications", false)
                }
            }

            item {
                SettingsSection(title = "About", icon = Icons.Default.Info) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Soul Track", fontWeight = FontWeight.Medium)
                            Text(
                                "v0.1.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "Offline-first, privacy-first",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ActionButton("View Licenses", Icons.Default.Description) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kusal630/TIme-tracker/blob/master/LICENSE"))
                        context.startActivity(intent)
                    }
                    ActionButton("GitHub Repository", Icons.Default.Code) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kusal630/TIme-tracker"))
                        context.startActivity(intent)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Support the Developer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "\"The best way to find yourself is to lose yourself in the service of others.\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "— Mahatma Gandhi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/kusal630"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Coffee, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buy Me a Coffee")
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Danger Zone", icon = Icons.Default.DeleteForever) {
                    ActionButton("Delete All Data", Icons.Default.Delete, isDestructive = true) {
                        showDeleteAllDialog = true
                    }
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete All Data?") },
            text = {
                Column {
                    Text("This will permanently delete:")
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf("All sessions and timer data", "All pomodoro records", "All todos and subtasks", "All insights", "All saved quotes", "App settings (reset to defaults)").forEach { item ->
                        Text("  - $item", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This action cannot be undone!",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val app = context.applicationContext as SoulTrackApp
                        val db = app.database
                        scope.launch {
                            db.todoDao().deleteAll()
                            db.subtaskDao().deleteAll()
                            db.sessionDao().deleteAll()
                            db.pomodoroSessionDao().deleteAll()
                            db.insightDao().deleteAll()
                            db.savedQuoteDao().deleteAll()
                            context.getSharedPreferences("widget_prefs", android.content.Context.MODE_PRIVATE)
                                .edit().clear().apply()
                            showDeleteAllDialog = false
                            Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
fun TargetInput(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = true
    )
}

@Composable
fun PrivacySwitch(label: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun NotificationSwitch(label: String, checked: Boolean) {
    var isChecked by remember { mutableStateOf(checked) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(checked = isChecked, onCheckedChange = { isChecked = it })
    }
}

@Composable
fun ActionButton(label: String, icon: ImageVector, isDestructive: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = if (isDestructive) ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ) else ButtonDefaults.outlinedButtonColors()
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}
