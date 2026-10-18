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


package io.github.dailytrack.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import io.github.dailytrack.MainActivity
import io.github.dailytrack.SoulTrackApp
import io.github.dailytrack.engine.WeeklyReview
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Weekly "here is what you actually got done" digest.
 *
 * Reporting recorded progress back to the user is the active ingredient of the
 * monitoring effect (Harkin et al., 2015, d+ = 0.40), so once a week - after
 * the working day - the app sends a short, factual summary and one gentle nudge
 * toward next week's most important item.
 */
class WeeklyReviewWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "soultrack_review"
        const val WORK_NAME = "weekly_review"
        private const val PREFS = "soultrack_review_prefs"
        private const val KEY_LAST_SENT_WEEK = "last_sent_week"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            // Runs daily; the worker itself only fires on the review day.
            val request = PeriodicWorkRequestBuilder<WeeklyReviewWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        // Send on Sunday evening (or Monday morning if Sunday was missed), one
        // notification per ISO week.
        val isReviewDay = today.dayOfWeek.value == 7 || (today.dayOfWeek.value == 1 && clockHour() < 12)
        if (!isReviewDay) return Result.success()

        val weekKey = today.with(java.time.DayOfWeek.MONDAY).toString()
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_SENT_WEEK, null) == weekKey) return Result.success()

        val weekStart = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        val db = (applicationContext as SoulTrackApp).database
        val completedTasks = runCatching {
            db.todoDao().getCompletedCountInRange(weekStart, now)
        }.getOrDefault(0)

        val categoryType = runCatching {
            db.categoryDao().getAllCategoriesSync().associate { it.id to it.type }
        }.getOrDefault(emptyMap())

        val sessions = runCatching {
            db.sessionDao().getSessionsInRangeSync(weekStart, now)
        }.getOrDefault(emptyList())

        var productiveMinutes = 0
        val activeDays = mutableSetOf<Long>()
        for (session in sessions) {
            val end = if (session.isActive) now else (session.endTime ?: session.startTime)
            val minutes = ((end - session.startTime).coerceAtLeast(0L) / 60_000L).toInt()
            if (minutes <= 0) continue
            activeDays.add(
                java.time.Instant.ofEpochMilli(session.startTime).atZone(zone).toLocalDate()
                    .toEpochDay()
            )
            val type = categoryType[session.categoryId] ?: session.type
            if (type == "PRODUCTIVE" || type == "LEARNING") productiveMinutes += minutes
        }

        prefs.edit().putString(KEY_LAST_SENT_WEEK, weekKey).apply()
        createChannel()
        sendNotification(
            WeeklyReview.digestLine(completedTasks, productiveMinutes, activeDays.size),
            WeeklyReview.reviewNudge(completedTasks, productiveMinutes)
        )
        return Result.success()
    }

    private fun clockHour(): Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Weekly Review",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "A short weekly summary of what you got done"
            }
            applicationContext.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title: String, message: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            970001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        applicationContext.getSystemService(NotificationManager::class.java)
            .notify(970001, notification)
    }
}
