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


package io.github.dailytrack.ui.settings

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Which parts of the app the user actually wants.
 *
 * Soul Track is a productivity app first: sleep, exercise, nutrition and the
 * body dashboard are an optional layer the user opts into during onboarding
 * (and can flip at any time in Settings). Everything health related - the
 * Explore tiles, the targets, the health derived insights and the growth
 * weights - reads this flag, so switching it off really turns the layer off
 * instead of just hiding a menu entry.
 */
@Stable
object AppFeatures {
    private const val PREFS = "soultrack_features"
    private const val KEY_HEALTH = "health_enabled"

    /**
     * Sleep, exercise, nutrition/hydration and the body dashboard.
     * Defaults to on so an existing install never loses its screens; the
     * onboarding question sets it explicitly for new users.
     */
    var healthEnabled by mutableStateOf(true)
        private set

    fun load(context: Context) {
        healthEnabled = prefs(context).getBoolean(KEY_HEALTH, true)
    }

    fun setHealthEnabled(value: Boolean, context: Context) {
        healthEnabled = value
        prefs(context).edit().putBoolean(KEY_HEALTH, value).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/**
 * The user's own targets.
 *
 * `0` always means "not set": the app then calibrates the target from the
 * user's own history instead of falling back to a number invented by us.
 * Nothing here is a hidden default - what you set is what the growth score,
 * the insights and the nutrition rules are measured against.
 */
@Stable
object ProductTargets {
    private const val PREFS = "soultrack_targets"
    private const val KEY_PRODUCTIVE = "productive_minutes"
    private const val KEY_LEARNING = "learning_minutes"
    private const val KEY_EXERCISE = "exercise_minutes"
    private const val KEY_SLEEP = "sleep_hours"
    private const val KEY_WATER = "water_ml"
    private const val KEY_PROTEIN = "protein_g"
    private const val KEY_FIBER = "fiber_g"
    private const val KEY_POMODORO = "pomodoro_daily_goal"
    private const val KEY_MAINTENANCE = "maintenance_mode"

    /** Focus minutes per day the user aims for. 0 = auto from history. */
    var productiveMinutes by mutableStateOf(0)
        private set

    /** Learning minutes per day. 0 = auto from history. */
    var learningMinutes by mutableStateOf(0)
        private set

    /** Exercise minutes per day. 0 = auto from history. */
    var exerciseMinutes by mutableStateOf(0)
        private set

    /** Sleep hours per night. 0 = auto from history. */
    var sleepHours by mutableStateOf(0f)
        private set

    /** Water millilitres per day. 0 = no goal, hydration rules stay quiet. */
    var waterMl by mutableStateOf(0)
        private set

    /** Protein grams per day. 0 = no goal. */
    var proteinG by mutableStateOf(0)
        private set

    /** Fibre grams per day. 0 = no goal. */
    var fiberG by mutableStateOf(0)
        private set

    /** Pomodoros the user wants to close in a day. 0 = auto from history. */
    var pomodoroDailyGoal by mutableStateOf(0)
        private set

    /** Stops routine/nagging insights without deleting any data. */
    var maintenanceMode by mutableStateOf(false)
        private set

    fun load(context: Context) {
        val p = prefs(context)
        productiveMinutes = p.getInt(KEY_PRODUCTIVE, 0)
        learningMinutes = p.getInt(KEY_LEARNING, 0)
        exerciseMinutes = p.getInt(KEY_EXERCISE, 0)
        sleepHours = p.getFloat(KEY_SLEEP, 0f)
        waterMl = p.getInt(KEY_WATER, 0)
        proteinG = p.getInt(KEY_PROTEIN, 0)
        fiberG = p.getInt(KEY_FIBER, 0)
        pomodoroDailyGoal = p.getInt(KEY_POMODORO, 0)
        maintenanceMode = p.getBoolean(KEY_MAINTENANCE, false)
    }

    private fun save(context: Context, key: String, value: Any) {
        val editor = prefs(context).edit()
        when (value) {
            is Int -> editor.putInt(key, value)
            is Float -> editor.putFloat(key, value)
            is Boolean -> editor.putBoolean(key, value)
        }
        editor.apply()
    }

    fun setProductiveMinutes(value: Int, context: Context) {
        productiveMinutes = value.coerceIn(0, 24 * 60)
        save(context, KEY_PRODUCTIVE, productiveMinutes)
    }

    fun setLearningMinutes(value: Int, context: Context) {
        learningMinutes = value.coerceIn(0, 24 * 60)
        save(context, KEY_LEARNING, learningMinutes)
    }

    fun setExerciseMinutes(value: Int, context: Context) {
        exerciseMinutes = value.coerceIn(0, 24 * 60)
        save(context, KEY_EXERCISE, exerciseMinutes)
    }

    fun setSleepHours(value: Float, context: Context) {
        sleepHours = value.coerceIn(0f, 16f)
        save(context, KEY_SLEEP, sleepHours)
    }

    fun setWaterMl(value: Int, context: Context) {
        waterMl = value.coerceIn(0, 10_000)
        save(context, KEY_WATER, waterMl)
    }

    fun setProteinG(value: Int, context: Context) {
        proteinG = value.coerceIn(0, 500)
        save(context, KEY_PROTEIN, proteinG)
    }

    fun setFiberG(value: Int, context: Context) {
        fiberG = value.coerceIn(0, 200)
        save(context, KEY_FIBER, fiberG)
    }

    fun setPomodoroDailyGoal(value: Int, context: Context) {
        pomodoroDailyGoal = value.coerceIn(0, 40)
        save(context, KEY_POMODORO, pomodoroDailyGoal)
    }

    fun setMaintenanceMode(value: Boolean, context: Context) {
        maintenanceMode = value
        save(context, KEY_MAINTENANCE, value)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
