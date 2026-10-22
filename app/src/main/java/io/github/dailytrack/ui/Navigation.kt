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


package io.github.dailytrack.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import io.github.dailytrack.SoulTrackApp
import io.github.dailytrack.ui.dashboard.DashboardScreen
import io.github.dailytrack.ui.timer.TimerScreen
import io.github.dailytrack.ui.todo.TodoScreen
import io.github.dailytrack.ui.pomodoro.PomodoroScreen
import io.github.dailytrack.ui.insights.InsightsScreen
import io.github.dailytrack.ui.settings.SettingsScreen
import io.github.dailytrack.ui.savedquotes.SavedQuotesScreen
import io.github.dailytrack.ui.sync.SyncSettingsScreen
import io.github.dailytrack.ui.habit.HabitScreen
import io.github.dailytrack.ui.explore.ExploreScreen
import io.github.dailytrack.ui.focus.FocusModeScreen
import io.github.dailytrack.ui.reminders.RemindersScreen
import io.github.dailytrack.ui.history.HistoryScreen
import io.github.dailytrack.ui.insights.WeeklyReportScreen
import io.github.dailytrack.ui.planner.PlannerScreen
import io.github.dailytrack.ui.projects.ProjectsScreen
import io.github.dailytrack.ui.learning.LearningLoopScreen
import io.github.dailytrack.ui.compounding.CompoundingScreen
import io.github.dailytrack.ui.weekly.WeeklyReviewScreen
import io.github.dailytrack.ui.gamification.GamificationScreen
import io.github.dailytrack.ui.components.Hairline
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.data.db.entity.GoalEntity
import io.github.dailytrack.data.db.entity.ReminderEntity
import io.github.dailytrack.ui.viewmodel.MainViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    data object Timer : Screen("timer", "Timer", Icons.Default.Timer)
    data object Todo : Screen("todo", "Todo", Icons.Default.CheckCircle)
    data object Pomodoro : Screen("pomodoro", "Pomodoro", Icons.Default.Coffee)
    data object Insights : Screen("insights", "Insights", Icons.Default.Lightbulb)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object SavedQuotes : Screen("saved_quotes", "Saved Quotes", Icons.Default.Favorite)
    data object SyncSettings : Screen("sync_settings", "Sync", Icons.Default.Sync)
    data object Habits : Screen("habits", "Habits", Icons.Default.Flag)
    data object Explore : Screen("explore", "Explore", Icons.Default.Explore)
    data object FocusMode : Screen("focus_mode", "Focus", Icons.Default.CenterFocusStrong)
    data object Reminders : Screen("reminders", "Reminders", Icons.Default.Alarm)
    data object History : Screen("history", "History", Icons.Default.History)
    data object WeeklyReport : Screen("weekly_report", "Report", Icons.Default.Assessment)
    data object Achievements : Screen("achievements", "Achievements", Icons.Default.EmojiEvents)
    data object Planner : Screen("planner", "Planner", Icons.Default.EventNote)
    data object Projects : Screen("projects", "Projects", Icons.Default.ReportProblem)
    data object LearningLoop : Screen("learning_loop", "Learning", Icons.Default.School)
    data object Compounding : Screen("compounding", "Compounding", Icons.Default.TrendingUp)
    data object WeeklyReview : Screen("weekly_review", "Review", Icons.Default.FactCheck)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Timer,
    Screen.Todo,
    Screen.Pomodoro,
    Screen.Insights,
)

fun NavController.navigateSingleTop(route: String) {
    navigate(route) { launchSingleTop = true }
}

fun NavController.navigateToHome() {
    navigate(Screen.Dashboard.route) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = false
        }
        launchSingleTop = true
    }
}

