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


package io.github.dailytrack.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.dailytrack.engine.DayStatus
import io.github.dailytrack.ui.theme.Aqua
import io.github.dailytrack.ui.theme.DayStatusPalette
import io.github.dailytrack.ui.theme.LocalDayStatusColors
import io.github.dailytrack.ui.theme.Motion
import io.github.dailytrack.ui.theme.Mint
import io.github.dailytrack.ui.theme.Sky

/**
 * Soul Track geometry, on a 512-unit grid that is scaled to whatever size the
 * composable gets. Keeping the numbers in one place means the hero rings, the
 * history mini rings and the previews can never drift apart.
 *
 * Outer ring (radius 150, stroke 26) = the week.
 * Inner ring (radius 92, stroke 14)  = today.
 */
object SoulRingsSpec {
    const val GRID = 512f
    const val OUTER_RADIUS = 150f
    const val OUTER_STROKE = 26f
    const val INNER_RADIUS = 92f
    const val INNER_STROKE = 14f
    const val NEEDLE_LENGTH = 70f
    const val HUB_RADIUS = 16f
    const val TICKS = 48

    /** Total gap (degrees) left between two week segments. */
    const val WEEK_SEGMENT_INSET_DEG = 8f

    /** Number of days on the outer ring. */
    const val WEEK_DAYS = 7
}

/** Brand gradient position -> colour. 0 = mint at 12 o'clock, 1 = sky. */
fun brandColorAt(t: Float): Color {
    val x = t.coerceIn(0f, 1f)
    return if (x < 0.5f) lerpColor(Mint, Aqua, x / 0.5f)
    else lerpColor(Aqua, Sky, (x - 0.5f) / 0.5f)
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t
)

/**
 * True when the user asked the system to keep animations off. Every animated
 * surface in the app checks this before springing.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * The Soul Track mark, live.
 *
 * - **Inner ring = today**: fills clockwise from 12 o'clock as tasks are
 *   completed, closes at 12 when everything is done and resets every day. The
 *   needle rides the head of that arc.
 * - **Outer ring = week**: seven segments (Mon..Sun). A day is written into its
 *   segment when it ends and keeps that colour for the rest of the week;
 *   today's segment stays dim and carries a white dot.
 *
 * @param dayProgress completed / assigned for today, 0f..1f.
 * @param weekStatuses seven entries (Mon..Sun); `null` or [DayStatus.IN_PROGRESS]
 * means "not written yet".
 * @param todayIndex 0 = Monday .. 6 = Sunday.
 */
@Composable
fun SoulRings(
    dayProgress: Float,
    weekStatuses: List<DayStatus?>,
    todayIndex: Int,
    modifier: Modifier = Modifier,
    showNeedle: Boolean = true,
    animate: Boolean = true,
    onDayCompleted: (() -> Unit)? = null,
    contentDescription: String? = null
) {
    val statusColors = LocalDayStatusColors.current
    val reduceMotion = rememberReduceMotion()
    val shouldAnimate = animate && !reduceMotion
    val context = LocalContext.current

    // A tween keeps the arc gliding to its new value instead of bouncing past
    // it - a day ring should feel like a dial, not a spring.
    val animatedProgress by animateFloatAsState(
        targetValue = dayProgress.coerceIn(0f, 1f),
        animationSpec = if (shouldAnimate) {
            tween(durationMillis = Motion.Slow, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 0)
        },
        label = "soul_rings_progress"
    )

    var wasComplete by remember { mutableStateOf(dayProgress >= 1f) }
    LaunchedEffect(dayProgress) {
        val complete = dayProgress >= 1f
        if (complete && !wasComplete) {
            tickHaptic(context)
            onDayCompleted?.invoke()
        }
        wasComplete = complete
    }

    Box(
        modifier = if (contentDescription != null) {
            modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
        } else {
            modifier
        },
        contentAlignment = Alignment.Center
    ) {
        // Soft glow pass, only where the platform can blur cheaply.
        if (!reduceMotion) {
            RingCanvas(
                progress = animatedProgress,
                weekStatuses = weekStatuses,
                todayIndex = todayIndex,
                statusColors = statusColors,
                showNeedle = showNeedle,
                glow = true,
                modifier = Modifier.blur(18.dp)
            )
        }
        RingCanvas(
            progress = animatedProgress,
            weekStatuses = weekStatuses,
            todayIndex = todayIndex,
            statusColors = statusColors,
            showNeedle = showNeedle,
            glow = false
        )
        // The mark keeps breathing: a slow sheen travels the inner arc so the
        // logo is never a dead sticker, and it parks itself when the user asks
        // the system to reduce motion.
        if (shouldAnimate && animatedProgress > 0f) {
            RingSheen(progress = animatedProgress, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Slow travelling highlight along the inner arc. */
@Composable
private fun RingSheen(progress: Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ring_sheen")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_sheen_rotation"
    )

    Canvas(modifier = modifier) {
        val unit = size.minDimension / SoulRingsSpec.GRID
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = SoulRingsSpec.INNER_RADIUS * unit
        val sweep = 55f
        val start = -90f + rotation + (progress * 360f * 0.7f)

        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.28f),
                    Color.Transparent
                )
            ),
            startAngle = start,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = SoulRingsSpec.INNER_STROKE * unit, cap = StrokeCap.Round),
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2f, radius * 2f),
            alpha = 0.9f
        )
    }
}

