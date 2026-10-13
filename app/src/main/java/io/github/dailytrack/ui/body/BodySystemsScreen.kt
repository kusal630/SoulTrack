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


package io.github.dailytrack.ui.body

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.dailytrack.engine.BodySystemCard
import io.github.dailytrack.ui.Screen
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.components.TonalCta
import io.github.dailytrack.ui.navigateSingleTop
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.NeutralColor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.theme.WastedColor

@Composable
fun BodySystemsScreen(navController: NavController) {
    val sampleCards = listOf(
        BodySystemCard("Sleep & Recovery", "Unknown", listOf("Insufficient data"), "→", "Sleep debt may reduce focus, mood, and physical recovery.", "Aim for consistent sleep and rest when needed."),
        BodySystemCard("Energy & Metabolism", "Unknown", listOf("Insufficient data"), "→", "Low energy intake and high sugar may affect energy levels.", "Consider balanced meals."),
        BodySystemCard("Cardiovascular & Fitness", "Unknown", listOf("Insufficient data"), "→", "Regular movement supports cardiovascular health.", "Aim for regular moderate movement."),
        BodySystemCard("Musculoskeletal & Movement", "Unknown", listOf("Insufficient data"), "→", "Regular movement supports musculoskeletal health.", "Gentle stretching or walking may help."),
        BodySystemCard("Digestive & Hydration", "Unknown", listOf("Insufficient data"), "→", "Adequate fiber and water support digestive health.", "Consider vegetables, fruits, whole grains, and fluids."),
        BodySystemCard("Nutrition & Micronutrients", "Unknown", listOf("Insufficient data"), "→", "Long-term low intake of key micronutrients may affect health.", "Review your diet or consult a professional."),
        BodySystemCard("Mood & Stress", "Unknown", listOf("Insufficient data"), "→", "Persistent low mood or high stress may benefit from attention.", "Consider reflection or professional support."),
        BodySystemCard("Cognitive Focus", "Unknown", listOf("Insufficient data"), "→", "Sleep, focus, and learning activity influence cognitive performance.", "Prioritize sleep and focused learning blocks.")
    )

    ScreenList(
        title = "Body systems",
        subtitle = "Educational indicators, not a diagnosis",
        navController = navController
    ) {
        item {
            SurfaceCard(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Dimens.TitleGap))
                    Text(
                        text = "Medical disclaimer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.TitleGap))
                Text(
                    text = "These indicators are educational and not a medical diagnosis. " +
                        "If you have persistent symptoms, please consult a healthcare professional.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            TonalCta(
                text = "Log how you feel",
                icon = Icons.Default.Edit,
                onClick = { navController.navigateSingleTop(Screen.Journal.route) }
            )
        }

        items(sampleCards.size) { index ->
            BodySystemCardView(sampleCards[index])
        }
    }
}

@Composable
fun BodySystemCardView(card: BodySystemCard) {
    val statusColor = when (card.status) {
        "Good" -> ProductiveColor
        "Caution" -> ExerciseColor
        "Attention" -> WastedColor
        else -> NeutralColor
    }

    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(Dimens.TitleGap))
            StatusPill(status = card.status, color = statusColor)
        }
        Spacer(modifier = Modifier.height(Dimens.CardGap))
        Text(
            text = card.explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimens.CardGap))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(Dimens.TitleGap))
            Text(
                text = "Suggested: ${card.suggestedAction}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(Dimens.TitleGap))
        Text(
            text = card.disclaimer,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusPill(status: String, color: Color) {
    val shape = RoundedCornerShape(50)
    Text(
        text = status,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.16f))
            .border(width = 1.dp, color = color.copy(alpha = 0.45f), shape = shape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