@Composable
fun DailyTrackNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as SoulTrackApp
    val haptic = LocalHapticFeedback.current
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val widthDp = LocalConfiguration.current.screenWidthDp
    val useRail = widthDp >= 600

    BackHandler(enabled = currentRoute != Screen.Dashboard.route) {
        navController.navigateToHome()
    }

    // The home button is a promise: leave the app, come back, and you land on
    // Home - not on whichever deep screen happened to be open.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var wasStopped by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> wasStopped = true
                androidx.lifecycle.Lifecycle.Event.ON_START -> {
                    if (wasStopped) {
                        wasStopped = false
                        navController.navigateToHome()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onSelect: (Screen) -> Unit = { screen ->
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (screen.route == Screen.Dashboard.route) {
            navController.navigateToHome()
        } else {
            navController.navigate(screen.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    if (useRail) {
        // Medium and expanded windows use a rail: it keeps every destination
        // one tap away without spending 80dp of vertical space on a bar that
        // would otherwise stretch the full width of a tablet.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .width(88.dp)
                    .fillMaxHeight(),
                header = {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            ) {
                bottomNavItems.forEach { screen ->
                    NavigationRailItem(
                        selected = currentRoute == screen.route,
                        onClick = { onSelect(screen) },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        alwaysShowLabel = true,
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Hairline()
                Spacer(modifier = Modifier.height(8.dp))
                NavigationRailItem(
                    selected = currentRoute == Screen.Explore.route,
                    onClick = { onSelect(Screen.Explore) },
                    icon = { Icon(Screen.Explore.icon, contentDescription = "Explore") },
                    label = { Text("Explore") },
                    alwaysShowLabel = true,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationRailItem(
                    selected = currentRoute == Screen.Settings.route,
                    onClick = { onSelect(Screen.Settings) },
                    icon = { Icon(Screen.Settings.icon, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    alwaysShowLabel = true,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            )
            AppNavHost(
                navController = navController,
                app = app,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    } else {
        Scaffold(
            bottomBar = {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    Hairline()
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.1f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "nav_scale"
                            )

                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        screen.icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.scale(scale)
                                    )
                                },
                                label = {
                                    Text(
                                        screen.title,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                selected = isSelected,
                                onClick = { onSelect(screen) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                app = app,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    app: SoulTrackApp,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) { DashboardScreen(navController) }
        composable(Screen.Timer.route) { TimerScreen(navController) }
        composable(Screen.Todo.route) { TodoScreen(navController) }
        composable(Screen.Pomodoro.route) { PomodoroScreen(navController) }
        composable(Screen.Insights.route) { InsightsScreen(navController) }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
        composable(Screen.SavedQuotes.route) { SavedQuotesScreen(navController) }
        composable(Screen.SyncSettings.route) {
            SyncSettingsScreen(navController, app.syncManager)
        }
        composable(Screen.Explore.route) { ExploreScreen(navController) }
        composable(Screen.History.route) { HistoryScreen(navController) }
        composable(Screen.FocusMode.route) {
            var focusActive by rememberSaveable { mutableStateOf(false) }
            var focusSound by rememberSaveable { mutableStateOf("") }
            FocusModeScreen(
                isFocusActive = focusActive,
                selectedSound = focusSound.ifEmpty { null },
                onToggleFocus = { focusActive = !focusActive },
                onSelectSound = { focusSound = it },
                onBack = { navController.navigateToHome() }
            )
        }
        composable(Screen.Reminders.route) {
            val viewModel: MainViewModel = viewModel()
            val reminders by viewModel.reminders.collectAsState()
            RemindersScreen(
                reminders = reminders,
                onAddReminder = viewModel::addReminder,
                onToggleReminder = viewModel::toggleReminder,
                onDeleteReminder = viewModel::deleteReminder,
                onBack = { navController.navigateToHome() }
            )
        }
        composable(Screen.WeeklyReport.route) {
            val viewModel: MainViewModel = viewModel()
            val weeklyData by viewModel.weeklyReport.collectAsState()
            val productivityScore by viewModel.productivityScore.collectAsState()
            WeeklyReportScreen(
                weeklyData = weeklyData,
                productivityScore = productivityScore,
                onBack = { navController.navigateToHome() }
            )
        }
        composable(Screen.Planner.route) { PlannerScreen(navController) }
        composable(Screen.Projects.route) { ProjectsScreen(navController) }
        composable(Screen.LearningLoop.route) { LearningLoopScreen(navController) }
        composable(Screen.Compounding.route) { CompoundingScreen(navController) }
        composable(Screen.WeeklyReview.route) { WeeklyReviewScreen(navController) }
        composable(Screen.Achievements.route) {
            val viewModel: MainViewModel = viewModel()
            val userStats by viewModel.userStats.collectAsState()
            val achievements by viewModel.achievements.collectAsState()
            GamificationScreen(
                userStats = userStats,
                achievements = achievements,
                onBack = { navController.navigateToHome() }
            )
        }
        composable(Screen.Habits.route) {
            val viewModel: MainViewModel = viewModel()
            val habits by viewModel.habits.collectAsState()
            val completions by viewModel.habitCompletions.collectAsState()
            HabitScreen(
                navController = navController,
                habits = habits,
                completions = completions,
                onAddHabit = viewModel::addHabit,
                onToggleHabit = viewModel::toggleHabit,
                onDeleteHabit = viewModel::deleteHabit
            )
        }
    }
}