/** One short tick when the day ring closes. */
private fun tickHaptic(context: Context) {
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(16, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(16)
        }
    }.onFailure {
        // No vibrator (or no permission) - the visual close is enough.
    }
    @Suppress("DEPRECATION")
    runCatching {
        val view = (context as? android.app.Activity)?.window?.decorView
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}

@Composable
private fun RingCanvas(
    progress: Float,
    weekStatuses: List<DayStatus?>,
    todayIndex: Int,
    statusColors: DayStatusPalette,
    showNeedle: Boolean,
    glow: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawSoulRings(
            progress = progress,
            weekStatuses = weekStatuses,
            todayIndex = todayIndex,
            statusColors = statusColors,
            showNeedle = showNeedle,
            glow = glow
        )
    }
}

private fun DrawScope.drawSoulRings(
    progress: Float,
    weekStatuses: List<DayStatus?>,
    todayIndex: Int,
    statusColors: DayStatusPalette,
    showNeedle: Boolean,
    glow: Boolean
) {
    val unit = size.minDimension / SoulRingsSpec.GRID
    val cx = size.width / 2f
    val cy = size.height / 2f

    fun arcRect(radius: Float) = Rect(
        cx - radius * unit,
        cy - radius * unit,
        cx + radius * unit,
        cy + radius * unit
    )

    fun arcSize(radius: Float) = Size(radius * 2 * unit, radius * 2 * unit)

    with(SoulRingsSpec) {
        // ---------------------------------------------------------- week ring
        val segmentSweep = 360f / WEEK_DAYS
        val inset = WEEK_SEGMENT_INSET_DEG / 2f
        if (!glow) {
            for (day in 0 until WEEK_DAYS) {
                val start = -90f + day * segmentSweep + inset
                val sweep = segmentSweep - WEEK_SEGMENT_INSET_DEG
                val isToday = day == todayIndex
                val color = if (isToday) statusColors.pending
                else statusColors.colorFor(weekStatuses.getOrNull(day))

                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = OUTER_STROKE * unit, cap = StrokeCap.Round),
                    topLeft = Offset(arcRect(OUTER_RADIUS).left, arcRect(OUTER_RADIUS).top),
                    size = arcSize(OUTER_RADIUS)
                )

                if (isToday) {
                    val mid = Math.toRadians((start + sweep / 2f).toDouble())
                    drawCircle(
                        color = statusColors.todayDot,
                        radius = 7f * unit,
                        center = Offset(
                            cx + (OUTER_RADIUS * unit * Math.cos(mid)).toFloat(),
                            cy + (OUTER_RADIUS * unit * Math.sin(mid)).toFloat()
                        )
                    )
                }
            }
        }

        // --------------------------------------------------------- today ring
        val innerTopLeft = Offset(arcRect(INNER_RADIUS).left, arcRect(INNER_RADIUS).top)
        val innerSize = arcSize(INNER_RADIUS)

        if (!glow) {
            drawArc(
                color = Color(0xFF17171F),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = INNER_STROKE * unit, cap = StrokeCap.Round),
                topLeft = innerTopLeft,
                size = innerSize
            )
        }

        val sweep = (progress.coerceIn(0f, 1f)) * 360f
        if (sweep > 0.4f) {
            if (glow) {
                drawArc(
                    color = Aqua.copy(alpha = 0.30f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = INNER_STROKE * 2.2f * unit, cap = StrokeCap.Round),
                    topLeft = innerTopLeft,
                    size = innerSize
                )
                drawArc(
                    color = Mint.copy(alpha = 0.14f),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = INNER_STROKE * 3.6f * unit, cap = StrokeCap.Round),
                    topLeft = innerTopLeft,
                    size = innerSize
                )
            } else {
                drawProgressSweep(
                    cx = cx,
                    cy = cy,
                    unit = unit,
                    sweep = sweep,
                    stroke = INNER_STROKE,
                    radius = INNER_RADIUS
                )
            }
        }

        if (!glow) {
            // Scale ticks punched across the inner ring, like the logo.
            val tickHalf = INNER_STROKE / 2f + 1f
            for (i in 0 until TICKS) {
                val angle = -90f + i * (360f / TICKS)
                val rad = Math.toRadians(angle.toDouble())
                val cos = Math.cos(rad).toFloat()
                val sin = Math.sin(rad).toFloat()
                drawLine(
                    color = Color.Black.copy(alpha = 0.55f),
                    start = Offset(
                        cx + (INNER_RADIUS - tickHalf) * unit * cos,
                        cy + (INNER_RADIUS - tickHalf) * unit * sin
                    ),
                    end = Offset(
                        cx + (INNER_RADIUS + tickHalf) * unit * cos,
                        cy + (INNER_RADIUS + tickHalf) * unit * sin
                    ),
                    strokeWidth = 2.4f * unit
                )
            }

            if (sweep > 0.4f) {
                val head = Math.toRadians((-90f + sweep).toDouble())
                drawCircle(
                    color = Color.White,
                    radius = INNER_STROKE * 0.42f * unit,
                    center = Offset(
                        cx + INNER_RADIUS * unit * Math.cos(head).toFloat(),
                        cy + INNER_RADIUS * unit * Math.sin(head).toFloat()
                    )
                )
            }
        }

        // ------------------------------------------------------------- needle
        if (showNeedle) {
            val needleAngle = Math.toRadians((-90f + sweep).toDouble())
            val cos = Math.cos(needleAngle).toFloat()
            val sin = Math.sin(needleAngle).toFloat()

            if (glow) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Aqua.copy(alpha = 0.75f), Aqua.copy(alpha = 0f))
                    ),
                    radius = HUB_RADIUS * 2.6f * unit,
                    center = Offset(cx, cy)
                )
            } else {
                drawLine(
                    color = Color.White,
                    start = Offset(cx, cy),
                    end = Offset(cx + NEEDLE_LENGTH * unit * cos, cy + NEEDLE_LENGTH * unit * sin),
                    strokeWidth = 7f * unit,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Aqua.copy(alpha = 0.9f), Aqua.copy(alpha = 0f))
                    ),
                    radius = HUB_RADIUS * 1.9f * unit,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color.White,
                    radius = HUB_RADIUS * 0.55f * unit,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

