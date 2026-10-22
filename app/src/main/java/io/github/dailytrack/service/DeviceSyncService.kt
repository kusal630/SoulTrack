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
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.github.dailytrack.MainActivity
import io.github.dailytrack.R
import io.github.dailytrack.SoulTrackApp
import io.github.dailytrack.sync.P2pSyncEngine
import io.github.dailytrack.sync.P2pSyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Keeps the encrypted peer listener alive so a paired phone/tablet can reach
 * this device at any time, not only while the app is open.
 */
class DeviceSyncService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var engine: P2pSyncEngine
    private var observerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        engine = (application as SoulTrackApp).p2pSync
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!engine.isEnabled()) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SYNC -> {
                startForegroundCompat(buildNotification(engine.state.value))
                engine.start()
                scope.launch { runCatching { engine.syncNow("manual") } }
                observeState()
                return START_STICKY
            }
            else -> {
                startForegroundCompat(buildNotification(engine.state.value))
                engine.start()
                observeState()
                return START_STICKY
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (engine.isEnabled()) {
            runCatching { start(applicationContext) }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observerJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun observeState() {
        if (observerJob != null) return
        observerJob = scope.launch {
            engine.state.collectLatest { state ->
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, buildNotification(state))
            }
        }
    }

    private fun buildNotification(state: P2pSyncState): Notification {
        val text = when {
            state.lastError != null -> state.lastError
            state.lastSyncAt != null -> "Last synced ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(state.lastSyncAt))}"
            state.peers.isEmpty() -> "Waiting for a paired device"
            else -> "Waiting for ${state.peers.size} paired device${if (state.peers.size == 1) "" else "s"}"
        } ?: "Device-to-device sync"

        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Device sync")
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Device sync",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps phone and tablet synced directly, without a cloud"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "device_sync"
        private const val NOTIFICATION_ID = 480001
        const val ACTION_START = "io.github.dailytrack.action.START_DEVICE_SYNC"
        const val ACTION_STOP = "io.github.dailytrack.action.STOP_DEVICE_SYNC"
        const val ACTION_SYNC = "io.github.dailytrack.action.SYNC_DEVICES"

        fun start(context: Context) {
            val intent = Intent(context, DeviceSyncService::class.java).apply {
                action = ACTION_START
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun syncNow(context: Context) {
            val intent = Intent(context, DeviceSyncService::class.java).apply {
                action = ACTION_SYNC
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, DeviceSyncService::class.java)) }
        }
    }
}
