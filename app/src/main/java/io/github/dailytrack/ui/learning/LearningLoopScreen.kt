package io.github.dailytrack.ui.learning

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.db.entity.LearningReviewEntity
import io.github.dailytrack.engine.LearningLoop
import io.github.dailytrack.engine.LearningStage
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.navigateToHome
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.viewmodel.MainViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LearningLoopScreen(
    navController: NavController,
    initialTopic: String = "",
    viewModel: MainViewModel = viewModel()
) {
    val due by viewModel.dueLearningReviews.collectAsState()
    val history by viewModel.learningReviews.collectAsState()

    var step by remember { mutableIntStateOf(0) }
    var topic by remember { mutableStateOf(initialTopic) }
    var recall by remember { mutableStateOf("") }
    var missed by remember { mutableStateOf("") }
    var explanation by remember { mutableStateOf("") }
    var stillUnclear by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    ScreenList(
        title = "Learning loop",
        subtitle = "Recall first, then compare, then explain simply",
        onBack = { navController.navigateToHome() },
        compact = true
    ) {
        if (due.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "Due for review (${due.size})",
                    subtitle = "Spaced recall - the review that keeps it learned"
                )
            }
            items(due, key = { "due-${it.id}" }) { review ->
                DueReviewCard(
                    review = review,
                    onResolve = { unclear, text ->
                        viewModel.resolveLearningReview(review, unclear, text)
                    },
                    onDone = { viewModel.markLearningReviewDone(review) }
                )
            }
        }

        item {
            SectionTitle(
                title = "New review",
                subtitle = "Step ${step + 1} of 3 · ${LearningStage.entries[step].title}"
            )
        }

        item {
            LinearProgressIndicator(
                progress = { (step + 1) / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ProductiveColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }

        item {
            SurfaceCard {
                when (step) {
                    0 -> {
                        OutlinedTextField(
                            value = topic,
                            onValueChange = { topic = it },
                            label = { Text("What did you study or work on?") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            LearningStage.RECALL.prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = recall,
                            onValueChange = { recall = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Write everything you remember, without looking.") },
                            minLines = 4,
                            maxLines = 8
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PrimaryCta(
                            text = "Compare with notes",
                            enabled = topic.isNotBlank() && LearningLoop.isSubstantive(recall),
                            onClick = { step = 1 }
                        )
                    }
                    1 -> {
                        Text(
                            LearningStage.COMPARE.prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SurfaceCard(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Text(
                                "What you recalled",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(recall, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = missed,
                            onValueChange = { missed = it },
                            label = { Text("What did you miss or get wrong?") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { missed = "Nothing - full recall." }) {
                                Text("Nothing missed")
                            }
                            TextButton(onClick = { step = 0 }) { Text("Back") }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        PrimaryCta(
                            text = "Explain it simply",
                            enabled = missed.isNotBlank(),
                            onClick = { step = 2 }
                        )
                    }
                    else -> {
                        Text(
                            LearningStage.EXPLAIN.prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (missed.isNotBlank()) {
                            Text(
                                "Gap to close: $missed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = explanation,
                            onValueChange = { explanation = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Explain it like teaching a beginner...") },
                            minLines = 4,
                            maxLines = 8
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = stillUnclear, onCheckedChange = { stillUnclear = it })
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "I still cannot explain part of this",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { step = 1 }) { Text("Back") }
                            PrimaryCta(
                                text = "Finish & schedule review",
                                modifier = Modifier.weight(1f),
                                enabled = stillUnclear || LearningLoop.isSubstantive(explanation),
                                onClick = {
                                    viewModel.saveLearningReview(
                                        topic = topic,
                                        recallText = recall,
                                        missedPoints = missed,
                                        explanation = explanation,
                                        stillUnclear = stillUnclear
                                    )
                                    savedMessage = if (stillUnclear) {
                                        "Saved. It comes back tomorrow for another simple pass."
                                    } else {
                                        "Saved. A spaced review is scheduled before the memory fades."
                                    }
                                    step = 0
                                    topic = ""
                                    recall = ""
                                    missed = ""
                                    explanation = ""
                                    stillUnclear = false
                                }
                            )
                        }
                    }
                }
            }
        }

        savedMessage?.let { message ->
            item {
                SurfaceCard(containerColor = ProductiveColor.copy(alpha = 0.12f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProductiveColor)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "Your learning log",
                    subtitle = "Every loop is recorded, including what was fuzzy"
                )
            }
            items(history.take(30), key = { "history-${it.id}" }) { review ->
                HistoryRow(
                    review = review,
                    onDelete = { viewModel.deleteLearningReview(review) }
                )
            }
        }

        if (history.isEmpty() && due.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing reviewed yet",
                    message = "Run the three steps after a study block. Recall, compare, explain - the gap you cannot explain is the next review.",
                    icon = Icons.Default.School
                )
            }
        }
    }
}

@Composable
private fun DueReviewCard(
    review: LearningReviewEntity,
    onResolve: (Boolean, String) -> Unit,
    onDone: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    var unclear by remember { mutableStateOf(review.stillUnclear) }
    var text by remember { mutableStateOf(review.explanation) }

    SurfaceCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    review.topic,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (review.missedPoints.isBlank()) "Full recall"
                    else "Gap: ${review.missedPoints}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Scheduled ${review.nextReviewAt?.let { formatDate(it) } ?: "now"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onDone) { Text("Done") }
        }
        if (open) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Explain the gap again in simple words") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = unclear, onCheckedChange = { unclear = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Still unclear", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    onResolve(unclear, text)
                    open = false
                }) { Text("Save review") }
            }
        } else {
            TextButton(onClick = { open = true }) { Text("Review now") }
        }
    }
}

@Composable
private fun HistoryRow(
    review: LearningReviewEntity,
    onDelete: () -> Unit
) {
    SurfaceCard(contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    review.topic,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${Remembered(review)} · attempt ${review.attempt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Next: ${review.nextReviewAt?.let { formatDate(it) } ?: "-"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun Remembered(review: LearningReviewEntity): String = when {
    review.completedAt != null -> "Completed"
    review.stillUnclear -> "Still fuzzy"
    else -> "Explained"
}

private fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d"))
