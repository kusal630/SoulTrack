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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.db.entity.SubtaskEntity
import io.github.dailytrack.data.db.entity.TodoEntity
import io.github.dailytrack.ui.components.*
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todo List") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateToHome() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                TodoProgressOverview(
                    activeCount = activeTodoCount,
                    completedCount = completedTodoCount,
                    highPriorityCount = highPriorityCount,
                    overdueCount = overdueCount
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
            } else if (activeTodos.isEmpty()) {
                item { /* empty state below */ }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            "No tasks match this filter",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (completedTodos.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Completed (${completedTodos.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
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
                }
                items(completedTodos.take(15), key = { it.id }) { todo ->
                    CompletedTodoItemCard(todo = todo, categories = categories)
                }
            }

            if (activeTodos.isEmpty() && completedTodos.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No tasks yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Add your first todo to get started",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
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
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF57C00)) },
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
                                    3 -> Color(0xFFE94560).copy(alpha = 0.15f)
                                    2 -> Color(0xFFF57C00).copy(alpha = 0.15f)
                                    else -> Color(0xFF4CAF50).copy(alpha = 0.15f)
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
                                        3 -> Color(0xFFE94560)
                                        2 -> Color(0xFFF57C00)
                                        else -> Color(0xFF4CAF50)
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
            onAdd = { title, description, categoryId, deadline, estimatedMinutes, priority ->
                viewModel.addTodo(title, description, categoryId, deadline, estimatedMinutes, priority)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TodoProgressOverview(
    activeCount: Int,
    completedCount: Int,
    highPriorityCount: Int,
    overdueCount: Int
) {
    val total = activeCount + completedCount
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PieChart(
                slices = listOf(
                    PieChartSlice(activeCount.toFloat(), Color(0xFFF57C00), "Pending"),
                    PieChartSlice(completedCount.toFloat(), Color(0xFF2E7D32), "Done"),
                ),
                size = 80.dp,
                strokeWidth = 12.dp,
                centerContent = {
                    Text(
                        text = if (total > 0) "${(completedCount * 100 / total)}%" else "0%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Task Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "$completedCount done / $activeCount pending",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (highPriorityCount > 0) {
                    Text(
                        "$highPriorityCount high priority",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE94560)
                    )
                }
                if (overdueCount > 0) {
                    Text(
                        "$overdueCount overdue",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828)
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
                label = {
                    Text(
                        when (filter) {
                            TodoFilter.ALL -> "All"
                            TodoFilter.HIGH -> "High"
                            TodoFilter.MEDIUM -> "Med"
                            TodoFilter.LOW -> "Low"
                            TodoFilter.OVERDUE -> "Overdue"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                leadingIcon = if (activeFilter == filter) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
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
        3 -> Color(0xFFE94560)
        2 -> Color(0xFFF57C00)
        1 -> Color(0xFF4CAF50)
        else -> Color.Transparent
    }
    val priorityLabel = when (todo.priority) {
        3 -> "HIGH"
        2 -> "MED"
        1 -> "LOW"
        else -> ""
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                            fontWeight = FontWeight.Medium
                        )
                        if (priorityLabel.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = priorityColor.copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = priorityLabel,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = priorityColor,
                                    fontWeight = FontWeight.Bold
                                )
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
                                    color = if (isOverdue) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (subtasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { completedSubtasks.toFloat() / subtasks.size },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = Color(0xFF2E7D32),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
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
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (subtasks.isNotEmpty()) {
                    IconButton(onClick = { showSubtasks = !showSubtasks }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            if (showSubtasks) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle subtasks",
                            modifier = Modifier.size(18.dp)
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
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add subtask", modifier = Modifier.size(18.dp))
                        }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    categories: Map<Long, io.github.dailytrack.data.db.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onAdd: (String, String, Long?, Long?, Int, Int) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableLongStateOf(0L) }
    var deadline by remember { mutableLongStateOf(0L) }
    var estimatedMinutes by remember { mutableStateOf("25") }
    var priority by remember { mutableIntStateOf(0) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Todo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
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
                            priority
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
