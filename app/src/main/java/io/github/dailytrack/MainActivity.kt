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


package io.github.dailytrack

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.dailytrack.engine.DayStatus
import io.github.dailytrack.ui.DailyTrackNavHost
import io.github.dailytrack.ui.components.SoulRings
import io.github.dailytrack.ui.components.rememberReduceMotion
import io.github.dailytrack.ui.theme.*
import io.github.dailytrack.ui.onboarding.OnboardingScreen
import io.github.dailytrack.ui.settings.AppFeatures
import io.github.dailytrack.ui.settings.ProductTargets
import io.github.dailytrack.widget.QuoteWidgetProvider
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var showOnboarding by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        ThemeSettings.load(this)
        AppFeatures.load(this)
        ProductTargets.load(this)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
        }

        val prefs = getSharedPreferences("soultrack_prefs", MODE_PRIVATE)
        showOnboarding = !prefs.getBoolean("onboarding_complete", false)

        setContent {
            SoulTrackTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onSplashComplete = { showSplash = false })
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (showOnboarding) {
                            OnboardingScreen(
                                onComplete = {
                                    prefs.edit().putBoolean("onboarding_complete", true).apply()
                                    showOnboarding = false
                                }
                            )
                        } else {
                            DailyTrackNavHost()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        QuoteWidgetProvider.updateWidgets(this)
    }

}

/**
 * The Compose half of the splash: the mark itself, breathing on pure black,
 * before the app takes over. The system splash (SplashScreen API) covers the
 * cold start; this is the second beat that shows the rings coming alive.
 */
@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val reduceMotion = rememberReduceMotion()

    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        if (reduceMotion) {
            progress = 0.62f
            delay(260)
        } else {
            delay(60)
            progress = 0.25f
            delay(460)
            progress = 0.62f
            delay(220)
        }
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SoulRings(
                dayProgress = progress,
                weekStatuses = listOf(
                    DayStatus.DONE,
                    DayStatus.TASKS_NOT_MET,
                    DayStatus.NOT_PRODUCTIVE,
                    DayStatus.WASTING_TIME,
                    DayStatus.DONE,
                    null,
                    null
                ),
                todayIndex = 5,
                modifier = Modifier.size(240.dp),
                animate = !reduceMotion,
                contentDescription = "Soul Track"
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Soul Track",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Every second, accounted for",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
