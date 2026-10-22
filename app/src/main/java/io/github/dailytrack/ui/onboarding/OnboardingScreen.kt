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


package io.github.dailytrack.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.dailytrack.ui.components.PrimaryCta
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.settings.AppFeatures
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.platform.LocalContext
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.GradientEnd
import io.github.dailytrack.ui.theme.GradientStart
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import io.github.dailytrack.engine.DayStatus
import io.github.dailytrack.ui.components.SoulRings
import io.github.dailytrack.ui.theme.ProductiveColor
import android.Manifest
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DonutSmall
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val description: String,
    val isQuote: Boolean = false,
    /** The first page: a real choice, not an illustration. */
    val isChoice: Boolean = false,
    /** Live ring demo - the app's logo, filling as you record time. */
    val isRingDemo: Boolean = false,
    /** One page that asks for every permission the app actually uses. */
    val isPermissions: Boolean = false
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var healthChosen by remember { mutableStateOf(AppFeatures.healthEnabled) }

    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Timer,
            title = "What do you want to track?",
            subtitle = "Time first. Health only if you want it.",
            description = "",
            isChoice = true
        ),
        OnboardingPage(
            icon = Icons.Default.DonutSmall,
            title = "This is your day",
            subtitle = "The inner ring is today. The outer ring is the week.",
            description = "Everything you record - tasks, timers, pomodoros, even work you write down after the fact - feeds the same ring. Time nobody records counts as waste.",
            isRingDemo = true
        ),
        OnboardingPage(
            icon = Icons.Default.FormatQuote,
            title = "Parkinson's Law",
            subtitle = "\"The less time you give a task, the more quickly you will finish it.\"",
            description = "Parkinson's Law states that work expands to fill the time available. By setting tight deadlines, you focus your energy and complete tasks faster.",
            isQuote = true
        ),
        OnboardingPage(
            icon = Icons.Default.Timer,
            title = "Timer & Pomodoro",
            subtitle = "Track every moment",
            description = "Use the Timer for open-ended tracking or Pomodoro for structured focus sessions. Both record your time automatically to build your activity history."
        ),
        OnboardingPage(
            icon = Icons.Default.CheckBox,
            title = "Smart Todos",
            subtitle = "Prioritize what matters",
            description = "Add tasks with priority levels (High/Medium/Low), set deadlines, and break them into subtasks. Link todos to your Pomodoro sessions for focused execution."
        ),
        OnboardingPage(
            icon = Icons.Default.ShowChart,
            title = "Growth Tracking",
            subtitle = "See your progress",
            description = "Your growth score updates automatically based on how you spend your time. Learning, productivity, exercise, and rest all contribute to your overall score."
        ),
        OnboardingPage(
            icon = Icons.Default.Lightbulb,
            title = "Smart Insights",
            subtitle = "Understand your patterns",
            description = "Get AI-powered insights about your habits, detect routine loops, and discover your comfort zone. The more you track, the smarter the insights become."
        ),
        OnboardingPage(
            icon = Icons.Default.Widgets,
            title = "Home Widgets",
            subtitle = "Control at a glance",
            description = "Add Timer, Pomodoro, or Quote widgets to your home screen. Start, stop, and reset directly from the widget without opening the app."
        ),
        OnboardingPage(
            icon = Icons.Default.PhoneAndroid,
            title = "Device Sync",
            subtitle = "Private & secure",
            description = "Sync data between your devices using Syncthing - no cloud, no accounts, no data leaves your control. Your privacy is guaranteed."
        ),
        OnboardingPage(
            icon = Icons.Default.Shield,
            title = "Permissions",
            subtitle = "Everything runs on this device",
            description = "Notifications for timers, the microphone for noise sensing, Do Not Disturb for focus sessions, and usage access so time you never opened Soul Track for is still accounted for. Nothing is uploaded.",
            isPermissions = true
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage >= pages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            GradientStart.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0f),
                            GradientEnd.copy(alpha = 0.16f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                .widthIn(max = Dimens.ContentMaxWidth),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Soul Track",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when {
                    pages[page].isChoice -> OnboardingChoicePage(
                        healthEnabled = healthChosen,
                        onSelect = { enabled ->
                            healthChosen = enabled
                            AppFeatures.setHealthEnabled(enabled, context)
                        }
                    )
                    pages[page].isRingDemo -> OnboardingRingDemoPage()
                    pages[page].isPermissions -> OnboardingPermissionsPage()
                    else -> OnboardingPageContent(pages[page])
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        label = "indicator_width"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.1f else 1f,
                        label = "indicator_scale"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .scale(scale)
                            .width(width)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier.height(Dimens.MinTouchTarget)
                        ) {
                            Text(
                                text = "Back",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (!isLastPage) {
                        TextButton(
                            onClick = onComplete,
                            modifier = Modifier.height(Dimens.MinTouchTarget)
                        ) {
                            Text(
                                text = "Skip",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                PrimaryCta(
                    text = if (isLastPage) "Get started" else "Continue",
                    icon = if (isLastPage) null else Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = {
                        if (isLastPage) {
                            onComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * The ring the whole app is built around, filling once so the user knows what
 * they are looking at before they ever open Home.
 */
@Composable
private fun OnboardingRingDemoPage() {
    var demoProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400)
        demoProgress = 0.72f
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SoulRings(
            dayProgress = demoProgress,
            weekStatuses = listOf(
                DayStatus.DONE, DayStatus.DONE, DayStatus.WASTING_TIME,
                DayStatus.TASKS_NOT_MET, DayStatus.DONE, DayStatus.IN_PROGRESS, null
            ),
            todayIndex = 5,
            modifier = Modifier.size(240.dp),
            animate = true,
            contentDescription = "Demo ring filling as time is recorded"
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Inner ring: today's tasks and recorded focus time.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Outer ring: one segment per day. It is written the moment the day closes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Every permission the app uses, requested here once instead of mid-flow. */
@Composable
private fun OnboardingPermissionsPage() {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { tick++ }

    val permissionRows = remember(tick) {
        listOf(
            PermissionRow(
                label = "Notifications",
                caption = "Timer and pomodoro countdowns",
                granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                    android.os.Build.VERSION.SDK_INT < 33,
                request = {
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            ),
            PermissionRow(
                label = "Microphone",
                caption = "Noise sensing during focus",
                granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
                request = {
                    notifPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            ),
            PermissionRow(
                label = "Do Not Disturb",
                caption = "Silence notifications while you focus",
                granted = io.github.dailytrack.service.FocusDndManager.hasAccess(context),
                request = {
                    runCatching {
                        context.startActivity(
                            io.github.dailytrack.service.FocusDndManager.settingsIntent()
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            ),
            PermissionRow(
                label = "Usage access",
                caption = "Accounts for time in other apps - stays on this device",
                granted = io.github.dailytrack.wellbeing.UsageAccess.has(context),
                request = {
                    runCatching {
                        context.startActivity(
                            io.github.dailytrack.wellbeing.UsageAccess.settingsIntent(context)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        permissionRows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (row.granted) Icons.Default.CheckCircle else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (row.granted) ProductiveColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = row.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!row.granted) {
                    TextButton(onClick = row.request) { Text("Allow") }
                } else {
                    Text(
                        text = "On",
                        style = MaterialTheme.typography.labelMedium,
                        color = ProductiveColor
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "You can change any of this later in Settings. Soul Track never sends your data anywhere.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class PermissionRow(
    val label: String,
    val caption: String,
    val granted: Boolean,
    val request: () -> Unit
)

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (page.isQuote) {
            Text(
                text = page.subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = page.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentPadding = PaddingValues(20.dp)
        ) {
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * The one onboarding screen that actually asks something: this app is a time
 * tracker, the health layer is an opt-in on top of it.
 */
@Composable
private fun OnboardingChoicePage(
    healthEnabled: Boolean,
    onSelect: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "What do you want to track?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You can change this any time in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        ChoiceCard(
            icon = Icons.Default.Timer,
            title = "Time & tasks",
            body = "Timer, pomodoro, todos, growth score, insights and reports - built only from what you track.",
            selected = !healthEnabled,
            onClick = { onSelect(false) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        ChoiceCard(
            icon = Icons.Default.FavoriteBorder,
            title = "Time, tasks & health",
            body = "Adds sleep, exercise, nutrition, hydration and the body dashboard as optional extras.",
            selected = healthEnabled,
            onClick = { onSelect(true) }
        )
    }
}

@Composable
private fun ChoiceCard(
    icon: ImageVector,
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        containerColor = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = border,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (title == "Time & tasks") {
                    Text(
                        text = "Recommended",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
