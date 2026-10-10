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


package io.github.dailytrack.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.github.dailytrack.MainActivity
import io.github.dailytrack.SoulTrackApp
import io.github.dailytrack.widget.TimerWidgetProvider
import kotlinx.coroutines.*

class TimerForegroundService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var startTime: Long = 0L
    private var sessionTitle: String = ""
    private var categoryName: String = ""
    private var categoryType: String = ""
    private var sedentaryJob: Job? = null
    private var sedentaryReminderCount: Int = 0
    private var isPomodoroSession: Boolean = false
    private var wastedWarningJob: Job? = null
    private var breakReminderJob: Job? = null
    private var notificationManager: NotificationManager? = null
    private var timerJob: Job? = null

    companion object {
        const val CHANNEL_ID = "soultrack_timer"
        const val CHANNEL_SEDENTARY = "soultrack_sedentary"
        const val NOTIFICATION_ID = 1001
        const val SEDENTARY_NOTIFICATION_ID = 1004
        const val ACTION_STOP = "io.github.dailytrack.STOP_TIMER"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_TITLE = "title"
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_CATEGORY_TYPE = "category_type"
        const val EXTRA_IS_POMODORO = "is_pomodoro"

        fun getCategoryColor(type: String, isWaste: Boolean = false): Int {
            if (isWaste) return 0xFFF44336.toInt()
            return when (type) {
                "PRODUCTIVE", "LEARNING" -> 0xFF4CAF50.toInt()
                "EXERCISE" -> 0xFF2196F3.toInt()
                "SLEEP" -> 0xFF9C27B0.toInt()
                "SOCIAL" -> 0xFFFF9800.toInt()
                "RECOVERY", "REST" -> 0xFF607D8B.toInt()
                "WASTED" -> 0xFFF44336.toInt()
                else -> 0xFF757575.toInt()
            }
        }

        fun getCategoryLabel(type: String, isWaste: Boolean = false): String {
            if (isWaste) return "Waste"
            return when (type) {
                "PRODUCTIVE", "LEARNING" -> "Productive"
                "EXERCISE" -> "Exercise"
                "SLEEP" -> "Sleep"
                "SOCIAL" -> "Social"
                "RECOVERY", "REST" -> "Rest"
                "WASTED" -> "Waste"
                else -> "Activity"
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notificationManager = getSystemService(NotificationManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                timerJob?.cancel()
                sedentaryJob?.cancel()
                wastedWarningJob?.cancel()
                breakReminderJob?.cancel()
                val prefs = getSharedPreferences("widget_prefs", MODE_PRIVATE)
                prefs.edit().putBoolean("is_running", false).putLong("session_id", -1L).apply()
                closeActiveSession()
                if (isPomodoroSession) {
                    val stopPomodoro = Intent(this, PomodoroForegroundService::class.java)
                        .setAction(PomodoroForegroundService.ACTION_STOP)
                    runCatching { startService(stopPomodoro) }
                }
                notificationManager?.cancel(NOTIFICATION_ID)
                notificationManager?.cancel(SEDENTARY_NOTIFICATION_ID)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                TimerWidgetProvider.updateWidgets(this)
                return START_NOT_STICKY
            }
        }

        val prefs = getSharedPreferences("widget_prefs", MODE_PRIVATE)
        if (intent == null) {
            startTime = prefs.getLong("start_time", 0L).takeIf { it > 0L } ?: System.currentTimeMillis()
            sessionTitle = prefs.getString("session_name", null) ?: "Active Session"
            categoryName = prefs.getString("category_name", "") ?: ""
            categoryType = prefs.getString("category_type", "") ?: ""
        } else {
            startTime = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())
            sessionTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Active Session"
            categoryName = intent.getStringExtra(EXTRA_CATEGORY) ?: ""
            categoryType = intent.getStringExtra(EXTRA_CATEGORY_TYPE) ?: ""
            isPomodoroSession = intent.getBooleanExtra(EXTRA_IS_POMODORO, false)
        }

        prefs.edit()
            .putBoolean("is_running", true)
            .putLong("start_time", startTime)
            .putString("session_name", sessionTitle)
            .putString("category_name", categoryName)
            .putString("category_type", categoryType)
            .apply()

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        startNotificationUpdates()
        if (!isPomodoroSession) {
            scheduleSedentaryReminder()
            if (categoryType in listOf("PRODUCTIVE", "LEARNING", "EXERCISE")) {
                scheduleBreakReminder()
            }
        }
        if (categoryType == "WASTED") {
            scheduleWastedWarning()
        }
        TimerWidgetProvider.updateWidgets(this)
        return START_STICKY
    }

    private fun closeActiveSession() {
        val db = (applicationContext as SoulTrackApp).database
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { db.sessionDao().stopActiveSession(System.currentTimeMillis()) }
        }
    }

    private fun startNotificationUpdates() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                val notification = buildNotification()
                notificationManager?.notify(NOTIFICATION_ID, notification)
                TimerWidgetProvider.updateWidgets(this@TimerForegroundService)
                delay(1000)
            }
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TimerForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(0L)
        val hours = elapsed / 3600000
        val minutes = (elapsed % 3600000) / 60000
        val seconds = (elapsed % 60000) / 1000
        val timeStr = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

        val catColor = getCategoryColor(categoryType)
        val catLabel = getCategoryLabel(categoryType)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(sessionTitle.ifBlank { "Active Session" })
            .setContentText("Running: $timeStr")
            .setSubText(catLabel)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setUsesChronometer(false)
            .setShowWhen(false)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(catColor)
            .setStyle(
                NotificationCompat.DecoratedCustomViewStyle()
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active timer session"
                setShowBadge(false)
            }
            val sedentaryChannel = NotificationChannel(
                CHANNEL_SEDENTARY,
                "Movement Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to take breaks and stay active"
                setShowBadge(true)
                lightColor = 0xFF4CAF50.toInt()
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(sedentaryChannel)
        }
    }

    private fun scheduleSedentaryReminder() {
        sedentaryJob?.cancel()
        sedentaryReminderCount = 0
        sedentaryJob = scope.launch {
            while (isActive) {
                delay(25 * 60 * 1000L)
                if (!isActive) break
                sedentaryReminderCount++
                showSedentaryNotification()
            }
        }
    }

    private fun showSedentaryNotification() {
        val isProductive = categoryType in listOf("PRODUCTIVE", "LEARNING")
        val tips = if (isProductive) {
            listOf(
                "You've been focused for ${sedentaryReminderCount * 25} min! Stand up and stretch your back.",
                "Quick break: Do 10 jumping jacks to reset your focus.",
                "Eye strain break: Look at something 20 feet away for 20 seconds.",
                "Walk around for 2 minutes - your brain will thank you.",
                "Stretch your wrists and fingers to prevent strain.",
                "Stand up and do 5 shoulder rolls each direction.",
                "Deep breath time: Inhale 4 sec, hold 4, exhale 6. Repeat 3 times.",
                "Walk to get some water - hydration helps focus."
            )
        } else {
            listOf(
                "Time to get moving! Stand up and stretch for 2 minutes.",
                "Active break: Do 10 squats or walk around the room.",
                "Shake out your body - get the blood flowing!",
                "Quick stretch: Reach for the ceiling, then touch your toes.",
                "Take a walk outside if you can - fresh air helps!",
                "Do some neck rolls to release tension.",
                "Stand up and march in place for 1 minute.",
                "Stretch your hips: Stand and lift each knee to chest."
            )
        }
        val tip = tips.random()

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_SEDENTARY)
            .setContentTitle("Time to Move!")
            .setContentText(tip)
            .setStyle(NotificationCompat.BigTextStyle().bigText(tip))
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager?.notify(SEDENTARY_NOTIFICATION_ID, notification)
    }

    private fun scheduleWastedWarning() {
        wastedWarningJob?.cancel()
        wastedWarningJob = scope.launch {
            delay(5 * 60 * 1000L)
            if (!isActive) return@launch
            showWastedWarning()
        }
    }

    private fun showWastedWarning() {
        val warnings = listOf(
            "Time is being wasted. You can be better than this!",
            "Every minute counts. Switch to something meaningful!",
            "This time won't come back. Choose wisely!",
            "You're capable of so much more. Get back on track!",
            "Don't let this moment slip. Do something that matters!",
            "Your future self will thank you for stopping now.",
            "Progress or regret \u2014 the choice is yours.",
            "Small changes lead to big results. Start now!"
        )

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_SEDENTARY)
            .setContentTitle("Time Check")
            .setContentText(warnings.random())
            .setStyle(NotificationCompat.BigTextStyle().bigText(warnings.random()))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager?.notify(SEDENTARY_NOTIFICATION_ID + 10, notification)
    }

    private fun scheduleBreakReminder() {
        breakReminderJob?.cancel()
        breakReminderJob = scope.launch {
            delay(45 * 60 * 1000L)
            if (!isActive) return@launch
            showBreakReminder()
        }
    }

    private fun showBreakReminder() {
        val elapsed = (System.currentTimeMillis() - startTime) / 60000
        val reminders = listOf(
            "You've been working for $elapsed minutes. Time for a break!",
            "Great focus session! Consider taking a short break to recharge.",
            "You've been at it for a while. A quick break boosts productivity.",
            "Stretch break time! Your body and mind will thank you.",
            "You've earned a break. Step away for a few minutes.",
            "Long focus sessions reduce quality. Take a 5-10 minute break."
        )

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_SEDENTARY)
            .setContentTitle("Take a Break!")
            .setContentText(reminders.random())
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminders.random()))
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager?.notify(SEDENTARY_NOTIFICATION_ID + 30, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timerJob?.cancel()
        sedentaryJob?.cancel()
        wastedWarningJob?.cancel()
        breakReminderJob?.cancel()
        val prefs = getSharedPreferences("widget_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("is_running", false).apply()
        notificationManager?.cancel(NOTIFICATION_ID)
        notificationManager?.cancel(SEDENTARY_NOTIFICATION_ID)
        scope.cancel()
        super.onDestroy()
    }
}
