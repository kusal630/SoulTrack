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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.db.entity.InsightEntity
import io.github.dailytrack.engine.DayStatus
import io.github.dailytrack.engine.LoopDetectionResult
import io.github.dailytrack.engine.SwitchReport
import io.github.dailytrack.ui.Screen
import io.github.dailytrack.ui.components.*
import io.github.dailytrack.ui.components.brandColorAt
import io.github.dailytrack.ui.navigateSingleTop
import io.github.dailytrack.ui.theme.Aqua
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.LocalDayStatusColors
import io.github.dailytrack.ui.theme.Mint
import io.github.dailytrack.ui.theme.Sky
import io.github.dailytrack.ui.theme.StatusWastingTime
import io.github.dailytrack.ui.theme.GradientStart
import io.github.dailytrack.ui.theme.LearningColor
import io.github.dailytrack.ui.theme.NeutralColor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.theme.RecoveryColor
import io.github.dailytrack.ui.theme.SleepColor
import io.github.dailytrack.ui.theme.SocialColor
import io.github.dailytrack.ui.theme.WastedColor
import io.github.dailytrack.ui.navigateToHome
import io.github.dailytrack.ui.settings.ProductTargets
import io.github.dailytrack.ui.viewmodel.MainViewModel
import io.github.dailytrack.ui.viewmodel.WeeklyStats
import java.time.LocalDate

