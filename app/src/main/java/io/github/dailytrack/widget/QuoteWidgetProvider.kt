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
import android.widget.RemoteViews
import io.github.dailytrack.R
import io.github.dailytrack.SoulTrackApp
import io.github.dailytrack.data.api.QuotesApi
import io.github.dailytrack.data.db.entity.SavedQuoteEntity
import kotlinx.coroutines.*

class QuoteWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_NEXT_QUOTE = "io.github.dailytrack.ACTION_QUOTE_WIDGET_NEXT"
        const val ACTION_SAVE_QUOTE = "io.github.dailytrack.ACTION_QUOTE_WIDGET_SAVE"
        const val ACTION_UPDATE = "io.github.dailytrack.ACTION_QUOTE_WIDGET_UPDATE"
        const val PREFS_NAME = "quote_widget_prefs"
        const val REFRESH_INTERVAL_MS = 3600000L

        fun updateWidgets(context: Context) {
            val intent = Intent(context, QuoteWidgetProvider::class.java).apply {
                action = ACTION_UPDATE
                component = ComponentName(context, QuoteWidgetProvider::class.java)
            }
            context.sendBroadcast(intent)
        }

        /** Pushes a quote straight into the widget (used on every app launch). */
        fun showQuote(context: Context, text: String, author: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString("quote_text", text)
                .putString("quote_author", author)
                .putLong("last_refresh_time", System.currentTimeMillis())
                .apply()
            updateWidgets(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            checkAndRefreshQuote(context)
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_NEXT_QUOTE -> {
                fetchNewQuote(context)
            }

            ACTION_SAVE_QUOTE -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val quoteText = prefs.getString("quote_text", "") ?: ""
                val quoteAuthor = prefs.getString("quote_author", "") ?: ""

                if (quoteText.isNotBlank()) {
                    val db = (context.applicationContext as SoulTrackApp).database
                    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                        val existing = db.savedQuoteDao().getQuoteByText(quoteText)
                        if (existing == null) {
                            db.savedQuoteDao().insert(
                                SavedQuoteEntity(text = quoteText, author = quoteAuthor)
                            )
                        }
                        withContext(Dispatchers.Main) {
                            updateAllWidgets(context)
                        }
                    }
                }
            }

            ACTION_UPDATE -> {
                checkAndRefreshQuote(context)
                updateAllWidgets(context)
            }

            Intent.ACTION_SCREEN_ON -> {
                checkAndRefreshQuote(context)
                updateAllWidgets(context)
            }

            Intent.ACTION_USER_PRESENT -> {
                checkAndRefreshQuote(context)
                updateAllWidgets(context)
            }
        }
    }

    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, QuoteWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun checkAndRefreshQuote(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastRefresh = prefs.getLong("last_refresh_time", 0L)
        val now = System.currentTimeMillis()

        if (now - lastRefresh > REFRESH_INTERVAL_MS) {
            fetchNewQuote(context)
        }
    }

    private fun fetchNewQuote(context: Context) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val quote = QuotesApi.getTimedQuote()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("quote_text", quote.text)
                .putString("quote_author", quote.author)
                .putLong("last_refresh_time", System.currentTimeMillis())
                .apply()

            withContext(Dispatchers.Main) {
                updateAllWidgets(context)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_quote)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var quoteText = prefs.getString("quote_text", "") ?: ""
        var quoteAuthor = prefs.getString("quote_author", "") ?: ""

        if (quoteText.isBlank()) {
            quoteText = "Loading inspiration..."
            quoteAuthor = ""
            fetchNewQuote(context)
        }

        views.setTextViewText(R.id.widget_quote_text, "\"$quoteText\"")
        views.setTextViewText(R.id.widget_quote_author, if (quoteAuthor.isNotBlank()) "— $quoteAuthor" else "")

        val nextIntent = Intent(context, QuoteWidgetProvider::class.java).apply {
            action = ACTION_NEXT_QUOTE
            component = ComponentName(context, QuoteWidgetProvider::class.java)
        }
        val nextPendingIntent = android.app.PendingIntent.getBroadcast(
            context, 0, nextIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

        val saveIntent = Intent(context, QuoteWidgetProvider::class.java).apply {
            action = ACTION_SAVE_QUOTE
            component = ComponentName(context, QuoteWidgetProvider::class.java)
        }
        val savePendingIntent = android.app.PendingIntent.getBroadcast(
            context, 1, saveIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_save, savePendingIntent)

        val rootClickIntent = Intent(context, QuoteWidgetProvider::class.java).apply {
            action = ACTION_UPDATE
            component = ComponentName(context, QuoteWidgetProvider::class.java)
        }
        val rootPendingIntent = android.app.PendingIntent.getBroadcast(
            context, 20, rootClickIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_quote_root, rootPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
