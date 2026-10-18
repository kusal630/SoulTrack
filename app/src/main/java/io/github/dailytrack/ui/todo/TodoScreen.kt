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


package io.github.dailytrack.ui.todo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.db.entity.SubtaskEntity
import io.github.dailytrack.data.db.entity.TodoEntity
import io.github.dailytrack.engine.DayStatus
import io.github.dailytrack.engine.TaskQuality
import io.github.dailytrack.ui.components.*
import io.github.dailytrack.ui.components.dayProgressFor
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.navigateToHome
import io.github.dailytrack.ui.viewmodel.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class TodoFilter { ALL, HIGH, MEDIUM, LOW, OVERDUE }
enum class TodoSortOrder { PRIORITY_DESC, PRIORITY_ASC, DEADLINE, NEWEST, OLDEST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val activeTodos by viewModel.activeTodos.collectAsState()
    val completedTodos by viewModel.completedTodos.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val activeTodoCount by viewModel.activeTodoCount.collectAsState()
    val completedTodoCount by viewModel.completedTodoCount.collectAsState()
    val highPriorityCount by viewModel.highPriorityCount.collectAsState()
    val overdueCount by viewModel.overdueCount.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(TodoFilter.ALL) }
    var sortOrder by remember { mutableStateOf(TodoSortOrder.PRIORITY_DESC) }
    var showPriorityWarning by remember { mutableStateOf(false) }
    var pendingPomodoroTodo by remember { mutableStateOf<TodoEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showClearCompletedDialog by remember { mutableStateOf(false) }
    var showLogWorkDialog by remember { mutableStateOf(false) }

    val today = java.time.LocalDate.now()
    val allTodos = remember(activeTodos, completedTodos) { activeTodos + completedTodos }
    val dayProgressValue = remember(activeTodos, completedTodos) {
        dayProgressFor(today, allTodos)
    }

    val filteredTodos = remember(activeTodos, activeFilter, sortOrder) {
        val filtered = when (activeFilter) {
            TodoFilter.ALL -> activeTodos
            TodoFilter.HIGH -> activeTodos.filter { it.priority == 3 }
            TodoFilter.MEDIUM -> activeTodos.filter { it.priority == 2 }
            TodoFilter.LOW -> activeTodos.filter { it.priority == 1 }
            TodoFilter.OVERDUE -> activeTodos.filter {
                it.deadline != null && it.deadline < System.currentTimeMillis()
            }
        }
        when (sortOrder) {
            TodoSortOrder.PRIORITY_DESC -> filtered.sortedByDescending { it.priority }
            TodoSortOrder.PRIORITY_ASC -> filtered.sortedBy { it.priority }
            TodoSortOrder.DEADLINE -> filtered.sortedBy { it.deadline ?: Long.MAX_VALUE }
            TodoSortOrder.NEWEST -> filtered.sortedByDescending { it.createdAt }
            TodoSortOrder.OLDEST -> filtered.sortedBy { it.createdAt }
        }
    }

    ScreenList(
        title = "Tasks",
        onBack = { navController.navigateToHome() },
        actions = {
            Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Priority (High first)") },
                                onClick = { sortOrder = TodoSortOrder.PRIORITY_DESC; showSortMenu = false },
                                leadingIcon = if (sortOrder == TodoSortOrder.PRIORITY_DESC) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Priority (Low first)") },
                                onClick = { sortOrder = TodoSortOrder.PRIORITY_ASC; showSortMenu = false },
                                leadingIcon = if (sortOrder == TodoSortOrder.PRIORITY_ASC) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Deadline") },
                                onClick = { sortOrder = TodoSortOrder.DEADLINE; showSortMenu = false },
                                leadingIcon = if (sortOrder == TodoSortOrder.DEADLINE) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Newest first") },
                                onClick = { sortOrder = TodoSortOrder.NEWEST; showSortMenu = false },
                                leadingIcon = if (sortOrder == TodoSortOrder.NEWEST) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest first") },
                                onClick = { sortOrder = TodoSortOrder.OLDEST; showSortMenu = false },
                                leadingIcon = if (sortOrder == TodoSortOrder.OLDEST) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                        }
            }
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Todo")
            }
        }
    ) {
            item {
                TodoProgressOverview(
                    activeCount = activeTodoCount,
                    completedCount = completedTodoCount,
                    highPriorityCount = highPriorityCount,
                    overdueCount = overdueCount,
                    dayProgress = dayProgressValue
                )
            }

            item {
                TodoFilterChips(
                    activeFilter = activeFilter,
                    onFilterChange = { activeFilter = it }
                )
            }

            if (filteredTodos.isNotEmpty()) {
                items(filteredTodos, key = { it.id }) { todo ->
                    SwipeToCompleteRow(onComplete = { viewModel.completeTodo(todo.id) }) {
                    TodoItemCard(
                        todo = todo,
                        categories = categories,
                        onComplete = { viewModel.completeTodo(todo.id) },
                        onDelete = { viewModel.deleteTodo(todo) },
                        onStartPomodoro = {
                            val higherPending = activeTodos.filter {
                                it.priority > todo.priority && it.id != todo.id
                            }
                            if (higherPending.isNotEmpty() && todo.priority < 3) {
                                pendingPomodoroTodo = todo
                                showPriorityWarning = true
                            } else {
                                viewModel.startPomodoro(todo.id, todo.categoryId, 25)
                            }
                        },
                        onEdit = { viewModel.updateTodo(it) },
                        viewModel = viewModel
                    )
                    }
                }
            } else if (activeTodos.isNotEmpty()) {
                item {
                    EmptyState(
                        title = "No tasks match this filter",
                        message = "Clear the filter to see every task again.",
                        icon = Icons.Default.FilterList,
                        actionLabel = "Clear filter",
                        onAction = { activeFilter = TodoFilter.ALL }
                    )
                }
            }

            if (completedTodos.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "Completed (${completedTodos.size})",
                        trailing = {
                            TextButton(
                                onClick = { showClearCompletedDialog = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear All")
                            }
                        }
                    )
                }
                items(completedTodos.take(15), key = { it.id }) { todo ->
                    CompletedTodoItemCard(todo = todo, categories = categories)
                }
            }

            if (activeTodos.isEmpty() && completedTodos.isEmpty()) {
                item {
                    EmptyState(
                        title = "No tasks yet",
                        message = "Every task you finish and every minute you track feeds the day ring. " +
                            "Add one, start a timer, or write down work you already did.",
                        icon = Icons.Default.CheckCircle,
                        actionLabel = "Add task",
                        onAction = { showAddDialog = true }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)
                ) {
                    PrimaryCta(
                        text = "Add task",
                        icon = Icons.Default.Add,
                        onClick = { showAddDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    GhostCta(
                        text = "Log done work",
                        icon = Icons.Default.History,
                        onClick = { showLogWorkDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
    }

    if (showLogWorkDialog) {
        LogCompletedWorkDialog(
            categories = categories,
            onDismiss = { showLogWorkDialog = false },
            onLog = { title, minutes, categoryId ->
                viewModel.logCompletedWork(title, minutes, categoryId)
                showLogWorkDialog = false
            }
        )
    }

    if (showClearCompletedDialog) {
        AlertDialog(
            onDismissRequest = { showClearCompletedDialog = false },
            title = { Text("Clear completed tasks?") },
            text = {
                Text(
                    "This permanently deletes ${completedTodos.size} completed " +
                        if (completedTodos.size == 1) "task." else "tasks. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCompletedDialog = false
                        viewModel.deleteCompletedTodos()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCompletedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPriorityWarning && pendingPomodoroTodo != null) {
        val higherTasks = activeTodos.filter {
            it.priority > pendingPomodoroTodo!!.priority && it.id != pendingPomodoroTodo!!.id
        }
        val priorityLabel = when (pendingPomodoroTodo!!.priority) {
            2 -> "Medium"
            1 -> "Low"
            else -> "None"
        }
        val higherLabel = when (higherTasks.maxOfOrNull { it.priority }) {
            3 -> "High"
            2 -> "Medium"
            else -> "higher"
        }

        AlertDialog(
            onDismissRequest = { showPriorityWarning = false; pendingPomodoroTodo = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = ExerciseColor) },
            title = { Text("Priority Warning") },
            text = {
                Column {
                    Text(
                        "You're starting a ${priorityLabel} priority task, but you have ${higherTasks.size} $higherLabel priority task(s) pending:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    higherTasks.take(3).forEach { task ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = when (task.priority) {
                                    3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    2 -> ExerciseColor.copy(alpha = 0.15f)
                                    else -> ProductiveColor.copy(alpha = 0.15f)
                                },
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = when (task.priority) {
                                        3 -> "HIGH"
                                        2 -> "MED"
                                        else -> "LOW"
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (task.priority) {
                                        3 -> MaterialTheme.colorScheme.primary
                                        2 -> ExerciseColor
                                        else -> ProductiveColor
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(task.title, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (higherTasks.size > 3) {
                        Text(
                            "...and ${higherTasks.size - 3} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Consider completing higher priority tasks first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingPomodoroTodo?.let { todo ->
                        viewModel.startPomodoro(todo.id, todo.categoryId, 25)
                    }
                    showPriorityWarning = false
                    pendingPomodoroTodo = null
                }) {
                    Text("Start Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriorityWarning = false; pendingPomodoroTodo = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddDialog) {
        AddTodoDialog(
            categories = categories,
            onDismiss = { showAddDialog = false },
            onAdd = { title, description, categoryId, deadline, estimatedMinutes, priority, triggerText, triggerHour, obstacleText, outcomeText ->
                viewModel.addTodo(title, description, categoryId, deadline, estimatedMinutes, priority, triggerText, triggerHour, obstacleText, outcomeText)
                showAddDialog = false
            }
        )
    }
}

/**
 * Swipe left to finish a task. The row springs back - the task is completed,
 * not removed, so the ring jumps and the card lands in the completed section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToCompleteRow(
    onComplete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onComplete()
            false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        MaterialTheme.shapes.large
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) {
        content()
    }
}

@Composable
fun TodoProgressOverview(
    activeCount: Int,
    completedCount: Int,
    highPriorityCount: Int,
    overdueCount: Int,
    dayProgress: Float = 0f
) {
    val total = activeCount + completedCount
    val status = when {
        total == 0 -> DayStatus.NO_TASKS
        dayProgress >= 1f -> DayStatus.DONE
        else -> DayStatus.IN_PROGRESS
    }
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniSoulRing(
                progress = dayProgress,
                status = status,
                isToday = true,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Today's ring",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (total == 0) {
                        "Assign tasks to start the day ring"
                    } else {
                        "$completedCount done / $activeCount pending · ${status.label}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (highPriorityCount > 0) {
                    Text(
                        "$highPriorityCount high priority",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (overdueCount > 0) {
                    Text(
                        "$overdueCount overdue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun TodoFilterChips(
    activeFilter: TodoFilter,
    onFilterChange: (TodoFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TodoFilter.entries.forEach { filter ->
            FilterChip(
                selected = activeFilter == filter,
                onClick = { onFilterChange(filter) },
                modifier = Modifier.height(48.dp),
                label = {
                    Text(
                        when (filter) {
                            TodoFilter.ALL -> "All"
                            TodoFilter.HIGH -> "High"
                            TodoFilter.MEDIUM -> "Med"
                            TodoFilter.LOW -> "Low"
                            TodoFilter.OVERDUE -> "Overdue"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (activeFilter == filter) FontWeight.SemiBold else FontWeight.Medium
                    )
                },
                leadingIcon = if (activeFilter == filter) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    enabled = true,
                    selected = activeFilter == filter
                ),
                shape = RoundedCornerShape(50)
            )
        }
    }
}

@Composable
fun TodoItemCard(
    todo: TodoEntity,
    categories: Map<Long, io.github.dailytrack.data.db.entity.CategoryEntity>,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onStartPomodoro: () -> Unit,
    onEdit: (TodoEntity) -> Unit,
    viewModel: MainViewModel
) {
    val categoryName = todo.categoryId?.let { categories[it]?.name } ?: ""
    val deadlineStr = todo.deadline?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
    }
    val isOverdue = todo.deadline != null && todo.deadline < System.currentTimeMillis()
    val subtasks by viewModel.getSubtasksForTodo(todo.id).collectAsState(initial = emptyList())
    val completedSubtasks = subtasks.count { it.isCompleted }
    var showSubtasks by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val priorityColor = when (todo.priority) {
        3 -> MaterialTheme.colorScheme.primary
        2 -> ExerciseColor
        1 -> ProductiveColor
        else -> Color.Transparent
    }
    val priorityLabel = when (todo.priority) {
        3 -> "HIGH"
        2 -> "MED"
        1 -> "LOW"
        else -> ""
    }

    SurfaceCard(contentPadding = PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = false,
                onCheckedChange = { onComplete() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (priorityLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = priorityColor.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (todo.priority) {
                                        3 -> Icons.Default.PriorityHigh
                                        2 -> Icons.Default.Remove
                                        else -> Icons.Default.LowPriority
                                    },
                                    contentDescription = null,
                                    tint = priorityColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = priorityLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = priorityColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                if (categoryName.isNotBlank() || deadlineStr != null) {
                    Row {
                        if (categoryName.isNotBlank()) {
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (deadlineStr != null) {
                            if (categoryName.isNotBlank()) Text(" · ", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "Due: $deadlineStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverdue) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (subtasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { completedSubtasks.toFloat() / subtasks.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = ProductiveColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    Text(
                        "$completedSubtasks/${subtasks.size} subtasks",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (todo.estimatedMinutes > 0) {
                    Text(
                        text = "Est: ${todo.estimatedMinutes}min | Done: ${todo.actualMinutes}min | Pomodoros: ${todo.pomodoroCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TaskQuality.calibrationNote(todo.estimatedMinutes, todo.actualMinutes)?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (todo.triggerText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = ProductiveColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = buildString {
                                append("If ")
                                append(todo.triggerText)
                                if (todo.triggerHour in 0..23) append(" · ${todo.triggerHour}:00 cue")
                                append(", then this.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ProductiveColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (todo.obstacleText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "If I can't start, I'll ${todo.obstacleText}.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (todo.outcomeText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Best case: ${todo.outcomeText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Hairline()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (subtasks.isNotEmpty()) {
                IconButton(onClick = { showSubtasks = !showSubtasks }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        if (showSubtasks) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle subtasks",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onStartPomodoro, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Timer, contentDescription = "Start Pomodoro", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
        }

            AnimatedVisibility(visible = showSubtasks && subtasks.isNotEmpty()) {
                Column {
                    subtasks.forEach { subtask ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = subtask.isCompleted,
                                onCheckedChange = { viewModel.toggleSubtask(subtask) },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = subtask.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var newSubtask by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = newSubtask,
                            onValueChange = { newSubtask = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Add subtask...", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                if (newSubtask.isNotBlank()) {
                                    viewModel.addSubtask(todo.id, newSubtask)
                                    newSubtask = ""
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add subtask", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

    if (showEditDialog) {
        EditTodoDialog(
            todo = todo,
            categories = categories,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                viewModel.updateTodo(updated)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun CompletedTodoItemCard(
    todo: TodoEntity,
    categories: Map<Long, io.github.dailytrack.data.db.entity.CategoryEntity>
) {
    val categoryName = todo.categoryId?.let { categories[it]?.name } ?: ""

    SurfaceCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProductiveColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (categoryName.isNotBlank()) {
                    Text(
                        text = "$categoryName · ${todo.pomodoroCount} pomodoros · ${todo.actualMinutes}min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTodoDialog(
    todo: TodoEntity,
    categories: Map<Long, io.github.dailytrack.data.db.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (TodoEntity) -> Unit
) {
    var title by remember { mutableStateOf(todo.title) }
    var description by remember { mutableStateOf(todo.description) }
    var selectedCategoryId by remember { mutableLongStateOf(todo.categoryId ?: 0L) }
    var estimatedMinutes by remember { mutableStateOf(todo.estimatedMinutes.toString()) }
    var priority by remember { mutableIntStateOf(todo.priority) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Todo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = estimatedMinutes, onValueChange = { estimatedMinutes = it }, label = { Text("Estimated minutes") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                Text("Priority *", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "None", 1 to "Low", 2 to "Medium", 3 to "High").forEach { (p, label) ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it }
                ) {
                    OutlinedTextField(
                        value = categories[selectedCategoryId]?.name ?: "No category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No category") },
                            onClick = { selectedCategoryId = 0L; showCategoryDropdown = false }
                        )
                        categories.values.filter { !it.archived }.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = { selectedCategoryId = category.id; showCategoryDropdown = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            todo.copy(
                                title = title,
                                description = description,
                                categoryId = if (selectedCategoryId == 0L) null else selectedCategoryId,
                                estimatedMinutes = estimatedMinutes.toIntOrNull() ?: todo.estimatedMinutes,
                                priority = priority,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/**
 * Backfill: work the user already finished and only now wrote down.
 * Writes the task *and* the recorded minutes, so the ring, growth, loops and
 * insights treat it exactly like a tracked session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogCompletedWorkDialog(
    categories: Map<Long, io.github.dailytrack.data.db.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onLog: (String, Int, Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("25") }
    var selectedCategoryId by remember { mutableLongStateOf(0L) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log completed work") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Write down a block you already finished. It counts as tracked " +
                        "time, not as untracked waste.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What did you finish? *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter { c -> c.isDigit() } },
                    label = { Text("Approximate minutes *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it }
                ) {
                    OutlinedTextField(
                        value = categories[selectedCategoryId]?.name ?: "No category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type of work") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No category") },
                            onClick = { selectedCategoryId = 0L; showCategoryDropdown = false }
                        )
                        categories.values.filter { !it.archived }.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = { selectedCategoryId = category.id; showCategoryDropdown = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val mins = minutes.toIntOrNull() ?: 0
                    if (title.isNotBlank() && mins > 0) {
                        onLog(
                            title.trim(),
                            mins,
                            selectedCategoryId.takeIf { it > 0L }
                        )
                    }
                },
                enabled = title.isNotBlank() && (minutes.toIntOrNull() ?: 0) > 0
            ) { Text("Log it") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    categories: Map<Long, io.github.dailytrack.data.db.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onAdd: (String, String, Long?, Long?, Int, Int, String, Int, String, String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableLongStateOf(0L) }
    var deadline by remember { mutableLongStateOf(0L) }
    var estimatedMinutes by remember { mutableStateOf("25") }
    var priority by remember { mutableIntStateOf(0) }
    var triggerCue by remember { mutableStateOf("") }
    var triggerHour by remember { mutableIntStateOf(-1) }
    var obstacleText by remember { mutableStateOf("") }
    var outcomeText by remember { mutableStateOf("") }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Todo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (TaskQuality.isVague(title)) {
                    Text(
                        text = "Name the action and the thing: specific tasks get done far more often than vague ones.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

                Text("Priority *", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "None", 1 to "Low", 2 to "Medium", 3 to "High").forEach { (p, label) ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it }
                ) {
                    OutlinedTextField(
                        value = categories[selectedCategoryId]?.name ?: "No category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No category") },
                            onClick = { selectedCategoryId = 0L; showCategoryDropdown = false }
                        )
                        categories.values.filter { !it.archived }.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = { selectedCategoryId = category.id; showCategoryDropdown = false }
                            )
                        }
                    }
                }
                OutlinedTextField(value = estimatedMinutes, onValueChange = { estimatedMinutes = it }, label = { Text("Estimated minutes") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (TaskQuality.needsBreakdown(estimatedMinutes.toIntOrNull() ?: 0)) {
                    Text(
                        text = "Long task - break it into smaller steps when you get to it. Graded steps are easier to start and finish.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = {
                        val cal = java.util.Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        cal.set(year, month, day, hour, minute)
                                        deadline = cal.timeInMillis
                                    },
                                    cal.get(java.util.Calendar.HOUR_OF_DAY),
                                    cal.get(java.util.Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH),
                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (deadline > 0) {
                        "Deadline: ${Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))}"
                    } else "Set Deadline")
                }

                Text("If-then plan (optional)", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = triggerCue,
                    onValueChange = { triggerCue = it },
                    label = { Text("If/when this happens…") },
                    placeholder = { Text("e.g. after I sit at my desk") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = if (triggerCue.isBlank()) {
                        "Naming the moment you'll start makes follow-through much more likely."
                    } else {
                        "If ${triggerCue.trim()}, then I will \"${title.ifBlank { "…" }}\"."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-1 to "No cue time", 7 to "7am", 9 to "9am", 12 to "12pm", 15 to "3pm", 18 to "6pm", 20 to "8pm").forEach { (h, label) ->
                        FilterChip(
                            selected = triggerHour == h,
                            onClick = { triggerHour = if (triggerHour == h) -1 else h },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = obstacleText,
                    onValueChange = { obstacleText = it },
                    label = { Text("If I can't start, then I'll… (optional)") },
                    placeholder = { Text("e.g. just do 2 minutes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = outcomeText,
                    onValueChange = { outcomeText = it },
                    label = { Text("Best outcome if it gets done (optional)") },
                    placeholder = { Text("e.g. I can relax this weekend") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(
                            title,
                            description,
                            if (selectedCategoryId == 0L) null else selectedCategoryId,
                            if (deadline > 0) deadline else null,
                            estimatedMinutes.toIntOrNull() ?: 25,
                            priority,
                            triggerCue,
                            triggerHour,
                            obstacleText,
                            outcomeText
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
