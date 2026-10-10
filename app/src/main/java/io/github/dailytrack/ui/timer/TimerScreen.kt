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

package io.github.dailytrack.ui.timer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.service.TimerForegroundService
import io.github.dailytrack.ui.theme.*
import io.github.dailytrack.ui.navigateToHome
import io.github.dailytrack.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlin.math.sin

private fun getCategoryColor(type: String): Color = when (type) {
    "PRODUCTIVE" -> ProductiveColor
    "WASTED" -> WastedColor
    "EXERCISE" -> ExerciseColor
    "LEARNING" -> LearningColor
    "SOCIAL" -> SocialColor
    "SLEEP" -> SleepColor
    "RECOVERY" -> RecoveryColor
    else -> NeutralColor
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val activeSession by viewModel.activeSession.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var sessionTitle by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableLongStateOf(0L) }
    var showCategoryError by remember { mutableStateOf(false) }
    var showTitleError by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val isRunning = activeSession != null
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startTimerService(
                context, viewModel, sessionTitle, selectedCategoryId, categories
            )
        }
    }

    LaunchedEffect(activeSession) {
        val session = activeSession
        if (session != null) {
            while (activeSession === session) {
                elapsedSeconds = ((System.currentTimeMillis() - session.startTime) / 1000)
                    .coerceAtLeast(0L)
                delay(1000)
            }
        } else {
            elapsedSeconds = 0
        }
    }

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60

    val selectedCategory = categories[selectedCategoryId]
    val categoryType = selectedCategory?.type ?: ""
    val categoryColor = if (selectedCategoryId > 0L) getCategoryColor(categoryType) else MaterialTheme.colorScheme.primary

    val animatedCategoryColor by animateColorAsState(
        targetValue = categoryColor,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "category_color"
    )

    val glowAlpha = if (isRunning) {
        val infiniteTransition = rememberInfiniteTransition(label = "glow")
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_pulse"
        )
        pulse
    } else 0f

    val ringProgress = if (isRunning) {
        val totalSeconds = 8 * 3600L
        (elapsedSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
    } else 0f

    val animatedRingProgress by animateFloatAsState(
        targetValue = ringProgress,
        animationSpec = tween(300, easing = LinearEasing),
        label = "ring_progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timer") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateToHome() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.radialGradient(
                        colors = if (isRunning) {
                            listOf(
                                animatedCategoryColor.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.05f),
                                MaterialTheme.colorScheme.surface
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.surface
                            )
                        },
                        radius = 800f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(320.dp)
                ) {
                    if (isRunning) {
                        Canvas(
                            modifier = Modifier
                                .size(360.dp)
                                .blur(32.dp)
                        ) {
                            drawCircle(
                                color = animatedCategoryColor.copy(alpha = glowAlpha * 0.4f),
                                radius = size.minDimension / 2
                            )
                        }
                        Canvas(
                            modifier = Modifier
                                .size(340.dp)
                                .blur(12.dp)
                        ) {
                            drawCircle(
                                color = animatedCategoryColor.copy(alpha = glowAlpha * 0.25f),
                                radius = size.minDimension / 2
                            )
                        }
                        Canvas(
                            modifier = Modifier
                                .size(320.dp)
                                .blur(4.dp)
                        ) {
                            drawCircle(
                                color = animatedCategoryColor.copy(alpha = glowAlpha * 0.15f),
                                radius = size.minDimension / 2
                            )
                        }
                    }

                    Canvas(modifier = Modifier.size(300.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                        val arcSize = Size(diameter, diameter)

                        drawArc(
                            color = Color.Gray.copy(alpha = 0.1f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        if (isRunning) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        animatedCategoryColor,
                                        animatedCategoryColor.copy(alpha = 0.6f),
                                        animatedCategoryColor,
                                        animatedCategoryColor.copy(alpha = 0.6f),
                                        animatedCategoryColor
                                    )
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * animatedRingProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format(
                                java.util.Locale.US,
                                "%02d:%02d:%02d",
                                hours, minutes, seconds
                            ),
                            style = MaterialTheme.typography.displayLarge,
                            color = if (isRunning) animatedCategoryColor
                            else MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRunning) {
                                activeSession?.title?.ifBlank {
                                    categories[activeSession?.categoryId]?.name ?: "Tracking"
                                } ?: "Tracking"
                            } else "Ready to focus",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (isRunning) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedCategory?.name ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = animatedCategoryColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!isRunning) {
                    Text(
                        text = "Choose Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (showCategoryError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val quickCategories = categories.values
                        .filter { !it.archived }
                        .sortedBy { it.name }
                        .take(8)

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickCategories) { category ->
                            val isSelected = selectedCategoryId == category.id
                            val chipColor = getCategoryColor(category.type)

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategoryId = category.id
                                    showCategoryError = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                label = {
                                    Text(
                                        category.name,
                                        maxLines = 1,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor.copy(alpha = 0.2f),
                                    selectedLabelColor = chipColor,
                                    selectedLeadingIconColor = chipColor,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = chipColor.copy(alpha = 0.2f),
                                    selectedBorderColor = chipColor,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }

                    if (quickCategories.isEmpty()) {
                        Text(
                            text = "No categories yet. Create one in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = sessionTitle,
                        onValueChange = {
                            sessionTitle = it
                            showTitleError = false
                        },
                        label = {
                            Text("Session name (optional)")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (selectedCategoryId == 0L) {
                                showCategoryError = true
                                return@Button
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED

                                if (!hasPermission) {
                                    showPermissionDialog = true
                                    return@Button
                                }
                            }

                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            startTimerService(
                                context, viewModel, sessionTitle, selectedCategoryId, categories
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Start Tracking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.stopSession()
                            context.stopService(
                                Intent(context, TimerForegroundService::class.java)
                            )
                            sessionTitle = ""
                            selectedCategoryId = 0L
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Stop",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = animatedCategoryColor.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            DetailRow("Duration", String.format(
                                java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds
                            ))
                            if ((activeSession?.title ?: "").isNotBlank()) {
                                DetailRow("Title", activeSession?.title ?: "")
                            }
                            val catName = activeSession?.categoryId?.let { categories[it]?.name } ?: ""
                            if (catName.isNotBlank()) {
                                DetailRow("Category", catName)
                            }
                            DetailRow("Started", java.time.Instant.ofEpochMilli(activeSession?.startTime ?: 0L)
                                .atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Notification Permission") },
            text = { Text("Soul Track needs notification permission to show the running timer.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun startTimerService(
    context: android.content.Context,
    viewModel: MainViewModel,
    sessionTitle: String,
    selectedCategoryId: Long,
    categories: Map<Long, io.github.dailytrack.data.db.entity.CategoryEntity>
) {
    val catName = categories[selectedCategoryId]?.name ?: ""
    val catType = categories[selectedCategoryId]?.type ?: ""
    viewModel.startSession(sessionTitle, selectedCategoryId)
    val intent = Intent(context, TimerForegroundService::class.java).apply {
        putExtra(TimerForegroundService.EXTRA_START_TIME, System.currentTimeMillis())
        putExtra(TimerForegroundService.EXTRA_TITLE, sessionTitle.ifBlank { catName })
        putExtra(TimerForegroundService.EXTRA_CATEGORY, catName)
        putExtra(TimerForegroundService.EXTRA_CATEGORY_TYPE, catType)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}
