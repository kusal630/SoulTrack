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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dailytrack.ui.components.GhostCta
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.SoulShapes

/** What the user told us in the mood sheet - kept in the UI layer only. */
data class MoodSelection(
    val mood: String? = null,
    val energy: String? = null,
    val place: String? = null
) {
    /** Chips shown on the Up Next card so the suggestion stays explainable. */
    fun chips(): List<String> = listOfNotNull(energy, mood, place)
}

private data class Option(val label: String, val icon: ImageVector)

private val MoodOptions = listOf(
    Option("Rough", Icons.Default.SentimentDissatisfied),
    Option("Okay", Icons.Default.SentimentNeutral),
    Option("Good", Icons.Default.SentimentSatisfied),
    Option("Great", Icons.Default.SentimentVerySatisfied)
)

private val EnergyOptions = listOf(
    Option("Low energy", Icons.Default.BatteryAlert),
    Option("Steady", Icons.Default.BatteryStd),
    Option("Charged", Icons.Default.BatteryFull)
)

private val PlaceOptions = listOf(
    Option("Home", Icons.Default.Home),
    Option("Work", Icons.Default.Work),
    Option("Cafe", Icons.Default.LocalCafe),
    Option("Resting", Icons.Default.Hotel)
)

/**
 * One tap, always skippable. The answers only shape the suggestion - nothing
 * is written to the database here, and the card always shows why the pick was
 * made.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodEnvironmentSheet(
    current: MoodSelection,
    onDismiss: () -> Unit,
    onSave: (MoodSelection) -> Unit
) {
    var mood by remember { mutableStateOf(current.mood) }
    var energy by remember { mutableStateOf(current.energy) }
    var place by remember { mutableStateOf(current.place) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = SoulShapes.Sheet,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(50)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "How are you, and where?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Text(
                text = "One tap, skip if you like. The app uses this to explain why a task fits right now.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SheetGroup(title = "Mood", options = MoodOptions, selected = mood) {
                mood = if (mood == it) null else it
            }
            SheetGroup(title = "Energy", options = EnergyOptions, selected = energy) {
                energy = if (energy == it) null else it
            }
            SheetGroup(title = "Place", options = PlaceOptions, selected = place) {
                place = if (place == it) null else it
            }

            Spacer(modifier = Modifier.height(Dimens.CardGap))
            PrimaryCta(
                text = "Use this",
                onClick = { onSave(MoodSelection(mood, energy, place)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            GhostCta(text = "Skip", onClick = { onSave(current) })
            Spacer(modifier = Modifier.height(Dimens.SectionGap))
        }
    }
}

@Composable
private fun SheetGroup(
    title: String,
    options: List<Option>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    Spacer(modifier = Modifier.height(Dimens.CardGap))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            IconChip(
                label = option.label,
                icon = option.icon,
                selected = selected == option.label,
                onClick = { onSelect(option.label) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun IconChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceContainerHighest,
                RoundedCornerShape(50)
            )
            .then(
                if (selected) {
                    Modifier.border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        RoundedCornerShape(50)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 1
            )
        }
    }
}
