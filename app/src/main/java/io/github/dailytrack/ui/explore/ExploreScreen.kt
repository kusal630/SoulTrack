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


package io.github.dailytrack.ui.explore

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.dailytrack.ui.Screen
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.navigateSingleTop

private data class Destination(
    val label: String,
    val caption: String,
    val icon: ImageVector,
    val route: String,
    val tint: Color
)

private val mindDestinations = listOf(
    Destination("Techniques", "Plan, focus and finish", Icons.AutoMirrored.Filled.MenuBook, Screen.Techniques.route, Color(0xFF7CF5C4)),
    Destination("Daily planner", "Important work first", Icons.Default.EventNote, Screen.Planner.route, Color(0xFF10B981)),
    Destination("Projects", "Pre-mortem the risks", Icons.Default.ReportProblem, Screen.Projects.route, Color(0xFFF97316)),
    Destination("Learning loop", "Recall, compare, explain", Icons.Default.School, Screen.LearningLoop.route, Color(0xFF8B5CF6)),
    Destination("Weekly review", "Five questions, five minutes", Icons.Default.FactCheck, Screen.WeeklyReview.route, Color(0xFF14B8A6)),
    Destination("Focus mode", "Block distractions", Icons.Default.CenterFocusStrong, Screen.FocusMode.route, Color(0xFF10B981)),
    Destination("Saved quotes", "Words that keep you going", Icons.Default.FormatQuote, Screen.SavedQuotes.route, Color(0xFF60A5FA)),
    Destination("Targets", "Your goals and features", Icons.Default.Flag, Screen.Settings.route, Color(0xFFA78BFA)),
    Destination("Reminders", "Time and habit nudges", Icons.Default.Alarm, Screen.Reminders.route, Color(0xFFFBBF24))
)

private val progressDestinations = listOf(
    Destination("Compounding", "Your 1% curves", Icons.Default.TrendingUp, Screen.Compounding.route, Color(0xFF22C55E)),
    Destination("History", "Every session logged", Icons.Default.History, Screen.History.route, Color(0xFF94A3B8)),
    Destination("Home", "Today's ring and time", Icons.Default.Home, Screen.Dashboard.route, Color(0xFF2DD4BF)),
    Destination("Weekly report", "Your seven-day recap", Icons.Default.Assessment, Screen.WeeklyReport.route, Color(0xFF38BDF8)),
    Destination("Achievements", "Levels and unlocks", Icons.Default.EmojiEvents, Screen.Achievements.route, Color(0xFFFACC15))
)

/**
 * The hub that makes every secondary feature reachable. Without this a good
 * half of the app is dead code the user can never open.
 */
@Composable
fun ExploreScreen(navController: NavController) {
    ScreenList(
        title = "Explore",
        subtitle = "Everything else Soul Track tracks",
        navController = navController,
        compact = true
    ) {
        item {
            SectionTitle(
                title = "Mind & habits",
                subtitle = "Plan, reflect and stay on track"
            )
        }
        item { DestinationGrid(mindDestinations, navController) }
        item {
            SectionTitle(
                title = "Progress",
                subtitle = "Look back at what you actually did",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item { DestinationGrid(progressDestinations, navController) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun DestinationGrid(items: List<Destination>, navController: NavController) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { destination ->
                    DestinationTile(
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
private fun DestinationTile(
    destination: Destination,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    SurfaceCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(14.dp))
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
