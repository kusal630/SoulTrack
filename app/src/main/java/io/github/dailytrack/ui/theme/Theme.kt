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


package io.github.dailytrack.ui.theme

import android.content.Context
import io.github.dailytrack.engine.DayClassificationThresholds
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** The three surfaces a user can pick between. */
enum class ThemeMode {
    /** Follow the system setting. */
    SYSTEM,

    /** Light surfaces (rarely used, kept for completeness). */
    LIGHT,

    /** Pure black AMOLED - the default. */
    AMOLED,

    /** Near-black `#0E0E13` for people who find pure black too harsh. */
    SOFT_DARK;

}

/**
 * App-wide appearance preferences. Held in snapshot state so flipping a toggle
 * in Settings repaints the whole tree immediately, and mirrored into
 * SharedPreferences so the choice survives a restart.
 */
@Stable
object ThemeSettings {
    var mode by mutableStateOf(ThemeMode.AMOLED)
        private set
    var dynamicColor by mutableStateOf(false)
        private set

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        mode = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_MODE, ThemeMode.AMOLED.name) ?: ThemeMode.AMOLED.name)
        }.getOrDefault(ThemeMode.AMOLED)
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC, false)
        wastingThreshold = prefs.getFloat(KEY_WASTING, 0.25f)
        productiveThreshold = prefs.getFloat(KEY_PRODUCTIVE, 0.40f)
        applyThresholds()
    }

    fun setMode(value: ThemeMode, context: Context) {
        mode = value
        persist(context)
    }

    fun setDynamicColor(value: Boolean, context: Context) {
        dynamicColor = value
        persist(context)
    }

    /** Distraction share above which a day reads as wasted (0.05..0.60). */
    var wastingThreshold by mutableStateOf(0.25f)
        private set

    /** Productive share below which a day reads as unproductive (0.10..0.90). */
    var productiveThreshold by mutableStateOf(0.40f)
        private set

    fun setWastingThreshold(value: Float, context: Context) {
        wastingThreshold = value.coerceIn(0.05f, 0.60f)
        applyThresholds()
        persist(context)
    }

    fun setProductiveThreshold(value: Float, context: Context) {
        productiveThreshold = value.coerceIn(0.10f, 0.90f)
        applyThresholds()
        persist(context)
    }

    private fun applyThresholds() {
        DayClassificationThresholds.WASTING_TIME_DISTRACTION_RATIO = wastingThreshold.toDouble()
        DayClassificationThresholds.NOT_PRODUCTIVE_PRODUCTIVE_RATIO = productiveThreshold.toDouble()
    }

    private fun persist(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_MODE, mode.name)
            .putBoolean(KEY_DYNAMIC, dynamicColor)
            .putFloat(KEY_WASTING, wastingThreshold)
            .putFloat(KEY_PRODUCTIVE, productiveThreshold)
            .apply()
    }

    private const val PREFS = "soultrack_theme"
    private const val KEY_MODE = "mode"
    private const val KEY_DYNAMIC = "dynamic_color"
    private const val KEY_WASTING = "wasting_threshold"
    private const val KEY_PRODUCTIVE = "productive_threshold"
}

/** Category colours for charts and breakdowns (unchanged semantics). */
private val AmoledColorScheme = darkColorScheme(
    primary = Mint,
    onPrimary = OnMint,
    primaryContainer = Color(0xFF0D2A22),
    onPrimaryContainer = Mint,
    secondary = Aqua,
    onSecondary = Color(0xFF00202A),
    secondaryContainer = Color(0xFF0C2A33),
    onSecondaryContainer = Aqua,
    tertiary = Sky,
    onTertiary = Color(0xFF001634),
    tertiaryContainer = Color(0xFF11233D),
    onTertiaryContainer = Sky,
    background = AmoledBlack,
    onBackground = SoulOnSurface,
    surface = AmoledBlack,
    onSurface = SoulOnSurface,
    surfaceVariant = SoulSurfaceContainer,
    onSurfaceVariant = SoulOnSurfaceVariant,
    surfaceContainerLowest = AmoledBlack,
    surfaceContainerLow = SoulSurface,
    surfaceContainer = SoulSurfaceContainer,
    surfaceContainerHigh = SoulSurfaceContainerHigh,
    surfaceContainerHighest = SoulSurfaceContainerHighest,
    surfaceDim = AmoledBlack,
    surfaceBright = SoulSurfaceContainerHighest,
    inverseSurface = SoulOnSurface,
    inverseOnSurface = SoulSurfaceContainer,
    inversePrimary = Color(0xFF157F60),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = SoulOutline,
    outlineVariant = SoulHairline,
    scrim = Color(0xFF000000),
    surfaceTint = Mint
)

