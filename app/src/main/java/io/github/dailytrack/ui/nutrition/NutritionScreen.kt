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


package io.github.dailytrack.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.ScreenSection
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.StatTile
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.LearningColor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.theme.RecoveryColor

@Composable
fun NutritionScreen(navController: NavController) {
    ScreenList(
        title = "Nutrition",
        subtitle = "Macros, micros and daily quality",
        navController = navController
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                StatTile(
                    label = "Calories",
                    value = "0 kcal",
                    icon = Icons.Default.LocalDining,
                    tint = RecoveryColor,
                    caption = "Target 2,000 kcal",
                    progress = 0f,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Quality score",
                    value = "0",
                    icon = Icons.Default.WaterDrop,
                    tint = ProductiveColor,
                    caption = "Log a meal to score it",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            PrimaryCta(
                text = "Log food",
                icon = Icons.Default.Add,
                onClick = {}
            )
        }

        item {
            ScreenSection(
                title = "Daily macros",
                subtitle = "0 of 2,000 kcal logged today",
                leadingIcon = Icons.Default.Restaurant,
                tint = RecoveryColor
            ) {
                NutrientRow("Calories", 0.0, 2000.0, "kcal")
                NutrientRow("Protein", 0.0, 50.0, "g")
                NutrientRow("Carbohydrates", 0.0, 250.0, "g")
                NutrientRow("Fat", 0.0, 65.0, "g")
                NutrientRow("Fiber", 0.0, 25.0, "g")
                NutrientRow("Sugar", 0.0, 36.0, "g")
                NutrientRow("Added Sugar", 0.0, 25.0, "g")
                NutrientRow("Sodium", 0.0, 2300.0, "mg")
            }
        }

        item {
            ScreenSection(
                title = "Micronutrients",
                subtitle = "Targets for a balanced day",
                leadingIcon = Icons.Default.WaterDrop,
                tint = LearningColor
            ) {
                NutrientRow("Iron", 0.0, 18.0, "mg")
                NutrientRow("Calcium", 0.0, 1000.0, "mg")
                NutrientRow("Vitamin D", 0.0, 20.0, "mcg")
                NutrientRow("Vitamin B12", 0.0, 2.4, "mcg")
                NutrientRow("Folate", 0.0, 400.0, "mcg")
            }
        }

        item {
            SectionTitle(
                title = "Weekly trends",
                subtitle = "How your intake moves over seven days",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            EmptyState(
                title = "No nutrition trends yet",
                message = "Log food for a few days and weekly macro and micronutrient charts appear here.",
                icon = Icons.Default.BarChart,
                tint = RecoveryColor
            )
        }

        item {
            MedicalDisclaimerCard()
        }
        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}

@Composable
fun NutrientRow(name: String, current: Double, target: Double, unit: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(Dimens.TitleGap))
            Text(
                text = "${String.format(java.util.Locale.US, "%.1f", current)} / " +
                    "${String.format(java.util.Locale.US, "%.0f", target)} $unit",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (current / target).coerceIn(0.0, 1.0).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = RecoveryColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Spacer(modifier = Modifier.height(Dimens.CardGap))
    }
}

@Composable
fun MedicalDisclaimerCard() {
    SurfaceCard(
        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
    ) {
        Text(
            text = "Medical disclaimer",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(Dimens.TitleGap))
        Text(
            text = "Nutrition information is for educational purposes only and is not " +
                "medical advice. If you have dietary concerns, please consult a healthcare professional.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
