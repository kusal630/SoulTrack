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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.db.entity.TodoEntity
import io.github.dailytrack.engine.DayStatus
import io.github.dailytrack.engine.TimeCoverage
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.components.WeekDayShortNames
import io.github.dailytrack.ui.components.dayProgressFor
import io.github.dailytrack.ui.components.weekStatusesFor
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.LocalDayStatusColors
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.theme.WastedColor
import io.github.dailytrack.ui.viewmodel.MainViewModel
import java.time.LocalDate

@Composable
fun InsightsScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val activeInsights by viewModel.activeInsights.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val growthScore by viewModel.growthScore.collectAsState()
    val dailyProductivity by viewModel.dailyProductivity.collectAsState()
    val weeklyReport by viewModel.weeklyReport.collectAsState()
    val activeTodos by viewModel.activeTodos.collectAsState()
    val completedTodos by viewModel.completedTodos.collectAsState()
    val today = LocalDate.now()

    ScreenList(
        title = "Insights",
        subtitle = "The few numbers worth acting on"
    ) {
        item {
            GrowthScoreCard(
                growthScore = growthScore,
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

        activeInsights.firstOrNull()?.let { insight ->
            item {
                SurfaceCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = insight.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = insight.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.dismissInsight(insight.id) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss insight",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowthScoreCard(
    growthScore: Double,
    hasData: Boolean,
    trendDelta: Float?
) {
    val scoreColor = when {
        !hasData -> MaterialTheme.colorScheme.onSurfaceVariant
        growthScore >= 70 -> ProductiveColor
        growthScore >= 40 -> ExerciseColor
        else -> WastedColor
    }

    SurfaceCard {
        Column {
            Text(
                text = "Daily growth score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (hasData) "${growthScore.toInt()}" else "--",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                Text(
                    text = if (hasData) " / 100" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                if (hasData && trendDelta != null) {
                    Icon(
                        imageVector = if (trendDelta >= 0f) Icons.Default.TrendingUp
                        else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (trendDelta >= 0f) ProductiveColor else WastedColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%+.0f%%".format(trendDelta),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (trendDelta >= 0f) ProductiveColor else WastedColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (growthScore / 100.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = scoreColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = when {
                    !hasData -> "Track a few sessions to unlock your score."
                    growthScore >= 70 -> "Strong week. Protect the routine that got you here."
                    growthScore >= 40 -> "Steady progress. One more focused block would move it."
                    else -> "Start with one small focus block today."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Seven bars, one per day, in this week's classification colours. No legend:
 * each bar carries a status icon and today is marked with a dot.
 */
@Composable
private fun WeekStatusBarsCard(
    today: LocalDate,
    weeklyReport: Map<LocalDate, TimeCoverage>,
    todos: List<TodoEntity>
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
        }
    }
}

private fun statusIcon(status: DayStatus?): ImageVector = when (status) {
    DayStatus.DONE -> Icons.Default.CheckCircle
    DayStatus.TASKS_NOT_MET -> Icons.Default.Repeat
    DayStatus.NOT_PRODUCTIVE -> Icons.Default.Timer
    DayStatus.WASTING_TIME -> Icons.Default.LocalFireDepartment
    else -> Icons.Default.Timer
}

private fun productivityTrendDelta(dailyProductivity: List<Pair<String, Float>>): Float? {
    if (dailyProductivity.size < 2) return null
    val values = dailyProductivity.map { it.second }
    val latest = values.last()
    val rest = values.dropLast(1)
    val average = rest.average().toFloat()
    if (average <= 0f) return null
    return (latest - average) / average * 100f
}
