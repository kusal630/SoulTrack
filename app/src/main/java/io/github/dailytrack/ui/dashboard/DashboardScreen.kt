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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.db.entity.SessionEntity
import io.github.dailytrack.data.db.entity.TodoEntity
import io.github.dailytrack.data.db.entity.GoalEntity
import androidx.compose.foundation.BorderStroke
import io.github.dailytrack.engine.DayStatus
import io.github.dailytrack.engine.SwitchReport
import io.github.dailytrack.engine.dayProgress
import io.github.dailytrack.engine.focusProgress
import io.github.dailytrack.engine.goalGradientMessage
import io.github.dailytrack.engine.goalProgress
import io.github.dailytrack.engine.recordedDayProgress
import io.github.dailytrack.engine.todayProgressLabel
import io.github.dailytrack.engine.weekStatusLabel
import io.github.dailytrack.service.FocusSettings
import io.github.dailytrack.ui.Screen
import io.github.dailytrack.ui.brain.MoodEnvironmentSheet
import io.github.dailytrack.ui.brain.MoodSelection
import io.github.dailytrack.ui.brain.UpNextCard
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.GhostCta
import io.github.dailytrack.ui.components.Hairline
import io.github.dailytrack.ui.components.PieChartData
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.SoulRings
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.components.TimeBreakdownPieChart
import io.github.dailytrack.ui.components.WeekDayShortNames
import io.github.dailytrack.ui.components.dayProgressFor
import io.github.dailytrack.ui.components.rememberReduceMotion
import io.github.dailytrack.ui.components.weekStatusesFor
import io.github.dailytrack.ui.navigateSingleTop
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.LocalDayStatusColors
import io.github.dailytrack.ui.theme.NeutralColor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.theme.RecoveryColor
import io.github.dailytrack.ui.theme.SleepColor
import io.github.dailytrack.ui.theme.SocialColor
import io.github.dailytrack.ui.theme.TimerDigitsSmall
import io.github.dailytrack.ui.theme.WastedColor
import io.github.dailytrack.ui.theme.tabularFigures
import io.github.dailytrack.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Today - the home screen.
 *
 * The ring hero is the product: the inner arc is today's tasks, the outer ring
 * is the week that is being written one day at a time. Everything below it is
 * supporting cast: what to do next, what is still open, and how the time went.
 */
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val todaySessions by viewModel.todaySessions.collectAsState()
    val todayCoverage by viewModel.todayCoverage.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val loopStatus by viewModel.loopStatus.collectAsState()
    val brainResult by viewModel.brainResult.collectAsState()
    val quietness by viewModel.quietness.collectAsState()
    val activeTodos by viewModel.activeTodos.collectAsState()
    val completedTodos by viewModel.completedTodos.collectAsState()
    val weeklyReport by viewModel.weeklyReport.collectAsState()
    val activeInsights by viewModel.activeInsights.collectAsState()
    val switchReport by viewModel.switchReport.collectAsState()
    val wellbeing by viewModel.wellbeing.collectAsState()
    val switchEvents by viewModel.switchEventsToday.collectAsState()

    val context = LocalContext.current
    val noiseSensingEnabled = remember {
        FocusSettings.isNoiseSensingEnabled(context.applicationContext)
    }

    val today = LocalDate.now()
    val zone = remember { ZoneId.systemDefault() }
    val allTodos = remember(activeTodos, completedTodos) { activeTodos + completedTodos }

    val focusGoalSeconds by viewModel.focusGoalSeconds.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val activeGoals = remember(goals) { goals.filter { !it.isCompleted && !it.isArchived } }

    // The ring is one number for one day: finished tasks and recorded focus
    // time both earn it. Nothing moves the ring that was not written down.
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

    var suggestionDismissed by rememberSaveable { mutableStateOf(false) }
    var showMoodSheet by rememberSaveable { mutableStateOf(false) }
    var moodSelection by remember { mutableStateOf(MoodSelection()) }

    val completedSessions = todaySessions.filter { !it.isActive }
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
            IconButton(onClick = { navController.navigateSingleTop(Screen.Explore.route) }) {
                Icon(Icons.Default.Explore, contentDescription = "Explore")
            }
            IconButton(onClick = { navController.navigateSingleTop(Screen.Habits.route) }) {
                Icon(Icons.Default.Flag, contentDescription = "Habits")
            }
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

        if (activeGoals.isNotEmpty()) {
            item {
                GoalsProgressCard(goals = activeGoals)
            }
        }

        val lastAbandoned = switchEvents.lastOrNull { it.fromTodoId != null }?.fromTodoId
        if (switchReport.switchCount >= 3 && lastAbandoned != null) {
            item {
                JumpAttentionCard(
                    report = switchReport,
                    taskTitle = allTodos.firstOrNull { it.id == lastAbandoned }?.title,
                    onResume = { viewModel.startTodo(lastAbandoned) }
                )
            }
        }

        if (activeSession == null) {
            item {
                if (suggestionDismissed || brainResult?.primary == null) {
                    if (brainResult?.primary == null) {
                        UpNextEmpty(
                            hasTasks = activeTodos.isNotEmpty(),
                            onOpenTasks = { navController.navigateSingleTop(Screen.Todo.route) }
                        )
                    } else {
                        AlternativesCard(
                            onOpenTasks = { navController.navigateSingleTop(Screen.Todo.route) },
                            onUndo = { suggestionDismissed = false }
                        )
                    }
                } else {
                    UpNextCard(
                        result = brainResult,
                        quietness = quietness,
                        noiseEnabled = noiseSensingEnabled,
                        environmentChips = moodSelection.chips(),
                        onStartTask = { viewModel.startTask(it) },
                        onNotNow = { suggestionDismissed = true },
                        onOpenTasks = { navController.navigateSingleTop(Screen.Todo.route) },
                        onTune = { showMoodSheet = true }
                    )
                }
            }
        }

        item {
            SectionTitle(
                title = "Tasks for today",
                subtitle = if (assignedCount == 0) {
                    "Assigning a task starts the day ring"
                } else {
                    goalGradientMessage(completedCount, assignedCount)
                        ?: "$completedCount of $assignedCount done"
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
                ).take(4),
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

        item {
            PrimaryCta(
                text = if (activeSession != null) "Open timer" else "Start focus session",
                onClick = {
                    navController.navigateSingleTop(
                        if (activeSession != null) Screen.Timer.route else Screen.Pomodoro.route
                    )
                },
                icon = Icons.Default.PlayArrow
            )
        }

        if (loopStatus?.isLoopDetected == true || loopStatus?.comfortZoneWarning == true) {
            item {
                ComfortZoneCard(
                    isLoop = loopStatus?.isLoopDetected == true,
                    consecutiveDays = loopStatus?.consecutiveDays ?: 0
                )
            }
        }

        if (wellbeing != null) {
            item { DirectionCard(summary = wellbeing!!) }
        }

        if (todayCoverage != null &&
            (todayCoverage!!.trackedSeconds + todayCoverage!!.untrackedSeconds) > 0
        ) {
            item {
                TimeBreakdownPieChart(
                    data = listOf(
                        PieChartData(
                            "Productive",
                            (todayCoverage!!.productiveSeconds + todayCoverage!!.learningSeconds).toFloat(),
                            ProductiveColor
                        ),
                        PieChartData("Exercise", todayCoverage!!.exerciseSeconds.toFloat(), ExerciseColor),
                        PieChartData("Sleep", todayCoverage!!.sleepSeconds.toFloat(), SleepColor),
                        PieChartData("Social", todayCoverage!!.socialSeconds.toFloat(), SocialColor),
                        PieChartData("Recovery", todayCoverage!!.recoverySeconds.toFloat(), RecoveryColor),
                        PieChartData("Wasted", todayCoverage!!.wastedSeconds.toFloat(), WastedColor),
                        PieChartData("Untracked (waste)", todayCoverage!!.untrackedSeconds.toFloat(), NeutralColor)
                    ).filter { it.value > 0f }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)
            ) {
                GhostCta(
                    text = "History",
                    onClick = { navController.navigateSingleTop(Screen.History.route) },
                    icon = Icons.Default.History,
                    modifier = Modifier.weight(1f)
                )
                GhostCta(
                    text = "Insights",
                    onClick = { navController.navigateSingleTop(Screen.Insights.route) },
                    icon = Icons.Default.BarChart,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (completedSessions.isNotEmpty()) {
            item { SectionTitle(title = "Finished sessions (${completedSessions.size})") }
            items(completedSessions.take(4), key = { it.id }) { session ->
                CompletedSessionRow(
                    session = session,
                    categoryName = session.categoryId?.let { categories[it]?.name } ?: ""
                )
            }
        }

        if (activeInsights.isNotEmpty()) {
            item { SectionTitle(title = "Insights") }
            items(activeInsights.take(2)) { insight ->
                InsightCard(insight = insight, onDismiss = { viewModel.dismissInsight(insight.id) })
            }
        }
    }

    if (showMoodSheet) {
        MoodEnvironmentSheet(
            current = moodSelection,
            onDismiss = { showMoodSheet = false },
            onSave = {
                moodSelection = it
                showMoodSheet = false
            }
        )
    }
}

/** The hero: rings, plain-language counts, and the day's verdict. */
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeekLabel("Mon", "Tue")
                WeekLabel("Wed", "Thu")
                WeekLabel("Fri", "Sat")
                WeekLabel("Sun", "")
            }
            Spacer(modifier = Modifier.height(8.dp))
            StatusLegendRow(tint = statusTint, status = status)
        }
    }
}

