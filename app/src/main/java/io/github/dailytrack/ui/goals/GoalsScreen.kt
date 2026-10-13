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

package io.github.dailytrack.ui.goals

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dailytrack.data.db.entity.GoalEntity
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.PillChip
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.StatTile
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.LearningColor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.theme.SleepColor
import io.github.dailytrack.ui.theme.SocialColor

@Composable
fun GoalsScreen(
    goals: List<GoalEntity>,
    onAddGoal: (String, String, String, Float) -> Unit,
    onUpdateProgress: (Long, Float) -> Unit,
    onDeleteGoal: (GoalEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newGoalTitle by remember { mutableStateOf("") }
    var newGoalDescription by remember { mutableStateOf("") }
    var newGoalCategory by remember { mutableStateOf("PRODUCTIVE") }
    var newGoalTarget by remember { mutableStateOf("") }
    var newGoalType by remember { mutableStateOf("WEEKLY") }

    val categories = listOf("PRODUCTIVE", "EXERCISE", "LEARNING", "SLEEP", "SOCIAL")
    val targetTypes = listOf("DAILY", "WEEKLY", "MONTHLY")
    val activeCount = goals.count { !it.isCompleted }
    val completedCount = goals.count { it.isCompleted }

    ScreenList(
        title = "Goals",
        subtitle = "Targets you are working toward",
        onBack = null
    ) {
        if (goals.isEmpty()) {
            item {
                EmptyState(
                    title = "No goals yet",
                    message = "Set a weekly target — deep work hours, workouts, study time — and watch the bar fill up.",
                    icon = Icons.Default.Flag,
                    tint = ProductiveColor,
                    actionLabel = "Add goal",
                    onAction = { showAddDialog = true }
                )
            }
        } else {
            item {
                PrimaryCta(
                    text = "Add goal",
                    icon = Icons.Default.Add,
                    onClick = { showAddDialog = true }
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                    StatTile(
                        label = "Active",
                        value = "$activeCount",
                        icon = Icons.Default.TrackChanges,
                        tint = ProductiveColor,
                        caption = "In progress",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Completed",
                        value = "$completedCount",
                        icon = Icons.Default.CheckCircle,
                        tint = LearningColor,
                        caption = "All time",
                        progress = if (goals.isEmpty()) 0f else completedCount / goals.size.toFloat(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            items(goals, key = { it.id }) { goal ->
                GoalCard(
                    goal = goal,
                    onUpdateProgress = { hours -> onUpdateProgress(goal.id, hours) },
                    onDelete = { onDeleteGoal(goal) }
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add new goal") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newGoalTitle,
                        onValueChange = { newGoalTitle = it },
                        label = { Text("Goal title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(Dimens.CardGap))
                    OutlinedTextField(
                        value = newGoalDescription,
                        onValueChange = { newGoalDescription = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(Dimens.CardGap))
                    Text("Category", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(Dimens.TitleGap))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.TitleGap)) {
                        categories.forEach { category ->
                            PillChip(
                                label = category.lowercase().replaceFirstChar { it.uppercase() },
                                selected = newGoalCategory == category,
                                onClick = { newGoalCategory = category },
                                tint = ProductiveColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Dimens.CardGap))
                    OutlinedTextField(
                        value = newGoalTarget,
                        onValueChange = { newGoalTarget = it },
                        label = { Text("Target hours") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(Dimens.CardGap))
                    Text("Timeframe", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(Dimens.TitleGap))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.TitleGap)) {
                        targetTypes.forEach { type ->
                            PillChip(
                                label = type.lowercase().replaceFirstChar { it.uppercase() },
                                selected = newGoalType == type,
                                onClick = { newGoalType = type },
                                tint = ProductiveColor
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetHours = newGoalTarget.toFloatOrNull() ?: 0f
                        if (newGoalTitle.isNotBlank() && targetHours > 0) {
                            onAddGoal(newGoalTitle, newGoalDescription, newGoalCategory, targetHours)
                            newGoalTitle = ""
                            newGoalDescription = ""
                            newGoalCategory = "PRODUCTIVE"
                            newGoalTarget = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GoalCard(
    goal: GoalEntity,
    onUpdateProgress: (Float) -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (goal.targetHours > 0) (goal.currentHours / goal.targetHours).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val categoryColor = when (goal.category) {
        "PRODUCTIVE" -> ProductiveColor
        "EXERCISE" -> ExerciseColor
        "LEARNING" -> LearningColor
        "SLEEP" -> SleepColor
        "SOCIAL" -> SocialColor
        else -> ProductiveColor
    }

    SurfaceCard(
        containerColor = if (goal.isCompleted) {
            categoryColor.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (goal.description.isNotBlank()) {
                    Text(
                        text = goal.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (goal.isCompleted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = categoryColor
                )
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Dimens.CardGap))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = categoryColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Spacer(modifier = Modifier.height(Dimens.TitleGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${goal.currentHours.toInt()}h / ${goal.targetHours.toInt()}h",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = categoryColor
            )
        }
        if (!goal.isCompleted) {
            Spacer(modifier = Modifier.height(Dimens.TitleGap))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.TitleGap)) {
                PillChip(
                    label = "+0.5h",
                    selected = false,
                    onClick = { onUpdateProgress(0.5f) },
                    tint = categoryColor,
                    modifier = Modifier.height(48.dp)
                )
                PillChip(
                    label = "+1h",
                    selected = false,
                    onClick = { onUpdateProgress(1f) },
                    tint = categoryColor,
                    modifier = Modifier.height(48.dp)
                )
                PillChip(
                    label = "+2h",
                    selected = false,
                    onClick = { onUpdateProgress(2f) },
                    tint = categoryColor,
                    modifier = Modifier.height(48.dp)
                )
            }
        }
    }
}
