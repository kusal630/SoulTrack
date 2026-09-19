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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.engine.GrowthScoreResult
import io.github.dailytrack.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val growthScore by viewModel.growthScore.collectAsState()
    val growthResult by viewModel.growthResult.collectAsState()
    val loopStatus by viewModel.loopStatus.collectAsState()
    val todayCoverage by viewModel.todayCoverage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Growth & Routine") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        }
    }
}

@Composable
fun GrowthScoreDetailCard(score: Double, result: GrowthScoreResult?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    score >= 70 -> Color(0xFF2E7D32)
                    score >= 40 -> Color(0xFFF57C00)
                    else -> Color(0xFFC62828)
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
            HorizontalDivider()
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

            ScoreGuideRow("25%", "Learning", "Target: 2h/day", Color(0xFF1565C0))
            ScoreGuideRow("25%", "Productive Work", "Target: 4h/day", Color(0xFF2E7D32))
            ScoreGuideRow("20%", "Exercise", "Target: 1h/day", Color(0xFFE65100))
            ScoreGuideRow("15%", "Sleep", "Target: 7-9h", Color(0xFF311B92))
            ScoreGuideRow("5%", "Social", "Target: 30min/day", Color(0xFF6A1B9A))
            ScoreGuideRow("5%", "Novelty", "Trying new things", Color(0xFF00838F))
            ScoreGuideRow("5%", "Consistency", "Active streak (7 days)", Color(0xFF4E342E))

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
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
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text(
                text = percentage,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                Color(0xFF1565C0)
            )
            GrowthComponentRow(
                "Productive",
                formatMinutes(coverage?.productiveSeconds ?: 0),
                "4h target",
                result?.productiveComponent ?: 0.0,
                Color(0xFF2E7D32)
            )
            GrowthComponentRow(
                "Exercise",
                formatMinutes(coverage?.exerciseSeconds ?: 0),
                "1h target",
                result?.exerciseComponent ?: 0.0,
                Color(0xFFE65100)
            )
            GrowthComponentRow(
                "Sleep",
                formatHoursMinutes(coverage?.sleepSeconds ?: 0),
                "7-9h target",
                result?.sleepComponent ?: 0.0,
                Color(0xFF311B92)
            )
            GrowthComponentRow(
                "Social",
                formatMinutes(coverage?.socialSeconds ?: 0),
                "30m target",
                result?.socialComponent ?: 0.0,
                Color(0xFF6A1B9A)
            )
            GrowthComponentRow(
                "Novelty",
                "--",
                "Variety",
                result?.noveltyComponent ?: 0.0,
                Color(0xFF00838F)
            )
            GrowthComponentRow(
                "Consistency",
                "${((result?.consistencyComponent ?: 0.0) / 5.0 * 7).toInt()}/7 days",
                "Streak",
                result?.consistencyComponent ?: 0.0,
                Color(0xFF4E342E)
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
            shape = MaterialTheme.shapes.extraSmall
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
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
