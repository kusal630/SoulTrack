package io.github.dailytrack.ui.planner

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.db.entity.TodoEntity
import io.github.dailytrack.engine.DailyPlan
import io.github.dailytrack.engine.EisenhowerQuadrant
import io.github.dailytrack.engine.PlannedBlock
import io.github.dailytrack.engine.SmartPlanner
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.GhostCta
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.StartRitualDialog
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.navigateToHome
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.viewmodel.MainViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PlannerScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val plan by viewModel.dailyPlan.collectAsState()
    val inbox by viewModel.inboxTodos.collectAsState()
    val todos by viewModel.activeTodos.collectAsState()
    val clutter by viewModel.clutterCleared.collectAsState()
    val focusReport by viewModel.focusInsightReport.collectAsState()

    val currentPlan = plan
    var capture by remember { mutableStateOf("") }
    var pendingBlock by remember { mutableStateOf<PlannedBlock?>(null) }
    var triageTodo by remember { mutableStateOf<TodoEntity?>(null) }

    ScreenList(
        title = "Daily plan",
        subtitle = "Eisenhower first, then deep work in your best hours",
        onBack = { navController.navigateToHome() },
        compact = true
    ) {
        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Your best focus hours",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (focusReport.bestHours.isEmpty()) {
                                "Not enough history yet - the plan starts with sensible defaults."
                            } else {
                                focusReport.bestHours.joinToString(" · ") { "%02d:00".format(it.hour) }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            SectionTitle(
                title = "Smart plan",
                subtitle = "Quadrant 1 first · quadrant 2 in deep-work blocks · pre-split into pomodoros"
            )
        }

        if (currentPlan == null || currentPlan.blocks.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing to plan yet",
                    message = "Add tasks or capture a thought in the inbox below. The plan sorts them and reserves deep work for your best hours.",
                    icon = Icons.Default.TaskAlt
                )
            }
        } else {
            item {
                SmartPlanCard(
                    plan = currentPlan,
                    onApply = { viewModel.applyDailyPlan(it) },
                    onClear = viewModel::clearDailyPlan,
                    onStart = { pendingBlock = it }
                )
            }
        }

        currentPlan?.deleteCandidates.orEmpty().let { candidates ->
            if (candidates.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "Delete candidates",
                        subtitle = "Quadrant 4 - neither urgent nor important"
                    )
                }
                items(candidates, key = { "delete-${it.id}" }) { candidate ->
                    SurfaceCard(contentPadding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    candidate.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "One tap removes it from every list",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = {
                                todos.firstOrNull { it.id == candidate.id }?.let(viewModel::deleteTodo)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionTitle(
                title = "Inbox",
                subtitle = "Capture first, decide once · $clutter clutter item(s) cleared"
            )
        }
        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = capture,
                        onValueChange = { capture = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Get it out of your head") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (capture.isNotBlank()) {
                                viewModel.addInboxItem(capture)
                                capture = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add to inbox")
                    }
                }
                if (inbox.isEmpty()) {
                    Text(
                        "Inbox zero. New captures land here for a two-minute check.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(inbox, key = { "inbox-${it.id}" }) { todo ->
            InboxRow(
                todo = todo,
                onTwoMinutes = { viewModel.triageInbox(todo, underTwoMinutes = true) },
                onDecide = { triageTodo = todo },
                onDismiss = { viewModel.dismissInboxItem(todo) }
            )
        }

        item {
            SectionTitle(
                title = "Eisenhower board",
                subtitle = "Tap the menu on a task to move it between quadrants"
            )
        }
        EisenhowerQuadrant.entries.forEach { quadrant ->
            val quadrantTasks = todos.filter {
                !it.isInbox && SmartPlanner.classify(
                    io.github.dailytrack.engine.PlannerTask(
                        id = it.id,
                        title = it.title,
                        priority = it.priority,
                        deadline = it.deadline,
                        estimatedMinutes = it.estimatedMinutes,
                        savedQuadrant = it.quadrant,
                        isCompleted = it.isCompleted
                    ),
                    System.currentTimeMillis()
                ) == quadrant
            }
            item {
                QuadrantCard(
                    quadrant = quadrant,
                    tasks = quadrantTasks,
                    onComplete = viewModel::completeTodo,
                    onMove = { todo, target -> viewModel.setTodoQuadrant(todo, target.number) },
                    onDelete = viewModel::deleteTodo
                )
            }
        }

        item {
            PrimaryCta(
                text = "Back to tasks",
                onClick = { navController.navigateToHome() }
            )
        }
    }

    triageTodo?.let { todo ->
        QuadrantPickerDialog(
            todo = todo,
            onDismiss = { triageTodo = null },
            onPick = { quadrant ->
                viewModel.triageInbox(todo, underTwoMinutes = false, quadrant = quadrant.number)
                triageTodo = null
            }
        )
    }

    pendingBlock?.let { block ->
        StartRitualDialog(
            taskTitle = block.title,
            durationMinutes = block.pomodoroMinutes,
            requirePhoneAway = block.quadrant == EisenhowerQuadrant.SCHEDULE,
            onDismiss = { pendingBlock = null },
            onBegin = {
                viewModel.startPlannedBlock(block)
                pendingBlock = null
            }
        )
    }
}

