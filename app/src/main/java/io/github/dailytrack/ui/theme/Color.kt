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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import io.github.dailytrack.engine.DayStatus

/**
 * Soul Track colour tokens.
 *
 * The whole app sits on pure black so an OLED screen pays nothing for the
 * canvas; depth comes from tonal steps of the surface ladder plus a 1dp
 * hairline, never from shadows. The brand is a mint -> aqua -> sky gradient
 * taken straight from the ring logo.
 */

// ----------------------------------------------------------------------------
// Surfaces - AMOLED (default) and "Soft dark" alternative
// ----------------------------------------------------------------------------

/** Pure black ground. Dark pixels are off, so this is the default theme. */
val AmoledBlack = Color(0xFF000000)

/** Softer near-black for people who find pure black too harsh. */
val SoftDarkBackground = Color(0xFF0E0E13)

val SoulSurface = Color(0xFF0A0A0F)
val SoftDarkSurface = Color(0xFF14141B)
val SoulSurfaceContainer = Color(0xFF12121A)
val SoftDarkSurfaceContainer = Color(0xFF191922)
val SoulSurfaceContainerHigh = Color(0xFF1B1B24)
val SoulSurfaceContainerHighest = Color(0xFF24242F)
val SoulOutline = Color(0xFF33333F)

/** Hairline separators: white at 8% alpha. */
val SoulHairline = Color(0x14FFFFFF)

// ----------------------------------------------------------------------------
// Text
// ----------------------------------------------------------------------------

val SoulOnSurface = Color(0xFFECECF2)
val SoulOnSurfaceVariant = Color(0xFFA3A3B2)
val SoulDisabled = Color(0xFF6B6B7A)

// ----------------------------------------------------------------------------
// Brand - mint -> aqua -> sky
// ----------------------------------------------------------------------------

/** Primary brand mint. Buttons, active arcs, focus rings, toggles. */
val Mint = Color(0xFF7CF5C4)

/** Text/icon colour on top of mint (and on top of the mint gradient). */
val OnMint = Color(0xFF00201A)

/** Secondary aqua - the focus/countdown colour. */
val Aqua = Color(0xFF4FD1E8)

/** Tertiary sky - breaks, links, secondary highlights. */
val Sky = Color(0xFF5B9DFF)

/** The brand gradient stops: 0 / 0.5 / 1 = mint / aqua / sky. */
val BrandGradientStops = listOf(Mint, Aqua, Sky)

fun brandGradient(horizontal: Boolean = true): Brush =
    if (horizontal) Brush.horizontalGradient(BrandGradientStops)
    else Brush.verticalGradient(BrandGradientStops)

// ----------------------------------------------------------------------------
// Day status - shapes and icons only, labels stay onSurface
// ----------------------------------------------------------------------------

/** All assigned tasks completed. */
val StatusDone = Color(0xFF5EF2B4)

/** Tasks were assigned but not every one was finished. */
val StatusTasksNotMet = Color(0xFFFFB84D)

/** Focus time fell short of the plan. */
val StatusNotProductive = Color(0xFFB39DFF)

/** Distraction ate the day. */
val StatusWastingTime = Color(0xFFFF5D6C)

/** Nothing assigned yet / day still running - the dim segment of the logo. */
val StatusPending = Color(0xFF2A2A33)

/** The white dot that marks today's segment on the week ring. */
val StatusTodayDot = Color(0xFFFFFFFF)

/**
 * The four statuses a finished day can land on, plus the two "not written
 * yet" states, bundled so the rings, history and widgets all agree.
 */
@Immutable
data class DayStatusPalette(
    val done: Color = StatusDone,
    val tasksNotMet: Color = StatusTasksNotMet,
    val notProductive: Color = StatusNotProductive,
    val wastingTime: Color = StatusWastingTime,
    val noTasks: Color = StatusPending,
    val inProgress: Color = StatusPending,
    val pending: Color = StatusPending,
    val todayDot: Color = StatusTodayDot
) {
    fun colorFor(status: DayStatus?): Color = when (status) {
        DayStatus.DONE -> done
        DayStatus.TASKS_NOT_MET -> tasksNotMet
        DayStatus.NOT_PRODUCTIVE -> notProductive
        DayStatus.WASTING_TIME -> wastingTime
        DayStatus.NO_TASKS -> noTasks
        DayStatus.IN_PROGRESS -> inProgress
        null -> pending
    }
}

/** Status colours are provided once, near the root of the theme. */
val LocalDayStatusColors = staticCompositionLocalOf { DayStatusPalette() }

// ----------------------------------------------------------------------------
// Category colours (charts and breakdowns)
// ----------------------------------------------------------------------------

val ProductiveColor = Color(0xFF10B981)
val WastedColor = StatusWastingTime
val ExerciseColor = Color(0xFFF59E0B)
val LearningColor = Color(0xFF3B82F6)
val SocialColor = Color(0xFF8B5CF6)
val SleepColor = Color(0xFF6366F1)
val RecoveryColor = Color(0xFF34D399)
val NeutralColor = Color(0xFF6B7280)

val GradientStart = Mint
val GradientMid = Aqua
val GradientEnd = Sky

val ProductiveGradient = listOf(Color(0xFF10B981), Color(0xFF059669))
val WastedGradient = listOf(StatusWastingTime, Color(0xFFF04657))
val ExerciseGradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706))
val LearningGradient = listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
val SocialGradient = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))
val SleepGradient = listOf(Color(0xFF6366F1), Color(0xFF4F46E5))

// Glass morphism (legacy surfaces still referencing it)
val GlassLight = Color(0x1AFFFFFF)
val GlassDark = Color(0x1A000000)
val GlassBorder = Color(0x33FFFFFF)
