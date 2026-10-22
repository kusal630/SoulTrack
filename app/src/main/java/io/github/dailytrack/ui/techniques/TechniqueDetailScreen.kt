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

package io.github.dailytrack.ui.techniques

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.dailytrack.ui.Screen
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.navigateSingleTop

private const val TECHNIQUE_ROUTE_PREFIX = "technique/"

@Composable
fun TechniqueDetailScreen(techniqueId: String, navController: NavController) {
    val technique = TechniqueCatalog.find(techniqueId)
    if (technique == null) {
        ScreenList(
            title = "Technique not found",
            subtitle = "This guide may have moved",
            navController = navController,
            compact = true
        ) {
            item {
                SurfaceCard {
                    Text(
                        text = "The technique you tried to open is not in the library.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        return
    }

    val tint = Color(technique.category.tint)
    val related = TechniqueCatalog.inCategory(technique.category)
        .filter { it.id != technique.id }
        .take(3)

    ScreenList(
        title = technique.title,
        subtitle = technique.category.label + " technique",
        navController = navController,
        compact = true
    ) {
        item {
            TechniqueFigure(
                kind = technique.figure,
                accent = tint,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                large = true
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = technique.figureCaption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(technique.category)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Best for: ${technique.bestFor}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = technique.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (technique.aliases.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Merged from: " + technique.aliases.joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        color = tint
                    )
                }
            }
        }

        item {
            SectionTitle(
                title = "How to run it",
                subtitle = "The procedure, in order"
            )
        }
        item {
            SurfaceCard {
                technique.steps.forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(tint.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = tint
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (index != technique.steps.lastIndex) {
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }

        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Make it daily",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = technique.daily,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            SurfaceCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFFFB84D),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Watch out",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = technique.pitfall,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (technique.appActions.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "Use it in Soul Track",
                    subtitle = "Shortcuts to the screens that fit this method"
                )
            }
            item {
                SurfaceCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        technique.appActions.forEach { action ->
                            val route = action.route()
                            if (route != null) {
                                AssistChip(
                                    onClick = { navController.navigateSingleTop(route) },
                                    label = { Text(action.label) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        labelColor = tint,
                                        leadingIconContentColor = tint,
                                        containerColor = tint.copy(alpha = 0.10f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        if (related.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "Pairs well with",
                    subtitle = "Other ${technique.category.label.lowercase()} techniques"
                )
            }
            related.forEach { other ->
                item(key = "related_${other.id}") {
                    RelatedTechniqueCard(other) {
                        navController.navigateSingleTop(TECHNIQUE_ROUTE_PREFIX + other.id)
                    }
                }
            }
        }

        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "One technique at a time",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Run this method for one or two weeks before adding another. The goal is a small stack that survives a busy day, not a perfect system.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun CategoryBadge(category: TechniqueCategory) {
    val tint = Color(category.tint)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RelatedTechniqueCard(technique: Technique, onClick: () -> Unit) {
    val tint = Color(technique.category.tint)
    SurfaceCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TechniqueFigure(
                kind = technique.figure,
                accent = tint,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = technique.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = technique.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun AppAction.route(): String? = when (this) {
    AppAction.TODO -> Screen.Todo.route
    AppAction.TIMER -> Screen.Timer.route
    AppAction.POMODORO -> Screen.Pomodoro.route
    AppAction.HABITS -> Screen.Habits.route
    AppAction.INSIGHTS -> Screen.Insights.route
    AppAction.EXPLORE -> Screen.Explore.route
    AppAction.FOCUS -> Screen.FocusMode.route
    AppAction.REMINDERS -> Screen.Reminders.route
}