private val ProductiveGreen = ProductiveColor
private val WastedRed = WastedColor
private val FreeGray = NeutralColor
private val LearningBlue = LearningColor
private val ExerciseOrange = ExerciseColor
private val SocialPurple = SocialColor
private val PomodoroRed = GradientStart

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
    val weeklyReport by viewModel.weeklyReport.collectAsState()
    val activeTodos by viewModel.activeTodos.collectAsState()
    val completedTodos by viewModel.completedTodos.collectAsState()
    val loopStatus by viewModel.loopStatus.collectAsState()
    val switchReport by viewModel.switchReport.collectAsState()
    val weeklySwitchCounts by viewModel.weeklySwitchCounts.collectAsState()
    val focusInsightReport by viewModel.focusInsightReport.collectAsState()
    val today = LocalDate.now()

    ScreenList(
        title = "Insights",
        subtitle = "Where your time actually goes",
        onBack = { navController.navigateToHome() }
    ) {
        item {
            PrimaryCta(
                text = "Open weekly report",
                icon = Icons.Default.Assessment,
                onClick = { navController.navigateSingleTop(Screen.WeeklyReport.route) }
            )
        }

        item {
            GrowthScoreCard(
                growthScore = growthScore,
                growthResult = growthResult,
                hasData = weeklyStats.totalTrackedMinutes > 0,
                trendDelta = productivityTrendDelta(dailyProductivity)
            )
        }

        item {
            WeekStatusBarsCard(
                today = today,
                weeklyReport = weeklyReport,
                todos = activeTodos + completedTodos
            )
        }

        loopStatus?.let { loop ->
            item {
                ComfortZoneCard(
                    loop = loop,
                    dayProgress = dayProgressOf(activeTodos + completedTodos),
                    weekProgress = weekStatusesFor(today, weeklyReport, activeTodos + completedTodos)
                        .count { it == DayStatus.DONE } / 7f
                )
            }
        }

        item {
            SwitchCostCard(
                report = switchReport,
                weeklyCounts = weeklySwitchCounts
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
                categories = categories
            )
        }

        item {
            WeeklyTrendCard(dailyProductivity = dailyProductivity)
        }

        item {
            PomodoroStatsCard(
                pomodoros = todayPomodoros,
                weeklyStats = weeklyStats,
                dailyGoal = ProductTargets.pomodoroDailyGoal
            )
        }

        item {
            FocusDataInsightsCard(report = focusInsightReport)
        }

        if (activeInsights.isEmpty()) {
            item {
                EmptyState(
                    title = "No insights yet",
                    message = "Smart insights show up once you have tracked a few days of activity.",
                    icon = Icons.Default.Lightbulb,
                    actionLabel = "Open weekly report",
                    onAction = { navController.navigateSingleTop(Screen.WeeklyReport.route) }
                )
            }
        } else {
            item {
                SectionTitle(
                    title = "Smart insights",
                    subtitle = "${activeInsights.size} active"
                )
            }
            items(activeInsights, key = { it.id }) { insight ->
                InsightDetailCard(
                    insight = insight,
                    onDismiss = { viewModel.dismissInsight(insight.id) },
                    onAction = insightAction(navController, insight)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

/**
 * Turns an insight's suggested action into a real navigation. Insights that
 * have no corresponding place to go (for example "seek help if needed") return
 * null, so the hint stays plain text instead of a button that does nothing.
 */
private fun insightAction(navController: NavController, insight: InsightEntity): (() -> Unit)? {
    val route = when (insight.category.uppercase()) {
        "GROWTH" -> Screen.Todo.route
        "ROUTINE" -> Screen.Habits.route
        "TIME", "PRODUCTIVITY", "EXERCISE" -> Screen.Timer.route
        "SLEEP", "HYDRATION", "NUTRITION", "JOURNAL", "MOOD" -> Screen.Explore.route
        else -> null
    } ?: return null
    return { navController.navigateSingleTop(route) }
}

@Composable
fun GrowthScoreCard(
    growthScore: Double,
    growthResult: io.github.dailytrack.engine.GrowthScoreResult?,
    hasData: Boolean = true,
    trendDelta: Float? = null
) {
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val scoreColor = when {
                !hasData -> MaterialTheme.colorScheme.onSurfaceVariant
                growthScore >= 70 -> ProductiveGreen
                growthScore >= 40 -> ExerciseOrange
                else -> WastedRed
            }

            BrandScoreArc(
                progress = (growthScore / 100.0).coerceIn(0.0, 1.0).toFloat(),
                centerContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${growthScore.toInt()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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
                if (trendDelta != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (trendDelta >= 0f) Icons.Default.TrendingUp
                            else Icons.Default.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (trendDelta >= 0f) Mint else StatusWastingTime
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%+.0f%% vs your week's average".format(trendDelta),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (trendDelta >= 0f) Mint else StatusWastingTime
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
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
            modifier = Modifier.width(88.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { (score / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
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

    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
        PieChartSlice(weeklyStats.totalWastedMinutes.toFloat(), StatusWastingTime, "Wasted"),
        PieChartSlice(weeklyStats.totalSocialMinutes.toFloat(), SocialColor, "Social"),
        PieChartSlice(weeklyStats.totalRecoveryMinutes.toFloat(), RecoveryColor, "Recovery"),
        PieChartSlice(weeklyStats.totalFreeMinutes.toFloat(), FreeGray, "Free"),
    ).filter { it.value > 0f }
    val total = weeklyStats.totalTrackedMinutes.toFloat()

    SurfaceCard {
        Column {
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
                    Column(modifier = Modifier.weight(1f)) {
                        StackedTimeBar(slices = slices, totalMinutes = total)
                        Spacer(modifier = Modifier.height(12.dp))
                        PieChartLegend(slices = slices)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
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
            } else {
                EmptyState(
                    title = "No time tracked yet",
                    message = "Start a timer and your week will fill in here.",
                    icon = Icons.Default.PieChart
                )
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(
    sessions: List<io.github.dailytrack.data.db.entity.SessionEntity>,
    categories: Map<Long, io.github.dailytrack.data.db.entity.CategoryEntity>
) {
    val categoryDurations = mutableMapOf<String, Float>()

    // One session row = one slice of the day. A running pomodoro already
    // mirrors a session (source = "POMODORO"), so pomodoro rows are never
    // added on top - that used to double every focus block.
    val now = System.currentTimeMillis()
    for (session in sessions) {
        val end = if (session.isActive) now else session.endTime ?: continue
        val duration = ((end - session.startTime) / 60000f).coerceAtLeast(0f)
        if (duration <= 0f) continue
        val cat = session.categoryId?.let { categories[it] }
        val label = cat?.name ?: (cat?.type ?: session.type)
        categoryDurations[label] = (categoryDurations[label] ?: 0f) + duration
    }

    val totalMinutes = categoryDurations.values.sum()

    val slices = categoryDurations.entries
        .sortedByDescending { it.value }
        .take(6)
        .mapIndexed { index, entry ->
            val colors = listOf(ProductiveGreen, LearningBlue, ExerciseOrange, SocialPurple, PomodoroRed, FreeGray)
            PieChartSlice(entry.value, colors[index % colors.size], entry.key)
        }

    SurfaceCard {
        Column {
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
                                    text = "${totalMinutes.toInt()}",
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
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val trendColor = ProductiveGreen

    SurfaceCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Weekly Productivity Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                productivityTrendDelta(dailyProductivity)?.let { delta ->
                    Text(
                        text = "%+.0f%%".format(delta),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (delta >= 0f) Mint else StatusWastingTime
                    )
                }
            }
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
                            color = gridColor,
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
                        brush = Brush.horizontalGradient(listOf(Mint, Aqua, Sky)),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Dots
                    dailyProductivity.forEachIndexed { index, (_, value) ->
                        val x = index * stepX
                        val y = height - (value / maxValue * (height - padding * 2)) - padding
                        drawCircle(
                            color = brandColorAt(index.toFloat() / (dailyProductivity.size - 1).coerceAtLeast(1)),
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
    weeklyStats: WeeklyStats,
    dailyGoal: Int = 0
) {
    val completedPomodoros = pomodoros.count { it.isCompleted && it.type == "WORK" }
    val goal = dailyGoal.takeIf { it > 0 }
    val totalPomodoroMinutes = pomodoros
        .filter { it.isCompleted && it.type == "WORK" }
        .sumOf { it.durationMinutes }
    val remainingColor = MaterialTheme.colorScheme.surfaceContainerHighest

    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val slices = if (goal == null) {
                listOf(PieChartSlice(completedPomodoros.toFloat(), PomodoroRed, "Completed"))
            } else {
                listOf(
                    PieChartSlice(completedPomodoros.toFloat(), PomodoroRed, "Completed"),
                    PieChartSlice(
                        (goal - completedPomodoros.toFloat()).coerceAtLeast(0f),
                        remainingColor,
                        "Remaining"
                    )
                )
            }
            PieChart(
                slices = slices,
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
                        Text(
                            if (goal == null) {
                                "no daily goal set"
                            } else {
                                "$completedPomodoros of $goal done"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (goal != null && completedPomodoros >= goal) {
                                ProductiveGreen
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
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
fun InsightDetailCard(insight: InsightEntity, onDismiss: () -> Unit, onAction: (() -> Unit)? = null) {    SurfaceCard(
        containerColor = when (insight.severity) {
            "CRITICAL" -> MaterialTheme.colorScheme.errorContainer
            "WARNING" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            "CAUTION" -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        border = null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    if (onAction != null) {
                        TextButton(
                            onClick = onAction,
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = insight.actionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Text(
                            text = insight.actionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
            }
        }
    }
}


/** Mint -> aqua -> sky arc, the same sweep the week ring uses. */
@Composable
private fun BrandScoreArc(
    progress: Float,
    centerContent: @Composable () -> Unit
) {
    val reduceMotion = rememberReduceMotion()
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion) {
            spring(dampingRatio = 1f, stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        },
        label = "growthArc"
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
        Canvas(modifier = Modifier.size(100.dp)) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (animated > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(Mint, Aqua, Sky, Mint)),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        centerContent()
    }
}

/** How far today sits from the week's average productivity, in percent. */
private fun productivityTrendDelta(dailyProductivity: List<Pair<String, Float>>): Float? {
    if (dailyProductivity.size < 2) return null
    val values = dailyProductivity.map { it.second }
    val latest = values.last()
    val rest = values.dropLast(1)
    val average = rest.average().toFloat()
    if (average <= 0f) return null
    return (latest - average) / average * 100f
}

private fun dayProgressOf(todos: List<io.github.dailytrack.data.db.entity.TodoEntity>): Float =
    io.github.dailytrack.ui.components.dayProgressFor(LocalDate.now(), todos)

/**
 * Seven bars, one per day, in this week's classification colours. Today's
 * bar is dim until midnight, exactly like the outer ring segment.
 */
@Composable
fun WeekStatusBarsCard(
    today: LocalDate,
    weeklyReport: Map<LocalDate, io.github.dailytrack.engine.TimeCoverage>,
    todos: List<io.github.dailytrack.data.db.entity.TodoEntity>
) {
    val palette = LocalDayStatusColors.current
    val statuses = weekStatusesFor(today, weeklyReport, todos)
    val barHeight = 84.dp

    SurfaceCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${statuses.count { it == DayStatus.DONE }} clean days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                statuses.forEachIndexed { index, status ->
                    val date = today.minusDays((6 - index).toLong())
                    val isToday = index == today.dayOfWeek.value - 1
                    val progress = if (status == null) 0f else dayProgressFor(date, todos)
                    val color = palette.colorFor(if (isToday) null else status)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(barHeight * progress.coerceAtLeast(0.08f))
                                    .clip(RoundedCornerShape(50))
                                    .background(color.copy(alpha = if (isToday) 0.45f else 1f))
                            )
                            if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.onSurface)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = WeekDayShortNames[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = statusIcon(status),
                            contentDescription = if (isToday) "Today, in progress"
                            else status?.label ?: "Not happened yet",
                            tint = color,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            // Four statuses need two rows on a phone; giving each label half
            // the width stops "Tasks not met" from truncating to "Tasks not".
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusLegendItem(DayStatus.DONE, modifier = Modifier.weight(1f))
                    StatusLegendItem(DayStatus.TASKS_NOT_MET, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusLegendItem(DayStatus.NOT_PRODUCTIVE, modifier = Modifier.weight(1f))
                    StatusLegendItem(DayStatus.WASTING_TIME, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatusLegendItem(status: DayStatus, modifier: Modifier = Modifier) {
    val palette = LocalDayStatusColors.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = statusIcon(status),
            contentDescription = null,
            tint = palette.colorFor(status),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/** Concentric rings: today inside the week, with the routine loop at the core. */
@Composable
private fun ComfortZoneCard(loop: LoopDetectionResult, dayProgress: Float, weekProgress: Float) {
    val palette = LocalDayStatusColors.current
    val warning = loop.isLoopDetected || loop.comfortZoneWarning
    val tint = if (warning) palette.wastingTime else palette.notProductive

    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConcentricZones(
                todayProgress = dayProgress,
                weekProgress = weekProgress,
                tint = tint,
                warning = warning
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (loop.isLoopDetected) "Routine loop detected" else "Comfort zone",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (loop.isLoopDetected) {
                        "Your routine has looked the same for ${loop.consecutiveDays} days. " +
                            "Add something new to break out."
                    } else {
                        "Same shape, day after day. Rotate an activity and the rings stay honest."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (loop.consecutiveDays > 0) {
                    Text(
                        text = "${loop.consecutiveDays} similar days in a row",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tint
                    )
                }
            }
        }
    }
}

@Composable
private fun ConcentricZones(
    todayProgress: Float,
    weekProgress: Float,
    tint: Color,
    warning: Boolean
) {
    val ringColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
        Canvas(modifier = Modifier.size(72.dp)) {
            val stroke = 7.dp.toPx()
            // Outer ring: the week.
            drawCircle(
                color = ringColor,
                radius = size.minDimension / 2f - stroke / 2f,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = Sky,
                startAngle = -90f,
                sweepAngle = 360f * weekProgress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Middle ring: today.
            drawCircle(
                color = ringColor,
                radius = size.minDimension / 2f - stroke * 1.8f,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = Aqua,
                startAngle = -90f,
                sweepAngle = 360f * todayProgress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Core: the loop itself.
            drawCircle(
                color = if (warning) tint else Mint,
                radius = size.minDimension / 2f - stroke * 3.2f
            )
        }
    }
}

/** A single horizontal bar, one segment per slice, rounded at both ends. */
@Composable
private fun StackedTimeBar(slices: List<PieChartSlice>, totalMinutes: Float) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
    ) {
        var x = 0f
        slices.forEach { slice ->
            val width = (slice.value / totalMinutes.coerceAtLeast(1f)) * size.width
            drawRect(
                color = slice.color,
                topLeft = Offset(x, 0f),
                size = Size(width, size.height)
            )
            x += width
        }
    }
}

private fun statusIcon(status: DayStatus?) = when (status) {
    DayStatus.DONE -> Icons.Default.CheckCircle
    DayStatus.TASKS_NOT_MET -> Icons.Default.Repeat
    DayStatus.NOT_PRODUCTIVE -> Icons.Default.Timer
    DayStatus.WASTING_TIME -> Icons.Default.LocalFireDepartment
    DayStatus.NO_TASKS -> Icons.Default.Assignment
    else -> Icons.Default.HourglassEmpty
}

/**
 * The attention-drift card: how often today's work jumped context, how long
 * the return took and what that did to focus. Every number is read back from
 * the user's own sessions - it appears only once something was actually tracked.
 */
@Composable
fun SwitchCostCard(
    report: SwitchReport,
    weeklyCounts: List<Int>
) {
    if (report.switchCount == 0 && weeklyCounts.none { it > 0 }) return

    val index = report.hallucinationIndex
    val indexColor = when {
        index >= 50 -> GradientStart
        index >= 25 -> ExerciseOrange
        else -> LearningBlue
    }

    SurfaceCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Focus jumps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (report.switchCount == 0) "No context change today" else "Context changed ${report.switchCount}x today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = indexColor.copy(alpha = 0.16f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "$index",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = indexColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (report.switchCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    when {
                        index >= 50 -> "Your day is steering you: pick one task and close the rest."
                        index >= 25 -> "More than one thread is open - finishing one pays for itself."
                        else -> "A few jumps, handled well. The thread is still intact."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                KeyValueRow(
                    label = "Tasks dropped",
                    value = "${report.abandonedTaskCount}"
                )
                report.averageResumeMinutes?.let { resume ->
                    KeyValueRow(
                        label = "Back after",
                        value = "${resume.toInt()} min"
                    )
                }
                val post = report.postSwitchProductivity
                val base = report.baselineProductivity
                if (post != null && base != null) {
                    KeyValueRow(
                        label = "Focus after a jump",
                        value = "${(post * 100).toInt()}% vs ${(base * 100).toInt()}% baseline"
                    )
                }
            }

            if (weeklyCounts.any { it > 0 }) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "This week",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SwitchWeekBars(counts = weeklyCounts)
            }
        }
    }
}

@Composable
private fun SwitchWeekBars(counts: List<Int>) {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    val peak = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val monday = remember { io.github.dailytrack.ui.components.weekStartFor(LocalDate.now()) }
    val todayIndex = java.time.temporal.ChronoUnit.DAYS.between(
        monday, LocalDate.now()
    ).toInt().coerceIn(0, 6)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        counts.take(7).forEachIndexed { i, count ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val fraction = if (count == 0) 0f else (count.toFloat() / peak).coerceAtLeast(0.15f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((10 + 34 * fraction).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                count == 0 -> MaterialTheme.colorScheme.surfaceContainerHighest
                                count >= 4 -> GradientStart
                                else -> LearningBlue
                            }
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    labels.getOrElse(i) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == todayIndex) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (i == todayIndex) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
