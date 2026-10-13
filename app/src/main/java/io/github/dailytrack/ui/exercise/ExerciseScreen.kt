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


package io.github.dailytrack.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.ScreenSection
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.StatTile
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.RecoveryColor

@Composable
fun ExerciseScreen(navController: NavController) {
    var activity by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var intensity by remember { mutableStateOf("") }

    ScreenList(
        title = "Exercise & movement",
        subtitle = "Workouts, minutes and strain",
        navController = navController
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                StatTile(
                    label = "Today",
                    value = "0 min",
                    icon = Icons.Default.DirectionsRun,
                    tint = ExerciseColor,
                    caption = "No workout yet",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Sessions",
                    value = "0",
                    icon = Icons.Default.FitnessCenter,
                    tint = RecoveryColor,
                    caption = "This week",
                    progress = 0f,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            ScreenSection(
                title = "Log exercise",
                subtitle = "What you did, for how long, how hard",
                leadingIcon = Icons.Default.FitnessCenter,
                tint = ExerciseColor
            ) {
                OutlinedTextField(
                    value = activity,
                    onValueChange = { activity = it },
                    label = { Text("Activity type") },
                    placeholder = { Text("Running") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Dimens.CardGap))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)
                ) {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration (minutes)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = intensity,
                        onValueChange = { intensity = it },
                        label = { Text("Intensity") },
                        placeholder = { Text("Moderate") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.CardGap))
                PrimaryCta(
                    text = "Log exercise",
                    icon = Icons.Default.Add,
                    onClick = {}
                )
            }
        }

        item {
            SectionTitle(
                title = "History",
                subtitle = "Your recent sessions",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            EmptyState(
                title = "No workouts logged yet",
                message = "Log a session above and it lands here with its duration, intensity and contribution to today's movement.",
                icon = Icons.Default.History,
                tint = ExerciseColor
            )
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}
