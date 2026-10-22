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


package io.github.dailytrack.ui.habit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dailytrack.data.db.entity.HabitEntity
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.ScreenSection
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.theme.ProductiveColor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitScreen(
    navController: androidx.navigation.NavController,
    habits: List<HabitEntity>,
    completions: Map<Long, List<LocalDate>>,
    onAddHabit: (String, String, String) -> Unit,
    onToggleHabit: (Long) -> Unit,
    onDeleteHabit: (HabitEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newHabitName by remember { mutableStateOf("") }
    var newHabitIcon by remember { mutableStateOf("⭐") }
    var newHabitColor by remember { mutableStateOf("#10B981") }

    val icons = listOf("⭐", "📚", "💪", "🧘", "💧", "🎯", "🏃", "😴", "🎨", "✍️")
    val colors = listOf("#10B981", "#3B82F6", "#8B5CF6", "#F59E0B", "#EF4444", "#EC4899")

    ScreenList(
        title = "Habits",
        subtitle = "Small things, done daily",
        navController = navController,
        compact = true,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(50),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add habit")
            }
        }
    ) {
        if (habits.isEmpty()) {
            item {
                EmptyState(
                    title = "No habits yet",
                    message = "Start building good habits today. One small win at a time.",
                    icon = Icons.Default.Flag,
                    tint = ProductiveColor,
                    actionLabel = "Add habit",
                    onAction = { showAddDialog = true }
                )
            }
        } else {
            item {
                WeeklyCalendar(completions = completions)
            }

            item {
                SectionTitle(
                    title = "Today's habits",
                    subtitle = "${habits.size} to check off"
                )
            }

            items(habits, key = { it.id }) { habit ->
                HabitCard(
                    habit = habit,
                    isCompletedToday = completions[habit.id]?.contains(LocalDate.now()) == true,
                    onToggle = { onToggleHabit(habit.id) },
                    onDelete = { onDeleteHabit(habit) }
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Habit") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newHabitName,
                        onValueChange = { newHabitName = it },
                        label = { Text("Habit Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Choose Icon", style = MaterialTheme.typography.titleSmall)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(icons) { icon ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (newHabitIcon == icon) ProductiveColor.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { newHabitIcon = icon },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(icon, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Choose Color", style = MaterialTheme.typography.titleSmall)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(colors) { color ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .clickable { newHabitColor = color },
                                contentAlignment = Alignment.Center
                            ) {
                                if (newHabitColor == color) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newHabitName.isNotBlank()) {
                            onAddHabit(newHabitName, newHabitIcon, newHabitColor)
                            newHabitName = ""
                            newHabitIcon = "⭐"
                            newHabitColor = "#10B981"
                            showAddDialog = false
                        }
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }, shape = RoundedCornerShape(50)) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WeeklyCalendar(completions: Map<Long, List<LocalDate>>) {
    val today = LocalDate.now()
    val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)

    ScreenSection(title = "This Week") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0 until 7) {
                val date = startOfWeek.plusDays(i.toLong())
                val isToday = date == today
                val hasCompletions = completions.values.any { it.contains(date) }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isToday -> ProductiveColor
                                    hasCompletions -> ProductiveColor.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HabitCard(
    habit: HabitEntity,
    isCompletedToday: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val habitColor = try {
        Color(android.graphics.Color.parseColor(habit.color))
    } catch (e: Exception) {
        ProductiveColor
    }

    val animatedColor by animateColorAsState(
        targetValue = if (isCompletedToday) habitColor else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(300),
        label = "habit_color"
    )

    val scale by animateFloatAsState(
        targetValue = if (isCompletedToday) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "habit_scale"
    )

    SurfaceCard(
        modifier = Modifier.scale(scale),
        containerColor = animatedColor,
        border = if (isCompletedToday) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = habit.icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompletedToday) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${habit.currentStreak} day streak 🔥",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCompletedToday) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (isCompletedToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isCompletedToday) "Completed" else "Complete",
                    tint = if (isCompletedToday) Color.White else habitColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = if (isCompletedToday) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
