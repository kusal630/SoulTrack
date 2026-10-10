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


package io.github.dailytrack.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.db.entity.InsightEntity
import io.github.dailytrack.ui.components.*
import io.github.dailytrack.ui.theme.RecoveryColor
import io.github.dailytrack.ui.theme.SleepColor
import io.github.dailytrack.ui.theme.SocialColor
import io.github.dailytrack.ui.navigateToHome
import io.github.dailytrack.ui.viewmodel.MainViewModel
import io.github.dailytrack.ui.viewmodel.WeeklyStats

private val ProductiveGreen = Color(0xFF2E7D32)
private val WastedRed = Color(0xFFC62828)
private val FreeGray = Color(0xFF757575)
private val LearningBlue = Color(0xFF1565C0)
private val ExerciseOrange = Color(0xFFF57C00)
private val SocialPurple = Color(0xFF7B1FA2)
private val SleepTeal = Color(0xFF00897B)
private val PomodoroRed = Color(0xFFE94560)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val activeInsights by viewModel.activeInsights.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val growthScore by viewModel.growthScore.collectAsState()
    val growthResult by viewModel.growthResult.collectAsState()
    val activeTodoCount by viewModel.activeTodoCount.collectAsState()
    val completedTodoCount by viewModel.completedTodoCount.collectAsState()
    val todaySessions by viewModel.todaySessions.collectAsState()
    val todayPomodoros by viewModel.todayPomodoros.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val dailyProductivity by viewModel.dailyProductivity.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateToHome() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GrowthScoreCard(
                    growthScore = growthScore,
                    growthResult = growthResult,
                    hasData = weeklyStats.totalTrackedMinutes > 0
                )
            }

            item {
                TodoProgressCard(activeCount = activeTodoCount, completedCount = completedTodoCount)
            }

            item {
                TimeDistributionCard(weeklyStats = weeklyStats)
            }

            item {
                CategoryBreakdownCard(
                    sessions = todaySessions,
                    categories = categories,
                    pomodoros = todayPomodoros
                )
            }

            item {
                WeeklyTrendCard(dailyProductivity = dailyProductivity)
            }

            item {
                PomodoroStatsCard(
                    pomodoros = todayPomodoros,
                    weeklyStats = weeklyStats
                )
            }

            if (activeInsights.isNotEmpty()) {
                item {
                    Text(
                        "Smart Insights (${activeInsights.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(activeInsights, key = { it.id }) { insight ->
                    InsightDetailCard(
                        insight = insight,
                        onDismiss = { viewModel.dismissInsight(insight.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun GrowthScoreCard(
    growthScore: Double,
    growthResult: io.github.dailytrack.engine.GrowthScoreResult?,
    hasData: Boolean = true
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val scoreColor = when {
                !hasData -> FreeGray
                growthScore >= 70 -> ProductiveGreen
                growthScore >= 40 -> ExerciseOrange
                else -> WastedRed
            }

            ProgressRing(
                progress = (growthScore / 100.0).coerceIn(0.0, 1.0).toFloat(),
                size = 100.dp,
                strokeWidth = 10.dp,
                color = scoreColor,
                centerContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${growthScore.toInt()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                        Text(
                            text = "Growth",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Daily Growth Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (growthResult != null) {
                    ScoreBreakdown("Learning", growthResult.learningComponent, LearningBlue)
                    ScoreBreakdown("Productive", growthResult.productiveComponent, ProductiveGreen)
                    ScoreBreakdown("Exercise", growthResult.exerciseComponent, ExerciseOrange)
                    ScoreBreakdown("Consistency", growthResult.consistencyComponent, PomodoroRed)
                } else {
                    Text(
                        "Start tracking to see your growth",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreBreakdown(label: String, score: Double, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(70.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { (score / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${score.toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TodoProgressCard(activeCount: Int, completedCount: Int) {
    val total = activeCount + completedCount

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PieChart(
                slices = listOf(
                    PieChartSlice(completedCount.toFloat(), ProductiveGreen, "Completed"),
                    PieChartSlice(activeCount.toFloat(), ExerciseOrange, "Pending"),
                ),
                size = 80.dp,
                strokeWidth = 12.dp,
                centerContent = {
                    Text(
                        text = if (total > 0) "${(completedCount * 100 / total)}%" else "0%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Todo Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                PieChartLegend(
                    slices = listOf(
                        PieChartSlice(completedCount.toFloat(), ProductiveGreen, "Completed ($completedCount)"),
                        PieChartSlice(activeCount.toFloat(), ExerciseOrange, "Pending ($activeCount)"),
                    )
                )
            }
        }
    }
}

@Composable
fun TimeDistributionCard(weeklyStats: WeeklyStats) {
    val slices = listOf(
        PieChartSlice(weeklyStats.totalProductiveMinutes.toFloat(), ProductiveGreen, "Productive"),
        PieChartSlice(weeklyStats.totalSleepMinutes.toFloat(), SleepColor, "Sleep"),
        PieChartSlice(weeklyStats.totalWastedMinutes.toFloat(), WastedRed, "Wasted"),
        PieChartSlice(weeklyStats.totalSocialMinutes.toFloat(), SocialColor, "Social"),
        PieChartSlice(weeklyStats.totalRecoveryMinutes.toFloat(), RecoveryColor, "Recovery"),
        PieChartSlice(weeklyStats.totalFreeMinutes.toFloat(), FreeGray, "Free"),
    ).filter { it.value > 0f }
    val total = weeklyStats.totalTrackedMinutes.toFloat()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Weekly Time Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (total > 0 && slices.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PieChart(
                        slices = slices,
                        size = 120.dp,
                        strokeWidth = 16.dp,
                        centerContent = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${(total / 60).toInt()}h",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    PieChartLegend(
                        slices = slices,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    "No data available yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(
    sessions: List<io.github.dailytrack.data.db.entity.SessionEntity>,
    categories: Map<Long, io.github.dailytrack.data.db.entity.CategoryEntity>,
    pomodoros: List<io.github.dailytrack.data.db.entity.PomodoroSessionEntity>
) {
    val categoryDurations = mutableMapOf<String, Float>()
    var productiveMinutes = 0f

    for (session in sessions) {
        if (session.isActive) continue
        val duration = ((session.endTime ?: System.currentTimeMillis()) - session.startTime) / 60000f
        val cat = session.categoryId?.let { categories[it] }
        val catType = cat?.type ?: session.type
        val label = cat?.name ?: catType
        categoryDurations[label] = (categoryDurations[label] ?: 0f) + duration
        if (catType in listOf("PRODUCTIVE", "LEARNING", "ACTIVITY")) {
            productiveMinutes += duration
        }
    }

    for (p in pomodoros) {
        if (p.isCompleted && p.type == "WORK") {
            productiveMinutes += p.durationMinutes
            categoryDurations["Pomodoro"] = (categoryDurations["Pomodoro"] ?: 0f) + p.durationMinutes
        }
    }

    val slices = categoryDurations.entries
        .sortedByDescending { it.value }
        .take(6)
        .mapIndexed { index, entry ->
            val colors = listOf(ProductiveGreen, LearningBlue, ExerciseOrange, SocialPurple, PomodoroRed, FreeGray)
            PieChartSlice(entry.value, colors[index % colors.size], entry.key)
        }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Today's Category Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (slices.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PieChart(
                        slices = slices,
                        size = 120.dp,
                        strokeWidth = 16.dp,
                        centerContent = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${productiveMinutes.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    PieChartLegend(
                        slices = slices,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    "No sessions recorded today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WeeklyTrendCard(dailyProductivity: List<Pair<String, Float>>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Weekly Productivity Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (dailyProductivity.isNotEmpty() && dailyProductivity.any { it.second > 0 }) {
                val maxValue = dailyProductivity.maxOf { it.second }.coerceAtLeast(1f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / (dailyProductivity.size - 1).coerceAtLeast(1)
                    val padding = 8.dp.toPx()

                    // Grid lines
                    for (i in 0..3) {
                        val y = height - (height * i / 3f)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Line path
                    val path = Path()
                    dailyProductivity.forEachIndexed { index, (_, value) ->
                        val x = index * stepX
                        val y = height - (value / maxValue * (height - padding * 2)) - padding
                        if (index == 0) path.moveTo(x, y)
                        else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = ProductiveGreen,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Dots
                    dailyProductivity.forEachIndexed { index, (_, value) ->
                        val x = index * stepX
                        val y = height - (value / maxValue * (height - padding * 2)) - padding
                        drawCircle(
                            color = ProductiveGreen,
                            radius = 5.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dailyProductivity.forEach { (label, _) ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    "No data for this week",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PomodoroStatsCard(
    pomodoros: List<io.github.dailytrack.data.db.entity.PomodoroSessionEntity>,
    weeklyStats: WeeklyStats
) {
    val completedPomodoros = pomodoros.count { it.isCompleted && it.type == "WORK" }
    val totalPomodoroMinutes = pomodoros
        .filter { it.isCompleted && it.type == "WORK" }
        .sumOf { it.durationMinutes }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
                    PieChart(
                        slices = listOf(
                            PieChartSlice(completedPomodoros.toFloat(), PomodoroRed, "Completed"),
                            PieChartSlice(
                                (8 - completedPomodoros.toFloat()).coerceAtLeast(0f),
                                Color.Gray.copy(alpha = 0.3f),
                                "Remaining"
                            ),
                        ),
                size = 80.dp,
                strokeWidth = 12.dp,
                centerContent = {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = PomodoroRed
                    )
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Pomodoro Today",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "$completedPomodoros sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${totalPomodoroMinutes}min focused",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${weeklyStats.averageProductivePerDay}min",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = ProductiveGreen
                        )
                        Text(
                            "daily avg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InsightDetailCard(insight: InsightEntity, onDismiss: () -> Unit) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (insight.severity) {
                            "CRITICAL" -> Icons.Default.Warning
                            "WARNING" -> Icons.Default.Info
                            "CAUTION" -> Icons.Default.Lightbulb
                            else -> Icons.Default.Lightbulb
                        },
                        contentDescription = null,
                        tint = when (insight.severity) {
                            "CRITICAL" -> WastedRed
                            "WARNING" -> ExerciseOrange
                            "CAUTION" -> LearningBlue
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
            }
        }
    }
}
