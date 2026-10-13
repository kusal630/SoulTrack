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

/** User facing switches for the focus/intelligence features. */
object FocusSettings {
    private const val PREFS_NAME = "focus_prefs"
    private const val KEY_AUTO_DND = "auto_dnd_enabled"
    private const val KEY_NOISE = "noise_enabled"
    private const val KEY_RECOMMENDATIONS = "recommendations_enabled"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAutoDndEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_DND, true)

    fun setAutoDndEnabled(context: Context, enabled: Boolean) {
        if (!enabled) FocusDndManager.restore(context)
        prefs(context).edit().putBoolean(KEY_AUTO_DND, enabled).apply()
    }

    fun isNoiseSensingEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOISE, false)

    fun setNoiseSensingEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOISE, enabled).apply()
        if (!enabled) NoiseMonitor.stop()
    }

    fun isRecommendationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_RECOMMENDATIONS, true)

    fun setRecommendationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_RECOMMENDATIONS, enabled).apply()
    }
}
