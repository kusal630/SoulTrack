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
    private const val KEY_DISTRACTION_BUDGET = "distraction_budget_minutes"

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

    /**
     * Daily budget, in minutes, for apps the on-device classifier ranks as
     * distracting. 0 disables the budget. A self-set limit plus a just-in-time,
     * autonomy-supportive prompt reduced time on the most problematic app by
     * about 29 minutes a day in a randomized trial (Keller et al., 2026).
     */
    fun getDistractionBudgetMinutes(context: Context): Int =
        prefs(context).getInt(KEY_DISTRACTION_BUDGET, 0)

    fun setDistractionBudgetMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_DISTRACTION_BUDGET, minutes.coerceIn(0, 600)).apply()
    }
}
