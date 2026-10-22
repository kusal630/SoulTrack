package io.github.dailytrack.ui.compounding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.db.entity.CompoundHabitEntity
import io.github.dailytrack.data.db.entity.CompoundLogEntity
import io.github.dailytrack.engine.Compounding
import io.github.dailytrack.engine.CompoundProgress
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.StatTile
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.navigateToHome
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.viewmodel.MainViewModel
import java.time.LocalDate

@Composable
fun CompoundingScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val habits by viewModel.compoundHabits.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    ScreenList(
        title = "Compounding",
        subtitle = "One tiny increment a day, protected by a forgiving streak",
        onBack = { navController.navigateToHome() },
        compact = true
    ) {
        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = ProductiveColor)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "The 1% rule",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Small daily gains compound. A missed day never zeroes the value or your best streak - the app simply says: back on track tomorrow.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                PrimaryCta(
                    text = "Track a skill or habit",
                    icon = Icons.Default.Add,
                    onClick = { showAdd = true }
                )
            }
        }

        if (habits.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing compounding yet",
                    message = "Pick one skill and a tiny daily increment. Ten minutes of practice, one page, one rep - the curve does the rest.",
                    icon = Icons.Default.ShowChart
                )
            }
        }

        item {
            SectionTitle(
                title = "Your curves",
                subtitle = "${habits.size} tracked skill(s)"
            )
        }

        items(habits, key = { it.id }) { habit ->
            CompoundCard(
                habit = habit,
                logs = viewModel.getCompoundLogs(habit.id).collectAsState(initial = emptyList()).value,
                onToggleToday = { viewModel.toggleCompoundToday(habit.id) },
                onArchive = { viewModel.archiveCompoundHabit(habit) }
            )
        }
    }

    if (showAdd) {
        AddCompoundDialog(
            onDismiss = { showAdd = false },
            onAdd = { name, unit, increment, percent, start ->
                viewModel.addCompoundHabit(name, unit, increment, percent, start)
                showAdd = false
            }
        )
    }
}

@Composable
private fun CompoundCard(
    habit: CompoundHabitEntity,
    logs: List<CompoundLogEntity>,
    onToggleToday: () -> Unit,
    onArchive: () -> Unit
) {
    val today = LocalDate.now()
    val todayLogged = logs.any { it.epochDay == today.toEpochDay() }
    val progress: CompoundProgress = Compounding.progress(
        startValue = habit.startValue,
        dailyIncrement = habit.dailyIncrement,
        dailyPercent = habit.dailyPercent,
        logs = logs.map { LocalDate.ofEpochDay(it.epochDay) to it.amount },
        today = today
    )

    SurfaceCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "+${formatNumber(habit.dailyIncrement)} ${habit.unit}/day · ${habit.dailyPercent}% compound",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onArchive) {
                Icon(Icons.Default.Delete, contentDescription = "Archive", modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                label = "Current",
                value = "${formatNumber(progress.currentValue)} ${habit.unit}",
                modifier = Modifier.weight(1f),
                tint = ProductiveColor
            )
            StatTile(
                label = "Streak",
                value = "${progress.streak.current} d",
                modifier = Modifier.weight(1f),
                caption = "best ${progress.streak.best} d"
            )
            StatTile(
                label = "Days",
                value = progress.daysLogged.toString(),
                modifier = Modifier.weight(1f),
                caption = "logged"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        GrowthCurve(
            curve = progress.curve.map { it.value },
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            progress.message,
            style = MaterialTheme.typography.bodySmall,
            color = if (progress.isOnTrack) ProductiveColor else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))
        PrimaryCta(
            text = if (todayLogged) "Undo today" else "Log today's increment",
            icon = if (todayLogged) null else Icons.Default.Add,
            onClick = onToggleToday
        )
    }
}

@Composable
private fun GrowthCurve(curve: List<Double>, modifier: Modifier = Modifier) {
    val color = ProductiveColor
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier = modifier) {
        if (curve.size < 2) return@Canvas
        val min = curve.min()
        val max = curve.max()
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val width = size.width
        val height = size.height
        val path = Path()
        curve.forEachIndexed { index, value ->
            val x = width * index / (curve.size - 1).toFloat()
            val y = height - ((value - min) / range * height * 0.86f + height * 0.07f).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawLine(
            color = track,
            start = Offset(0f, height * 0.95f),
            end = Offset(width, height * 0.95f),
            strokeWidth = 1f
        )
        drawPath(path = path, color = color, style = Stroke(width = 3f))
    }
}

@Composable
private fun AddCompoundDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("reps") }
    var increment by remember { mutableStateOf("1") }
    var percent by remember { mutableStateOf("1") }
    var start by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Track a compounding skill") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Skill or habit") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit (reps, pages, minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = increment,
                    onValueChange = { increment = it },
                    label = { Text("Daily increment") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = percent,
                    onValueChange = { percent = it },
                    label = { Text("Compounding percent per day") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Starting value") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onAdd(
                        name,
                        unit,
                        increment.toDoubleOrNull() ?: 1.0,
                        percent.toDoubleOrNull() ?: 1.0,
                        start.toDoubleOrNull() ?: 0.0
                    )
                }
            ) { Text("Start tracking") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else "%.1f".format(value)
