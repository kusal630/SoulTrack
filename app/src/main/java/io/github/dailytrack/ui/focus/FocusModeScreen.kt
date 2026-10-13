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


package io.github.dailytrack.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dailytrack.ui.components.PillChip
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.theme.WastedColor

data class AmbientSound(
    val name: String,
    val icon: String,
    val isPremium: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusModeScreen(
    isFocusActive: Boolean,
    selectedSound: String?,
    onToggleFocus: () -> Unit,
    onSelectSound: (String) -> Unit
) {
    val sounds = listOf(
        AmbientSound("Rain", "🌧️"),
        AmbientSound("Forest", "🌲"),
        AmbientSound("Ocean", "🌊"),
        AmbientSound("Cafe", "☕"),
        AmbientSound("Fire", "🔥"),
        AmbientSound("Wind", "💨"),
        AmbientSound("Night", "🌙"),
        AmbientSound("Birds", "🐦"),
        AmbientSound("Piano", "🎹", isPremium = true),
        AmbientSound("Lo-fi", "🎵", isPremium = true),
        AmbientSound("White Noise", "📻", isPremium = true),
        AmbientSound("Thunder", "⛈️", isPremium = true)
    )

    val stopGradient = Brush.horizontalGradient(
        listOf(WastedColor, WastedColor.copy(alpha = 0.75f))
    )
    val startGradient = Brush.horizontalGradient(
        listOf(ProductiveColor, ProductiveColor.copy(alpha = 0.75f))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding)
            .verticalScroll(rememberScrollState())
    ) {
        SurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = if (isFocusActive) {
                ProductiveColor.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isFocusActive) "🎯 Focus mode active" else "🎯 Focus mode",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Dimens.TitleGap))
                Text(
                    text = if (isFocusActive) {
                        "Stay focused and productive"
                    } else {
                        "Start a session to block distractions"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Dimens.CardGap))
                PrimaryCta(
                    text = if (isFocusActive) "End focus" else "Start focus",
                    icon = if (isFocusActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    gradient = if (isFocusActive) stopGradient else startGradient,
                    onClick = onToggleFocus
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.SectionGap))

        SectionTitle(
            title = "Ambient sounds",
            subtitle = "Pick a background sound to help you focus"
        )
        Spacer(modifier = Modifier.height(Dimens.TitleGap))

        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.TitleGap),
                verticalArrangement = Arrangement.spacedBy(Dimens.TitleGap)
            ) {
                sounds.forEach { sound ->
                    PillChip(
                        label = if (sound.isPremium) {
                            "${sound.icon} ${sound.name} PRO"
                        } else {
                            "${sound.icon} ${sound.name}"
                        },
                        selected = selectedSound == sound.name,
                        onClick = { onSelectSound(sound.name) },
                        tint = if (sound.isPremium) ExerciseColor else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.MinTouchTarget)
                    )
                }
            }
        }
    }
}
