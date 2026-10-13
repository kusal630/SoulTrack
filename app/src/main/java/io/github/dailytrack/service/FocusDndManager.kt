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

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Turns Do Not Disturb on for a focus session and restores the user's
 * original interruption filter afterwards.
 *
 * The "did we change it?" flag is persisted rather than held in memory so a
 * service that is killed mid-session still restores the user's filter when it
 * is later destroyed, and so the notification action, the widget and the app
 * all agree about whether DND is currently ours.
 */
object FocusDndManager {

    private const val PREFS_NAME = "focus_dnd_prefs"
    private const val KEY_CHANGED = "changed_by_us"
    private const val KEY_ORIGINAL = "original_filter"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasAccess(context: Context): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        return nm != null && nm.isNotificationPolicyAccessGranted
    }

    fun hasAccess(nm: NotificationManager): Boolean = nm.isNotificationPolicyAccessGranted

    /** Intent that opens the system screen granting DND access. */
    fun settingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun didChangeFilter(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CHANGED, false)

    /**
     * Turns DND on (priority-only) unless it is already active.
     * Returns true if we changed the user's filter.
     */
    fun enable(context: Context): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        if (!nm.isNotificationPolicyAccessGranted) return false
        if (prefs(context).getBoolean(KEY_CHANGED, false)) return true
        val current = nm.currentInterruptionFilter
        if (current == NotificationManager.INTERRUPTION_FILTER_PRIORITY) return false
        val safeOriginal = if (current == NotificationManager.INTERRUPTION_FILTER_ALL ||
            current == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
            current == NotificationManager.INTERRUPTION_FILTER_NONE
        ) current else NotificationManager.INTERRUPTION_FILTER_ALL
        prefs(context).edit()
            .putBoolean(KEY_CHANGED, true)
            .putInt(KEY_ORIGINAL, safeOriginal)
            .apply()
        runCatching { nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY) }
        return true
    }

    /** Restores the user's original filter if we changed it. Safe to call repeatedly. */
    fun restore(context: Context) {
        val p = prefs(context)
        if (!p.getBoolean(KEY_CHANGED, false)) return
        val original = p.getInt(KEY_ORIGINAL, NotificationManager.INTERRUPTION_FILTER_ALL)
        p.edit().remove(KEY_CHANGED).remove(KEY_ORIGINAL).apply()
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (!nm.isNotificationPolicyAccessGranted) return
        runCatching { nm.setInterruptionFilter(original) }
    }
}
