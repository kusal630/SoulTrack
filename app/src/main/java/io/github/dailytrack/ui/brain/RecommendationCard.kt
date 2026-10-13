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


package io.github.dailytrack.ui.brain

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dailytrack.engine.BrainResult
import io.github.dailytrack.engine.FocusBand
import io.github.dailytrack.engine.TaskCandidate
import io.github.dailytrack.service.NoiseMonitor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.theme.WastedColor

/**
 * The "do this next" surface for the brain layer: one clear pick with a human
 * reason, a couple of alternatives and a live read on how focusable the moment
 * is. Suggestions only - nothing here ever blocks the user.
 */
@Composable
fun RecommendationCard(
    result: BrainResult?,
    quietness: Int?,
    noiseEnabled: Boolean,
    onStartTask: (TaskCandidate) -> Unit,
    onDoneTask: (Long) -> Unit,
    onOpenTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focus = result?.focusability
    val focusColor = when (focus?.band) {
        FocusBand.HIGH -> ProductiveColor
        FocusBand.MEDIUM -> MaterialTheme.colorScheme.primary
        FocusBand.LOW -> WastedColor
        null -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = focusColor.copy(alpha = 0.07f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Right now",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (focus != null) {
                    FocusBadge(score = focus.score, label = when (focus.band) {
                        FocusBand.HIGH -> "Deep work"
                        FocusBand.MEDIUM -> "Steady"
                        FocusBand.LOW -> "Low focus"
                    }, color = focusColor)
                }
            }

            if (focus != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val animated by animateFloatAsState(
                    targetValue = focus.score / 100f,
                    animationSpec = tween(700),
                    label = "focus_meter"
                )
                LinearProgressIndicator(
                    progress = { animated },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = focusColor,
                    trackColor = focusColor.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = buildString {
                        append("Focus ${focus.score}/100")
                        focus.reasons.firstOrNull()?.let { append(" · ") ; append(it) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (noiseEnabled) {
                        "Environment: ${NoiseMonitor.label(quietness)}"
                    } else {
                        "Environment: microphone off"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val primary = result?.primary
            if (primary == null) {
                Text(
                    text = "Nothing queued. Add a task and the app will tell you which one fits this moment best.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onOpenTasks) {
                    Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open tasks")
                }
            } else {
                PrimarySuggestion(
                    recommendation = primary,
                    onStart = { onStartTask(primary.task) },
                    onDone = { onDoneTask(primary.task.id) }
                )

                if (result.alternatives.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Alternatives",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    result.alternatives.take(2).forEach { alt ->
                        AlternativeRow(
                            title = alt.task.title,
                            reason = alt.reason,
                            onStart = { onStartTask(alt.task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusBadge(score: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label · $score",
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PrimarySuggestion(
    recommendation: io.github.dailytrack.engine.Recommendation,
    onStart: () -> Unit,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recommendation.task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = recommendation.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onStart,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                if (recommendation.deepFocus) "Start focus" else "Start",
                maxLines = 1
            )
        }
        OutlinedButton(
            onClick = onDone,
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Done")
        }
    }
    if (recommendation.deepFocus) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.NotificationsOff,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Do Not Disturb turns on automatically while this runs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlternativeRow(title: String, reason: String, onStart: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = onStart) {
            Text("Start")
        }
    }
}