@Composable
private fun SmartPlanCard(
    plan: DailyPlan,
    onApply: (DailyPlan) -> Unit,
    onClear: () -> Unit,
    onStart: (PlannedBlock) -> Unit
) {
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${plan.blocks.size} blocks · ${plan.totalCycles} pomodoros · ${plan.totalMinutes} min",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Q1 is front-loaded. Q2 is protected at your best hours. Q4 is queued for deletion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        plan.blocks.forEach { block ->
            BlockRow(block = block, onStart = { onStart(block) })
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryCta(
                text = "Lock this plan",
                onClick = { onApply(plan) },
                modifier = Modifier.weight(1f)
            )
            GhostCta(
                text = "Clear",
                onClick = onClear,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BlockRow(block: PlannedBlock, onStart: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val start = Instant.ofEpochMilli(block.startMillis).atZone(zone).format(formatter)
    val end = Instant.ofEpochMilli(block.endMillis).atZone(zone).format(formatter)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(quadrantColor(block.quadrant).copy(alpha = 0.08f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$start-$end",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = quadrantColor(block.quadrant)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    block.quadrant.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                block.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                SmartPlanner.cycleLabel(block.cycles, block.pomodoroMinutes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onStart) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Start")
        }
    }
}

@Composable
private fun InboxRow(
    todo: TodoEntity,
    onTwoMinutes: () -> Unit,
    onDecide: () -> Unit,
    onDismiss: () -> Unit
) {
    SurfaceCard(contentPadding = PaddingValues(12.dp)) {
        Text(
            todo.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onTwoMinutes) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Under 2 min")
            }
            TextButton(onClick = onDecide) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Send to matrix")
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun QuadrantCard(
    quadrant: EisenhowerQuadrant,
    tasks: List<TodoEntity>,
    onComplete: (Long) -> Unit,
    onMove: (TodoEntity, EisenhowerQuadrant) -> Unit,
    onDelete: (TodoEntity) -> Unit
) {
    SurfaceCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(quadrantColor(quadrant))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    quadrant.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${quadrant.action} · ${tasks.size} task(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (tasks.isEmpty()) {
            Text(
                "Nothing here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        tasks.forEach { todo ->
            var menu by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = false, onCheckedChange = { onComplete(todo.id) })
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        todo.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (todo.deadline != null) {
                        Text(
                            "Due " + Instant.ofEpochMilli(todo.deadline!!)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("MMM d, HH:mm")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Move task")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        EisenhowerQuadrant.entries.forEach { target ->
                            DropdownMenuItem(
                                text = { Text("Move to ${target.label}") },
                                leadingIcon = {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = quadrantColor(target))
                                },
                                onClick = {
                                    onMove(todo, target)
                                    menu = false
                                }
                            )
                        }
                        if (quadrant == EisenhowerQuadrant.DELETE) {
                            DropdownMenuItem(
                                text = { Text("Delete task") },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    onDelete(todo)
                                    menu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuadrantPickerDialog(
    todo: TodoEntity,
    onDismiss: () -> Unit,
    onPick: (EisenhowerQuadrant) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Where does this belong?") },
        text = {
            Column {
                Text(
                    todo.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                EisenhowerQuadrant.entries.forEach { quadrant ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(quadrant.label, fontWeight = FontWeight.Medium)
                                Text(
                                    quadrant.action,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(quadrantColor(quadrant))
                            )
                        },
                        onClick = { onPick(quadrant) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun quadrantColor(quadrant: EisenhowerQuadrant): Color = when (quadrant) {
    EisenhowerQuadrant.DO_FIRST -> MaterialTheme.colorScheme.error
    EisenhowerQuadrant.SCHEDULE -> MaterialTheme.colorScheme.primary
    EisenhowerQuadrant.DELEGATE -> ExerciseColor
    EisenhowerQuadrant.DELETE -> ProductiveColor
}
