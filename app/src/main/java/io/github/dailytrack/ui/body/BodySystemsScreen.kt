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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.dailytrack.engine.BodySystemCard

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Body Systems Dashboard") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Medical Disclaimer", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("These indicators are educational and not a medical diagnosis. If you have persistent symptoms, please consult a healthcare professional.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            items(sampleCards) { card ->
                BodySystemCardView(card)
            }
        }
    }
}

@Composable
fun BodySystemCardView(card: BodySystemCard) {
    val statusColor = when (card.status) {
        "Good" -> Color(0xFF2E7D32)
        "Caution" -> Color(0xFFF57C00)
        "Attention" -> Color(0xFFC62828)
        else -> Color(0xFF757575)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(card.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(card.status, color = statusColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(card.explanation, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Suggested: ${card.suggestedAction}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(card.disclaimer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