/** Same ladder, lifted off pure black. */
private val SoftDarkColorScheme = AmoledColorScheme.copy(
    background = SoftDarkBackground,
    surface = SoftDarkBackground,
    surfaceDim = SoftDarkBackground,
    surfaceContainerLowest = SoftDarkBackground,
    surfaceContainerLow = SoftDarkSurface,
    surfaceContainer = SoftDarkSurfaceContainer,
    surfaceContainerHigh = Color(0xFF22222D),
    surfaceContainerHighest = Color(0xFF2C2C38),
    surfaceVariant = SoftDarkSurfaceContainer
)

/** Mint reads as a tinted neutral on light backgrounds, so it is darkened. */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0B7C5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F5DE),
    onPrimaryContainer = Color(0xFF00211A),
    secondary = Color(0xFF0B6E80),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCBEFF7),
    onSecondaryContainer = Color(0xFF00202A),
    tertiary = Color(0xFF2C63C4),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9E2FF),
    onTertiaryContainer = Color(0xFF001A42),
    background = Color(0xFFFBFCFF),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFBFCFF),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDDE4E0),
    onSurfaceVariant = Color(0xFF414844),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F5F2),
    surfaceContainer = Color(0xFFEDEFEC),
    surfaceContainerHigh = Color(0xFFE7E9E6),
    surfaceContainerHighest = Color(0xFFE1E3E0),
    surfaceDim = Color(0xFFD9DBD7),
    surfaceBright = Color(0xFFF9FBF8),
    inverseSurface = Color(0xFF2E312F),
    inverseOnSurface = Color(0xFFEFF1EE),
    inversePrimary = Color(0xFF7CF5C4),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF717975),
    outlineVariant = Color(0xFFC1C9C4),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF0E9B73)
)

/**
 * Dynamic colour, without giving up the AMOLED canvas.
 *
 * Material's dynamic scheme also replaces the near-black surface ladder with
 * wallpaper-tinted greys. On OLED that wastes power and breaks the ring
 * identity, so we take only the accent slots from the wallpaper and keep our
 * own black surfaces, text and semantic colours.
 */
private fun dynamicAccents(base: ColorScheme, dynamic: ColorScheme): ColorScheme = base.copy(
    primary = dynamic.primary,
    onPrimary = dynamic.onPrimary,
    primaryContainer = dynamic.primaryContainer,
    onPrimaryContainer = dynamic.onPrimaryContainer,
    secondary = dynamic.secondary,
    onSecondary = dynamic.onSecondary,
    secondaryContainer = dynamic.secondaryContainer,
    onSecondaryContainer = dynamic.onSecondaryContainer,
    tertiary = dynamic.tertiary,
    onTertiary = dynamic.onTertiary,
    tertiaryContainer = dynamic.tertiaryContainer,
    onTertiaryContainer = dynamic.onTertiaryContainer,
    inversePrimary = dynamic.inversePrimary,
    surfaceTint = dynamic.primary
)

@Composable
fun SoulTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = ThemeSettings.dynamicColor,
    content: @Composable () -> Unit
) {
    val chosen = ThemeSettings.mode
    val useDark = when (chosen) {
        ThemeMode.SYSTEM -> darkTheme
        ThemeMode.LIGHT -> false
        else -> true
    }

    val baseScheme = when {
        !useDark -> LightColorScheme
        chosen == ThemeMode.SOFT_DARK -> SoftDarkColorScheme
        else -> AmoledColorScheme
    }
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val dynamic = if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dynamicAccents(baseScheme, dynamic)
    } else {
        baseScheme
    }

    CompositionLocalProvider(LocalDayStatusColors provides DayStatusPalette()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SoulTrackTypography,
            shapes = SoulTrackShapes,
            content = content
        )
    }
}
