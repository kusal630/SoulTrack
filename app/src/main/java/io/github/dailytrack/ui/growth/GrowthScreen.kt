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


package io.github.dailytrack.ui.growth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.engine.GrowthScoreResult
import io.github.dailytrack.ui.Screen
import io.github.dailytrack.ui.components.*
import io.github.dailytrack.ui.navigateSingleTop
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.LearningColor
import io.github.dailytrack.ui.theme.NeutralColor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.theme.RecoveryColor
import io.github.dailytrack.ui.theme.SleepColor
import io.github.dailytrack.ui.theme.SocialColor
import io.github.dailytrack.ui.theme.WastedColor
import io.github.dailytrack.ui.viewmodel.MainViewModel

private data class GrowthDestination(
    val label: String,
    val caption: String,
    val icon: ImageVector,
    val route: String,
    val tint: Color
)

@Composable
fun GrowthScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val growthScore by viewModel.growthScore.collectAsState()
    val growthResult by viewModel.growthResult.collectAsState()
    val loopStatus by viewModel.loopStatus.collectAsState()
    val todayCoverage by viewModel.todayCoverage.collectAsState()

    ScreenList(
        title = "Growth & Routine",
        subtitle = "Balanced days, week after week",
        navController = navController
    ) {
        item {
            GrowthScoreDetailCard(score = growthScore, result = growthResult)
        }

        item {
            GrowthComponentsCard(result = growthResult, coverage = todayCoverage)
        }

        item {
            RoutineLoopCard(loopStatus = loopStatus)
        }

        item {
            ComfortZoneCard(loopStatus = loopStatus)
        }

        item {
            SectionTitle(
                title = "Go deeper",
                subtitle = "Body, goals, history and rewards",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            GrowthDestinationGrid(navController = navController)
        }

        item {
            PrimaryCta(
                text = "Set goals & targets",
                icon = Icons.Default.TrackChanges,
                onClick = { navController.navigateSingleTop(Screen.Goals.route) }
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun GrowthDestinationGrid(navController: NavController) {
    val destinations = listOf(
        GrowthDestination(
            "Body systems",
            "How your body is doing",
            Icons.Default.Favorite,
            Screen.BodySystems.route,
            SocialColor
        ),
        GrowthDestination(
            "History",
            "Every session you logged",
            Icons.Default.History,
            Screen.History.route,
            LearningColor
        ),
        GrowthDestination(
            "Analytics",
            "Trends across weeks",
            Icons.Default.BarChart,
            Screen.Analytics.route,
            RecoveryColor
        ),
        GrowthDestination(
            "Achievements",
            "Levels and unlocks",
            Icons.Default.EmojiEvents,
            Screen.Achievements.route,
            ExerciseColor
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
        destinations.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                row.forEach { destination ->
                    GrowthDestinationTile(
                        destination = destination,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigateSingleTop(destination.route) }
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GrowthDestinationTile(
    destination: GrowthDestination,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    PressableCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(destination.tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                destination.icon,
                contentDescription = null,
                tint = destination.tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = destination.caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GrowthScoreDetailCard(score: Double, result: GrowthScoreResult?) {
    SurfaceCard {
        Column {
            Text(
                "Growth Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${score.toInt()}",
                style = MaterialTheme.typography.displaySmall,
                color = when {
                    score >= 70 -> ProductiveColor
                    score >= 40 -> ExerciseColor
                    else -> WastedColor
                },
                fontWeight = FontWeight.Bold
            )
            Text(
                when {
                    score >= 80 -> "Excellent growth day"
                    score >= 60 -> "Good growth day"
                    score >= 40 -> "Moderate growth day"
                    score >= 20 -> "Low growth day"
                    score > 0 -> "Minimal activity today"
                    else -> "No activity tracked today"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Hairline()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "How It Works",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Your score reflects balanced activity across 7 areas of personal growth:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            ScoreGuideRow("25%", "Learning", "Target: 2h/day", LearningColor)
            ScoreGuideRow("25%", "Productive Work", "Target: 4h/day", ProductiveColor)
            ScoreGuideRow("20%", "Exercise", "Target: 1h/day", ExerciseColor)
            ScoreGuideRow("15%", "Sleep", "Target: 7-9h", SleepColor)
            ScoreGuideRow("5%", "Social", "Target: 30min/day", SocialColor)
            ScoreGuideRow("5%", "Novelty", "Trying new things", RecoveryColor)
            ScoreGuideRow("5%", "Consistency", "Active streak (7 days)", NeutralColor)

            Spacer(modifier = Modifier.height(8.dp))

            SurfaceCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                border = null,
                contentPadding = PaddingValues(10.dp)
            ) {
                Column {
                    Text(
                        "Scoring Guide",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("70-100: Excellent — strong balanced growth", style = MaterialTheme.typography.bodySmall)
                    Text("40-69: Good — consistent progress in key areas", style = MaterialTheme.typography.bodySmall)
                    Text("20-39: Fair — some activity, room to improve", style = MaterialTheme.typography.bodySmall)
                    Text("1-19: Low — minimal activity today", style = MaterialTheme.typography.bodySmall)
                    Text("0: Rest day or no data yet", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Bonuses: Balance across all areas (+3), High focus time (+2)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreGuideRow(percentage: String, label: String, target: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = percentage,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            target,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GrowthComponentsCard(result: GrowthScoreResult?, coverage: io.github.dailytrack.engine.TimeCoverage?) {
    SurfaceCard {
        Column {
            Text(
                "Today's Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            GrowthComponentRow(
                "Learning",
                formatMinutes(coverage?.learningSeconds ?: 0),
                "2h target",
                result?.learningComponent ?: 0.0,
                LearningColor
            )
            GrowthComponentRow(
                "Productive",
                formatMinutes(coverage?.productiveSeconds ?: 0),
                "4h target",
                result?.productiveComponent ?: 0.0,
                ProductiveColor
            )
            GrowthComponentRow(
                "Exercise",
                formatMinutes(coverage?.exerciseSeconds ?: 0),
                "1h target",
                result?.exerciseComponent ?: 0.0,
                ExerciseColor
            )
            GrowthComponentRow(
                "Sleep",
                formatHoursMinutes(coverage?.sleepSeconds ?: 0),
                "7-9h target",
                result?.sleepComponent ?: 0.0,
                SleepColor
            )
            GrowthComponentRow(
                "Social",
                formatMinutes(coverage?.socialSeconds ?: 0),
                "30m target",
                result?.socialComponent ?: 0.0,
                SocialColor
            )
            GrowthComponentRow(
                "Novelty",
                "--",
                "Variety",
                result?.noveltyComponent ?: 0.0,
                RecoveryColor
            )
            GrowthComponentRow(
                "Consistency",
                "${((result?.consistencyComponent ?: 0.0) / 5.0 * 7).toInt()}/7 days",
                "Streak",
                result?.consistencyComponent ?: 0.0,
                NeutralColor
            )
        }
    }
}

@Composable
fun GrowthComponentRow(label: String, value: String, target: String, score: Double, color: Color, totalScore: Double = 100.0) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "${score.toInt()}",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "$value (target: $target)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RoutineLoopCard(loopStatus: io.github.dailytrack.engine.LoopDetectionResult?) {
    SurfaceCard {
        Column {
            Text(
                "Routine Loop Detection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (loopStatus?.isLoopDetected == true) {
                Text(
                    "Loop detected for ${loopStatus.consecutiveDays} consecutive days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    "Consider adding new activities to break the pattern.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "No routine loop detected. Your routine has enough variety.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ComfortZoneCard(loopStatus: io.github.dailytrack.engine.LoopDetectionResult?) {
    SurfaceCard {
        Column {
            Text(
                "Comfort Zone Warnings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (loopStatus?.isLoopDetected == true && loopStatus.consecutiveDays >= 5) {
                Text(
                    "You've been in your comfort zone for ${loopStatus.consecutiveDays} days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    "Try something new today to stimulate growth.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "No comfort zone warnings. Keep pushing your boundaries!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun formatMinutes(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) "${minutes}m ${secs}s" else "${secs}s"
}

fun formatHoursMinutes(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m ${secs}s"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}
