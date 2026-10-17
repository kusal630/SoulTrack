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

import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.dailytrack.SoulTrackApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for the running timer.
 *
 * The widget, the foreground service, the notification and every screen used
 * to keep their own idea of what was running, which is how the UI and the
 * widget ended up disagreeing. Everything now reads and writes this store,
 * which persists to the same SharedPreferences file the widget already used
 * so state survives process death.
 */
object TimerStateStore {

    const val PREFS_NAME = "widget_prefs"

    data class TimerState(
        val isRunning: Boolean = false,
        val startTime: Long = 0L,
        val sessionId: Long = -1L,
        val title: String = "",
        val categoryName: String = "",
        val categoryType: String = "",
        val source: String = "",
        val isPomodoro: Boolean = false,
        val autoDnd: Boolean = false
    )

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state

    @Volatile
    private var serviceAlive: Boolean = false

    private val mutex = Mutex()
    private var lastReconcileAt = 0L

    fun isServiceAlive(): Boolean = serviceAlive

    fun markServiceAlive(alive: Boolean) {
        serviceAlive = alive
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context): TimerState {
        val p = prefs(context)
        val state = TimerState(
            isRunning = p.getBoolean("is_running", false),
            startTime = p.getLong("start_time", 0L),
            sessionId = p.getLong("session_id", -1L),
            title = p.getString("session_name", "") ?: "",
            categoryName = p.getString("category_name", "") ?: "",
            categoryType = p.getString("category_type", "") ?: "",
            source = p.getString("source", "") ?: "",
            isPomodoro = p.getBoolean("is_pomodoro", false),
            autoDnd = p.getBoolean("auto_dnd", false)
        )
        _state.value = state
        return state
    }

    fun publish(context: Context, state: TimerState): TimerState {
        prefs(context).edit()
            .putBoolean("is_running", state.isRunning)
            .putLong("start_time", state.startTime)
            .putLong("session_id", state.sessionId)
            .putString("session_name", state.title)
            .putString("category_name", state.categoryName)
            .putString("category_type", state.categoryType)
            .putString("source", state.source)
            .putBoolean("is_pomodoro", state.isPomodoro)
            .putBoolean("auto_dnd", state.autoDnd)
            .apply()
        _state.value = state
        return state
    }

    fun clear(context: Context): TimerState = publish(context, TimerState())

    fun current(context: Context): TimerState {
        val cached = _state.value
        if (cached.isRunning || cached.startTime > 0L) return cached
        return load(context)
    }

    /**
     * Merges the database's active session with the stored widget state.
     *
     * This is what makes the widget and the app agree: whichever side was
     * started or stopped last, the other side converges on the same session.
     */
    suspend fun reconcile(context: Context, force: Boolean = false): TimerState {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        if (!force && now - lastReconcileAt < 1_500L) return _state.value
        mutex.withLock {
            if (!force && System.currentTimeMillis() - lastReconcileAt < 1_500L) return _state.value
            lastReconcileAt = System.currentTimeMillis()
            val active = runCatching {
                (app as SoulTrackApp).database.sessionDao().getActiveSessionSync()
            }.getOrNull()

            if (active != null) {
                val category = active.categoryId?.let {
                    runCatching {
                        (app as SoulTrackApp).database.categoryDao().getCategoryById(it)
                    }.getOrNull()
                }
                val existing = load(app)
                val merged = TimerState(
                    isRunning = true,
                    startTime = active.startTime,
                    sessionId = active.id,
                    title = active.title.ifBlank { existing.title },
                    categoryName = category?.name ?: existing.categoryName,
                    categoryType = category?.type ?: active.type,
                    source = active.source,
                    isPomodoro = existing.isPomodoro,
                    autoDnd = existing.autoDnd || isFocusType(category?.type ?: active.type)
                )
                if (merged != existing) publish(app, merged)
                if (!serviceAlive) startTimerService(app, merged)
            } else {
                val existing = load(app)
                if (existing.isRunning || existing.startTime > 0L) clear(app)
                if (serviceAlive) {
                    runCatching { app.stopService(Intent(app, TimerForegroundService::class.java)) }
                }
            }
        }
        return _state.value
    }

    fun isFocusType(type: String?): Boolean =
        type == "PRODUCTIVE" || type == "LEARNING"

    fun startTimerService(
        context: Context,
        state: TimerState
    ) {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            putExtra(TimerForegroundService.EXTRA_START_TIME, state.startTime)
            putExtra(TimerForegroundService.EXTRA_TITLE, state.title)
            putExtra(TimerForegroundService.EXTRA_CATEGORY, state.categoryName)
            putExtra(TimerForegroundService.EXTRA_CATEGORY_TYPE, state.categoryType)
            putExtra(TimerForegroundService.EXTRA_IS_POMODORO, state.isPomodoro)
            putExtra(TimerForegroundService.EXTRA_AUTO_DND, state.autoDnd)
            putExtra(TimerForegroundService.EXTRA_SESSION_ID, state.sessionId)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    /**
     * Sends ACTION_STOP to the service carrying the exact session to stop, so a
     * stop can never close a session the user started a moment later.
     * Returns true if the service accepted the request.
     */
    fun requestStop(
        context: Context,
        sessionId: Long = -1L,
        isPomodoro: Boolean = false,
        cascadePomodoro: Boolean = true
    ): Boolean {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_STOP
            putExtra(TimerForegroundService.EXTRA_SESSION_ID, sessionId)
            putExtra(TimerForegroundService.EXTRA_IS_POMODORO, isPomodoro)
            putExtra(TimerForegroundService.EXTRA_CASCADE_POMODORO, cascadePomodoro)
        }
        return runCatching { context.startService(intent); true }.getOrDefault(false)
    }
}
