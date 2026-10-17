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


package io.github.dailytrack.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.db.entity.SessionEntity
import io.github.dailytrack.data.db.entity.TodoEntity
import io.github.dailytrack.engine.DayClassificationThresholds
import io.github.dailytrack.engine.DaySnapshot
import io.github.dailytrack.engine.DayStatus
import io.github.dailytrack.engine.classifyDay
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.MiniSoulRing
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.components.WeekDayShortNames
import io.github.dailytrack.ui.components.daySnapshotFor
import io.github.dailytrack.ui.components.weekStartFor
import io.github.dailytrack.ui.theme.LocalDayStatusColors
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.viewmodel.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * History: the week as seven mini rings, and one day opened up into a
 * second-by-second timeline plus the plain-language reason it was classified
 * the way it was.
 */
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val sessions by viewModel.todaySessions.collectAsState()
    val coverage by viewModel.todayCoverage.collectAsState()
    val weeklyReport by viewModel.weeklyReport.collectAsState()
    val activeTodos by viewModel.activeTodos.collectAsState()
    val completedTodos by viewModel.completedTodos.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val today = LocalDate.now()
    val zone = remember { ZoneId.systemDefault() }
    val allTodos = remember(activeTodos, completedTodos) { activeTodos + completedTodos }
    val monday = remember(today) { weekStartFor(today) }
    val days = remember(monday) { (0 until 7).map { monday.plusDays(it.toLong()) } }

    ScreenList(
        title = "History",
        subtitle = "One mark per day, on the week ring",
        navController = navController
    ) {
        item {
            SurfaceCard(modifier = Modifier.padding(top = 8.dp)) {
                Column {
                    Text(
                        text = "This week",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(DimensGap))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEachIndexed { index, day ->
                            DayRingCell(
                                day = day,
                                isToday = day == today,
                                isSelected = day == selectedDate,
                                snapshot = daySnapshotFor(
                                    date = day,
                                    coverage = weeklyReport[day],
                                    todos = allTodos,
                                    zone = zone
                                ),
                                onSelect = { viewModel.selectDate(day) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(DimensGap))
            SectionTitle(
                title = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                subtitle = if (selectedDate == today) "Today, still being written" else "Closed day"
            )
        }

        item {
            DayVerdictCard(
                snapshot = daySnapshotFor(
                    date = selectedDate,
                    coverage = coverage,
                    todos = allTodos,
                    zone = zone
                ),
                isToday = selectedDate == today
            )
        }

        if (sessions.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(DimensGap))
                SurfaceCard {
                    Column {
                        Text(
                            text = "Timeline",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DayTimelineBar(
                            sessions = sessions,
                            day = selectedDate,
                            zone = zone,
                            categoryColors = categories.mapValues { Color(it.value.color) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Midnight to midnight, coloured by what you were tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(DimensGap))
                SectionTitle(title = "Sessions (${sessions.size})")
            }
            items(sessions, key = { it.id }) { session ->
                SessionRow(session = session)
            }
        } else {
            item {
                EmptyState(
                    title = "Nothing tracked on this day",
                    message = "Sessions you log will line up here as a second-by-second bar.",
                    icon = Icons.Default.Timer
                )
            }
        }
    }
}

private val DimensGap = 16.dp

@Composable
private fun DayRingCell(
    day: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    snapshot: DaySnapshot,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = classifyDay(snapshot)
    val palette = LocalDayStatusColors.current
    val progress = snapshot.completion

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MiniSoulRing(
            progress = progress,
            status = if (isToday) null else status,
            isToday = isToday,
            modifier = Modifier
                .size(44.dp)
                .then(
                    if (isSelected) Modifier.background(
                        palette.colorFor(if (isToday) null else status).copy(alpha = 0.12f),
                        RoundedCornerShape(50)
                    ) else Modifier
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = WeekDayShortNames[day.dayOfWeek.value - 1],
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Icon(
            imageVector = statusIcon(if (isToday) null else status),
            contentDescription = if (isToday) "Today, in progress" else status.label,
            tint = if (isToday) MaterialTheme.colorScheme.onSurfaceVariant
            else palette.colorFor(status),
            modifier = Modifier.size(12.dp)
        )
    }
}

/** Colour + icon + word: the classification always spells itself out. */
@Composable
private fun DayVerdictCard(snapshot: DaySnapshot, isToday: Boolean) {
    val palette = LocalDayStatusColors.current
    val status = classifyDay(snapshot)
    val shown = if (isToday) DayStatus.IN_PROGRESS else status
    val tint = palette.colorFor(if (isToday) null else status)

    SurfaceCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(statusIcon(if (isToday) null else status), contentDescription = null, tint = tint)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shown.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = reasonFor(snapshot, isToday),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                VerdictStat("Tracked", formatHours(snapshot.trackedSeconds), Modifier.weight(1f))
                VerdictStat("Focused", formatHours(snapshot.productiveSeconds), Modifier.weight(1f))
                VerdictStat(
                    "Tasks",
                    "${snapshot.completedTasks}/${snapshot.assignedTasks}",
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun VerdictStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun reasonFor(snapshot: DaySnapshot, isToday: Boolean): String = when {
    isToday -> "The day is still open. It is written into the week ring at midnight."
    snapshot.assignedTasks <= 0 -> "No tasks were assigned, so nothing was judged."
    snapshot.distractionRatio > DayClassificationThresholds.WASTING_TIME_DISTRACTION_RATIO -> {
        val pct = (snapshot.distractionRatio * 100).toInt()
        "Distraction was ${pct}% of tracked time, over the " +
            "${(DayClassificationThresholds.WASTING_TIME_DISTRACTION_RATIO * 100).toInt()}% limit."
    }
    snapshot.productiveRatio < DayClassificationThresholds.NOT_PRODUCTIVE_PRODUCTIVE_RATIO -> {
        val pct = (snapshot.productiveRatio * 100).toInt()
        "Focus time was ${pct}% of the planned session, under the " +
            "${(DayClassificationThresholds.NOT_PRODUCTIVE_PRODUCTIVE_RATIO * 100).toInt()}% bar."
    }
    snapshot.completion < 1f -> "Not every task was finished: " +
        "${snapshot.completedTasks} of ${snapshot.assignedTasks} done."
    else -> "Every task finished and the day stayed focused."
}

@Composable
private fun DayTimelineBar(
    sessions: List<SessionEntity>,
    day: LocalDate,
    zone: ZoneId,
    categoryColors: Map<Long, Color>
) {
    val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val span = (dayEnd - dayStart).toFloat()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            sessions.forEach { session ->
                val start = ((session.startTime - dayStart).coerceIn(0, (dayEnd - dayStart)) / span)
                val sessionEnd = session.endTime
                    ?: if (session.isActive) System.currentTimeMillis() else session.startTime
                val end = ((sessionEnd - dayStart)
                    .coerceIn(0, dayEnd - dayStart)) / span
                val width = ((end - start) * size.width).coerceAtLeast(2f)
                drawRect(
                    color = session.categoryId?.let { categoryColors[it] } ?: ProductiveColor,
                    topLeft = Offset(start * size.width, 0f),
                    size = Size(width, size.height)
                )
            }
        }
    }
}

@Composable
private fun SessionRow(session: SessionEntity) {
    val sessionEnd = session.endTime
        ?: if (session.isActive) System.currentTimeMillis() else session.startTime
    val duration = (sessionEnd - session.startTime).coerceAtLeast(0L)
    SurfaceCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title.ifBlank { "Session" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${formatClock(session.startTime)} – ${
                        session.endTime?.let { formatClock(it) } ?: "now"
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatHours(duration / 1000),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun statusIcon(status: DayStatus?) = when (status) {
    DayStatus.DONE -> Icons.Default.CheckCircle
    DayStatus.TASKS_NOT_MET -> Icons.Default.Repeat
    DayStatus.NOT_PRODUCTIVE -> Icons.Default.Timer
    DayStatus.WASTING_TIME -> Icons.Default.LocalFireDepartment
    else -> Icons.Default.Timer
}

private fun formatHours(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${secs}s"
    }
}

private fun formatClock(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
