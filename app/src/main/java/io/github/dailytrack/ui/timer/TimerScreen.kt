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
import io.github.dailytrack.ui.components.*
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
    val focusGoalSeconds by viewModel.focusGoalSeconds.collectAsState()
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startSession(sessionTitle, selectedCategoryId)
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
        // Fill the ring against the user's own focus goal (or an honest 8h
        // fallback) so the timer and the dashboard ring agree.
        val totalSeconds = focusGoalSeconds.takeIf { it > 0L } ?: (8 * 3600L)
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
                title = {
                    Text(
                        "Timer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateToHome() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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

                    val ringTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    Canvas(modifier = Modifier.size(300.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                        val arcSize = Size(diameter, diameter)

                        drawArc(
                            color = ringTrackColor,
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
                                text = activeSession?.categoryId?.let { categories[it]?.name } ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = animatedCategoryColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!isRunning) {
                    SectionTitle(title = "Choose category")
                    if (showCategoryError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pick a category before starting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.TitleGap))

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
                                modifier = Modifier.height(48.dp),
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
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = chipColor,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }

                    if (quickCategories.isEmpty()) {
                        EmptyState(
                            title = "No categories yet",
                            message = "Create a category in Settings, then come back to start tracking.",
                            icon = Icons.Default.Category,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        shape = MaterialTheme.shapes.medium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    PrimaryCta(
                        text = "Start Tracking",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            if (selectedCategoryId == 0L) {
                                showCategoryError = true
                                return@PrimaryCta
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED

                                if (!hasPermission) {
                                    showPermissionDialog = true
                                    return@PrimaryCta
                                }
                            }

                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.startSession(sessionTitle, selectedCategoryId)
                        }
                    )
                } else {
                    DangerCta(
                        text = "Stop",
                        icon = Icons.Default.Stop,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.stopSession()
                            sessionTitle = ""
                            selectedCategoryId = 0L
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SurfaceCard(
                        containerColor = animatedCategoryColor.copy(alpha = 0.08f),
                        contentPadding = PaddingValues(20.dp)
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

/**
 * Stop / destructive counterpart to [PrimaryCta]: same pill geometry and height,
 * filled with the error token so it reads as the one action that matters while
 * a session is running.
 */
@Composable
private fun DangerCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val shape = RoundedCornerShape(50)
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ButtonHeight)
            .clip(shape),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            hoveredElevation = 0.dp,
            focusedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

