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


package io.github.dailytrack.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.db.entity.SessionEntity
import io.github.dailytrack.data.db.entity.TodoEntity
import io.github.dailytrack.engine.DayStatus
import io.github.dailytrack.engine.focusProgress
import io.github.dailytrack.engine.recordedDayProgress
import io.github.dailytrack.engine.todayProgressLabel
import io.github.dailytrack.engine.weekStatusLabel
import io.github.dailytrack.ui.Screen
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.GhostCta
import io.github.dailytrack.ui.components.Hairline
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.SoulRings
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.components.WeekDayShortNames
import io.github.dailytrack.ui.components.dayProgressFor
import io.github.dailytrack.ui.components.rememberReduceMotion
import io.github.dailytrack.ui.components.weekStatusesFor
import io.github.dailytrack.ui.navigateSingleTop
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.LocalDayStatusColors
import io.github.dailytrack.ui.theme.TimerDigitsSmall
import io.github.dailytrack.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Today - the home screen.
 *
 * One ring, one focus action, and the tasks that matter today. Everything
 * else lives one level down so the first screen stays calm.
 */
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val todaySessions by viewModel.todaySessions.collectAsState()
    val todayCoverage by viewModel.todayCoverage.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val activeTodos by viewModel.activeTodos.collectAsState()
    val completedTodos by viewModel.completedTodos.collectAsState()
    val weeklyReport by viewModel.weeklyReport.collectAsState()
    val focusGoalSeconds by viewModel.focusGoalSeconds.collectAsState()

    val today = LocalDate.now()
    val zone = remember { ZoneId.systemDefault() }
    val allTodos = remember(activeTodos, completedTodos) { activeTodos + completedTodos }

    val taskProgressValue = remember(activeTodos, completedTodos, today) {
        dayProgressFor(today, allTodos, zone)
    }
    val todayFocusedSeconds = todayCoverage?.totalProductiveSeconds ?: 0L
    val focusProgressValue = remember(todayFocusedSeconds, focusGoalSeconds) {
        focusProgress(todayFocusedSeconds, focusGoalSeconds)
    }
    val dayProgressValue = remember(taskProgressValue, focusProgressValue) {
        recordedDayProgress(taskProgressValue, focusProgressValue)
    }
    val weekStatuses = remember(weeklyReport, allTodos, today) {
        weekStatusesFor(today, weeklyReport, allTodos, zone)
    }

    val assignedCount = remember(activeTodos, completedTodos, today) {
        allTodos.count {
            java.time.Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() == today ||
                it.deadline?.let { d ->
                    java.time.Instant.ofEpochMilli(d).atZone(zone).toLocalDate() == today
                } == true
        }
    }
    val completedCount = remember(completedTodos, today) {
        completedTodos.count {
            it.completedAt != null &&
                java.time.Instant.ofEpochMilli(it.completedAt!!).atZone(zone).toLocalDate() == today
        }
    }

    val todayStatus = when {
        dayProgressValue >= 1f -> DayStatus.DONE
        assignedCount == 0 && todayFocusedSeconds <= 0L -> DayStatus.NO_TASKS
        else -> DayStatus.IN_PROGRESS
    }
    val statusColors = LocalDayStatusColors.current

    ScreenList(
        title = "Today",
        subtitle = today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
        actions = {
            IconButton(onClick = { navController.navigateSingleTop(Screen.Settings.route) }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    ) {
        item {
            TodayHero(
                dayProgress = dayProgressValue,
                weekStatuses = weekStatuses,
                todayIndex = today.dayOfWeek.value - 1,
                completed = completedCount,
                assigned = assignedCount,
                focusedMinutes = (todayFocusedSeconds / 60).toInt(),
                status = todayStatus,
                statusTint = statusColors.colorFor(
                    if (todayStatus == DayStatus.IN_PROGRESS) null else todayStatus
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            TrackerChip(
                activeSession = activeSession,
                categoryName = activeSession?.categoryId?.let { categories[it]?.name } ?: "",
                onStop = { viewModel.stopSession() }
            )
        }

        item {
            PrimaryCta(
                text = when {
                    activeSession != null -> "Open running timer"
                    todaySessions.any { !it.isActive } -> "Start another focus block"
                    else -> "Start focus block"
                },
                onClick = {
                    navController.navigateSingleTop(
                        if (activeSession != null) Screen.Timer.route else Screen.Pomodoro.route
                    )
                },
                icon = Icons.Default.PlayArrow
            )
        }

        item {
            SectionTitle(
                title = "Today's tasks",
                subtitle = if (assignedCount == 0) {
                    "Add a task to fill the day ring"
                } else {
                    "$completedCount of $assignedCount done today"
                },
                trailing = {
                    if (activeTodos.isNotEmpty()) {
                        GhostCta(text = "All tasks", onClick = {
                            navController.navigateSingleTop(Screen.Todo.route)
                        })
                    }
                }
            )
        }

        if (activeTodos.isNotEmpty()) {
            items(
                activeTodos.sortedWith(
                    compareByDescending<TodoEntity> { it.priority }
                        .thenBy { it.deadline ?: Long.MAX_VALUE }
                ).take(3),
                key = { it.id }
            ) { todo ->
                TodayTaskRow(todo = todo, onToggle = { viewModel.completeTodo(todo.id) })
            }
        } else {
            item {
                EmptyState(
                    title = "No tasks yet",
                    message = "Add what you want to finish today. The ring fills as each one gets done.",
                    icon = Icons.Default.AddTask,
                    actionLabel = "Add a task",
                    onAction = { navController.navigateSingleTop(Screen.Todo.route) }
                )
            }
        }
    }
}

/** The hero: one ring, plain-language counts, and the day's verdict. */
@Composable
private fun TodayHero(
    dayProgress: Float,
    weekStatuses: List<DayStatus?>,
    todayIndex: Int,
    completed: Int,
    assigned: Int,
    focusedMinutes: Int,
    status: DayStatus,
    statusTint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val reduceMotion = rememberReduceMotion()

    SurfaceCard(modifier = modifier, contentPadding = PaddingValues(Dimens.CardPadding)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SoulRings(
                dayProgress = dayProgress,
                weekStatuses = weekStatuses,
                todayIndex = todayIndex,
                modifier = Modifier.size(Dimens.HeroRing),
                animate = !reduceMotion,
                contentDescription = todayProgressLabel(completed, assigned) + " " +
                    weekStatusLabel(weekStatuses, WeekDayShortNames)
            )

            Spacer(modifier = Modifier.height(Dimens.CardGap))

            Text(
                text = when {
                    assigned > 0 && focusedMinutes > 0 -> "$completed of $assigned done · ${focusedMinutes}m focus"
                    assigned > 0 -> "$completed of $assigned done"
                    focusedMinutes > 0 -> "${focusedMinutes}m focus recorded"
                    else -> "Nothing recorded yet"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (status) {
                    DayStatus.NO_TASKS -> "The inner ring fills from tasks you finish and time you track"
                    DayStatus.DONE -> "Day closed - the week ring just recorded it"
                    else -> "The week ring records today when the day ends"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Dimens.CardGap))
            Hairline()
            Spacer(modifier = Modifier.height(Dimens.CardGap))

            Row(modifier = Modifier.fillMaxWidth()) {
                WeekDayShortNames.forEachIndexed { index, name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == todayIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (index == todayIndex) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            StatusLegendRow(tint = statusTint, status = status)
        }
    }
}

/** Status badge always pairs the colour with an icon and a word. */
@Composable
private fun StatusLegendRow(tint: androidx.compose.ui.graphics.Color, status: DayStatus) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = when (status) {
                DayStatus.DONE -> Icons.Default.CheckCircle
                DayStatus.TASKS_NOT_MET -> Icons.Default.Repeat
                DayStatus.NOT_PRODUCTIVE -> Icons.Default.Timer
                DayStatus.WASTING_TIME -> Icons.Default.LocalFireDepartment
                else -> Icons.Default.Timer
            },
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Live tracking chip with tabular figures so the digits never jitter. */
@Composable
private fun TrackerChip(
    activeSession: SessionEntity?,
    categoryName: String,
    onStop: () -> Unit
) {
    var elapsed by remember(activeSession?.id) {
        mutableLongStateOf(
            activeSession?.let { System.currentTimeMillis() - it.startTime } ?: 0L
        )
    }

    LaunchedEffect(activeSession?.id, activeSession?.startTime) {
        val start = activeSession?.startTime ?: return@LaunchedEffect
        while (true) {
            elapsed = System.currentTimeMillis() - start
            kotlinx.coroutines.delay(1000)
        }
    }

    if (activeSession == null) return

    SurfaceCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentPadding = PaddingValues(horizontal = Dimens.CardPadding, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(10.dp))
            val chipTitle = activeSession.title.ifBlank {
                categoryName.ifBlank { "Tracking" }
            }
            val chipSubtitle = categoryName.takeIf { it.isNotBlank() && it != chipTitle }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chipTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (chipSubtitle != null) {
                    Text(
                        text = chipSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = formatHMS(elapsed),
                style = TimerDigitsSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = onStop, modifier = Modifier.size(Dimens.MinTouchTarget)) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop tracking",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** One task, right on the home screen. Ticking it springs the ring. */
@Composable
private fun TodayTaskRow(todo: TodoEntity, onToggle: () -> Unit) {
    SurfaceCard(
        contentPadding = PaddingValues(horizontal = Dimens.CardPadding, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(Dimens.MinTouchTarget)
            ) {
                Icon(
                    Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "Complete ${todo.title}",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = buildList {
                    if (todo.estimatedMinutes > 0) add("${todo.estimatedMinutes} min")
                    if (todo.priority >= 2) add(if (todo.priority >= 3) "High priority" else "Medium priority")
                }.joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun formatHMS(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val hours = TimeUnit.SECONDS.toHours(totalSeconds)
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
