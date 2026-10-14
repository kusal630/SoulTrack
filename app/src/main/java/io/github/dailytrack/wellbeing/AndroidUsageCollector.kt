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


package io.github.dailytrack.wellbeing

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import java.time.Instant

/**
 * Reads the platform's own on-device usage stats into plain intervals.
 *
 * This is the only piece that needs a system permission; everything above it
 * is pure Kotlin and stays testable without a device.
 */
object AndroidUsageCollector {

    /** Foreground stretches inside [startMillis, endMillis], oldest first. */
    fun collect(context: Context, startMillis: Long, endMillis: Long): List<AppInterval> {
        if (!UsageAccess.has(context)) return emptyList()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()

        val events = manager.queryEvents(startMillis, endMillis) ?: return emptyList()
        val event = UsageEvents.Event()
        val intervals = mutableListOf<AppInterval>()
        var currentPackage: String? = null
        var currentStart = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val pkg = event.packageName ?: continue
                    val isResume = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                    val movedAway = !isResume && currentPackage == pkg
                    val switched = isResume && currentPackage != null && currentPackage != pkg

                    if (switched || movedAway) {
                        currentPackage?.let {
                            val start = Instant.ofEpochMilli(currentStart)
                            val end = Instant.ofEpochMilli(event.timeStamp)
                            if (end.isAfter(start)) intervals += AppInterval(it, start, end)
                        }
                        currentPackage = null
                    }
                    if (isResume) {
                        currentPackage = pkg
                        currentStart = event.timeStamp
                    }
                }
            }
        }
        currentPackage?.let {
            val start = Instant.ofEpochMilli(currentStart)
            val end = Instant.ofEpochMilli(endMillis)
            if (end.isAfter(start)) intervals += AppInterval(it, start, end)
        }

        return intervals.sortedBy { it.start }
    }

    /** App label plus platform category, for display and classification. */
    fun describe(context: Context, packageName: String): Pair<String, Int?> {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            val label = context.packageManager.getApplicationLabel(info)?.toString() ?: packageName
            val category = if (android.os.Build.VERSION.SDK_INT >= 26) info.category else null
            label to category
        } catch (e: PackageManager.NameNotFoundException) {
            packageName to null
        }
    }
}

/** Weights and overrides in local prefs - nothing leaves the device. */
class SharedPreferencesClassifierStore(context: Context) : ClassifierStore {
    private val prefs = context.applicationContext
        .getSharedPreferences("soultrack_wellbeing", Context.MODE_PRIVATE)

    override fun weightsFor(packageName: String): Map<Int, Float> {
        val raw = prefs.getString(key(packageName), null) ?: return emptyMap()
        return raw.split(',')
            .mapNotNull { it.toFloatOrNull() }
            .mapIndexed { index, value -> index to value }
            .toMap()
    }

    override fun saveWeights(packageName: String, weights: List<Float>) {
        prefs.edit().putString(key(packageName), weights.joinToString(",")).apply()
    }

    override fun overrideFor(packageName: String): UsageKind? {
        val raw = prefs.getString(key(packageName) + "_label", null) ?: return null
        return runCatching { UsageKind.valueOf(raw) }.getOrNull()
    }

    private fun key(packageName: String) = "w_$packageName"
}
