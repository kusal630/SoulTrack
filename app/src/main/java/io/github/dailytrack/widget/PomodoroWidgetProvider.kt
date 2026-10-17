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


package io.github.dailytrack.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import io.github.dailytrack.R
import io.github.dailytrack.service.PomodoroForegroundService
import kotlinx.coroutines.*

class PomodoroWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_START = "io.github.dailytrack.ACTION_POM_WIDGET_START"
        const val ACTION_STOP = "io.github.dailytrack.ACTION_POM_WIDGET_STOP"
        const val ACTION_RESET = "io.github.dailytrack.ACTION_POM_WIDGET_RESET"
        const val ACTION_SKIP = "io.github.dailytrack.ACTION_POM_WIDGET_SKIP"
        const val ACTION_CYCLE_DURATION = "io.github.dailytrack.ACTION_POM_WIDGET_CYCLE_DURATION"
        const val ACTION_UPDATE = "io.github.dailytrack.ACTION_POM_WIDGET_UPDATE"

        val WORK_DURATIONS = intArrayOf(25, 30, 45, 60)
        val BREAK_DURATIONS = intArrayOf(5, 5, 10, 15)
        val LONG_BREAK_DURATIONS = intArrayOf(15, 15, 20, 30)

        fun updateWidgets(context: Context) {
            val intent = Intent(context, PomodoroWidgetProvider::class.java).apply {
                action = ACTION_UPDATE
                component = ComponentName(context, PomodoroWidgetProvider::class.java)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_START -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = context.checkSelfPermission(
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPermission) return
                }

                val prefs = context.getSharedPreferences("pom_widget_prefs", Context.MODE_PRIVATE)
                val startTime = System.currentTimeMillis()
                val durationIndex = prefs.getInt("duration_index", 0)
                val duration = WORK_DURATIONS[durationIndex]
                val breakDuration = BREAK_DURATIONS[durationIndex]
                val longBreakDuration = LONG_BREAK_DURATIONS[durationIndex]
                val sessionNumber = prefs.getInt("session_number", 0)

                val serviceIntent = Intent(context, PomodoroForegroundService::class.java).apply {
                    putExtra(PomodoroForegroundService.EXTRA_START_TIME, startTime)
                    putExtra(PomodoroForegroundService.EXTRA_WORK_DURATION, duration)
                    putExtra(PomodoroForegroundService.EXTRA_BREAK_DURATION, breakDuration)
                    putExtra(PomodoroForegroundService.EXTRA_LONG_BREAK_DURATION, longBreakDuration)
                    putExtra(PomodoroForegroundService.EXTRA_TODO_TITLE, "Quick Focus")
                    putExtra(PomodoroForegroundService.EXTRA_IS_BREAK, false)
                    putExtra(PomodoroForegroundService.EXTRA_SESSION_NUMBER, sessionNumber)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                prefs.edit()
                    .putBoolean("is_running", true)
                    .putLong("start_time", startTime)
                    .putInt("duration", duration)
                    .putBoolean("is_break", false)
                    .apply()

                updateAllWidgets(context)
            }

            ACTION_STOP -> {
                val stopIntent = Intent(context, PomodoroForegroundService::class.java).apply {
                    action = PomodoroForegroundService.ACTION_STOP
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(stopIntent)
                } else {
                    context.startService(stopIntent)
                }

                val prefs = context.getSharedPreferences("pom_widget_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_running", false)
                    .putBoolean("is_break", false)
                    .apply()

                updateAllWidgets(context)
            }

            ACTION_RESET -> {
                val stopIntent = Intent(context, PomodoroForegroundService::class.java).apply {
                    action = PomodoroForegroundService.ACTION_STOP
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(stopIntent)
                } else {
                    context.startService(stopIntent)
                }

                val prefs = context.getSharedPreferences("pom_widget_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_running", false)
                    .putLong("start_time", 0L)
                    .putInt("duration", WORK_DURATIONS[prefs.getInt("duration_index", 0)])
                    .putBoolean("is_break", false)
                    .putInt("session_number", 0)
                    .apply()

                updateAllWidgets(context)
            }

            ACTION_SKIP -> {
                val prefs = context.getSharedPreferences("pom_widget_prefs", Context.MODE_PRIVATE)
                val isBreak = prefs.getBoolean("is_break", false)

                val stopIntent = Intent(context, PomodoroForegroundService::class.java).apply {
                    action = PomodoroForegroundService.ACTION_STOP
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(stopIntent)
                } else {
                    context.startService(stopIntent)
                }

                if (isBreak) {
                    prefs.edit()
                        .putBoolean("is_running", false)
                        .putBoolean("is_break", false)
                        .apply()
                } else {
                    val sessionNumber = prefs.getInt("session_number", 0) + 1
                    val isLongBreak = sessionNumber % 4 == 0
                    val breakDuration = if (isLongBreak) {
                        LONG_BREAK_DURATIONS[prefs.getInt("duration_index", 0)]
                    } else {
                        BREAK_DURATIONS[prefs.getInt("duration_index", 0)]
                    }

                    val breakStart = System.currentTimeMillis()
                    val serviceIntent = Intent(context, PomodoroForegroundService::class.java).apply {
                        putExtra(PomodoroForegroundService.EXTRA_START_TIME, breakStart)
                        putExtra(PomodoroForegroundService.EXTRA_WORK_DURATION, prefs.getInt("duration", 25))
                        putExtra(PomodoroForegroundService.EXTRA_BREAK_DURATION, breakDuration)
                        putExtra(PomodoroForegroundService.EXTRA_LONG_BREAK_DURATION, LONG_BREAK_DURATIONS[prefs.getInt("duration_index", 0)])
                        putExtra(PomodoroForegroundService.EXTRA_IS_BREAK, true)
                        putExtra(PomodoroForegroundService.EXTRA_SESSION_NUMBER, sessionNumber)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }

                    prefs.edit()
                        .putBoolean("is_running", true)
                        .putLong("start_time", breakStart)
                        .putInt("duration", breakDuration)
                        .putBoolean("is_break", true)
                        .putInt("session_number", sessionNumber)
                        .apply()
                }

                updateAllWidgets(context)
            }

            ACTION_CYCLE_DURATION -> {
                val prefs = context.getSharedPreferences("pom_widget_prefs", Context.MODE_PRIVATE)
                var index = prefs.getInt("duration_index", 0)
                index = (index + 1) % WORK_DURATIONS.size
                prefs.edit().putInt("duration_index", index).apply()

                updateAllWidgets(context)
            }

            ACTION_UPDATE -> {
                updateAllWidgets(context)
            }
        }
    }

    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, PomodoroWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_pomodoro)

        val prefs = context.getSharedPreferences("pom_widget_prefs", Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean("is_running", false)
        val startTime = prefs.getLong("start_time", 0L)
        val duration = prefs.getInt("duration", 25)
        val isBreak = prefs.getBoolean("is_break", false)
        val durationIndex = prefs.getInt("duration_index", 0)
        val sessionNumber = prefs.getInt("session_number", 0)
        val isLongBreakPref = prefs.getBoolean("is_long_break", false)
        val workColor = 0xFF4FD1E8.toInt()

        views.setTextViewText(
            R.id.widget_pom_session_count,
            when {
                !isRunning -> "Pomodoro"
                isBreak -> "Break"
                else -> "Session ${sessionNumber}/4"
            }
        )
        views.setTextViewText(R.id.widget_pom_duration, "${WORK_DURATIONS[durationIndex]} min")

        if (isRunning && startTime > 0) {
            val elapsed = System.currentTimeMillis() - startTime
            val totalMillis = duration * 60 * 1000L
            val remaining = (totalMillis - elapsed).coerceAtLeast(0)
            val minutes = remaining / 60000
            val seconds = (remaining % 60000) / 1000
            val timeStr = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

            views.setTextViewText(R.id.widget_pom_timer, timeStr)

            if (isBreak) {
                views.setTextViewText(
                    R.id.widget_pom_status,
                    if (isLongBreakPref) "Long Break" else "Short Break"
                )
                views.setTextColor(R.id.widget_pom_timer, 0xFF5B9DFF.toInt())
            } else {
                views.setTextViewText(R.id.widget_pom_status, "Focusing...")
                val progress = (elapsed.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
                val fromR = android.graphics.Color.red(workColor).toFloat()
                val fromG = android.graphics.Color.green(workColor).toFloat()
                val fromB = android.graphics.Color.blue(workColor).toFloat()
                val toR = android.graphics.Color.red(0xFF5B9DFF.toInt()).toFloat()
                val toG = android.graphics.Color.green(0xFF5B9DFF.toInt()).toFloat()
                val toB = android.graphics.Color.blue(0xFF5B9DFF.toInt()).toFloat()
                val r = (fromR + (toR - fromR) * progress).toInt()
                val g = (fromG + (toG - fromG) * progress).toInt()
                val b = (fromB + (toB - fromB) * progress).toInt()
                val timerColor = android.graphics.Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
                views.setTextColor(R.id.widget_pom_timer, timerColor)
            }

            views.setViewVisibility(R.id.widget_pom_btn_start, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_pom_btn_skip, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_pom_btn_stop, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_pom_btn_reset, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_pom_duration, android.view.View.GONE)
        } else {
            views.setTextViewText(R.id.widget_pom_timer, String.format(java.util.Locale.US, "%02d:00", WORK_DURATIONS[durationIndex]))
            views.setTextViewText(R.id.widget_pom_status, "Ready to focus")
            views.setTextColor(R.id.widget_pom_timer, android.graphics.Color.WHITE)
            views.setViewVisibility(R.id.widget_pom_btn_start, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_pom_btn_skip, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_pom_btn_stop, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_pom_btn_reset, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_pom_duration, android.view.View.VISIBLE)
        }

        val startIntent = Intent(context, PomodoroWidgetProvider::class.java).apply {
            action = ACTION_START
            component = ComponentName(context, PomodoroWidgetProvider::class.java)
        }
        val startPendingIntent = android.app.PendingIntent.getBroadcast(
            context, 12, startIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_pom_btn_start, startPendingIntent)

        val skipIntent = Intent(context, PomodoroWidgetProvider::class.java).apply {
            action = ACTION_SKIP
            component = ComponentName(context, PomodoroWidgetProvider::class.java)
        }
        val skipPendingIntent = android.app.PendingIntent.getBroadcast(
            context, 15, skipIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_pom_btn_skip, skipPendingIntent)

        val stopIntent = Intent(context, PomodoroWidgetProvider::class.java).apply {
            action = ACTION_STOP
            component = ComponentName(context, PomodoroWidgetProvider::class.java)
        }
        val stopPendingIntent = android.app.PendingIntent.getBroadcast(
            context, 13, stopIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_pom_btn_stop, stopPendingIntent)

        val resetIntent = Intent(context, PomodoroWidgetProvider::class.java).apply {
            action = ACTION_RESET
            component = ComponentName(context, PomodoroWidgetProvider::class.java)
        }
        val resetPendingIntent = android.app.PendingIntent.getBroadcast(
            context, 14, resetIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_pom_btn_reset, resetPendingIntent)

        val durationIntent = Intent(context, PomodoroWidgetProvider::class.java).apply {
            action = ACTION_CYCLE_DURATION
            component = ComponentName(context, PomodoroWidgetProvider::class.java)
        }
        val durationPendingIntent = android.app.PendingIntent.getBroadcast(
            context, 16, durationIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_pom_duration, durationPendingIntent)

        val rootClickIntent = Intent(context, PomodoroWidgetProvider::class.java).apply {
            action = ACTION_UPDATE
            component = ComponentName(context, PomodoroWidgetProvider::class.java)
        }
        val rootPendingIntent = android.app.PendingIntent.getBroadcast(
            context, 20, rootClickIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_pom_root, rootPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
