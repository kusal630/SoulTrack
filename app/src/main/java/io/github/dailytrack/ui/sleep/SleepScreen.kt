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


package io.github.dailytrack.ui.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.SelfImprovement
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
import io.github.dailytrack.ui.theme.RecoveryColor
import io.github.dailytrack.ui.theme.SleepColor

@Composable
fun SleepScreen(navController: NavController) {
    var bedtime by remember { mutableStateOf("") }
    var wakeTime by remember { mutableStateOf("") }

    ScreenList(
        title = "Sleep & recovery",
        subtitle = "Last night, debt and how you woke up",
        navController = navController
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                StatTile(
                    label = "Last night",
                    value = "0h",
                    icon = Icons.Default.Hotel,
                    tint = SleepColor,
                    caption = "Nothing logged yet",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Sleep debt",
                    value = "0h",
                    icon = Icons.Default.SelfImprovement,
                    tint = RecoveryColor,
                    caption = "Target 8h per night",
                    progress = 0f,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            ScreenSection(
                title = "Log sleep",
                subtitle = "When you went to bed and when you woke",
                leadingIcon = Icons.Default.Bedtime,
                tint = SleepColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)
                ) {
                    OutlinedTextField(
                        value = bedtime,
                        onValueChange = { bedtime = it },
                        label = { Text("Bedtime") },
                        placeholder = { Text("23:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = wakeTime,
                        onValueChange = { wakeTime = it },
                        label = { Text("Wake time") },
                        placeholder = { Text("07:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.CardGap))
                PrimaryCta(
                    text = "Log sleep session",
                    icon = Icons.Default.Add,
                    onClick = {}
                )
            }
        }

        item {
            SectionTitle(
                title = "Recovery",
                subtitle = "Warnings built from your recent nights",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            EmptyState(
                title = "No recovery warnings",
                message = "Log a few nights and we will flag short sleep, late bedtimes and growing sleep debt here.",
                icon = Icons.Default.HealthAndSafety,
                tint = RecoveryColor
            )
        }

        item {
            SectionTitle(
                title = "Sleep history",
                subtitle = "Your last seven nights",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            EmptyState(
                title = "No sleep history yet",
                message = "Once you log your first night, duration and quality chart here night by night.",
                icon = Icons.Default.BarChart,
                tint = SleepColor
            )
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}
