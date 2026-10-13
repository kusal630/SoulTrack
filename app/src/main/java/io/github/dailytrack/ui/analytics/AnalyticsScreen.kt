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


package io.github.dailytrack.ui.analytics

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.PieChart
import io.github.dailytrack.ui.components.PieChartSlice
import io.github.dailytrack.ui.components.ProgressRing
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.ScreenSection
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.StatTile
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.LearningColor
import io.github.dailytrack.ui.theme.NeutralColor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.theme.SleepColor
import io.github.dailytrack.ui.theme.SocialColor
import io.github.dailytrack.ui.theme.WastedColor
import io.github.dailytrack.ui.viewmodel.MainViewModel

/**
 * Seven-day analytics: what the time actually went to, how productive each day
 * was, and how the growth score is tracking.
 */
@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val weeklyReport by viewModel.weeklyReport.collectAsState()
    val dailyProductivity by viewModel.dailyProductivity.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val growthScore by viewModel.growthScore.collectAsState()

    ScreenList(
        title = "Analytics",
        subtitle = "Your last seven days, in numbers",
        navController = navController,
        compact = true
    ) {
        if (weeklyReport.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing to analyse yet",
                    message = "Track a few days and your time distribution, daily productivity and growth trend will show up here.",
                    icon = Icons.Default.BarChart
                )
            }
            return@ScreenList
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                StatTile(
                    label = "Productive",
                    value = formatHours(weeklyStats.totalProductiveMinutes.toLong()),
                    icon = Icons.Default.TrendingUp,
                    tint = ProductiveColor,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Per day",
                    value = formatHours(weeklyStats.averageProductivePerDay.toLong()),
                    icon = Icons.Default.Timer,
                    tint = LearningColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                StatTile(
                    label = "Wasted",
                    value = formatHours(weeklyStats.totalWastedMinutes.toLong()),
                    icon = Icons.Default.LocalFireDepartment,
                    tint = WastedColor,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Tasks done",
                    value = weeklyStats.completedTodos.toString(),
                    icon = Icons.Default.CheckCircle,
                    tint = ProductiveColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionTitle(title = "Where the week went", subtitle = "All seven days combined")
        }

        item {
            val totals = weeklyReport.values
            val slices = buildList {
                add(
                    PieChartSlice(
                        value = (totals.sumOf { it.totalProductiveSeconds }).toFloat(),
                        color = ProductiveColor,
                        label = "Productive"
                    )
                )
                add(
                    PieChartSlice(
                        value = (totals.sumOf { it.exerciseSeconds }).toFloat(),
                        color = ExerciseColor,
                        label = "Exercise"
                    )
                )
                add(
                    PieChartSlice(
                        value = (totals.sumOf { it.sleepSeconds }).toFloat(),
                        color = SleepColor,
                        label = "Sleep"
                    )
                )
                add(
                    PieChartSlice(
                        value = (totals.sumOf { it.socialSeconds }).toFloat(),
                        color = SocialColor,
                        label = "Social"
                    )
                )
                add(
                    PieChartSlice(
                        value = (totals.sumOf { it.recoverySeconds }).toFloat(),
                        color = NeutralColor,
                        label = "Recovery"
                    )
                )
                add(
                    PieChartSlice(
                        value = (totals.sumOf { it.wastedSeconds }).toFloat(),
                        color = WastedColor,
                        label = "Wasted"
                    )
                )
            }.filter { it.value > 0f }

            ScreenSection(title = "Time distribution", subtitle = "Seconds tracked across the week") {
                if (slices.isEmpty()) {
                    EmptyState(
                        title = "No time tracked",
                        message = "Start a timer and this chart fills itself in.",
                        icon = Icons.Default.Timer
                    )
                } else {
                    PieChart(
                        slices = slices,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        item {
            SectionTitle(title = "Productivity by day", subtitle = "Share of the day that was productive")
        }

        item {
            ScreenSection(title = "Daily split") {
                val maxMinutes = dailyProductivity.maxOfOrNull { it.second } ?: 0f
                dailyProductivity.forEach { (label, minutes) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            LinearProgressIndicator(
                                progress = { if (maxMinutes > 0f) minutes / maxMinutes else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = ProductiveColor,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        }
                        Text(
                            text = formatHours(minutes.toLong()),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }

        item {
            SectionTitle(title = "Growth score", subtitle = "How the week is moving overall")
        }

        item {
            ScreenSection(title = "Score") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProgressRing(
                        progress = (growthScore / 100.0).toFloat().coerceIn(0f, 1f),
                        color = ProductiveColor
                    ) {
                        Text(
                            text = growthScore.toInt().toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Out of 100, based on the balance of your tracked week.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

private fun formatHours(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours <= 0) "${mins}m" else if (mins == 0L) "${hours}h" else "${hours}h ${mins}m"
}
