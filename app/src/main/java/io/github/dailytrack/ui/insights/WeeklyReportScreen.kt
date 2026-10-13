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

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dailytrack.engine.TimeCoverage
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.KeyValueRow
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.ScreenSection
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.LearningColor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.theme.WastedColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WeeklyReportScreen(
    weeklyData: Map<LocalDate, TimeCoverage>,
    productivityScore: Float
) {
    ScreenList(
        title = "Weekly report",
        subtitle = "How your last seven days stacked up",
        onBack = null
    ) {
        item {
            ProductivityScoreCard(score = productivityScore)
        }

        if (weeklyData.isEmpty()) {
            item {
                EmptyState(
                    title = "No data this week",
                    message = "Track a few days and your weekly score, overview and daily breakdown will appear here.",
                    icon = Icons.Default.BarChart,
                    tint = ProductiveColor
                )
            }
        } else {
            item {
                WeeklyOverviewCard(weeklyData = weeklyData)
            }

            item {
                DailyBreakdownCard(weeklyData = weeklyData)
            }

            item {
                InsightsCard(
                    insights = listOf(
                        "You've been most productive on Tuesdays",
                        "Consider adding more exercise time",
                        "Your sleep schedule has been consistent"
                    )
                )
            }
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}

@Composable
private fun ProductivityScoreCard(score: Float) {
    val animatedScore by animateFloatAsState(
        targetValue = score,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "score"
    )

    val scoreColor = when {
        score >= 80 -> ProductiveColor
        score >= 60 -> ExerciseColor
        score >= 40 -> LearningColor
        else -> WastedColor
    }

    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Weekly score",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Dimens.TitleGap))
                Text(
                    text = getScoreMessage(score),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Out of 100",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(Dimens.CardGap))
            val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(88.dp)) {
                    val strokeWidth = 9.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = scoreColor,
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
                    style = MaterialTheme.typography.headlineMedium,
                    color = scoreColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WeeklyOverviewCard(weeklyData: Map<LocalDate, TimeCoverage>) {
    val totalHours = weeklyData.values.sumOf { it.trackedSeconds } / 3600.0
    val productiveHours =
        weeklyData.values.sumOf { it.productiveSeconds + it.learningSeconds } / 3600.0
    val exerciseHours = weeklyData.values.sumOf { it.exerciseSeconds } / 3600.0
    val wastedHours = weeklyData.values.sumOf { it.wastedSeconds } / 3600.0

    ScreenSection(
        title = "Weekly overview",
        subtitle = "Hours tracked across the week"
    ) {
        KeyValueRow(
            label = "Total tracked",
            value = "%.1fh".format(totalHours)
        )
        KeyValueRow(
            label = "Productive + learning",
            value = "%.1fh".format(productiveHours),
            valueColor = LearningColor
        )
        KeyValueRow(
            label = "Exercise",
            value = "%.1fh".format(exerciseHours),
            valueColor = ExerciseColor
        )
        KeyValueRow(
            label = "Wasted",
            value = "%.1fh".format(wastedHours),
            valueColor = WastedColor
        )
    }
}

@Composable
private fun DailyBreakdownCard(weeklyData: Map<LocalDate, TimeCoverage>) {
    val sortedData = weeklyData.entries.sortedBy { it.key }

    ScreenSection(
        title = "Daily breakdown",
        subtitle = "Productive share of tracked time"
    ) {
        sortedData.forEach { (date, coverage) ->
            val dayName = date.format(DateTimeFormatter.ofPattern("EEE"))
            val productiveRatio = if (coverage.trackedSeconds > 0) {
                (coverage.productiveSeconds + coverage.learningSeconds)
                    .toFloat() / coverage.trackedSeconds
            } else {
                0f
            }.coerceIn(0f, 1f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp)
                )
                LinearProgressIndicator(
                    progress = { productiveRatio },
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color = ProductiveColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Text(
                    text = "%.0f%%".format(productiveRatio * 100),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(44.dp)
                )
            }
        }
    }
}

@Composable
private fun InsightsCard(insights: List<String>) {
    ScreenSection(
        title = "Insights",
        subtitle = "Patterns picked from your week",
        leadingIcon = Icons.Default.Lightbulb,
        tint = LearningColor
    ) {
        insights.forEach { insight ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(LearningColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun getScoreMessage(score: Float): String = when {
    score >= 80 -> "Excellent work this week!"
    score >= 60 -> "Good progress, keep it up!"
    score >= 40 -> "Room for improvement"
    else -> "Try to be more productive"
}
