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

package io.github.dailytrack.ui.reminders

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dailytrack.data.db.entity.ReminderEntity
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.PillChip
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.StatTile
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.ProductiveColor

@Composable
fun RemindersScreen(
    reminders: List<ReminderEntity>,
    onAddReminder: (String, String, String, Int, Int) -> Unit,
    onToggleReminder: (Long, Boolean) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newMessage by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf("TIME") }
    var selectedHour by remember { mutableIntStateOf(9) }
    var selectedMinute by remember { mutableIntStateOf(0) }

    val types = listOf("TIME", "LOCATION", "HABIT", "STREAK")
    val activeCount = reminders.count { it.isEnabled }

    ScreenList(
        title = "Reminders",
        subtitle = "Time, habit and streak nudges",
        onBack = null
    ) {
        if (reminders.isEmpty()) {
            item {
                EmptyState(
                    title = "No reminders yet",
                    message = "A well-timed nudge keeps streaks alive. Add your first reminder and DailyTrack will tap you on the shoulder.",
                    icon = Icons.Default.Alarm,
                    tint = ExerciseColor,
                    actionLabel = "Add reminder",
                    onAction = { showAddDialog = true }
                )
            }
        } else {
            item {
                PrimaryCta(
                    text = "Add reminder",
                    icon = Icons.Default.Add,
                    onClick = { showAddDialog = true }
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                    StatTile(
                        label = "Active",
                        value = "$activeCount",
                        icon = Icons.Default.NotificationsActive,
                        tint = ProductiveColor,
                        caption = "Firing on schedule",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Paused",
                        value = "${reminders.size - activeCount}",
                        icon = Icons.Default.PauseCircle,
                        tint = ExerciseColor,
                        caption = "Switched off",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            items(reminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onToggle = { enabled -> onToggleReminder(reminder.id, enabled) },
                    onDelete = { onDeleteReminder(reminder) }
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add reminder") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(Dimens.CardGap))
                    OutlinedTextField(
                        value = newMessage,
                        onValueChange = { newMessage = it },
                        label = { Text("Message (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(Dimens.CardGap))
                    Text("Type", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(Dimens.TitleGap))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.TitleGap)) {
                        types.forEach { type ->
                            PillChip(
                                label = type.lowercase().replaceFirstChar { it.uppercase() },
                                selected = newType == type,
                                onClick = { newType = type },
                                tint = ProductiveColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Dimens.CardGap))
                    Text("Time", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(Dimens.TitleGap))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.TitleGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = selectedHour.toString(),
                            onValueChange = { selectedHour = it.toIntOrNull() ?: 9 },
                            label = { Text("Hour") },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                        Text(":")
                        OutlinedTextField(
                            value = selectedMinute.toString(),
                            onValueChange = { selectedMinute = it.toIntOrNull() ?: 0 },
                            label = { Text("Min") },
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onAddReminder(newTitle, newMessage, newType, selectedHour, selectedMinute)
                            newTitle = ""
                            newMessage = ""
                            newType = "TIME"
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
fun ReminderCard(
    reminder: ReminderEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val tint = if (reminder.isEnabled) {
        ProductiveColor
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    SurfaceCard(
        containerColor = if (reminder.isEnabled) {
            ProductiveColor.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    reminderIcon(reminder.type),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(Dimens.CardGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (reminder.isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (reminder.message.isNotBlank()) {
                    Text(
                        text = reminder.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    text = String.format("%02d:%02d", reminder.timeHour, reminder.timeMinute),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tint
                )
            }
            Switch(
                checked = reminder.isEnabled,
                onCheckedChange = onToggle
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun reminderIcon(type: String): ImageVector = when (type) {
    "TIME" -> Icons.Default.Schedule
    "LOCATION" -> Icons.Default.LocationOn
    "HABIT" -> Icons.Default.Flag
    "STREAK" -> Icons.Default.LocalFireDepartment
    else -> Icons.Default.Alarm
}
