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


package io.github.dailytrack.ui.journal

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
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
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.LearningColor
import io.github.dailytrack.ui.theme.SocialColor

@Composable
fun JournalScreen(navController: NavController) {
    var well by remember { mutableStateOf("") }
    var drained by remember { mutableStateOf("") }
    var learned by remember { mutableStateOf("") }
    var improve by remember { mutableStateOf("") }

    ScreenList(
        title = "Journal",
        subtitle = "Reflections and life events",
        navController = navController
    ) {
        item {
            ScreenSection(
                title = "Daily reflection",
                subtitle = "Four questions, one honest minute",
                leadingIcon = Icons.Default.EditNote,
                tint = LearningColor
            ) {
                OutlinedTextField(
                    value = well,
                    onValueChange = { well = it },
                    label = { Text("What went well?") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Dimens.CardGap))
                OutlinedTextField(
                    value = drained,
                    onValueChange = { drained = it },
                    label = { Text("What drained energy?") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Dimens.CardGap))
                OutlinedTextField(
                    value = learned,
                    onValueChange = { learned = it },
                    label = { Text("What did I learn?") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Dimens.CardGap))
                OutlinedTextField(
                    value = improve,
                    onValueChange = { improve = it },
                    label = { Text("What should I improve tomorrow?") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Dimens.CardGap))
                PrimaryCta(
                    text = "Save reflection",
                    icon = Icons.Default.Check,
                    onClick = {}
                )
            }
        }

        item {
            SectionTitle(
                title = "Entries",
                subtitle = "Reflections and life events, newest first",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            EmptyState(
                title = "No entries yet",
                message = "Saved reflections and life events show up here as a timeline you can look back on.",
                icon = Icons.Default.EditNote,
                tint = SocialColor,
                actionLabel = "Write entry",
                onAction = {}
            )
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}
