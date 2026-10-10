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


package io.github.dailytrack.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.db.entity.SessionEntity
import io.github.dailytrack.ui.Screen
import io.github.dailytrack.ui.navigateSingleTop
import io.github.dailytrack.ui.components.*
import io.github.dailytrack.ui.theme.*
import io.github.dailytrack.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val todaySessions by viewModel.todaySessions.collectAsState()
    val todayCoverage by viewModel.todayCoverage.collectAsState()
    val growthScore by viewModel.growthScore.collectAsState()
    val activeInsights by viewModel.activeInsights.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val loopStatus by viewModel.loopStatus.collectAsState()

    val completedSessions = todaySessions.filter { !it.isActive }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("DailyTrack")
                        Text(
                            selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigateSingleTop(Screen.Habits.route) }) {
                        Icon(Icons.Default.Flag, contentDescription = "Habits")
                    }
                    IconButton(onClick = { navController.navigateSingleTop(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                DatePickerCard(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.selectDate(it) }
                )
            }

            if (activeSession != null) {
                item {
                    ActiveSessionCard(
                        activeSession = activeSession!!,
                        categoryName = activeSession!!.categoryId?.let { categories[it]?.name } ?: "",
                        onStop = { viewModel.stopSession() }
                    )
                }
            }

            item {
                TimeTrackingCard(coverage = todayCoverage)
            }

            if (todayCoverage != null &&
                (todayCoverage!!.trackedSeconds + todayCoverage!!.untrackedSeconds) > 0
            ) {
                item {
                    TimeBreakdownPieChart(
                        data = listOf(
                            PieChartData("Productive", (todayCoverage!!.productiveSeconds + todayCoverage!!.learningSeconds).toFloat(), ProductiveColor),
                            PieChartData("Exercise", todayCoverage!!.exerciseSeconds.toFloat(), ExerciseColor),
                            PieChartData("Sleep", todayCoverage!!.sleepSeconds.toFloat(), SleepColor),
                            PieChartData("Social", todayCoverage!!.socialSeconds.toFloat(), SocialColor),
                            PieChartData("Recovery", todayCoverage!!.recoverySeconds.toFloat(), RecoveryColor),
                            PieChartData("Wasted", todayCoverage!!.wastedSeconds.toFloat(), WastedColor),
                            PieChartData("Untracked", todayCoverage!!.untrackedSeconds.toFloat(), NeutralColor)
                        ).filter { it.value > 0f }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { navController.navigateSingleTop(Screen.Timer.route) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (activeSession != null) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (activeSession != null) "View Timer" else "Start Timer")
                    }
                    FilledTonalButton(
                        onClick = { navController.navigateSingleTop(Screen.Growth.route) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Growth")
                    }
                }
            }

            if (loopStatus?.isLoopDetected == true || loopStatus?.comfortZoneWarning == true) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (loopStatus?.isLoopDetected == true)
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (loopStatus?.isLoopDetected == true) Icons.Default.Repeat else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (loopStatus?.isLoopDetected == true)
                                    MaterialTheme.colorScheme.onErrorContainer
                                else
                                    MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    if (loopStatus?.isLoopDetected == true) "Routine Loop Detected" else "Comfort Zone Warning",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (loopStatus?.isLoopDetected == true)
                                        "Your routine has been similar for ${loopStatus?.consecutiveDays} days. Try something new to grow."
                                    else
                                        "Your routine lacks variety. Add new activities to break out of your comfort zone.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            item {
                GrowthScoreCard(
                    score = growthScore,
                    hasData = (todayCoverage?.trackedSeconds ?: 0L) > 0L
                )
            }

            if (completedSessions.isNotEmpty()) {
                item {
                    SectionHeader(title = "Completed Today (${completedSessions.size})")
                }
                items(completedSessions, key = { it.id }) { session ->
                    CompletedSessionCard(session = session, categories = categories)
                }
            }

            if (activeInsights.isNotEmpty()) {
                item {
                    SectionHeader(title = "Insights")
                }
                items(activeInsights.take(3)) { insight ->
                    InsightCard(insight = insight, onDismiss = { viewModel.dismissInsight(insight.id) })
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun DatePickerCard(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onDateSelected(selectedDate.minusDays(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
            }
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                val today = LocalDate.now()
                if (selectedDate < today) onDateSelected(selectedDate.plusDays(1))
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
            }
        }
    }
}

@Composable
fun ActiveSessionCard(
    activeSession: SessionEntity,
    categoryName: String,
    onStop: () -> Unit
) {
    var elapsed by remember { mutableLongStateOf(System.currentTimeMillis() - activeSession.startTime) }

    LaunchedEffect(activeSession.startTime) {
        while (true) {
            elapsed = System.currentTimeMillis() - activeSession.startTime
            kotlinx.coroutines.delay(1000)
        }
    }

    val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = activeSession.title.ifBlank { "Active Session" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (categoryName.isNotBlank()) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                FilledTonalButton(onClick = onStop) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TimeTrackingCard(coverage: io.github.dailytrack.engine.TimeCoverage?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Time Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val hasTracked = (coverage?.trackedSeconds ?: 0) > 0

            if (hasTracked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        "Productive",
                        formatDurationHMS((coverage?.productiveSeconds ?: 0) + (coverage?.learningSeconds ?: 0)),
                        ProductiveColor
                    )
                    MetricItem(
                        "Exercise",
                        formatDurationHMS(coverage?.exerciseSeconds ?: 0),
                        ExerciseColor
                    )
                    MetricItem(
                        "Sleep",
                        formatDurationHMS(coverage?.sleepSeconds ?: 0),
                        SleepColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(
                        "Wasted",
                        formatDurationHMS(coverage?.wastedSeconds ?: 0),
                        WastedColor
                    )
                    MetricItem(
                        "Tracked",
                        formatDurationHMS(coverage?.trackedSeconds ?: 0),
                        MaterialTheme.colorScheme.primary
                    )
                    MetricItem(
                        "Free",
                        formatDurationHMS(coverage?.untrackedSeconds ?: 0),
                        NeutralColor
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { (coverage?.trackedRatio ?: 0.0).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            } else {
                Text(
                    text = "No time tracked today. Start a timer to begin tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CompletedSessionCard(
    session: SessionEntity,
    categories: Map<Long, io.github.dailytrack.data.db.entity.CategoryEntity>
) {
    val duration = if (session.endTime != null) {
        session.endTime - session.startTime
    } else 0L

    val hours = TimeUnit.MILLISECONDS.toHours(duration)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60

    val categoryName = session.categoryId?.let { categories[it]?.name } ?: ""
    val startTimeStr = java.time.Instant.ofEpochMilli(session.startTime)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    val endTimeStr = session.endTime?.let {
        java.time.Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    } ?: ""

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = ProductiveColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title.ifBlank { "Session" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row {
                    if (categoryName.isNotBlank()) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$startTimeStr - $endTimeStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun GrowthScoreCard(score: Double, hasData: Boolean = true) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "score"
    )

    val scoreColor = when {
        !hasData -> NeutralColor
        score >= 70 -> ProductiveColor
        score >= 40 -> ExerciseColor
        else -> WastedColor
    }

    val animatedScoreColor by animateColorAsState(
        targetValue = scoreColor,
        animationSpec = tween(600),
        label = "score_color"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = scoreColor.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Growth Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (hasData) "Today's progress" else "No tracked time yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (animatedScore / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .width(120.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = animatedScoreColor,
                    trackColor = animatedScoreColor.copy(alpha = 0.15f)
                )
            }

            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(72.dp)) {
                    val strokeWidth = 6.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color = Color.Gray.copy(alpha = 0.12f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = animatedScoreColor,
                        startAngle = -90f,
                        sweepAngle = 360f * (animatedScore / 100f).coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${score.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    color = animatedScoreColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InsightCard(insight: io.github.dailytrack.data.db.entity.InsightEntity, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (insight.severity) {
                "CRITICAL" -> MaterialTheme.colorScheme.errorContainer
                "WARNING" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                "CAUTION" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodySmall
                )
                if (insight.actionLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = insight.actionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun formatDurationHMS(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
        minutes > 0 -> String.format(java.util.Locale.US, "%02d:%02d", minutes, secs)
        else -> String.format(java.util.Locale.US, "%02d", secs)
    }
}
