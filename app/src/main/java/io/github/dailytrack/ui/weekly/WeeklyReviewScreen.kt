package io.github.dailytrack.ui.weekly

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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.dailytrack.data.db.entity.WeeklyReviewSessionEntity
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

private data class ReviewStep(val title: String, val prompt: String, val placeholder: String)

private val STEPS = listOf(
    ReviewStep(
        "What got done",
        "List what you actually finished, not what you meant to finish.",
        "Finished: ..."
    ),
    ReviewStep(
        "What to delete",
        "Which task, commitment or project should disappear from next week?",
        "Dropping: ..."
    ),
    ReviewStep(
        "What failed and why",
        "A mini post-mortem. Name one thing that failed and the real reason.",
        "It failed because ..."
    ),
    ReviewStep(
        "Recall the lesson",
        "Close your notes. What is the one lesson from this week you can explain simply?",
        "The lesson, in my own words: ..."
    ),
    ReviewStep(
        "Next week's focus",
        "One important, not urgent thing. It becomes a task when you save.",
        "Next week I will ..."
    )
)

@Composable
fun WeeklyReviewScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val history by viewModel.weeklyReviewSessions.collectAsState()
    var step by remember { mutableIntStateOf(0) }
    var answers by remember { mutableStateOf(List(STEPS.size) { "" }) }
    var saved by remember { mutableStateOf(false) }

    ScreenList(
        title = "Weekly review",
        subtitle = "Five minutes: what worked, what failed, what comes next",
        onBack = { navController.navigateToHome() },
        compact = true
    ) {
        if (!saved) {
            item {
                SectionTitle(
                    title = STEPS[step].title,
                    subtitle = "Step ${step + 1} of ${STEPS.size}"
                )
            }
            item {
                LinearProgressIndicator(
                    progress = { (step + 1) / STEPS.size.toFloat() },
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
                    Text(
                        STEPS[step].prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = answers[step],
                        onValueChange = { value ->
                            answers = answers.toMutableList().also { it[step] = value }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(STEPS[step].placeholder) },
                        minLines = 4,
                        maxLines = 8
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (step > 0) {
                            TextButton(onClick = { step-- }) { Text("Back") }
                        }
                        PrimaryCta(
                            text = if (step == STEPS.lastIndex) "Save review" else "Next",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (step == STEPS.lastIndex) {
                                    viewModel.saveWeeklyReview(
                                        doneText = answers[0],
                                        deleteText = answers[1],
                                        failedText = answers[2],
                                        lessonText = answers[3],
                                        nextWeekFocus = answers[4]
                                    )
                                    saved = true
                                } else {
                                    step++
                                }
                            }
                        )
                    }
                }
            }
        } else {
            item {
                SurfaceCard(containerColor = ProductiveColor.copy(alpha = 0.12f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProductiveColor)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Review saved",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (answers[4].isBlank()) {
                                    "Your next week is set. Come back next Sunday."
                                } else {
                                    "\"${answers[4]}\" was added to your tasks as a high-priority focus."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            item {
                TextButton(onClick = {
                    step = 0
                    answers = List(STEPS.size) { "" }
                    saved = false
                }) { Text("Start another review") }
            }
        }

        if (history.isNotEmpty()) {
            item {
                SectionTitle(
                    title = "Past reviews",
                    subtitle = "Your own record of what changed"
                )
            }
            items(history.take(12), key = { it.id }) { session ->
                ReviewHistoryCard(session)
            }
        } else if (!saved) {
            item {
                EmptyState(
                    title = "No review yet",
                    message = "A weekly review closes the loop: what got done, what failed, the lesson, and one focus for next week.",
                    icon = Icons.Default.Assessment
                )
            }
        }
    }
}

@Composable
private fun ReviewHistoryCard(session: WeeklyReviewSessionEntity) {
    SurfaceCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Week of " + Instant.ofEpochMilli(session.weekStart)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("MMM d")),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (session.nextWeekFocus.isNotBlank()) {
                    Text(
                        "Focus: ${session.nextWeekFocus}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        listOf(
            "Done" to session.doneText,
            "Dropped" to session.deleteText,
            "Failed" to session.failedText,
            "Lesson" to session.lessonText
        ).filter { it.second.isNotBlank() }.forEach { (label, value) ->
            Text(
                "$label: $value",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
