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

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.dailytrack.SoulTrackApp
import io.github.dailytrack.service.DeviceSyncService
import java.util.concurrent.TimeUnit

/**
 * Watchdog that keeps device sync alive across reboots, process death and
 * Doze. The long-lived listener is a foreground service; this worker simply
 * brings it back and performs a sync whenever the network is available.
 */
class DeviceSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SoulTrackApp ?: return Result.success()
        val engine = app.p2pSync
        if (!engine.isEnabled()) return Result.success()

        // Starting a foreground service can be refused while the app is in the
        // background on newer Android versions. Even then, outbound sync works.
        runCatching { DeviceSyncService.start(applicationContext) }
        runCatching { engine.start() }
        runCatching { engine.syncNow("worker") }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "soultrack_device_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DeviceSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
