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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition Analytics") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Daily Macros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
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
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Micronutrients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        NutrientRow("Iron", 0.0, 18.0, "mg")
                        NutrientRow("Calcium", 0.0, 1000.0, "mg")
                        NutrientRow("Vitamin D", 0.0, 20.0, "mcg")
                        NutrientRow("Vitamin B12", 0.0, 2.4, "mcg")
                        NutrientRow("Folate", 0.0, 400.0, "mcg")
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Nutrition Quality Score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("--", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Log food to see your nutrition quality score.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Weekly Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Charts will appear once you have logged food data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                MedicalDisclaimerCard()
            }
        }
    }
}

@Composable
fun NutrientRow(name: String, current: Double, target: Double, unit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            "${String.format(java.util.Locale.US, "%.1f", current)} / ${String.format(java.util.Locale.US, "%.0f", target)} $unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
    LinearProgressIndicator(
        progress = { (current / target).coerceIn(0.0, 1.0).toFloat() },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun MedicalDisclaimerCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Medical Disclaimer",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Nutrition information is for educational purposes only and is not medical advice. If you have dietary concerns, please consult a healthcare professional.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
