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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.dailytrack.SoulTrackApp
import io.github.dailytrack.data.api.QuotesApi
import io.github.dailytrack.ui.Screen
import io.github.dailytrack.service.FocusDndManager
import io.github.dailytrack.service.FocusSettings
import io.github.dailytrack.service.NoiseMonitor
import io.github.dailytrack.ui.components.GhostCta
import io.github.dailytrack.ui.components.PillChip
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.ScreenSection
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.ThemeMode
import io.github.dailytrack.ui.theme.ThemeSettings
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    // Empty string means "auto": the app then calibrates from your own history.
    var productiveMinutes by remember {
        mutableStateOf(ProductTargets.productiveMinutes.takeIf { it > 0 }?.toString() ?: "")
    }
    var learningMinutes by remember {
        mutableStateOf(ProductTargets.learningMinutes.takeIf { it > 0 }?.toString() ?: "")
    }
    var exerciseMinutes by remember {
        mutableStateOf(ProductTargets.exerciseMinutes.takeIf { it > 0 }?.toString() ?: "")
    }
    var sleepTarget by remember {
        mutableStateOf(ProductTargets.sleepHours.takeIf { it > 0f }?.toString() ?: "")
    }
    var maintenanceMode by remember { mutableStateOf(ProductTargets.maintenanceMode) }
    var healthEnabled by remember { mutableStateOf(AppFeatures.healthEnabled) }
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

    ScreenList(
        title = "Settings",
        subtitle = "Tune Soul Track to your day",
        navController = navController,
        compact = true
    ) {
        item {
            SectionTitle(
                title = "Discover",
                subtitle = "Everything beyond the timer",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            SettingsSection(title = "Explore", icon = Icons.Default.Explore) {
                Text(
                    "Sleep, exercise, nutrition, body, journal, goals, reminders, history, analytics, reports and achievements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                PrimaryCta(
                    text = "Open Explore",
                    icon = Icons.Default.Explore,
                    onClick = { navController.navigate(Screen.Explore.route) }
                )
            }
        }

        item {
            SectionTitle(
                title = "Preferences",
                subtitle = "Targets, privacy, focus and look",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            SettingsSection(title = "Targets", icon = Icons.Default.Flag) {
                Text(
                    "Leave a field empty and the target is calibrated from your own tracked days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TargetInput("Productive minutes/day", productiveMinutes) {
                    productiveMinutes = it
                    it.filter(Char::isDigit).toIntOrNull()
                        ?.let { v -> ProductTargets.setProductiveMinutes(v, context) }
                }
                TargetInput("Learning minutes/day", learningMinutes) {
                    learningMinutes = it
                    it.filter(Char::isDigit).toIntOrNull()
                        ?.let { v -> ProductTargets.setLearningMinutes(v, context) }
                }
                TargetInput("Exercise minutes/day", exerciseMinutes) {
                    exerciseMinutes = it
                    it.filter(Char::isDigit).toIntOrNull()
                        ?.let { v -> ProductTargets.setExerciseMinutes(v, context) }
                }
                TargetInput("Sleep target (hours)", sleepTarget) {
                    sleepTarget = it
                    it.filter { c -> c.isDigit() || c == '.' }.toFloatOrNull()
                        ?.let { v -> ProductTargets.setSleepHours(v, context) }
                }
            }
        }

        item {
            SettingsSection(title = "Features", icon = Icons.Default.FavoriteBorder) {
                Text(
                    "Soul Track is a time tracker first. Sleep, exercise, nutrition and the body "
                        + "dashboard are optional - turning them off stops the health targets, "
                        + "health insights and growth weights, and your history stays untouched.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                PrivacySwitch("Health & body layer", Icons.Default.FavoriteBorder, healthEnabled) {
                    healthEnabled = it
                    AppFeatures.setHealthEnabled(it, context)
                }
            }
        }

        item {
            SettingsSection(title = "Privacy", icon = Icons.Default.Shield) {
                PrivacySwitch("Maintenance mode", Icons.Default.Pause, maintenanceMode) {
                    maintenanceMode = it
                    ProductTargets.setMaintenanceMode(it, context)
                }
            }
        }

        item {
            SettingsSection(title = "Focus & Permissions", icon = Icons.Default.NotificationsOff) {
                FocusPermissionSection()
            }
        }

        item {
            val modes = listOf(ThemeMode.AMOLED, ThemeMode.SOFT_DARK, ThemeMode.LIGHT, ThemeMode.SYSTEM)
            val currentMode = ThemeSettings.mode
            val dynamic = ThemeSettings.dynamicColor

            SettingsSection(title = "Appearance", icon = Icons.Default.Palette) {
                Text(
                    "Theme",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    modes.forEach { mode ->
                        PillChip(
                            label = when (mode) {
                                ThemeMode.AMOLED -> "AMOLED"
                                ThemeMode.SOFT_DARK -> "Soft dark"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.SYSTEM -> "System"
                            },
                            selected = currentMode == mode,
                            onClick = { ThemeSettings.setMode(mode, context) },
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "AMOLED is pure black, soft dark is #0E0E13. Both keep the mint ring accents.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PrivacySwitch(
                        "Dynamic colour",
                        Icons.Default.Palette,
                        dynamic
                    ) { ThemeSettings.setDynamicColor(it, context) }
                }
            }
        }

        item {
            val wasting = ThemeSettings.wastingThreshold
            val productive = ThemeSettings.productiveThreshold

            SettingsSection(title = "Day ring", icon = Icons.Default.DonutSmall) {
                Text(
                    "The outer ring writes a colour into each day. These two numbers decide which one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                ThresholdSlider(
                    label = "Wasting time above",
                    value = wasting,
                    valueLabel = "${(wasting * 100).toInt()}% distracted",
                    valueRange = 0.05f..0.60f
                ) { ThemeSettings.setWastingThreshold(it, context) }
                ThresholdSlider(
                    label = "Not productive below",
                    value = productive,
                    valueLabel = "${(productive * 100).toInt()}% of planned focus",
                    valueRange = 0.10f..0.90f
                ) { ThemeSettings.setProductiveThreshold(it, context) }
            }
        }

        item {
            SectionTitle(
                title = "Data & connections",
                subtitle = "Sync, quotes, tracking and alerts",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            SettingsSection(title = "Sync", icon = Icons.Default.Devices) {
                val app = context.applicationContext as SoulTrackApp
                val syncUiState by app.syncManager.uiState.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Device Sync", fontWeight = FontWeight.Medium)
                        Text(
                            when {
                                !syncUiState.p2pEnabled -> "Not set up"
                                syncUiState.p2pPeers.isEmpty() -> "On · no paired devices"
                                syncUiState.p2pPeers.size == 1 -> "On · 1 paired device"
                                else -> "On · ${syncUiState.p2pPeers.size} paired devices"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (syncUiState.p2pEnabled) MaterialTheme.colorScheme.primary
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
                ActionButton("Sync Settings", Icons.Default.Devices) {
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
                            "Downloads public quotes automatically (every 3 days or when running low). Sends nothing from your device.",
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
            SectionTitle(
                title = "About",
                subtitle = "Version, credits and support",
                modifier = Modifier.padding(top = 8.dp)
            )
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
                            "v${io.github.dailytrack.BuildConfig.VERSION_NAME}",
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
            ScreenSection(title = "Support the Developer", leadingIcon = Icons.Default.Favorite) {
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
                GhostCta(
                    text = "Buy Me a Coffee",
                    icon = Icons.Default.Coffee,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/kusal630"))
                        context.startActivity(intent)
                    }
                )
            }
        }

        item {
            SettingsSection(title = "Danger Zone", icon = Icons.Default.DeleteForever) {
                ActionButton("Delete All Data", Icons.Default.Delete, isDestructive = true) {
                    showDeleteAllDialog = true
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
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
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }, shape = RoundedCornerShape(50)) {
                    Text("Cancel")
                }
            }
        )
    }
}

/** A titled settings block: same card language as [ScreenSection] everywhere else. */
@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ScreenSection(
        title = title,
        leadingIcon = icon,
        content = content
    )
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
private fun FocusPermissionSection() {
    val context = LocalContext.current
    var autoDnd by remember { mutableStateOf(FocusSettings.isAutoDndEnabled(context)) }
    var noiseEnabled by remember { mutableStateOf(FocusSettings.isNoiseSensingEnabled(context)) }
    var recommendationsEnabled by remember { mutableStateOf(FocusSettings.isRecommendationsEnabled(context)) }
    var startRitual by remember { mutableStateOf(FocusSettings.isStartRitualEnabled(context)) }
    var stopWarning by remember { mutableStateOf(FocusSettings.isStopWarningEnabled(context)) }
    var phoneAway by remember { mutableStateOf(FocusSettings.isPhoneAwayPromptEnabled(context)) }
    var breakCoach by remember { mutableStateOf(FocusSettings.isBreakCoachEnabled(context)) }
    var budgetMinutes by remember { mutableIntStateOf(FocusSettings.getDistractionBudgetMinutes(context)) }
    var dndGranted by remember { mutableStateOf(FocusDndManager.hasAccess(context)) }
    var micGranted by remember { mutableStateOf(NoiseMonitor.hasPermission(context)) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                dndGranted = FocusDndManager.hasAccess(context)
                micGranted = NoiseMonitor.hasPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val dndLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { dndGranted = FocusDndManager.hasAccess(context) }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
        if (granted) {
            FocusSettings.setNoiseSensingEnabled(context, true)
            noiseEnabled = true
            if (io.github.dailytrack.service.TimerStateStore.load(context).isRunning) {
                NoiseMonitor.start(context)
            }
        } else {
            FocusSettings.setNoiseSensingEnabled(context, false)
            noiseEnabled = false
            NoiseMonitor.stop()
        }
    }

    PermissionToggle(
        title = "Auto Do Not Disturb",
        description = "Silence notifications automatically during Productive and Learning sessions and deep-focus task picks.",
        checked = autoDnd,
        granted = dndGranted,
        grantedLabel = "Permission granted",
        missingLabel = "Tap to allow Do Not Disturb access",
        onCheckedChange = { enabled ->
            autoDnd = enabled
            FocusSettings.setAutoDndEnabled(context, enabled)
            if (enabled && !dndGranted) dndLauncher.launch(FocusDndManager.settingsIntent())
        }
    )

    PermissionToggle(
        title = "Ambient noise sensing",
        description = "Reads how quiet the room is while a focus session runs. Audio is never recorded, saved or sent anywhere - only a quietness score is kept for 5 minutes.",
        checked = noiseEnabled,
        granted = micGranted,
        grantedLabel = "Microphone available",
        missingLabel = "Tap to grant microphone access",
        onCheckedChange = { enabled ->
            if (enabled && !micGranted) {
                micLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            } else {
                noiseEnabled = enabled
                FocusSettings.setNoiseSensingEnabled(context, enabled)
                if (enabled && io.github.dailytrack.service.TimerStateStore.load(context).isRunning) {
                    NoiseMonitor.start(context)
                } else if (!enabled) {
                    NoiseMonitor.stop()
                }
            }
        }
    )

    PermissionToggle(
        title = "Task recommendations",
        description = "Suggests which task fits this moment, using your own history, deadlines and focus. Runs entirely on the device.",
        checked = recommendationsEnabled,
        granted = true,
        grantedLabel = "On device only",
        missingLabel = "",
        onCheckedChange = { enabled ->
            recommendationsEnabled = enabled
            FocusSettings.setRecommendationsEnabled(context, enabled)
        }
    )

    PermissionToggle(
        title = "5-second start ritual",
        description = "Before a block begins, a short countdown asks you to put the phone away and name the one thing you will do.",
        checked = startRitual,
        granted = true,
        grantedLabel = "Guided start",
        missingLabel = "",
        onCheckedChange = { enabled ->
            startRitual = enabled
            FocusSettings.setStartRitualEnabled(context, enabled)
        }
    )

    PermissionToggle(
        title = "Stop warning",
        description = "When you end a focus block early, asks once whether it was a real stop or a reflex. Keeps accidental taps from erasing a session.",
        checked = stopWarning,
        granted = true,
        grantedLabel = "Confirmation on stop",
        missingLabel = "",
        onCheckedChange = { enabled ->
            stopWarning = enabled
            FocusSettings.setStopWarningEnabled(context, enabled)
        }
    )

    PermissionToggle(
        title = "Phone-away prompt",
        description = "During the start ritual, reminds you to leave the phone out of reach. This is the single highest-leverage change for deep work.",
        checked = phoneAway,
        granted = true,
        grantedLabel = "Shown at start",
        missingLabel = "",
        onCheckedChange = { enabled ->
            phoneAway = enabled
            FocusSettings.setPhoneAwayPromptEnabled(context, enabled)
        }
    )

    PermissionToggle(
        title = "Break coach",
        description = "After a completed block, suggests a concrete recovery activity instead of opening another feed.",
        checked = breakCoach,
        granted = true,
        grantedLabel = "Shown after blocks",
        missingLabel = "",
        onCheckedChange = { enabled ->
            breakCoach = enabled
            FocusSettings.setBreakCoachEnabled(context, enabled)
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = "Distraction budget",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Sends one time check when distracting apps pass your daily budget. It never blocks anything - you decide whether to continue or step away. Needs Usage Access.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0 to "Off", 60 to "1h", 90 to "1h 30m", 120 to "2h", 180 to "3h").forEach { (m, label) ->
                FilterChip(
                    selected = budgetMinutes == m,
                    onClick = {
                        budgetMinutes = m
                        FocusSettings.setDistractionBudgetMinutes(context, m)
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
private fun PermissionToggle(
    title: String,
    description: String,
    checked: Boolean,
    granted: Boolean,
    grantedLabel: String,
    missingLabel: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!granted && missingLabel.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    missingLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (granted && grantedLabel.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    grantedLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThresholdSlider(
    label: String,
    value: Float,
    valueLabel: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
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

/** Outlined pill row action - 52dp tall so it is comfortably tappable. */
@Composable
fun ActionButton(label: String, icon: ImageVector, isDestructive: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(Dimens.ButtonHeight),
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            1.dp,
            if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = if (isDestructive) {
            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}