/** The comet-like today arc: gradient colour, fading tail, crisp head. */
private fun DrawScope.drawProgressSweep(
    cx: Float,
    cy: Float,
    unit: Float,
    sweep: Float,
    stroke: Float,
    radius: Float
) {
    val rect = arcRectAt(cx, cy, radius * unit)
    val arcSize = Size(radius * 2 * unit, radius * 2 * unit)
    val topLeft = Offset(rect.left, rect.top)

    // Fine slices near the tail, coarse slices for the solid body.
    val tailSpan = minOf(sweep, 34f)
    val bodySpan = sweep - tailSpan
    var angle = -90f

    if (tailSpan > 0f) {
        val slices = 10
        val sliceSweep = tailSpan / slices
        for (i in 0 until slices) {
            val fromStart = (i + 1) * sliceSweep
            val alpha = (0.12f + 0.88f * (fromStart / tailSpan)).coerceIn(0.12f, 1f)
            drawArc(
                color = brandColorAt((angle + sliceSweep + 90f) / 360f).copy(alpha = alpha),
                startAngle = angle,
                sweepAngle = sliceSweep + 0.6f,
                useCenter = false,
                style = Stroke(width = stroke * unit, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize
            )
            angle += sliceSweep
        }
    }

    if (bodySpan > 0.01f) {
        val slices = (bodySpan / 12f).toInt().coerceAtLeast(1)
        val sliceSweep = bodySpan / slices
        for (i in 0 until slices) {
            val mid = angle + sliceSweep / 2f
            drawArc(
                color = brandColorAt((mid + 90f) / 360f),
                startAngle = angle,
                sweepAngle = sliceSweep + 0.6f,
                useCenter = false,
                style = Stroke(width = stroke * unit, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize
            )
            angle += sliceSweep
        }
    }
}

private fun arcRectAt(cx: Float, cy: Float, r: Float) = Rect(cx - r, cy - r, cx + r, cy + r)

/**
 * One day, one ring: the compact status mark used in History rows, the week
 * strip and widgets. The arc is painted in the day's status colour so the
 * colour always sits next to an icon and a label elsewhere in the UI.
 */
@Composable
fun MiniSoulRing(
    progress: Float,
    status: DayStatus?,
    modifier: Modifier = Modifier,
    isToday: Boolean = false
) {
    val palette = LocalDayStatusColors.current
    val reduceMotion = rememberReduceMotion()
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion) {
            spring(dampingRatio = 1f, stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        },
        label = "mini_ring"
    )
    val accent = if (isToday && status == null) palette.pending else palette.colorFor(status)

    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.11f
        val radius = size.minDimension / 2f - stroke / 2f - 1.dp.toPx()
        val topLeft = Offset(size.width / 2f - radius, size.height / 2f - radius)
        val arcSize = Size(radius * 2, radius * 2)

        drawArc(
            color = palette.pending,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = topLeft,
            size = arcSize
        )
        if (animated > 0.01f) {
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = animated * 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize
            )
        }
        if (isToday) {
            drawCircle(
                color = Color.White,
                radius = stroke * 0.38f,
                center = Offset(size.width / 2f, size.height / 2f - radius)
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Previews
// ----------------------------------------------------------------------------

private val previewWeek = listOf<DayStatus?>(
    DayStatus.DONE,
    DayStatus.TASKS_NOT_MET,
    DayStatus.NOT_PRODUCTIVE,
    DayStatus.WASTING_TIME,
    DayStatus.NO_TASKS,
    null,
    null
)

@Preview(widthDp = 300, heightDp = 300, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SoulRingsNoTasksPreview() {
    SoulRings(
        dayProgress = 0f,
        weekStatuses = listOf(null, null, null, null, null, null, null),
        todayIndex = 0,
        animate = false,
        contentDescription = "Today: no tasks assigned yet."
    )
}

@Preview(widthDp = 300, heightDp = 300, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SoulRingsOneOfFivePreview() {
    SoulRings(
        dayProgress = 0.2f,
        weekStatuses = previewWeek,
        todayIndex = 5,
        animate = false,
        contentDescription = "Today: 1 of 5 tasks done."
    )
}

@Preview(widthDp = 300, heightDp = 300, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SoulRingsAllDonePreview() {
    SoulRings(
        dayProgress = 1f,
        weekStatuses = previewWeek,
        todayIndex = 5,
        animate = false,
        contentDescription = "Today: 5 of 5 tasks done."
    )
}

@Preview(widthDp = 300, heightDp = 300, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SoulRingsFullWeekPreview() {
    SoulRings(
        dayProgress = 0.55f,
        weekStatuses = listOf(
            DayStatus.DONE,
            DayStatus.DONE,
            DayStatus.NOT_PRODUCTIVE,
            DayStatus.TASKS_NOT_MET,
            DayStatus.WASTING_TIME,
            DayStatus.DONE,
            DayStatus.IN_PROGRESS
        ),
        todayIndex = 6,
        animate = false,
        contentDescription = "Week at a glance"
    )
}

@Preview(widthDp = 72, heightDp = 72, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MiniSoulRingPreview() {
    MiniSoulRing(progress = 0.6f, status = DayStatus.TASKS_NOT_MET)
}
