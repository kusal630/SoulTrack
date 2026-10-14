package io.github.dailytrack.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.NotificationManager.Policy
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import io.github.dailytrack.MainActivity
import io.github.dailytrack.SoulTrackApp
import io.github.dailytrack.data.db.entity.SessionEntity
import io.github.dailytrack.widget.PomodoroWidgetProvider
import io.github.dailytrack.widget.TimerWidgetProvider
import kotlinx.coroutines.*

class PomodoroForegroundService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var sedentaryJob: Job? = null
    private var notificationManager: NotificationManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var workDurationMinutes: Int = 25
    private var breakDurationMinutes: Int = 5
    private var longBreakDurationMinutes: Int = 15
    private var todoTitle: String = ""
    private var todoId: Long? = null
    private var categoryId: Long? = null
    private var currentSessionNumber: Int = 0
    private var isBreak: Boolean = false
    private var isLongBreak: Boolean = false
    private var startTime: Long = 0L
    private var workColor: Int = 0xFF4FD1E8.toInt()
    private var transitioning: Boolean = false
    private var stopped: Boolean = false

    companion object {
        const val CHANNEL_WORK = "soultrack_work"
        const val CHANNEL_BREAK = "soultrack_break"
        const val CHANNEL_SEDENTARY = "soultrack_sedentary"
        const val CHANNEL_TRANSITION = "soultrack_transition"
        const val NOTIFICATION_ID = 1002
        const val SEDENTARY_NOTIFICATION_ID = 1003
        const val TRANSITION_NOTIFICATION_ID = 1005
        const val COMPLETION_NOTIFICATION_ID = 1006

        const val ACTION_STOP = "io.github.dailytrack.STOP_POMODORO"
        const val ACTION_COMPLETE_WORK = "io.github.dailytrack.COMPLETE_POMODORO_WORK"
        const val ACTION_COMPLETE_BREAK = "io.github.dailytrack.COMPLETE_POMODORO_BREAK"
        const val ACTION_START_NEW = "io.github.dailytrack.START_NEW_POMODORO"

        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_WORK_DURATION = "work_duration"
        const val EXTRA_BREAK_DURATION = "break_duration"
        const val EXTRA_TODO_TITLE = "todo_title"
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_CATEGORY_ID = "category_id"
        const val EXTRA_WORK_COLOR = "work_color"
        const val EXTRA_SESSION_NUMBER = "session_number"
        const val EXTRA_IS_BREAK = "is_break"
        const val EXTRA_IS_LONG_BREAK = "is_long_break"
        const val EXTRA_LONG_BREAK_DURATION = "long_break_duration"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        notificationManager = getSystemService(NotificationManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAll()
                return START_NOT_STICKY
            }
            ACTION_COMPLETE_WORK -> {
                onWorkSessionComplete()
                return START_STICKY
            }
            ACTION_COMPLETE_BREAK -> {
                onBreakComplete()
                return START_STICKY
            }
            ACTION_START_NEW -> {
                currentSessionNumber = 0
                isBreak = false
                isLongBreak = false
                startWorkSession(
                    workDuration = intent.getIntExtra(EXTRA_WORK_DURATION, workDurationMinutes),
                    title = intent.getStringExtra(EXTRA_TODO_TITLE) ?: todoTitle,
                    startTime = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())
                )
                return START_STICKY
            }
        }

        if (intent == null) {
            return if (restoreFromPrefs()) START_STICKY else START_NOT_STICKY
        }

        workDurationMinutes = intent.getIntExtra(EXTRA_WORK_DURATION, 25)
        breakDurationMinutes = intent.getIntExtra(EXTRA_BREAK_DURATION, 5)
        longBreakDurationMinutes = intent.getIntExtra(EXTRA_LONG_BREAK_DURATION, 15)
        todoTitle = intent.getStringExtra(EXTRA_TODO_TITLE) ?: ""
        todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L).takeIf { it > 0 }
        categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1L).takeIf { it > 0 }
        workColor = intent.getIntExtra(EXTRA_WORK_COLOR, 0xFF4FD1E8.toInt())
        currentSessionNumber = intent.getIntExtra(EXTRA_SESSION_NUMBER, 0)
        val requestedBreak = intent.getBooleanExtra(EXTRA_IS_BREAK, false)
        val requestedLongBreak = intent.getBooleanExtra(EXTRA_IS_LONG_BREAK, false)
        val requestedStartTime = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())

        if (requestedBreak) {
            startBreakSession(
                durationMinutes = intent.getIntExtra(EXTRA_BREAK_DURATION, breakDurationMinutes),
                isLong = requestedLongBreak,
                startTime = requestedStartTime
            )
        } else {
            startWorkSession(workDurationMinutes, todoTitle, requestedStartTime)
        }
        return START_STICKY
    }

    private fun restoreFromPrefs(): Boolean {
        val prefs = getSharedPreferences("pom_widget_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("is_running", false)) {
            stopSelf()
            return false
        }
        stopped = false
        transitioning = false
        isBreak = prefs.getBoolean("is_break", false)
        isLongBreak = prefs.getBoolean("is_long_break", false)
        currentSessionNumber = prefs.getInt("session_number", 0)
        workDurationMinutes = prefs.getInt("work_duration", 25)
        breakDurationMinutes = prefs.getInt("break_duration", 5)
        longBreakDurationMinutes = prefs.getInt("long_break_duration", 15)
        workColor = prefs.getInt("work_color", 0xFF4FD1E8.toInt())
        todoTitle = prefs.getString("todo_title", "") ?: ""
        startTime = prefs.getLong("start_time", 0L)
        if (startTime <= 0L) {
            stopSelf()
            return false
        }
        val totalMillis = (if (isBreak) breakDurationMinutes else workDurationMinutes) * 60_000L
        if (System.currentTimeMillis() - startTime > totalMillis + 60_000L) {
            finishActivePomodoros(System.currentTimeMillis())
            stopAll()
            return false
        }
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        startNotificationUpdates()
        PomodoroWidgetProvider.updateWidgets(this)
        return true
    }

    private fun startWorkSession(workDuration: Int, title: String, startTime: Long = System.currentTimeMillis()) {
        timerJob?.cancel()
        sedentaryJob?.cancel()
        stopped = false
        isBreak = false
        isLongBreak = false
        workDurationMinutes = workDuration
        todoTitle = title
        this.startTime = startTime

        insertActivePomodoroRow(type = "WORK", durationMinutes = workDuration, startTime = startTime)
        startTrackingSession(title, startTime)
        enableDnd()
        acquireWakeLock()
        playTransitionSound(isWork = true)
        showTransitionNotification(
            "Focus session ${currentSessionNumber + 1} started",
            "Stay focused for $workDuration minutes"
        )

        updatePrefs()
        promoteToForeground()
        startNotificationUpdates()
        PomodoroWidgetProvider.updateWidgets(this)
    }

    private fun startBreakSession(
        durationMinutes: Int,
        isLong: Boolean,
        startTime: Long = System.currentTimeMillis()
    ) {
        timerJob?.cancel()
        sedentaryJob?.cancel()
        stopped = false
        isBreak = true
        isLongBreak = isLong
        breakDurationMinutes = durationMinutes
        this.startTime = startTime

        stopTrackingSession()
        insertActivePomodoroRow(type = "BREAK", durationMinutes = durationMinutes, startTime = startTime)
        disableDnd()
        acquireWakeLock()
        playTransitionSound(isWork = false)
        val breakLabel = if (isLong) "Long break" else "Short break"
        showBreakQuoteNotification("$breakLabel started", "Relax for $durationMinutes minutes")

        updatePrefs()
        promoteToForeground()
        startNotificationUpdates()
        scheduleSedentaryReminder()
        scheduleEyeBlinkReminder()
        PomodoroWidgetProvider.updateWidgets(this)
    }

    private fun promoteToForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun insertActivePomodoroRow(type: String, durationMinutes: Int, startTime: Long) {
        val db = (applicationContext as SoulTrackApp).database
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                db.pomodoroSessionDao().insert(
                    io.github.dailytrack.data.db.entity.PomodoroSessionEntity(
                        todoId = todoId,
                        categoryId = categoryId,
                        startTime = startTime,
                        durationMinutes = durationMinutes,
                        type = type,
                        isCompleted = false
                    )
                )
            }
        }
    }

    private fun finishActivePomodoros(now: Long) {
        val db = (applicationContext as SoulTrackApp).database
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val active = db.pomodoroSessionDao().getAllActivePomodorosSync()
                for (pomodoro in active) {
                    val plannedEnd = pomodoro.startTime + pomodoro.durationMinutes * 60_000L
                    val naturalCompletion = now >= plannedEnd
                    val end = if (naturalCompletion) plannedEnd else now
                    db.pomodoroSessionDao().completePomodoro(pomodoro.id, end)
                    if (pomodoro.type == "WORK") {
                        creditTodo(pomodoro, end, naturalCompletion, db)
                    }
                }
            }
        }
    }

    private suspend fun creditTodo(
        pomodoro: io.github.dailytrack.data.db.entity.PomodoroSessionEntity,
        endTime: Long,
        naturalCompletion: Boolean,
        db: io.github.dailytrack.data.db.AppDatabase
    ) {
        val linkedTodoId = pomodoro.todoId ?: return
        val todo = db.todoDao().getTodoById(linkedTodoId) ?: return
        val actualMinutes = ((endTime - pomodoro.startTime) / 60_000L).toInt().coerceAtLeast(1)
        val countDelta = if (naturalCompletion) 1 else 0
        db.todoDao().update(
            todo.copy(
                actualMinutes = todo.actualMinutes + actualMinutes,
                pomodoroCount = todo.pomodoroCount + countDelta,
                updatedAt = endTime
            )
        )
    }

    private fun onWorkSessionComplete() {
        if (transitioning || stopped) return
        transitioning = true
        timerJob?.cancel()
        sedentaryJob?.cancel()
        playCompletionVibration()

        val db = (applicationContext as SoulTrackApp).database
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val now = System.currentTimeMillis()
                val active = db.pomodoroSessionDao().getActivePomodoroSync()
                if (active != null && active.type == "WORK") {
                    val plannedEnd = active.startTime + active.durationMinutes * 60_000L
                    val end = maxOf(active.startTime, minOf(now, plannedEnd))
                    db.pomodoroSessionDao().completePomodoro(active.id, end)
                    creditTodo(active, end, naturalCompletion = true, db = db)
                }
            }
        }

        currentSessionNumber++
        showCompletionNotification(
            "Session $currentSessionNumber completed!",
            "Great work! Time for a break."
        )

        val longBreak = currentSessionNumber % 4 == 0
        startBreakSession(
            durationMinutes = if (longBreak) longBreakDurationMinutes else breakDurationMinutes,
            isLong = longBreak
        )
        transitioning = false
    }

    private fun onBreakComplete() {
        if (transitioning || stopped) return
        transitioning = true
        timerJob?.cancel()
        sedentaryJob?.cancel()
        playCompletionVibration()

        val db = (applicationContext as SoulTrackApp).database
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val now = System.currentTimeMillis()
                val active = db.pomodoroSessionDao().getActivePomodoroSync()
                if (active != null && active.type == "BREAK") {
                    val plannedEnd = active.startTime + active.durationMinutes * 60_000L
                    db.pomodoroSessionDao().completePomodoro(active.id, minOf(now, plannedEnd))
                }
            }
        }

        showCompletionNotification(
            "Break ended",
            "Ready for the next focus session?"
        )

        if (isLongBreak) currentSessionNumber = 0
        startWorkSession(workDurationMinutes, todoTitle)
        transitioning = false
    }

    private fun stopAll() {
        stopped = true
        transitioning = false
        timerJob?.cancel()
        sedentaryJob?.cancel()
        finishActivePomodoros(System.currentTimeMillis())
        stopTrackingSession()
        disableDnd()
        releaseWakeLock()
        notificationManager?.cancel(NOTIFICATION_ID)
        notificationManager?.cancel(SEDENTARY_NOTIFICATION_ID)
        notificationManager?.cancel(TRANSITION_NOTIFICATION_ID)
        notificationManager?.cancel(COMPLETION_NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        PomodoroWidgetProvider.updateWidgets(this)
    }

    /**
     * Mirrors the running pomodoro into one session row.
     *
     * The pomodoro record itself is only the plan (how long it was meant to
     * run). The session row is the single source of truth for *time*, so the
     * ring, growth, loops and insights all read the same minutes and nothing
     * is ever counted twice.
     */
    private fun startTrackingSession(title: String, sessionStart: Long = System.currentTimeMillis()) {
        val db = (applicationContext as SoulTrackApp).database
        CoroutineScope(Dispatchers.IO).launch {
            db.sessionDao().deactivateAllSessions(sessionStart)

            val deepWorkCat = db.categoryDao().getIdByName("Deep Work")
            val catId = deepWorkCat ?: db.categoryDao().getIdByName("Productive")
            val catType = "PRODUCTIVE"

            // A pomodoro started from a task stays attached to it; one
            // started from a bare title still becomes a task of its own, so
            // the work shows up under Tasks and counts toward the day ring.
            val linkedTodoId = when {
                todoId != null && todoId!! > 0L -> todoId
                title.isNotBlank() && title != "Pomodoro Focus" -> {
                    db.todoDao().insert(
                        io.github.dailytrack.data.db.entity.TodoEntity(
                            title = title,
                            categoryId = catId,
                            createdAt = sessionStart,
                            updatedAt = sessionStart
                        )
                    )
                }
                else -> null
            }

            val session = SessionEntity(
                title = title.ifBlank { "Pomodoro Focus" },
                categoryId = catId,
                todoId = linkedTodoId,
                type = catType,
                startTime = sessionStart,
                isActive = true,
                source = "POMODORO",
                timezoneId = java.time.ZoneId.systemDefault().id
            )
            val sessionId = db.sessionDao().insert(session)

            val state = TimerStateStore.TimerState(
                isRunning = true,
                startTime = sessionStart,
                sessionId = sessionId,
                title = title.ifBlank { "Pomodoro Focus" },
                categoryName = "Deep Work",
                categoryType = "PRODUCTIVE",
                source = "POMODORO",
                isPomodoro = true,
                autoDnd = true
            )
            TimerStateStore.publish(this@PomodoroForegroundService, state)
            withContext(Dispatchers.Main) {
                TimerStateStore.startTimerService(this@PomodoroForegroundService, state)
            }
        }
    }

    private fun stopTrackingSession() {
        val db = (applicationContext as SoulTrackApp).database
        val stored = TimerStateStore.load(this)
        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            if (stored.sessionId >= 0L) {
                db.sessionDao().stopSessionById(stored.sessionId, now)
            } else {
                db.sessionDao().stopActiveSession(now)
            }
        }

        TimerStateStore.requestStop(this, stored.sessionId, isPomodoro = true)
        TimerStateStore.clear(this)
        runCatching { TimerWidgetProvider.updateWidgets(this) }
    }

    private fun acquireWakeLock() {
        releaseWakeLock()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "soultrack:pomodoro_transition"
        ).apply {
            acquire(10_000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun enableDnd() {
        FocusDndManager.enable(this)
    }

    private fun disableDnd() {
        FocusDndManager.restore(this)
    }

    private fun playTransitionSound(isWork: Boolean) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java) as Vibrator
        }

        val pattern = if (isWork) {
            longArrayOf(0, 300, 100, 300, 100, 500)
        } else {
            longArrayOf(0, 200, 100, 200, 100, 300)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }

        val sound = if (isWork) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        try {
            val ringtone = RingtoneManager.getRingtone(this, sound)
            ringtone?.play()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { ringtone?.stop() } catch (_: Exception) {}
            }, 3000)
        } catch (_: Exception) {}
    }

    private fun playCompletionVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java) as Vibrator
        }

        val pattern = longArrayOf(0, 400, 100, 400, 100, 400, 100, 600)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun showTransitionNotification(title: String, text: String) {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_TRANSITION)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        notificationManager?.notify(TRANSITION_NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(title: String, text: String) {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_TRANSITION)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        notificationManager?.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun showBreakQuoteNotification(title: String, text: String) {
        val breakQuotes = listOf(
            "Rest when you're weary. Refresh and renewal await.",
            "A short rest fuels a powerful comeback.",
            "Step back to leap forward.",
            "The pause between notes makes the music.",
            "Even the sun rests before rising again.",
            "Breathe. You've earned this moment.",
            "Stillness is not laziness \u2014 it's strategy.",
            "Recharge now, conquer later.",
            "Great things take breaks too.",
            "Your mind processes miracles in silence."
        )

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val quote = breakQuotes.random()
        val notification = NotificationCompat.Builder(this, CHANNEL_TRANSITION)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$quote\n\n$text")
                    .setSummaryText(text)
            )
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        notificationManager?.notify(TRANSITION_NOTIFICATION_ID, notification)
    }

    private fun startNotificationUpdates() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                if (!stopped) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val totalMillis =
                        (if (isBreak) breakDurationMinutes else workDurationMinutes) * 60_000L
                    if (elapsed >= totalMillis) {
                        if (isBreak) onBreakComplete() else onWorkSessionComplete()
                        return@launch
                    }
                }
                val notification = buildNotification()
                notificationManager?.notify(NOTIFICATION_ID, notification)
                PomodoroWidgetProvider.updateWidgets(this@PomodoroForegroundService)
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

        val action = if (isBreak) ACTION_COMPLETE_BREAK else ACTION_COMPLETE_WORK
        val completeIntent = PendingIntent.getService(
            this, 2,
            Intent(this, PomodoroForegroundService::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 3,
            Intent(this, PomodoroForegroundService::class.java).apply { this.action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(0L)
        val totalMillis = (if (isBreak) breakDurationMinutes else workDurationMinutes) * 60 * 1000L
        val remaining = (totalMillis - elapsed).coerceAtLeast(0)
        val minutes = remaining / 60000
        val seconds = (remaining % 60000) / 1000
        val timeStr = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

        val channel = if (isBreak) CHANNEL_BREAK else CHANNEL_WORK
        val title = when {
            isBreak && isLongBreak -> "Long Break"
            isBreak -> "Short Break"
            else -> "Focus Session ${currentSessionNumber + 1}"
        }
        val subtitle = when {
            isBreak -> "Relax before next session"
            todoTitle.isNotBlank() -> todoTitle
            else -> "Stay focused!"
        }

        val sessionLabel = if (isBreak) {
            "Break (${currentSessionNumber}/4)"
        } else {
            "Session ${currentSessionNumber + 1}/4"
        }

        return NotificationCompat.Builder(this, channel)
            .setContentTitle("$title: $timeStr")
            .setContentText(subtitle)
            .setSubText(sessionLabel)
            .setSmallIcon(if (isBreak) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setUsesChronometer(false)
            .setShowWhen(false)
            .setColor(if (isBreak) 0xFF5B9DFF.toInt() else workColor)
            .setColorized(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                if (isBreak) "Skip Break" else "Complete",
                completeIntent
            )
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updatePrefs() {
        val prefs = getSharedPreferences("pom_widget_prefs", MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_running", true)
            .putLong("start_time", startTime)
            .putInt("duration", if (isBreak) breakDurationMinutes else workDurationMinutes)
            .putBoolean("is_break", isBreak)
            .putInt("work_color", workColor)
            .putInt("session_number", currentSessionNumber)
            .putBoolean("is_long_break", isLongBreak)
            .putInt("work_duration", workDurationMinutes)
            .putInt("break_duration", breakDurationMinutes)
            .putInt("long_break_duration", longBreakDurationMinutes)
            .putString("todo_title", todoTitle)
            .apply()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val workChannel = NotificationChannel(
                CHANNEL_WORK, "Focus Sessions", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active focus session timer"
                setShowBadge(false)
                lightColor = 0xFF4FD1E8.toInt()
                enableLights(true)
            }

            val breakChannel = NotificationChannel(
                CHANNEL_BREAK, "Break Timer", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active break timer"
                setShowBadge(false)
                lightColor = 0xFF5B9DFF.toInt()
                enableLights(true)
            }

            val sedentaryChannel = NotificationChannel(
                CHANNEL_SEDENTARY, "Movement Reminders", NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to stand and stretch during breaks"
                setShowBadge(true)
                lightColor = 0xFF4CAF50.toInt()
                enableLights(true)
                enableVibration(true)
            }

            val transitionChannel = NotificationChannel(
                CHANNEL_TRANSITION, "Session Transitions", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when sessions start or end"
                setShowBadge(true)
                lightColor = 0xFF4FD1E8.toInt()
                enableLights(true)
                enableVibration(true)
            }

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(workChannel)
            nm.createNotificationChannel(breakChannel)
            nm.createNotificationChannel(sedentaryChannel)
            nm.createNotificationChannel(transitionChannel)
        }
    }

    private fun scheduleSedentaryReminder() {
        sedentaryJob?.cancel()
        sedentaryJob = scope.launch {
            delay(30_000L)
            if (isActive && isBreak) {
                showSedentaryNotification()
            }
        }
    }

    private fun showSedentaryNotification() {
        val tips = listOf(
            "Stand up and stretch your arms above your head!",
            "Take a walk around the room for 2 minutes",
            "Do 10 squats to get your blood flowing",
            "Stretch your neck: roll it slowly in circles",
            "Touch your toes - hold for 15 seconds",
            "Do some calf raises to activate your legs",
            "Open a window and take 5 deep breaths",
            "Shake out your hands and wrists"
        )

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_SEDENTARY)
            .setContentTitle("Time to Move!")
            .setContentText(tips.random())
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager?.notify(SEDENTARY_NOTIFICATION_ID, notification)
    }

    private fun scheduleEyeBlinkReminder() {
        scope.launch {
            delay(20_000L)
            if (isActive && isBreak) {
                showEyeBlinkNotification()
            }
        }
    }

    private fun showEyeBlinkNotification() {
        val tips = listOf(
            "Blink your eyes 20 times to reduce dryness and strain.",
            "Close your eyes for 10 seconds, then focus on something far away.",
            "Look at something 20 feet away for 20 seconds (20-20-20 rule).",
            "Gently press your closed eyelids for 5 seconds, then release.",
            "Roll your eyes clockwise 5 times, then counter-clockwise 5 times.",
            "Place your palms over your eyes for 30 seconds of darkness rest.",
            "Focus on a distant object for 15 seconds, then something close. Repeat 3 times."
        )

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_SEDENTARY)
            .setContentTitle("Eye Care Break")
            .setContentText(tips.random())
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager?.notify(SEDENTARY_NOTIFICATION_ID + 20, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopped = true
        timerJob?.cancel()
        sedentaryJob?.cancel()
        disableDnd()
        releaseWakeLock()
        val prefs = getSharedPreferences("pom_widget_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("is_running", false).apply()
        notificationManager?.cancel(NOTIFICATION_ID)
        notificationManager?.cancel(SEDENTARY_NOTIFICATION_ID)
        notificationManager?.cancel(TRANSITION_NOTIFICATION_ID)
        scope.cancel()
        super.onDestroy()
    }
}
