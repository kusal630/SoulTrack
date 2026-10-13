package io.github.dailytrack.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import io.github.dailytrack.R
import io.github.dailytrack.SoulTrackApp
import io.github.dailytrack.data.api.QuotesApi
import io.github.dailytrack.data.db.entity.SessionEntity
import io.github.dailytrack.service.TimerForegroundService
import io.github.dailytrack.service.TimerStateStore
import kotlinx.coroutines.*

class TimerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_START = "io.github.dailytrack.ACTION_WIDGET_START"
        const val ACTION_STOP = "io.github.dailytrack.ACTION_WIDGET_STOP"
        const val ACTION_RESET = "io.github.dailytrack.ACTION_WIDGET_RESET"
        const val ACTION_UPDATE = "io.github.dailytrack.ACTION_WIDGET_UPDATE"
        const val ACTION_CYCLE_CATEGORY = "io.github.dailytrack.ACTION_CYCLE_CATEGORY"
        const val ACTION_NEXT_QUOTE = "io.github.dailytrack.ACTION_NEXT_QUOTE"

        private val widgetCategories = listOf(
            Pair("Productive", "PRODUCTIVE"),
            Pair("Learning", "LEARNING"),
            Pair("Exercise", "EXERCISE"),
            Pair("Social", "SOCIAL"),
            Pair("Wasted", "WASTED")
        )

        fun updateWidgets(context: Context) {
            val intent = Intent(context, TimerWidgetProvider::class.java).apply {
                action = ACTION_UPDATE
                component = ComponentName(context, TimerWidgetProvider::class.java)
            }
            context.sendBroadcast(intent)
        }

        /** Pushes a quote straight into the widget (used on every app launch). */
        fun showQuote(context: Context, text: String, author: String) {
            context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE).edit()
                .putString("widget_quote", text)
                .putString("widget_quote_author", author)
                .apply()
            updateWidgets(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (!TimerStateStore.isServiceAlive()) {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                TimerStateStore.reconcile(context)
                withContext(Dispatchers.Main) {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            }
            return
        }
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_START -> {
                val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                val categoryIndex = prefs.getInt("category_index", 0)
                val (catName, catType) = widgetCategories[categoryIndex]

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = context.checkSelfPermission(
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPermission) return
                }

                val db = (context.applicationContext as SoulTrackApp).database
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    val alreadyRunning = db.sessionDao().getActiveSessionSync()
                    if (alreadyRunning != null) {
                        TimerStateStore.reconcile(context)
                        withContext(Dispatchers.Main) { updateAllWidgets(context) }
                        return@launch
                    }

                    val now = System.currentTimeMillis()
                    val sessionType = when (catType) {
                        "LEARNING" -> "LEARNING"
                        "EXERCISE" -> "EXERCISE"
                        "SOCIAL" -> "SOCIAL"
                        "WASTED" -> "WASTED"
                        else -> "ACTIVITY"
                    }

                    db.sessionDao().deactivateAllSessions(now)

                    val categoryId = db.categoryDao().getIdByName(catName)

                    val session = SessionEntity(
                        title = "Quick Session",
                        categoryId = categoryId,
                        type = sessionType,
                        startTime = now,
                        isActive = true,
                        source = "WIDGET",
                        timezoneId = java.time.ZoneId.systemDefault().id
                    )
                    val sessionId = db.sessionDao().insert(session)

                    val state = TimerStateStore.TimerState(
                        isRunning = true,
                        startTime = now,
                        sessionId = sessionId,
                        title = "Quick Session",
                        categoryName = catName,
                        categoryType = catType,
                        source = "WIDGET",
                        autoDnd = catType == "PRODUCTIVE" || catType == "LEARNING"
                    )
                    TimerStateStore.publish(context, state)

                    withContext(Dispatchers.Main) {
                        TimerStateStore.startTimerService(context, state)
                        updateAllWidgets(context)
                    }
                }

                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    val quote = QuotesApi.getRandomQuote()
                    val quotePrefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                    quotePrefs.edit()
                        .putString("widget_quote", quote.text)
                        .putString("widget_quote_author", quote.author)
                        .apply()

                    withContext(Dispatchers.Main) {
                        updateAllWidgets(context)
                    }
                }
            }

            ACTION_STOP -> {
                val now = System.currentTimeMillis()
                val targetSessionId = TimerStateStore.load(context).sessionId
                val accepted = TimerStateStore.requestStop(context, targetSessionId)

                val db = (context.applicationContext as SoulTrackApp).database
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    if (!accepted) {
                        runCatching { db.sessionDao().stopActiveSession(now) }
                    }
                    runCatching { TimerStateStore.clear(context) }
                    withContext(Dispatchers.Main) { updateAllWidgets(context) }
                }
            }

            ACTION_RESET -> {
                val now = System.currentTimeMillis()
                val targetSessionId = TimerStateStore.load(context).sessionId
                TimerStateStore.requestStop(context, targetSessionId)

                val db = (context.applicationContext as SoulTrackApp).database
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    runCatching { db.sessionDao().stopActiveSession(now) }
                    TimerStateStore.publish(
                        context,
                        TimerStateStore.TimerState(
                            title = "Ready to start"
                        )
                    )
                    withContext(Dispatchers.Main) { updateAllWidgets(context) }
                }
            }

            ACTION_CYCLE_CATEGORY -> {
                val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                var index = prefs.getInt("category_index", 0)
                index = (index + 1) % widgetCategories.size
                prefs.edit().putInt("category_index", index).apply()

                updateAllWidgets(context)
            }

            ACTION_NEXT_QUOTE -> {
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    val quote = QuotesApi.getRandomQuote()
                    val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("widget_quote", quote.text)
                        .putString("widget_quote_author", quote.author)
                        .apply()

                    withContext(Dispatchers.Main) {
                        updateAllWidgets(context)
                    }
                }
            }

            ACTION_UPDATE -> {
                if (!TimerStateStore.isServiceAlive()) {
                    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                        TimerStateStore.reconcile(context)
                        withContext(Dispatchers.Main) { updateAllWidgets(context) }
                    }
                } else {
                    updateAllWidgets(context)
                }
            }
        }
    }

    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val timerIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, TimerWidgetProvider::class.java)
        )

        for (id in timerIds) {
            updateAppWidget(context, appWidgetManager, id)
        }

        PomodoroWidgetProvider.updateWidgets(context)
        QuoteWidgetProvider.updateWidgets(context)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_timer)

        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val state = TimerStateStore.load(context)
        val isRunning = state.isRunning
        val sessionName = state.title.ifBlank { "Ready to start" }
        val categoryName = state.categoryName
        val startTime = state.startTime
        val categoryIndex = prefs.getInt("category_index", 0)
        val quote = prefs.getString("widget_quote", "") ?: ""
        val quoteAuthor = prefs.getString("widget_quote_author", "") ?: ""

        val (catName, _) = widgetCategories[categoryIndex]

        views.setTextViewText(R.id.widget_session_name, sessionName)

        if (isRunning && categoryName.isNotBlank()) {
            views.setTextViewText(R.id.widget_category, categoryName)
        } else {
            views.setTextViewText(R.id.widget_category, catName)
        }

        if (isRunning && startTime > 0) {
            val elapsed = ((System.currentTimeMillis() - startTime) / 1000).coerceAtLeast(0L)
            val h = elapsed / 3600
            val m = (elapsed % 3600) / 60
            val s = elapsed % 60
            views.setTextViewText(R.id.widget_timer, String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s))
        } else {
            views.setTextViewText(R.id.widget_timer, "00:00:00")
        }

        if (quote.isNotBlank()) {
            views.setViewVisibility(R.id.widget_quote, android.view.View.VISIBLE)
            views.setTextViewText(R.id.widget_quote, "\u201C$quote\u201D")
            if (quoteAuthor.isNotBlank()) {
                views.setViewVisibility(R.id.widget_quote_author, android.view.View.VISIBLE)
                views.setTextViewText(R.id.widget_quote_author, "\u2014 $quoteAuthor")
            } else {
                views.setViewVisibility(R.id.widget_quote_author, android.view.View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_quote, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_quote_author, android.view.View.GONE)
        }

        if (isRunning) {
            views.setViewVisibility(R.id.widget_btn_start, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_btn_stop, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_btn_reset, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_btn_start, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_btn_stop, android.view.View.GONE)
            views.setViewVisibility(R.id.widget_btn_reset, android.view.View.GONE)
        }

        val startPendingIntent = buildButtonPendingIntent(context, ACTION_START, appWidgetId)
        views.setOnClickPendingIntent(R.id.widget_btn_start, startPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_title, startPendingIntent)

        val stopPendingIntent = buildButtonPendingIntent(context, ACTION_STOP, appWidgetId)
        views.setOnClickPendingIntent(R.id.widget_btn_stop, stopPendingIntent)

        val resetPendingIntent = buildButtonPendingIntent(context, ACTION_RESET, appWidgetId)
        views.setOnClickPendingIntent(R.id.widget_btn_reset, resetPendingIntent)

        val cyclePendingIntent = buildButtonPendingIntent(context, ACTION_CYCLE_CATEGORY, appWidgetId)
        views.setOnClickPendingIntent(R.id.widget_category, cyclePendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun buildButtonPendingIntent(context: Context, action: String, appWidgetId: Int) =
        PendingIntent.getBroadcast(
            context,
            appWidgetId,
            Intent(context, TimerWidgetProvider::class.java).apply {
                this.action = action
                component = ComponentName(context, TimerWidgetProvider::class.java)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
