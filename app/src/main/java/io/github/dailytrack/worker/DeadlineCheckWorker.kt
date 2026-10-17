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
import io.github.dailytrack.data.db.dao.TodoDao
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DeadlineCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "soultrack_deadlines"
        const val WORK_NAME = "deadline_check"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<DeadlineCheckWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        val db = (applicationContext as SoulTrackApp).database
        val todoDao = db.todoDao()

        val now = System.currentTimeMillis()
        val oneHourFromNow = now + 60 * 60 * 1000
        val twentyFourHoursFromNow = now + 24 * 60 * 60 * 1000

        createNotificationChannel()

        val overdueTodos = todoDao.getTodosWithDeadlineApproaching(Long.MIN_VALUE, now)
        for (todo in overdueTodos) {
            if (todo.isCompleted) continue
            val hoursOverdue = ((now - todo.deadline!!) / (60 * 60 * 1000)).toInt()
            sendNotification(
                todoId = todo.id.toInt() + 10000,
                title = "Task Overdue!",
                message = "\"${todo.title}\" was due $hoursOverdue hours ago",
                priority = NotificationCompat.PRIORITY_HIGH
            )
        }

        val urgentTodos = todoDao.getTodosWithDeadlineApproaching(now, oneHourFromNow)
        for (todo in urgentTodos) {
            if (todo.isCompleted) continue
            val minutesUntil = ((todo.deadline!! - now) / (60 * 1000)).toInt()
            sendNotification(
                todoId = todo.id.toInt() + 20000,
                title = "Deadline in $minutesUntil minutes",
                message = "\"${todo.title}\" is due very soon!",
                priority = NotificationCompat.PRIORITY_HIGH
            )
        }

        val upcomingTodos = todoDao.getTodosWithDeadlineApproaching(oneHourFromNow, twentyFourHoursFromNow)
        for (todo in upcomingTodos) {
            if (todo.isCompleted) continue
            val hoursUntil = ((todo.deadline!! - now) / (60 * 60 * 1000)).toInt()
            val priorityLabel = when (todo.priority) {
                3 -> " [HIGH]"
                2 -> " [MED]"
                else -> ""
            }
            sendNotification(
                todoId = todo.id.toInt() + 30000,
                title = "Task Due in $hoursUntil hours$priorityLabel",
                message = "\"${todo.title}\" deadline approaching",
                priority = NotificationCompat.PRIORITY_DEFAULT
            )
        }

        notifyImplementationIntentions(todoDao, now)

        return Result.success()
    }

    /**
     * Fires the "if-then" cue for tasks the user planned for this hour. Cue +
     * response planning is one of the best-evidenced ways to close the gap
     * between intending to do something and actually starting it.
     */
    private suspend fun notifyImplementationIntentions(todoDao: TodoDao, now: Long) {
        val minute = Calendar.getInstance().get(Calendar.MINUTE)
        // This worker runs every 15 minutes; announcing in the first slot keeps
        // a cue to exactly one notification per hour.
        if (minute >= 15) return
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val prefs = applicationContext.getSharedPreferences("soultrack_cue_prefs", Context.MODE_PRIVATE)
        for (todo in todoDao.getTodosWithTriggerHour(hour)) {
            val key = "cue_${todo.id}_$dateKey"
            if (prefs.getBoolean(key, false)) continue
            prefs.edit().putBoolean(key, true).apply()
            sendNotification(
                todoId = todo.id.toInt() + 40000,
                title = "Time to start: ${todo.title}",
                message = if (todo.triggerText.isNotBlank()) {
                    "If ${todo.triggerText}, then this is your cue."
                } else {
                    "Your planned cue time has arrived."
                },
                priority = NotificationCompat.PRIORITY_DEFAULT
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Deadlines",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for approaching and overdue task deadlines"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(todoId: Int, title: String, message: String, priority: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TODO_ID", (todoId - 30000).toLong())
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            todoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.notify(todoId, notification)
    }
}
