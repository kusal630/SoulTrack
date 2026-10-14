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
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.navigateSingleTop
import io.github.dailytrack.ui.settings.AppFeatures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.LocalContext

private data class Destination(
    val label: String,
    val caption: String,
    val icon: ImageVector,
    val route: String,
    val tint: Color
)

private val healthDestinations = listOf(
    Destination("Sleep", "Debt, quality, recovery", Icons.Default.Hotel, Screen.Sleep.route, Color(0xFF818CF8)),
    Destination("Exercise", "Workouts and strain", Icons.Default.FitnessCenter, Screen.Exercise.route, Color(0xFFF59E0B)),
    Destination("Nutrition", "Meals, macros, hydration", Icons.Default.Restaurant, Screen.Nutrition.route, Color(0xFF34D399)),
    Destination("Body", "Systems and symptoms", Icons.Default.Favorite, Screen.BodySystems.route, Color(0xFFF472B6))
)

private val mindDestinations = listOf(
    Destination("Focus mode", "Block distractions", Icons.Default.CenterFocusStrong, Screen.FocusMode.route, Color(0xFF10B981)),
    Destination("Journal", "Daily reflection", Icons.Default.EditNote, Screen.Journal.route, Color(0xFF60A5FA)),
    Destination("Targets", "Your goals and features", Icons.Default.Flag, Screen.Settings.route, Color(0xFFA78BFA)),
    Destination("Reminders", "Time and habit nudges", Icons.Default.Alarm, Screen.Reminders.route, Color(0xFFFBBF24))
)

private val progressDestinations = listOf(
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
        subtitle = "Everything else DailyTrack tracks",
        navController = navController,
        compact = true
    ) {
        if (AppFeatures.healthEnabled) {
            item {
                SectionTitle(
                    title = "Health",
                    subtitle = "Body, movement and fuel"
                )
            }
            item { DestinationGrid(healthDestinations, navController) }
        } else {
            item {
                val context = LocalContext.current
                SurfaceCard(
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Health layer is off",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sleep, exercise, nutrition and the body dashboard are "
                                + "optional. Turn them on whenever you want them.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryCta(
                            text = "Turn on health layer",
                            icon = null,
                            onClick = { AppFeatures.setHealthEnabled(true, context) }
                        )
                    }
                }
            }
        }
        item {
            SectionTitle(
                title = "Mind & habits",
                subtitle = "Plan, reflect and stay on track",
                modifier = Modifier.padding(top = 8.dp)
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
