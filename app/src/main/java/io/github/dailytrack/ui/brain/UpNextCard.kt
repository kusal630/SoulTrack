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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dailytrack.engine.BrainResult
import io.github.dailytrack.engine.FocusBand
import io.github.dailytrack.engine.TaskCandidate
import io.github.dailytrack.service.NoiseMonitor
import io.github.dailytrack.ui.components.GhostCta
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.LocalDayStatusColors
import java.time.LocalTime

/**
 * The intelligent layer's surface: one clear pick, the reasons behind it as
 * chips, and exactly two actions - start it, or not now. The gradient border
 * is the only place in the app where the full mint -> aqua -> sky ramp shows,
 * which is what makes this card read as "the app is thinking".
 */
@Composable
fun UpNextCard(
    result: BrainResult?,
    quietness: Int?,
    noiseEnabled: Boolean,
    environmentChips: List<String>,
    onStartTask: (TaskCandidate) -> Unit,
    onNotNow: () -> Unit,
    onOpenTasks: () -> Unit,
    onTune: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = result?.primary
    val focus = result?.focusability
    val statusColors = LocalDayStatusColors.current

    val focusColor = when (focus?.band) {
        FocusBand.HIGH -> statusColors.done
        FocusBand.MEDIUM -> MaterialTheme.colorScheme.secondary
        FocusBand.LOW -> statusColors.notProductive
        null -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Up next",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (focus != null) {
                    FocusBadge(score = focus.score, label = bandLabel(focus.band), color = focusColor)
                }
                IconButton(onClick = onTune, modifier = Modifier.size(Dimens.MinTouchTarget)) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Tune mood and place",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (focus != null && focus.reasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.TitleGap))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    focus.reasons.take(2).forEach { ReasonChip(it) }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.CardGap))

            if (primary == null) {
                Text(
                    text = "Nothing queued. Assign a task and the app will tell you which one fits this moment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Dimens.CardGap))
                GhostCta(text = "Open tasks", onClick = onOpenTasks)
                return@Column
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = primary.task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildSuggestionLine(primary.reason, quietness, noiseEnabled, environmentChips),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.CardGap))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryCta(
                    text = if (primary.deepFocus) "Start focus" else "Start",
                    onClick = { onStartTask(primary.task) },
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.weight(1f)
                )
                GhostCta(text = "Not now", onClick = onNotNow)
            }
        }
    }
}

/** Short "why" chips: the engine's own reasons, plus place and time. */
@Composable
private fun ReasonChip(label: String) {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun FocusBadge(score: Int, label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label $score",
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun bandLabel(band: FocusBand): String = when (band) {
    FocusBand.HIGH -> "Deep work"
    FocusBand.MEDIUM -> "Steady"
    FocusBand.LOW -> "Low focus"
}

private fun partOfDay(hour: Int): String = when (hour) {
    in 5..11 -> "Morning"
    in 12..16 -> "Afternoon"
    in 17..21 -> "Evening"
    else -> "Late night"
}

private fun buildSuggestionLine(
    reason: String,
    quietness: Int?,
    noiseEnabled: Boolean,
    environmentChips: List<String>
): String {
    val now = LocalTime.now()
    val env = when {
        !noiseEnabled -> "microphone off"
        quietness == null -> "environment unknown"
        else -> NoiseMonitor.label(quietness)
    }
    val place = environmentChips.firstOrNull()
    val prefix = buildList {
        add(partOfDay(now.hour))
        if (place != null) add(place)
        add(env)
    }.joinToString(" · ")
    return "$prefix — $reason"
}