@Composable
private fun WeekLabel(a: String, b: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (b.isEmpty()) a else "$a · $b",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

/**
 * Where the day is actually heading, from the phone's own usage.
 *
 * Everything here is measured on-device: the hours Soul Track never saw are
 * still counted, and unrecorded time is treated as waste rather than as
 * free time. One number, one sentence, one thing to change.
 */
@Composable
private fun DirectionCard(summary: io.github.dailytrack.wellbeing.WellbeingSummary) {
    val share = summary.productiveShare
    val verdict = when {
        summary.trackedMillis <= 0L -> "Nothing measured yet"
        share >= 0.6f -> "You are heading in the right direction"
        share >= 0.35f -> "Half your measured time is going to work"
        else -> "Most measured time is going elsewhere"
    }
    val next = when {
        summary.untrackedShare >= 0.4f ->
            "A large slice of today was never recorded. Start the timer before the next block."
        share < 0.35f && summary.wastedMillis > summary.productiveMillis ->
            "Waste is outrunning focus. Pick one block and give it a name."
        share >= 0.6f && summary.wastedMillis > 0 ->
            "Focus is winning. Protect it by closing the smallest time leak next."
        else -> "Keep recording - the ring only knows what you write down."
    }

    SurfaceCard(contentPadding = PaddingValues(Dimens.CardPadding)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Direction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = verdict,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { share },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (share >= 0.5f) ProductiveColor else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "%.0f%% focus · %.0f%% waste · %.0f%% unrecorded".format(
                    share * 100,
                    (summary.wastedMillis.toFloat() / summary.totalMillis.coerceAtLeast(1L)) * 100,
                    summary.untrackedShare * 100
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = next,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeSession.title.ifBlank { "Tracking" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (categoryName.isNotBlank()) {
                    Text(
                        text = categoryName,
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
                    Icons.Default.CheckCircle,
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

@Composable
private fun UpNextEmpty(hasTasks: Boolean, onOpenTasks: () -> Unit) {
    SurfaceCard {
        Column {
            Text(
                text = if (hasTasks) "Nothing urgent" else "No tasks to suggest yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (hasTasks) {
                    "The app will surface the best next task once your mood, place and history say something useful."
                } else {
                    "Add a task and the intelligent layer starts ranking what fits this moment."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasTasks) {
                Spacer(modifier = Modifier.height(Dimens.CardGap))
                GhostCta(text = "Open tasks", onClick = onOpenTasks)
            }
        }
    }
}

@Composable
private fun AlternativesCard(onOpenTasks: () -> Unit, onUndo: () -> Unit) {
    SurfaceCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Suggestion skipped",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onUndo, modifier = Modifier.size(Dimens.MinTouchTarget)) {
                    Icon(Icons.Default.Undo, contentDescription = "Show suggestion again")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Nothing changes - the pick will come back the next time the screen refreshes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimens.CardGap))
            GhostCta(text = "Open tasks", onClick = onOpenTasks)
        }
    }
}

@Composable
private fun ComfortZoneCard(isLoop: Boolean, consecutiveDays: Int) {
    val statusColors = LocalDayStatusColors.current
    val tint = if (isLoop) statusColors.wastingTime else statusColors.notProductive
    SurfaceCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isLoop) Icons.Default.Repeat else Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = tint
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isLoop) "Routine loop detected" else "Comfort zone warning",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isLoop) {
                        "Your routine has looked the same for $consecutiveDays days. Try something new to grow."
                    } else {
                        "Your routine lacks variety. Add a new activity to break out of the comfort zone."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CompletedSessionRow(session: SessionEntity, categoryName: String) {
    val duration = (session.endTime ?: session.startTime) - session.startTime
    SurfaceCard(contentPadding = PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = ProductiveColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title.ifBlank { "Session" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (categoryName.isNotBlank()) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = formatDurationHMS(duration / 1000),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun InsightCard(insight: io.github.dailytrack.data.db.entity.InsightEntity, onDismiss: () -> Unit) {
    SurfaceCard(
        containerColor = when (insight.severity) {
            "CRITICAL", "WARNING" -> MaterialTheme.colorScheme.surfaceContainerHigh
            "CAUTION" -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = insight.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (insight.actionLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = insight.actionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(Dimens.MinTouchTarget)) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Dismiss insight",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun formatHMS(millis: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, secs)
    }
}

fun formatDurationHMS(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
        minutes > 0 -> String.format(java.util.Locale.US, "%02d:%02d", minutes, secs)
        else -> String.format(java.util.Locale.US, "%02d", secs)
    }
}


/**
 * Shows up once today's work has jumped context three or more times: the
 * abandoned task is named and put one tap away, so the drift is visible on
 * the very screen where it is happening.
 */
@Composable
private fun JumpAttentionCard(
    report: SwitchReport,
    taskTitle: String?,
    onResume: () -> Unit
) {
    SurfaceCard(
        containerColor = WastedColor.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, WastedColor.copy(alpha = 0.35f)),
        contentPadding = PaddingValues(Dimens.CardPadding)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = WastedColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Your focus keeps switching",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val abandonedLine = if (report.abandonedTaskCount > 0) {
                "${report.abandonedTaskCount} unfinished"
            } else {
                "several open threads"
            }
            Text(
                "Context changed ${report.switchCount} times today and $abandonedLine. "
                    + "Pick the task you walked away from and close it first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (taskTitle != null) {
                Spacer(modifier = Modifier.height(10.dp))
                GhostCta(
                    text = "Back to \"$taskTitle\"",
                    onClick = onResume
                )
            }
        }
    }
}

/**
 * Reporting goal progress back to the user is itself a proven lever on
 * attainment (Harkin et al., 2015), so the active goals are shown as concrete
 * progress bars rather than buried in a settings screen.
 */
@Composable
private fun GoalsProgressCard(goals: List<GoalEntity>) {
    SurfaceCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                tint = ProductiveColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "Goals",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        goals.take(3).forEach { goal ->
            val fraction = goalProgress(goal.currentHours, goal.targetHours)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${goal.currentHours.toInt()}/${goal.targetHours.toInt()}h",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                color = ProductiveColor
            )
        }

        if (goals.size > 3) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "+${goals.size - 3} more",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
