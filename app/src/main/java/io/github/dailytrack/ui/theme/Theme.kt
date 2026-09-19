package io.github.dailytrack.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Semantic Colors for Categories
val ProductiveColor = Color(0xFF10B981)
val WastedColor = Color(0xFFEF4444)
val ExerciseColor = Color(0xFFF59E0B)
val LearningColor = Color(0xFF3B82F6)
val SocialColor = Color(0xFF8B5CF6)
val SleepColor = Color(0xFF6366F1)
val RecoveryColor = Color(0xFF34D399)
val NeutralColor = Color(0xFF6B7280)

// Gradient Colors
val GradientStart = Color(0xFFE94560)
val GradientMid = Color(0xFF0F3460)
val GradientEnd = Color(0xFF533483)

val ProductiveGradient = listOf(Color(0xFF10B981), Color(0xFF059669))
val WastedGradient = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
val ExerciseGradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
val LearningGradient = listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
val SocialGradient = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))
val SleepGradient = listOf(Color(0xFF6366F1), Color(0xFF4F46E5))

// Glass Morphism Colors
val GlassLight = Color(0x1AFFFFFF)
val GlassDark = Color(0x1A000000)
val GlassBorder = Color(0x33FFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE94560),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC23152),
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = Color(0xFF0F3460),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF16213E),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = Color(0xFF533483),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3D2566),
    onTertiaryContainer = Color(0xFFEDDCFF),
    background = Color(0xFF0A0A0F),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF12121A),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF1A1A25),
    onSurfaceVariant = Color(0xFFC7C5CA),
    surfaceContainer = Color(0xFF1E1E28),
    surfaceContainerHigh = Color(0xFF252530),
    surfaceContainerHighest = Color(0xFF2D2D38),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF444449),
    surfaceTint = Color(0xFFE94560)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC23152),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E0),
    onPrimaryContainer = Color(0xFF3B0013),
    secondary = Color(0xFF16213E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E4FF),
    onSecondaryContainer = Color(0xFF001D36),
    tertiary = Color(0xFF3D2566),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDDCFF),
    onTertiaryContainer = Color(0xFF220046),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFF3DDE1),
    onSurfaceVariant = Color(0xFF514347),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEEEEEE),
    surfaceContainerHighest = Color(0xFFE0E0E0),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF837377),
    outlineVariant = Color(0xFFD5C2C6),
    surfaceTint = Color(0xFFC23152)
)

@Composable
fun SoulTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SoulTrackTypography,
        content = content
    )
}
