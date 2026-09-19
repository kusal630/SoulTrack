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


package io.github.dailytrack.ui.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.api.QuotesApi
import io.github.dailytrack.data.db.entity.TodoEntity
import io.github.dailytrack.service.PomodoroForegroundService
import io.github.dailytrack.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

private val WorkColor = Color(0xFFE94560)
private val BreakColor = Color(0xFFFFAB40)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val activePomodoro by viewModel.activePomodoro.collectAsState()
    val todayPomodoros by viewModel.todayPomodoros.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val activeTodos by viewModel.activeTodos.collectAsState()
    val savedQuotes by viewModel.savedQuotes.collectAsState()
    
    var isBreak by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var selectedWorkDuration by remember { mutableIntStateOf(25) }
    var selectedBreakDuration by remember { mutableIntStateOf(5) }
    var selectedTodoId by remember { mutableLongStateOf(0L) }
    var selectedCategoryId by remember { mutableLongStateOf(0L) }
    
    var currentQuote by remember { mutableStateOf("Loading inspiring quote...") }
    var quoteAuthor by remember { mutableStateOf("") }

    var showPriorityWarning by remember { mutableStateOf(false) }
    var pendingPomodoroTodo by remember { mutableStateOf<TodoEntity?>(null) }
    var pendingWorkDuration by remember { mutableIntStateOf(25) }

    val isRunning = activePomodoro != null
    val coroutineScope = rememberCoroutineScope()

    val activeColor = if (isBreak) BreakColor else WorkColor

    val animatedActiveColor by animateColorAsState(
        targetValue = activeColor,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "active_color"
    )

    LaunchedEffect(Unit) {
        val quote = QuotesApi.getTimedQuote()
        currentQuote = quote.text
        quoteAuthor = quote.author
    }

    LaunchedEffect(activePomodoro) {
        if (activePomodoro != null) {
            isBreak = activePomodoro?.type == "BREAK"
            elapsedSeconds = (System.currentTimeMillis() - (activePomodoro?.startTime ?: 0L)) / 1000
            while (true) {
                delay(100)
                elapsedSeconds = (System.currentTimeMillis() - (activePomodoro?.startTime ?: 0L)) / 1000

                val totalSeconds = (activePomodoro?.durationMinutes ?: 25) * 60L
                if (elapsedSeconds >= totalSeconds) {
                    try {
                        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                            manager.defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        }
                        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                    } catch (_: Exception) {}

                    val quote = QuotesApi.getTimedQuote()
                    currentQuote = quote.text
                    quoteAuthor = quote.author
                    elapsedSeconds = 0
                }
            }
        } else {
            elapsedSeconds = 0
        }
    }

    DisposableEffect(Unit) {
        val workReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                viewModel.completePomodoro()
            }
        }
        val breakReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                viewModel.completePomodoro()
            }
        }
        val filter = IntentFilter().apply {
            addAction(PomodoroForegroundService.ACTION_COMPLETE_WORK)
            addAction(PomodoroForegroundService.ACTION_COMPLETE_BREAK)
        }
        ContextCompat.registerReceiver(context, workReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(context, breakReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            try {
                context.unregisterReceiver(workReceiver)
                context.unregisterReceiver(breakReceiver)
            } catch (_: Exception) {}
        }
    }

    val remainingSeconds = if (isRunning) {
        val totalSeconds = (activePomodoro?.durationMinutes ?: selectedWorkDuration) * 60L
        (totalSeconds - elapsedSeconds).coerceAtLeast(0)
    } else {
        selectedWorkDuration * 60L
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val progress = if (isRunning) {
        elapsedSeconds.toFloat() / ((activePomodoro?.durationMinutes ?: selectedWorkDuration) * 60)
    } else 0f

    val completedPomodorosToday = todayPomodoros.count { it.type == "WORK" && it.isCompleted }
    val completedBreaksToday = todayPomodoros.count { it.type == "BREAK" && it.isCompleted }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pomodoro Timer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("saved_quotes") }) {
                        Icon(Icons.Default.Favorite, contentDescription = "Saved Quotes")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isRunning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    animatedActiveColor.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.03f),
                                    MaterialTheme.colorScheme.surface
                                ),
                                radius = 900f
                            )
                        )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(if (isRunning) 48.dp else 0.dp))
                }

                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(260.dp)
                    ) {
                        if (isRunning) {
                            Canvas(
                                modifier = Modifier
                                    .size(260.dp)
                                    .blur(20.dp)
                            ) {
                                drawCircle(
                                    color = animatedActiveColor.copy(alpha = 0.3f),
                                    radius = size.minDimension / 2
                                )
                            }
                        }

                        Canvas(modifier = Modifier.size(240.dp)) {
                            val strokeWidth = 10.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                            val arcSize = Size(diameter, diameter)

                            drawArc(
                                color = Color.Gray.copy(alpha = 0.12f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            if (isRunning || progress > 0f) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            animatedActiveColor,
                                            animatedActiveColor.copy(alpha = 0.5f),
                                            animatedActiveColor
                                        )
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedProgress,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isRunning) {
                                    if (isBreak) "Break Time" else "Focus Time"
                                } else "Ready to Focus",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds),
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Thin,
                                fontFamily = FontFamily.Monospace,
                                color = if (isRunning) animatedActiveColor
                                else MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$completedPomodorosToday focus | $completedBreaksToday breaks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = animatedActiveColor.copy(alpha = 0.06f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val isSaved by remember(currentQuote, savedQuotes) {
                                derivedStateOf { savedQuotes.any { it.text == currentQuote } }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isSaved) {
                                            val savedQuote = savedQuotes.find { it.text == currentQuote }
                                            savedQuote?.let { viewModel.deleteSavedQuote(it) }
                                        } else {
                                            viewModel.saveQuote(currentQuote, quoteAuthor)
                                        }
                                    }
                                ) {
                                    Icon(
                                        if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = if (isSaved) "Unsave" else "Save",
                                        tint = if (isSaved) Color(0xFFE94560) else animatedActiveColor
                                    )
                                }
                            }
                            Text(
                                text = "\u201C",
                                style = MaterialTheme.typography.headlineLarge,
                                color = animatedActiveColor.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentQuote,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            if (quoteAuthor.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "\u2014 $quoteAuthor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (!isRunning) {
                    item {
                        Text(
                            "Focus Duration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 25, 30, 45, 60).forEach { duration ->
                                FilterChip(
                                    selected = selectedWorkDuration == duration,
                                    onClick = { selectedWorkDuration = duration },
                                    label = { Text("${duration}m") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            "Break Duration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(3, 5, 10, 15, 20).forEach { duration ->
                                FilterChip(
                                    selected = selectedBreakDuration == duration,
                                    onClick = { selectedBreakDuration = duration },
                                    label = { Text("${duration}m") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            "Link to Todo (optional)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        var showTodoDropdown by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = showTodoDropdown,
                            onExpandedChange = { showTodoDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = activeTodos.find { it.id == selectedTodoId }?.title ?: "No todo",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Todo") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTodoDropdown) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = showTodoDropdown,
                                onDismissRequest = { showTodoDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("No todo") },
                                    onClick = {
                                        selectedTodoId = 0L
                                        showTodoDropdown = false
                                    }
                                )
                                activeTodos.forEach { todo ->
                                    DropdownMenuItem(
                                        text = { Text(todo.title) },
                                        onClick = {
                                            selectedTodoId = todo.id
                                            selectedCategoryId = todo.categoryId ?: 0L
                                            showTodoDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                val selectedTodo = activeTodos.find { it.id == selectedTodoId }
                                if (selectedTodo != null && selectedTodo.priority < 3) {
                                    val higherPending = activeTodos.filter {
                                        it.priority > selectedTodo.priority && it.id != selectedTodo.id
                                    }
                                    if (higherPending.isNotEmpty()) {
                                        pendingPomodoroTodo = selectedTodo
                                        pendingWorkDuration = selectedWorkDuration
                                        showPriorityWarning = true
                                        return@Button
                                    }
                                }

                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                val todoTitle = activeTodos.find { it.id == selectedTodoId }?.title ?: ""
                                val colorInt = android.graphics.Color.rgb(
                                    (WorkColor.red * 255).toInt(),
                                    (WorkColor.green * 255).toInt(),
                                    (WorkColor.blue * 255).toInt()
                                )
                                val intent = Intent(context, PomodoroForegroundService::class.java).apply {
                                    putExtra(PomodoroForegroundService.EXTRA_START_TIME, System.currentTimeMillis())
                                    putExtra(PomodoroForegroundService.EXTRA_WORK_DURATION, selectedWorkDuration)
                                    putExtra(PomodoroForegroundService.EXTRA_BREAK_DURATION, selectedBreakDuration)
                                    putExtra(PomodoroForegroundService.EXTRA_LONG_BREAK_DURATION, 15)
                                    putExtra(PomodoroForegroundService.EXTRA_TODO_TITLE, todoTitle)
                                    putExtra(PomodoroForegroundService.EXTRA_IS_BREAK, false)
                                    putExtra(PomodoroForegroundService.EXTRA_WORK_COLOR, colorInt)
                                    putExtra(PomodoroForegroundService.EXTRA_SESSION_NUMBER, 0)
                                }
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                                
                                viewModel.startPomodoro(
                                    if (selectedTodoId > 0) selectedTodoId else null,
                                    if (selectedCategoryId > 0) selectedCategoryId else null,
                                    selectedWorkDuration
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WorkColor
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Focus Session", fontSize = 16.sp)
                        }
                    }
                } else {
                    item {
                        Button(
                            onClick = {
                                val intent = Intent(context, PomodoroForegroundService::class.java).apply {
                                    action = PomodoroForegroundService.ACTION_STOP
                                }
                                context.startService(intent)
                                viewModel.completePomodoro()
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isBreak) "Skip Break" else "End Session",
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                val workSessions = todayPomodoros.filter { it.type == "WORK" }
                if (workSessions.isNotEmpty()) {
                    item {
                        Text(
                            "Today's Focus Sessions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(workSessions.reversed(), key = { it.id }) { pomodoro ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Work,
                                    contentDescription = null,
                                    tint = if (pomodoro.isCompleted) Color(0xFFE94560) else Color(0xFF757575)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Focus Session",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val todoName = pomodoro.todoId?.let { todoId ->
                                        activeTodos.find { it.id == todoId }?.title
                                    }
                                    if (todoName != null) {
                                        Text(
                                            text = todoName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = "${pomodoro.durationMinutes}min",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pomodoro.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
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
            onDismissRequest = {
                showPriorityWarning = false
                pendingPomodoroTodo = null
            },
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
                    val todo = pendingPomodoroTodo ?: return@TextButton
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    val todoTitle = todo.title
                    val colorInt = android.graphics.Color.rgb(
                        (WorkColor.red * 255).toInt(),
                        (WorkColor.green * 255).toInt(),
                        (WorkColor.blue * 255).toInt()
                    )
                    val intent = Intent(context, PomodoroForegroundService::class.java).apply {
                        putExtra(PomodoroForegroundService.EXTRA_START_TIME, System.currentTimeMillis())
                        putExtra(PomodoroForegroundService.EXTRA_WORK_DURATION, pendingWorkDuration)
                        putExtra(PomodoroForegroundService.EXTRA_BREAK_DURATION, selectedBreakDuration)
                        putExtra(PomodoroForegroundService.EXTRA_LONG_BREAK_DURATION, 15)
                        putExtra(PomodoroForegroundService.EXTRA_TODO_TITLE, todoTitle)
                        putExtra(PomodoroForegroundService.EXTRA_IS_BREAK, false)
                        putExtra(PomodoroForegroundService.EXTRA_WORK_COLOR, colorInt)
                        putExtra(PomodoroForegroundService.EXTRA_SESSION_NUMBER, 0)
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }

                    viewModel.startPomodoro(
                        todo.id,
                        todo.categoryId,
                        pendingWorkDuration
                    )

                    showPriorityWarning = false
                    pendingPomodoroTodo = null
                }) {
                    Text("Start Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPriorityWarning = false
                    pendingPomodoroTodo = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
