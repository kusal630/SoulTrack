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

package io.github.dailytrack.ui.techniques

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Draws one small explanatory diagram for a technique. The figures are vector
 * drawings rather than bitmaps: they stay crisp on every screen, follow the
 * accent colour of the category and do not add megabytes to the APK.
 */
@Composable
fun TechniqueFigure(
    kind: FigureKind,
    accent: Color,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    val shape = RoundedCornerShape(if (large) 24.dp else 16.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = if (large) 0.24f else 0.20f),
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            )
            .border(1.dp, accent.copy(alpha = 0.24f), shape)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (large) 22.dp else 10.dp)
        ) {
            drawTechniqueFigure(kind, accent)
        }
    }
}

private fun DrawScope.drawTechniqueFigure(kind: FigureKind, accent: Color) {
    val w = size.width
    val h = size.height
    val s = min(w, h)
    val cx = w / 2f
    val cy = h / 2f
    val line = (s * 0.035f).coerceAtLeast(1.5f)
    val soft = accent.copy(alpha = 0.55f)
    val faint = accent.copy(alpha = 0.20f)
    val ink = Color.White.copy(alpha = 0.86f)
    val dim = Color.White.copy(alpha = 0.30f)
    val radius = s * 0.10f

    fun box(left: Float, top: Float, right: Float, bottom: Float, color: Color, filled: Boolean = true) {
        val width = (right - left).coerceAtLeast(1f)
        val height = (bottom - top).coerceAtLeast(1f)
        if (filled) {
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.45f)
            )
        } else {
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.45f),
                style = Stroke(width = line)
            )
        }
    }

    fun dot(x: Float, y: Float, r: Float, color: Color) = drawCircle(color, r, Offset(x, y))

    fun lineTo(x1: Float, y1: Float, x2: Float, y2: Float, color: Color = soft) {
        drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = line, cap = StrokeCap.Round)
    }

    fun arrow(x1: Float, y1: Float, x2: Float, y2: Float, color: Color = accent) {
        drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth = line, cap = StrokeCap.Round)
        val angle = kotlin.math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        val head = s * 0.10f
        val left = angle - 0.55
        val right = angle + 0.55
        drawLine(
            color,
            Offset(x2, y2),
            Offset(x2 + (head * kotlin.math.cos(left)).toFloat(), y2 + (head * kotlin.math.sin(left)).toFloat()),
            strokeWidth = line,
            cap = StrokeCap.Round
        )
        drawLine(
            color,
            Offset(x2, y2),
            Offset(x2 + (head * kotlin.math.cos(right)).toFloat(), y2 + (head * kotlin.math.sin(right)).toFloat()),
            strokeWidth = line,
            cap = StrokeCap.Round
        )
    }

    fun person(x: Float, y: Float, scale: Float = 1f, color: Color = soft) {
        dot(x, y - s * 0.09f * scale, s * 0.055f * scale, color)
        drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(x - s * 0.11f * scale, y - s * 0.02f * scale),
            size = Size(s * 0.22f * scale, s * 0.22f * scale),
            style = Stroke(width = line)
        )
    }

    fun page(x: Float, y: Float, width: Float, height: Float) {
        box(x, y, x + width, y + height, faint)
        box(x, y, x + width, y + height, soft, filled = false)
        repeat(4) { i ->
            val yy = y + height * (0.22f + i * 0.17f)
            lineTo(x + width * 0.16f, yy, x + width * 0.82f, yy, dim)
        }
    }

    when (kind) {
        FigureKind.MATRIX -> {
            val left = w * 0.12f
            val top = h * 0.12f
            val gap = s * 0.05f
            val cw = (w * 0.76f - gap) / 2f
            val ch = (h * 0.76f - gap) / 2f
            box(left, top, left + cw, top + ch, accent, filled = false)
            box(left + cw + gap, top, left + cw * 2 + gap, top + ch, faint)
            box(left, top + ch + gap, left + cw, top + ch * 2 + gap, faint)
            box(left + cw + gap, top + ch + gap, left + cw * 2 + gap, top + ch * 2 + gap, faint)
            dot(left + cw * 0.5f, top + ch * 0.5f, s * 0.07f, accent)
            dot(left + cw * 1.75f + gap, top + ch * 0.5f, s * 0.045f, soft)
            dot(left + cw * 0.5f, top + ch * 1.5f + gap, s * 0.045f, soft)
            dot(left + cw * 1.75f + gap, top + ch * 1.5f + gap, s * 0.04f, dim)
        }

        FigureKind.PARETO -> {
            val baseline = h * 0.86f
            val bars = listOf(0.78f, 0.52f, 0.20f, 0.15f, 0.11f, 0.08f)
            val bw = w * 0.10f
            val gap = w * 0.055f
            bars.forEachIndexed { i, value ->
                val bh = h * 0.68f * value
                val x = w * 0.10f + i * (bw + gap)
                box(x, baseline - bh, x + bw, baseline, if (i < 2) accent else faint)
            }
            lineTo(w * 0.08f, baseline, w * 0.92f, baseline, dim)
            arrow(w * 0.22f, h * 0.20f, w * 0.22f, h * 0.06f, accent)
            dot(w * 0.72f, h * 0.20f, s * 0.035f, soft)
            dot(w * 0.82f, h * 0.28f, s * 0.025f, dim)
        }

        FigureKind.THREE_CARDS -> {
            val cardW = w * 0.24f
            val cardH = h * 0.64f
            val gap = w * 0.055f
            val start = w * 0.09f
            repeat(3) { i ->
                val x = start + i * (cardW + gap)
                box(x, cy - cardH / 2, x + cardW, cy + cardH / 2, if (i == 0) accent else faint, filled = i == 0)
                if (i != 0) box(x, cy - cardH / 2, x + cardW, cy + cardH / 2, soft, filled = false)
                lineTo(x + cardW * 0.2f, cy - cardH * 0.18f, x + cardW * 0.8f, cy - cardH * 0.18f, if (i == 0) Color.White.copy(alpha = 0.8f) else dim)
                lineTo(x + cardW * 0.2f, cy + cardH * 0.04f, x + cardW * 0.6f, cy + cardH * 0.04f, if (i == 0) Color.White.copy(alpha = 0.6f) else dim)
            }
        }

        FigureKind.FROG -> {
            dot(cx - s * 0.10f, cy - s * 0.10f, s * 0.07f, accent)
            dot(cx + s * 0.10f, cy - s * 0.10f, s * 0.07f, accent)
            dot(cx - s * 0.10f, cy - s * 0.10f, s * 0.035f, ink)
            dot(cx + s * 0.10f, cy - s * 0.10f, s * 0.035f, ink)
            drawArc(
                color = soft,
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.20f, cy - s * 0.10f),
                size = Size(s * 0.40f, s * 0.34f),
                style = Stroke(width = line)
            )
            lineTo(cx - s * 0.05f, cy + s * 0.12f, cx + s * 0.05f, cy + s * 0.12f, soft)
            repeat(3) { i ->
                val y = h * 0.80f - i * s * 0.08f
                lineTo(w * 0.60f, y, w * 0.88f, y, if (i == 0) accent else dim)
            }
        }

        FigureKind.ORDERED_LIST -> {
            repeat(6) { i ->
                val y = h * 0.14f + i * h * 0.13f
                dot(w * 0.13f, y, s * 0.035f, if (i == 0) accent else dim)
                lineTo(w * 0.22f, y, w * 0.88f, y, if (i == 0) accent else dim)
                lineTo(w * 0.22f, y + s * 0.035f, w * 0.60f, y + s * 0.035f, faint)
            }
        }

        FigureKind.TARGET -> {
            repeat(3) { i ->
                val r = s * (0.30f - i * 0.08f)
                drawCircle(if (i == 0) faint else soft, r, Offset(cx, cy), style = Stroke(width = line))
            }
            dot(cx, cy, s * 0.055f, accent)
            arrow(cx + s * 0.38f, cy - s * 0.34f, cx + s * 0.08f, cy - s * 0.04f, accent)
        }

        FigureKind.SCALES -> {
            val beamY = h * 0.30f
            lineTo(cx, h * 0.18f, cx, h * 0.80f, soft)
            lineTo(w * 0.18f, beamY, w * 0.82f, beamY, accent)
            dot(cx, h * 0.18f, s * 0.05f, accent)
            lineTo(w * 0.22f, beamY, w * 0.12f, h * 0.52f, soft)
            lineTo(w * 0.22f, beamY, w * 0.32f, h * 0.52f, soft)
            lineTo(w * 0.78f, beamY, w * 0.68f, h * 0.46f, soft)
            lineTo(w * 0.78f, beamY, w * 0.88f, h * 0.46f, soft)
            drawArc(
                color = accent,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.10f, h * 0.45f),
                size = Size(w * 0.24f, h * 0.18f),
                style = Stroke(width = line)
            )
            drawArc(
                color = soft,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.66f, h * 0.39f),
                size = Size(w * 0.24f, h * 0.18f),
                style = Stroke(width = line)
            )
            lineTo(w * 0.10f, h * 0.80f, w * 0.90f, h * 0.80f, dim)
        }

        FigureKind.COUNTDOWN -> {
            val positions = listOf(
                Offset(w * 0.18f, h * 0.68f),
                Offset(w * 0.32f, h * 0.48f),
                Offset(w * 0.46f, h * 0.34f),
                Offset(w * 0.60f, h * 0.28f),
                Offset(w * 0.74f, h * 0.30f)
            )
            positions.forEachIndexed { i, p ->
                dot(p.x, p.y, s * (0.055f - i * 0.005f), if (i < 3) accent else soft)
            }
            arrow(w * 0.76f, h * 0.34f, w * 0.90f, h * 0.52f, accent)
            lineTo(w * 0.14f, h * 0.84f, w * 0.86f, h * 0.84f, dim)
        }

        FigureKind.TWO_MINUTE -> {
            drawCircle(faint, s * 0.34f, Offset(cx, cy))
            drawCircle(accent, s * 0.34f, Offset(cx, cy), style = Stroke(width = line))
            lineTo(cx, cy, cx, cy - s * 0.20f, ink)
            lineTo(cx, cy, cx + s * 0.16f, cy + s * 0.08f, accent)
            dot(cx, cy, s * 0.035f, accent)
            drawArc(
                color = soft,
                startAngle = -90f,
                sweepAngle = 30f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.38f, cy - s * 0.38f),
                size = Size(s * 0.76f, s * 0.76f),
                style = Stroke(width = line * 1.2f)
            )
        }

        FigureKind.IF_THEN -> {
            val boxW = w * 0.28f
            val boxH = h * 0.30f
            box(w * 0.08f, cy - boxH / 2, w * 0.08f + boxW, cy + boxH / 2, faint)
            box(w * 0.08f, cy - boxH / 2, w * 0.08f + boxW, cy + boxH / 2, soft, filled = false)
            box(w * 0.64f, cy - boxH / 2, w * 0.64f + boxW, cy + boxH / 2, accent)
            lineTo(w * 0.40f, cy - s * 0.05f, w * 0.58f, cy - s * 0.05f, accent)
            arrow(w * 0.40f, cy - s * 0.05f, w * 0.62f, cy - s * 0.05f, accent)
            lineTo(w * 0.40f, cy + s * 0.08f, w * 0.50f, cy + s * 0.08f, dim)
        }

        FigureKind.LINK -> {
            drawCircle(accent, s * 0.15f, Offset(cx - s * 0.14f, cy), style = Stroke(width = line * 1.3f))
            drawCircle(soft, s * 0.15f, Offset(cx + s * 0.14f, cy), style = Stroke(width = line * 1.3f))
            lineTo(cx - s * 0.24f, cy, cx + s * 0.24f, cy, faint)
            dot(cx, cy, s * 0.03f, ink)
        }

        FigureKind.BIG_TO_SMALL -> {
            box(w * 0.08f, h * 0.20f, w * 0.38f, h * 0.80f, soft, filled = false)
            arrow(w * 0.42f, cy, w * 0.54f, cy, accent)
            repeat(3) { i ->
                val y = h * 0.24f + i * h * 0.22f
                box(w * 0.60f, y, w * 0.92f, y + h * 0.13f, if (i == 0) accent else faint)
            }
        }

        FigureKind.LOCK -> {
            drawRoundRect(
                color = accent,
                topLeft = Offset(cx - s * 0.22f, cy - s * 0.02f),
                size = Size(s * 0.44f, s * 0.34f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * 0.5f)
            )
            drawArc(
                color = soft,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.15f, cy - s * 0.28f),
                size = Size(s * 0.30f, s * 0.34f),
                style = Stroke(width = line * 1.3f)
            )
            dot(cx, cy + s * 0.12f, s * 0.05f, ink)
            lineTo(cx, cy + s * 0.12f, cx, cy + s * 0.22f, ink)
        }

        FigureKind.PEOPLE -> {
            person(w * 0.34f, h * 0.58f, 1f, accent)
            person(w * 0.66f, h * 0.58f, 1f, soft)
            drawArc(
                color = faint,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.38f, h * 0.58f),
                size = Size(w * 0.24f, h * 0.18f),
                style = Stroke(width = line)
            )
            lineTo(w * 0.20f, h * 0.84f, w * 0.80f, h * 0.84f, dim)
        }

        FigureKind.NAG -> {
            drawArc(
                color = faint,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.30f, cy - s * 0.30f),
                size = Size(s * 0.60f, s * 0.60f),
                style = Stroke(width = line * 1.4f)
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 250f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.30f, cy - s * 0.30f),
                size = Size(s * 0.60f, s * 0.60f),
                style = Stroke(width = line * 1.4f)
            )
            dot(cx + s * 0.20f, cy - s * 0.22f, s * 0.04f, ink)
            lineTo(cx - s * 0.18f, cy + s * 0.02f, cx + s * 0.18f, cy + s * 0.02f, soft)
        }

        FigureKind.NOTE -> {
            page(w * 0.20f, h * 0.14f, w * 0.60f, h * 0.72f)
            lineTo(w * 0.54f, h * 0.14f, w * 0.80f, h * 0.40f, soft)
            dot(w * 0.30f, h * 0.34f, s * 0.025f, accent)
            dot(w * 0.30f, h * 0.48f, s * 0.025f, accent)
            dot(w * 0.30f, h * 0.62f, s * 0.025f, accent)
        }

        FigureKind.CALENDAR -> {
            box(w * 0.10f, h * 0.18f, w * 0.90f, h * 0.84f, faint)
            box(w * 0.10f, h * 0.18f, w * 0.90f, h * 0.84f, soft, filled = false)
            lineTo(w * 0.10f, h * 0.32f, w * 0.90f, h * 0.32f, soft)
            lineTo(w * 0.22f, h * 0.12f, w * 0.22f, h * 0.24f, accent)
            lineTo(w * 0.78f, h * 0.12f, w * 0.78f, h * 0.24f, soft)
            val blocks = listOf(
                floatArrayOf(0.16f, 0.40f, 0.30f, 0.56f),
                floatArrayOf(0.38f, 0.40f, 0.56f, 0.56f),
                floatArrayOf(0.62f, 0.40f, 0.84f, 0.56f),
                floatArrayOf(0.16f, 0.62f, 0.46f, 0.78f),
                floatArrayOf(0.52f, 0.62f, 0.84f, 0.78f)
            )
            blocks.forEachIndexed { i, b ->
                box(w * b[0], h * b[1], w * b[2], h * b[3], if (i == 1) accent else faint)
            }
        }

        FigureKind.CLOCK -> {
            drawCircle(faint, s * 0.34f, Offset(cx, cy))
            drawCircle(soft, s * 0.34f, Offset(cx, cy), style = Stroke(width = line))
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 230f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.26f, cy - s * 0.26f),
                size = Size(s * 0.52f, s * 0.52f),
                style = Stroke(width = line * 1.8f, cap = StrokeCap.Round)
            )
            lineTo(cx, cy, cx, cy - s * 0.20f, ink)
            lineTo(cx, cy, cx + s * 0.15f, cy + s * 0.07f, ink)
            dot(cx, cy, s * 0.03f, accent)
        }

        FigureKind.WEEK_THEME -> {
            val cols = 7
            val gap = w * 0.018f
            val cw = (w * 0.84f - gap * (cols - 1)) / cols
            repeat(cols) { i ->
                val x = w * 0.08f + i * (cw + gap)
                val top = h * 0.20f + (if (i % 2 == 0) 0f else h * 0.04f)
                box(x, top, x + cw, h * 0.78f, if (i == 1 || i == 4) accent else faint)
                dot(x + cw / 2, top + h * 0.10f, s * 0.018f, if (i == 1 || i == 4) ink else dim)
            }
        }

        FigureKind.BATCH -> {
            repeat(3) { i ->
                val x = w * (0.12f + i * 0.09f)
                box(x, h * (0.16f + i * 0.04f), x + w * 0.16f, h * (0.30f + i * 0.04f), soft, filled = false)
            }
            arrow(w * 0.30f, h * 0.38f, w * 0.48f, h * 0.56f, accent)
            box(w * 0.44f, h * 0.56f, w * 0.88f, h * 0.82f, accent)
            lineTo(w * 0.52f, h * 0.64f, w * 0.80f, h * 0.64f, Color.White.copy(alpha = 0.75f))
            lineTo(w * 0.52f, h * 0.73f, w * 0.70f, h * 0.73f, Color.White.copy(alpha = 0.55f))
        }

        FigureKind.CURVE -> {
            val path = Path()
            path.moveTo(w * 0.08f, h * 0.68f)
            path.cubicTo(w * 0.24f, h * 0.10f, w * 0.36f, h * 0.12f, w * 0.48f, h * 0.40f)
            path.cubicTo(w * 0.60f, h * 0.68f, w * 0.72f, h * 0.74f, w * 0.92f, h * 0.34f)
            drawPath(path, soft, style = Stroke(width = line * 1.5f, cap = StrokeCap.Round))
            box(w * 0.30f, h * 0.14f, w * 0.48f, h * 0.34f, accent.copy(alpha = 0.25f))
            dot(w * 0.40f, h * 0.24f, s * 0.035f, accent)
            lineTo(w * 0.08f, h * 0.86f, w * 0.92f, h * 0.86f, dim)
            lineTo(w * 0.08f, h * 0.18f, w * 0.08f, h * 0.86f, dim)
        }

        FigureKind.AUDIT -> {
            val chartSize = s * 0.58f
            val topLeft = Offset(cx - chartSize / 2, cy - chartSize / 2)
            val sweeps = listOf(150f, 110f, 60f, 40f)
            val colors = listOf(accent, soft, faint, dim)
            var start = -90f
            sweeps.forEachIndexed { i, sweep ->
                drawArc(
                    color = colors[i],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = Size(chartSize, chartSize)
                )
                start += sweep
            }
            drawCircle(Color.Black.copy(alpha = 0.35f), chartSize * 0.16f, Offset(cx, cy))
        }

        FigureKind.BUFFER -> {
            val y = h * 0.48f
            val blocks = listOf(
                floatArrayOf(0.08f, 0.26f),
                floatArrayOf(0.32f, 0.44f),
                floatArrayOf(0.54f, 0.70f),
                floatArrayOf(0.78f, 0.92f)
            )
            blocks.forEachIndexed { i, b ->
                box(w * b[0], y - h * 0.12f, w * b[1], y + h * 0.12f, if (i == 1) accent else faint)
                if (i < blocks.lastIndex) {
                    dot((w * b[1] + w * blocks[i + 1][0]) / 2, y, s * 0.022f, dim)
                }
            }
            lineTo(w * 0.08f, h * 0.70f, w * 0.92f, h * 0.70f, dim)
            lineTo(w * 0.08f, h * 0.28f, w * 0.08f, h * 0.70f, dim)
        }

        FigureKind.ROUTINE -> {
            drawCircle(accent.copy(alpha = 0.85f), s * 0.10f, Offset(w * 0.20f, h * 0.30f))
            repeat(6) { i ->
                val angle = i * 60f * (Math.PI / 180f)
                dot(
                    w * 0.20f + (s * 0.17f * kotlin.math.cos(angle)).toFloat(),
                    h * 0.30f + (s * 0.17f * kotlin.math.sin(angle)).toFloat(),
                    s * 0.018f,
                    soft
                )
            }
            arrow(w * 0.32f, h * 0.30f, w * 0.62f, h * 0.30f, soft)
            val moon = Path()
            moon.addArc(
                androidx.compose.ui.geometry.Rect(w * 0.68f, h * 0.16f, w * 0.88f, h * 0.44f),
                300f,
                260f
            )
            moon.addArc(
                androidx.compose.ui.geometry.Rect(w * 0.72f, h * 0.16f, w * 0.92f, h * 0.44f),
                60f,
                240f
            )
            drawPath(moon, soft)
            repeat(3) { i ->
                dot(w * (0.16f + i * 0.16f), h * 0.66f, s * 0.026f, if (i == 0) accent else faint)
            }
            lineTo(w * 0.10f, h * 0.80f, w * 0.90f, h * 0.80f, dim)
        }

        FigureKind.DEEP_BLOCK -> {
            box(w * 0.08f, h * 0.18f, w * 0.92f, h * 0.82f, faint)
            box(w * 0.13f, h * 0.24f, w * 0.87f, h * 0.76f, accent)
            val shield = Path()
            shield.moveTo(cx, h * 0.34f)
            shield.lineTo(cx + s * 0.13f, h * 0.40f)
            shield.lineTo(cx + s * 0.13f, h * 0.54f)
            shield.quadraticBezierTo(cx, h * 0.68f, cx, h * 0.68f)
            shield.quadraticBezierTo(cx - s * 0.13f, h * 0.68f, cx - s * 0.13f, h * 0.54f)
            shield.lineTo(cx - s * 0.13f, h * 0.40f)
            shield.close()
            drawPath(shield, Color.White.copy(alpha = 0.85f))
            lineTo(cx - s * 0.05f, h * 0.50f, cx + s * 0.05f, h * 0.50f, accent)
            dot(w * 0.18f, h * 0.30f, s * 0.02f, soft)
            dot(w * 0.82f, h * 0.70f, s * 0.02f, soft)
        }

        FigureKind.SINGLE_TASK -> {
            val cell = s * 0.16f
            val gap = s * 0.05f
            val startX = cx - (cell * 1.5f + gap)
            val startY = cy - (cell * 1.5f + gap)
            repeat(3) { row ->
                repeat(3) { col ->
                    val x = startX + col * (cell + gap)
                    val y = startY + row * (cell + gap)
                    val active = row == 1 && col == 1
                    box(x, y, x + cell, y + cell, if (active) accent else faint)
                    if (!active) box(x, y, x + cell, y + cell, dim, filled = false)
                }
            }
            dot(cx, cy, s * 0.035f, Color.White.copy(alpha = 0.9f))
        }

        FigureKind.SHIELD -> {
            val shield = Path()
            shield.moveTo(cx, h * 0.10f)
            shield.lineTo(w * 0.78f, h * 0.24f)
            shield.lineTo(w * 0.72f, h * 0.64f)
            shield.quadraticBezierTo(cx, h * 0.92f, cx, h * 0.92f)
            shield.quadraticBezierTo(w * 0.28f, h * 0.64f, w * 0.28f, h * 0.64f)
            shield.lineTo(w * 0.22f, h * 0.24f)
            shield.close()
            drawPath(shield, faint)
            drawPath(shield, accent, style = Stroke(width = line * 1.4f))
            box(cx - s * 0.10f, h * 0.34f, cx + s * 0.10f, h * 0.64f, Color.White.copy(alpha = 0.75f))
            dot(cx, h * 0.44f, s * 0.018f, accent)
            lineTo(cx - s * 0.04f, h * 0.56f, cx + s * 0.04f, h * 0.56f, accent)
        }

        FigureKind.BELL -> {
            drawArc(
                color = accent,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.22f, h * 0.26f),
                size = Size(s * 0.44f, s * 0.40f),
                style = Stroke(width = line * 1.5f)
            )
            lineTo(cx - s * 0.22f, h * 0.46f, cx - s * 0.28f, h * 0.64f, accent)
            lineTo(cx + s * 0.22f, h * 0.46f, cx + s * 0.28f, h * 0.64f, accent)
            lineTo(cx - s * 0.28f, h * 0.64f, cx + s * 0.28f, h * 0.64f, accent)
            dot(cx, h * 0.70f, s * 0.035f, soft)
            dot(w * 0.78f, h * 0.28f, s * 0.025f, soft)
            dot(w * 0.84f, h * 0.36f, s * 0.018f, dim)
        }

        FigureKind.FOCUS_CUES -> {
            val shade = Path()
            shade.moveTo(cx - s * 0.18f, h * 0.30f)
            shade.lineTo(cx + s * 0.18f, h * 0.30f)
            shade.lineTo(cx + s * 0.09f, h * 0.52f)
            shade.lineTo(cx - s * 0.09f, h * 0.52f)
            shade.close()
            drawPath(shade, accent)
            lineTo(cx, h * 0.52f, cx, h * 0.72f, soft)
            lineTo(cx - s * 0.12f, h * 0.72f, cx + s * 0.12f, h * 0.72f, soft)
            drawArc(
                color = soft,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.10f, h * 0.54f),
                size = Size(w * 0.22f, h * 0.22f),
                style = Stroke(width = line)
            )
            drawArc(
                color = soft,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.68f, h * 0.54f),
                size = Size(w * 0.22f, h * 0.22f),
                style = Stroke(width = line)
            )
            dot(w * 0.21f, h * 0.68f, s * 0.025f, accent)
            dot(w * 0.79f, h * 0.68f, s * 0.025f, accent)
        }

        FigureKind.FLOW -> {
            val left = Path()
            left.moveTo(w * 0.08f, h * 0.24f)
            left.cubicTo(w * 0.28f, h * 0.24f, w * 0.30f, h * 0.50f, w * 0.50f, h * 0.50f)
            left.cubicTo(w * 0.70f, h * 0.50f, w * 0.72f, h * 0.76f, w * 0.92f, h * 0.76f)
            drawPath(left, soft, style = Stroke(width = line * 1.4f, cap = StrokeCap.Round))
            val right = Path()
            right.moveTo(w * 0.08f, h * 0.76f)
            right.cubicTo(w * 0.28f, h * 0.76f, w * 0.30f, h * 0.50f, w * 0.50f, h * 0.50f)
            right.cubicTo(w * 0.70f, h * 0.50f, w * 0.72f, h * 0.24f, w * 0.92f, h * 0.24f)
            drawPath(right, accent, style = Stroke(width = line * 1.4f, cap = StrokeCap.Round))
            dot(cx, cy, s * 0.045f, ink)
        }

        FigureKind.MONK -> {
            drawCircle(faint, s * 0.32f, Offset(cx, cy))
            drawCircle(accent, s * 0.32f, Offset(cx, cy), style = Stroke(width = line * 1.5f))
            person(cx, cy + s * 0.02f, 1.2f, ink)
            repeat(4) { i ->
                val angle = (45f + i * 90f) * (Math.PI / 180f)
                dot(
                    cx + (s * 0.42f * kotlin.math.cos(angle)).toFloat(),
                    cy + (s * 0.42f * kotlin.math.sin(angle)).toFloat(),
                    s * 0.02f,
                    soft
                )
            }
        }

        FigureKind.KANBAN -> {
            val colW = w * 0.24f
            val gap = w * 0.05f
            repeat(3) { col ->
                val x = w * 0.08f + col * (colW + gap)
                box(x, h * 0.16f, x + colW, h * 0.86f, faint)
                box(x, h * 0.16f, x + colW, h * 0.86f, dim, filled = false)
                val cards = if (col == 0) 3 else if (col == 1) 2 else 1
                repeat(cards) { row ->
                    val y = h * 0.24f + row * h * 0.17f
                    box(x + w * 0.025f, y, x + colW - w * 0.025f, y + h * 0.11f, if (col == 1) accent else soft)
                }
            }
        }

        FigureKind.BULLET -> {
            repeat(5) { i ->
                val y = h * (0.20f + i * 0.14f)
                dot(w * 0.16f, y, s * 0.030f, if (i < 2) accent else soft)
                lineTo(w * 0.25f, y, w * 0.84f, y, if (i < 2) accent else dim)
                if (i % 2 == 0) lineTo(w * 0.25f, y + s * 0.04f, w * 0.62f, y + s * 0.04f, faint)
            }
        }

        FigureKind.SPRINT -> {
            drawArc(
                color = faint,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.28f, cy - s * 0.28f),
                size = Size(s * 0.56f, s * 0.56f),
                style = Stroke(width = line * 1.4f)
            )
            drawArc(
                color = accent,
                startAngle = -80f,
                sweepAngle = 250f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.28f, cy - s * 0.28f),
                size = Size(s * 0.56f, s * 0.56f),
                style = Stroke(width = line * 1.8f, cap = StrokeCap.Round)
            )
            arrow(cx + s * 0.20f, cy - s * 0.20f, cx + s * 0.30f, cy - s * 0.04f, accent)
            dot(cx, cy, s * 0.035f, ink)
        }

        FigureKind.MILESTONE -> {
            lineTo(w * 0.12f, h * 0.82f, w * 0.88f, h * 0.20f, dim)
            repeat(3) { i ->
                val t = 0.20f + i * 0.25f
                val x = w * (0.12f + t * 0.76f)
                val y = h * (0.82f - t * 0.62f)
                dot(x, y, s * 0.045f, if (i == 1) accent else soft)
            }
            lineTo(w * 0.55f, h * 0.45f, w * 0.55f, h * 0.18f, accent)
            val flag = Path()
            flag.moveTo(w * 0.55f, h * 0.18f)
            flag.lineTo(w * 0.78f, h * 0.26f)
            flag.lineTo(w * 0.55f, h * 0.34f)
            flag.close()
            drawPath(flag, accent)
        }

        FigureKind.BACKWARD -> {
            lineTo(w * 0.10f, h * 0.50f, w * 0.90f, h * 0.50f, dim)
            repeat(4) { i ->
                val x = w * (0.18f + i * 0.20f)
                dot(x, h * 0.50f, s * 0.04f, if (i == 3) accent else soft)
            }
            arrow(w * 0.82f, h * 0.30f, w * 0.20f, h * 0.30f, accent)
            lineTo(w * 0.90f, h * 0.20f, w * 0.90f, h * 0.80f, faint)
            lineTo(w * 0.10f, h * 0.20f, w * 0.10f, h * 0.80f, faint)
        }

        FigureKind.MVP -> {
            box(w * 0.10f, h * 0.26f, w * 0.42f, h * 0.78f, accent)
            box(w * 0.50f, h * 0.16f, w * 0.90f, h * 0.86f, dim, filled = false)
            dot(w * 0.26f, h * 0.40f, s * 0.025f, ink)
            lineTo(w * 0.18f, h * 0.56f, w * 0.34f, h * 0.56f, ink)
            lineTo(w * 0.18f, h * 0.66f, w * 0.30f, h * 0.66f, ink)
            arrow(w * 0.44f, h * 0.50f, w * 0.50f, h * 0.50f, soft)
        }

        FigureKind.CRACK -> {
            repeat(3) { i ->
                val r = s * (0.30f - i * 0.08f)
                drawCircle(if (i == 0) faint else soft, r, Offset(cx, cy), style = Stroke(width = line))
            }
            dot(cx, cy, s * 0.045f, accent)
            val crack = Path()
            crack.moveTo(cx - s * 0.05f, h * 0.12f)
            crack.lineTo(cx + s * 0.02f, cy - s * 0.12f)
            crack.lineTo(cx - s * 0.03f, cy - s * 0.02f)
            crack.lineTo(cx + s * 0.04f, cy + s * 0.08f)
            crack.lineTo(cx - s * 0.02f, h * 0.88f)
            drawPath(crack, accent, style = Stroke(width = line * 1.2f, cap = StrokeCap.Round))
        }

        FigureKind.MAGNIFY -> {
            page(w * 0.14f, h * 0.16f, w * 0.52f, h * 0.68f)
            drawCircle(Color.Transparent, s * 0.20f, Offset(w * 0.68f, h * 0.56f))
            drawCircle(accent, s * 0.20f, Offset(w * 0.68f, h * 0.56f), style = Stroke(width = line * 1.5f))
            lineTo(w * 0.82f, h * 0.72f, w * 0.94f, h * 0.86f, accent)
        }

        FigureKind.CHECKLIST -> {
            repeat(4) { i ->
                val y = h * (0.20f + i * 0.16f)
                box(w * 0.14f, y - s * 0.04f, w * 0.14f + s * 0.08f, y + s * 0.04f, if (i < 3) accent else faint)
                if (i < 3) {
                    lineTo(w * 0.155f, y, w * 0.17f, y + s * 0.025f, Color.White.copy(alpha = 0.85f))
                    lineTo(w * 0.17f, y + s * 0.025f, w * 0.21f, y - s * 0.025f, Color.White.copy(alpha = 0.85f))
                }
                lineTo(w * 0.30f, y, w * 0.86f, y, if (i < 3) soft else dim)
            }
        }

        FigureKind.INBOX -> {
            val funnel = Path()
            funnel.moveTo(w * 0.12f, h * 0.18f)
            funnel.lineTo(w * 0.88f, h * 0.18f)
            funnel.lineTo(w * 0.58f, h * 0.52f)
            funnel.lineTo(w * 0.58f, h * 0.82f)
            funnel.lineTo(w * 0.42f, h * 0.88f)
            funnel.lineTo(w * 0.42f, h * 0.52f)
            funnel.close()
            drawPath(funnel, accent.copy(alpha = 0.80f))
            repeat(3) { i ->
                dot(w * (0.32f + i * 0.18f), h * 0.10f, s * 0.02f, soft)
            }
        }

        FigureKind.PARA -> {
            val labels = 4
            repeat(labels) { i ->
                val y = h * (0.16f + i * 0.19f)
                box(w * 0.14f, y, w * 0.86f, y + h * 0.14f, if (i == 0) accent else faint)
                if (i != 0) box(w * 0.14f, y, w * 0.86f, y + h * 0.14f, dim, filled = false)
                lineTo(w * 0.22f, y + h * 0.07f, w * 0.54f, y + h * 0.07f, if (i == 0) Color.White.copy(alpha = 0.8f) else dim)
            }
        }

        FigureKind.LINKS -> {
            val points = listOf(
                Offset(w * 0.26f, h * 0.30f),
                Offset(w * 0.72f, h * 0.28f),
                Offset(w * 0.50f, h * 0.72f)
            )
            lineTo(points[0].x, points[0].y, points[1].x, points[1].y, soft)
            lineTo(points[1].x, points[1].y, points[2].x, points[2].y, soft)
            lineTo(points[2].x, points[2].y, points[0].x, points[0].y, accent)
            points.forEachIndexed { i, p -> dot(p.x, p.y, s * 0.075f, if (i == 0) accent else faint) }
        }

        FigureKind.MINDMAP -> {
            dot(cx, cy, s * 0.075f, accent)
            val branches = listOf(
                Offset(w * 0.18f, h * 0.22f),
                Offset(w * 0.82f, h * 0.22f),
                Offset(w * 0.18f, h * 0.78f),
                Offset(w * 0.82f, h * 0.78f),
                Offset(w * 0.50f, h * 0.12f)
            )
            branches.forEach { p ->
                lineTo(cx, cy, p.x, p.y, soft)
                dot(p.x, p.y, s * 0.035f, faint)
            }
            repeat(4) { i ->
                val angle = i * 90f * (Math.PI / 180f)
                val x = cx + (s * 0.22f * kotlin.math.cos(angle)).toFloat()
                val y = cy + (s * 0.22f * kotlin.math.sin(angle)).toFloat()
                dot(x, y, s * 0.02f, ink)
            }
        }

        FigureKind.STACK -> {
            repeat(4) { i ->
                val y = h * 0.76f - i * h * 0.15f
                val inset = i * w * 0.03f
                box(w * 0.16f + inset, y, w * 0.84f - inset, y + h * 0.11f, if (i == 3) accent else faint)
                if (i != 3) lineTo(w * 0.24f + inset, y + h * 0.055f, w * 0.70f - inset, y + h * 0.055f, dim)
            }
        }

        FigureKind.MIRROR -> {
            drawCircle(faint, s * 0.18f, Offset(w * 0.36f, h * 0.36f))
            person(w * 0.36f, h * 0.56f, 1.1f, accent)
            lineTo(w * 0.58f, h * 0.16f, w * 0.58f, h * 0.84f, soft)
            drawCircle(faint.copy(alpha = 0.35f), s * 0.18f, Offset(w * 0.74f, h * 0.36f))
            person(w * 0.74f, h * 0.56f, 1.1f, soft)
            dot(w * 0.74f, h * 0.24f, s * 0.022f, ink)
        }

        FigureKind.TROPHY -> {
            drawArc(
                color = accent,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - s * 0.16f, h * 0.20f),
                size = Size(s * 0.32f, s * 0.26f)
            )
            lineTo(cx - s * 0.15f, h * 0.30f, cx - s * 0.26f, h * 0.34f, soft)
            lineTo(cx + s * 0.15f, h * 0.30f, cx + s * 0.26f, h * 0.34f, soft)
            drawArc(
                color = soft,
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.26f, h * 0.26f),
                size = Size(s * 0.16f, s * 0.20f),
                style = Stroke(width = line)
            )
            drawArc(
                color = soft,
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx + s * 0.10f, h * 0.26f),
                size = Size(s * 0.16f, s * 0.20f),
                style = Stroke(width = line)
            )
            lineTo(cx, h * 0.46f, cx, h * 0.62f, accent)
            box(cx - s * 0.12f, h * 0.62f, cx + s * 0.12f, h * 0.72f, accent)
            box(cx - s * 0.16f, h * 0.72f, cx + s * 0.16f, h * 0.80f, soft)
            dot(cx, h * 0.34f, s * 0.028f, ink)
        }

        FigureKind.OPEN_BOOK -> {
            val leftPage = Path()
            leftPage.moveTo(cx, h * 0.30f)
            leftPage.lineTo(w * 0.12f, h * 0.22f)
            leftPage.lineTo(w * 0.12f, h * 0.76f)
            leftPage.lineTo(cx, h * 0.84f)
            leftPage.close()
            drawPath(leftPage, faint)
            drawPath(leftPage, soft, style = Stroke(width = line))
            val rightPage = Path()
            rightPage.moveTo(cx, h * 0.30f)
            rightPage.lineTo(w * 0.88f, h * 0.22f)
            rightPage.lineTo(w * 0.88f, h * 0.76f)
            rightPage.lineTo(cx, h * 0.84f)
            rightPage.close()
            drawPath(rightPage, accent.copy(alpha = 0.28f))
            drawPath(rightPage, accent, style = Stroke(width = line))
            lineTo(cx, h * 0.30f, cx, h * 0.84f, accent)
            repeat(3) { i ->
                lineTo(w * 0.20f, h * (0.36f + i * 0.10f), w * 0.44f, h * (0.40f + i * 0.10f), dim)
                lineTo(w * 0.56f, h * (0.40f + i * 0.10f), w * 0.80f, h * (0.36f + i * 0.10f), dim)
            }
        }

        FigureKind.SPEECH -> {
            val bubble = Path()
            bubble.moveTo(w * 0.14f, h * 0.18f)
            bubble.lineTo(w * 0.86f, h * 0.18f)
            bubble.quadraticBezierTo(w * 0.94f, h * 0.18f, w * 0.94f, h * 0.28f)
            bubble.lineTo(w * 0.94f, h * 0.58f)
            bubble.quadraticBezierTo(w * 0.94f, h * 0.68f, w * 0.84f, h * 0.68f)
            bubble.lineTo(w * 0.46f, h * 0.68f)
            bubble.lineTo(w * 0.30f, h * 0.84f)
            bubble.lineTo(w * 0.30f, h * 0.68f)
            bubble.lineTo(w * 0.16f, h * 0.68f)
            bubble.quadraticBezierTo(w * 0.06f, h * 0.68f, w * 0.06f, h * 0.58f)
            bubble.lineTo(w * 0.06f, h * 0.28f)
            bubble.quadraticBezierTo(w * 0.06f, h * 0.18f, w * 0.14f, h * 0.18f)
            bubble.close()
            drawPath(bubble, accent.copy(alpha = 0.85f))
            repeat(3) { i ->
                dot(w * (0.28f + i * 0.14f), h * 0.43f, s * 0.030f, Color.White.copy(alpha = 0.85f))
            }
        }

        FigureKind.SPACED -> {
            val intervals = listOf(0.08f, 0.22f, 0.42f, 0.68f, 0.88f)
            lineTo(w * 0.08f, h * 0.70f, w * 0.92f, h * 0.70f, dim)
            intervals.forEachIndexed { i, x ->
                val barHeight = h * (0.12f + i * 0.07f)
                box(w * x, h * 0.70f - barHeight, w * x + w * 0.05f, h * 0.70f, if (i < 2) accent else soft)
                dot(w * x + w * 0.025f, h * 0.78f, s * 0.018f, faint)
            }
        }

        FigureKind.WEAVE -> {
            val one = Path()
            one.moveTo(w * 0.08f, h * 0.32f)
            one.cubicTo(w * 0.30f, h * 0.10f, w * 0.54f, h * 0.62f, w * 0.92f, h * 0.40f)
            drawPath(one, accent, style = Stroke(width = line * 1.5f, cap = StrokeCap.Round))
            val two = Path()
            two.moveTo(w * 0.08f, h * 0.68f)
            two.cubicTo(w * 0.30f, h * 0.90f, w * 0.54f, h * 0.38f, w * 0.92f, h * 0.60f)
            drawPath(two, soft, style = Stroke(width = line * 1.5f, cap = StrokeCap.Round))
        }

        FigureKind.PALACE -> {
            box(w * 0.16f, h * 0.26f, w * 0.84f, h * 0.82f, faint)
            val roof = Path()
            roof.moveTo(w * 0.10f, h * 0.28f)
            roof.lineTo(cx, h * 0.10f)
            roof.lineTo(w * 0.90f, h * 0.28f)
            roof.close()
            drawPath(roof, accent.copy(alpha = 0.75f))
            repeat(4) { i ->
                val x = w * (0.22f + i * 0.16f)
                box(x, h * 0.34f, x + w * 0.08f, h * 0.72f, soft, filled = false)
            }
            lineTo(w * 0.12f, h * 0.82f, w * 0.88f, h * 0.82f, dim)
            dot(w * 0.68f, h * 0.56f, s * 0.025f, accent)
        }

        FigureKind.DUAL -> {
            page(w * 0.10f, h * 0.22f, w * 0.34f, h * 0.56f)
            box(w * 0.54f, h * 0.22f, w * 0.90f, h * 0.78f, faint)
            box(w * 0.54f, h * 0.22f, w * 0.90f, h * 0.78f, soft, filled = false)
            drawCircle(accent, s * 0.08f, Offset(w * 0.65f, h * 0.38f))
            val mountain = Path()
            mountain.moveTo(w * 0.57f, h * 0.72f)
            mountain.lineTo(w * 0.68f, h * 0.50f)
            mountain.lineTo(w * 0.76f, h * 0.66f)
            mountain.lineTo(w * 0.82f, h * 0.56f)
            mountain.lineTo(w * 0.88f, h * 0.72f)
            mountain.close()
            drawPath(mountain, soft)
            lineTo(w * 0.40f, h * 0.48f, w * 0.48f, h * 0.48f, accent)
        }

        FigureKind.BUILD -> {
            box(w * 0.12f, h * 0.68f, w * 0.88f, h * 0.82f, accent)
            box(w * 0.22f, h * 0.48f, w * 0.50f, h * 0.68f, faint)
            box(w * 0.54f, h * 0.54f, w * 0.78f, h * 0.68f, faint)
            box(w * 0.34f, h * 0.28f, w * 0.62f, h * 0.48f, soft)
            dot(w * 0.48f, h * 0.38f, s * 0.02f, ink)
            lineTo(w * 0.40f, h * 0.56f, w * 0.46f, h * 0.56f, dim)
            lineTo(w * 0.58f, h * 0.62f, w * 0.72f, h * 0.62f, dim)
        }

        FigureKind.MOON -> {
            val moon = Path()
            moon.addArc(
                androidx.compose.ui.geometry.Rect(cx - s * 0.26f, cy - s * 0.30f, cx + s * 0.26f, cy + s * 0.30f),
                290f,
                270f
            )
            moon.addArc(
                androidx.compose.ui.geometry.Rect(cx - s * 0.16f, cy - s * 0.30f, cx + s * 0.36f, cy + s * 0.30f),
                60f,
                240f
            )
            drawPath(moon, accent)
            dot(w * 0.76f, h * 0.24f, s * 0.022f, soft)
            dot(w * 0.84f, h * 0.38f, s * 0.014f, dim)
            dot(w * 0.70f, h * 0.14f, s * 0.014f, dim)
            lineTo(w * 0.12f, h * 0.84f, w * 0.88f, h * 0.84f, dim)
        }

        FigureKind.BATTERY -> {
            box(w * 0.16f, h * 0.32f, w * 0.80f, h * 0.68f, faint)
            box(w * 0.16f, h * 0.32f, w * 0.80f, h * 0.68f, soft, filled = false)
            box(w * 0.80f, h * 0.42f, w * 0.86f, h * 0.58f, soft)
            box(w * 0.21f, h * 0.38f, w * 0.58f, h * 0.62f, accent)
            val bolt = Path()
            bolt.moveTo(w * 0.52f, h * 0.35f)
            bolt.lineTo(w * 0.40f, h * 0.52f)
            bolt.lineTo(w * 0.50f, h * 0.52f)
            bolt.lineTo(w * 0.44f, h * 0.66f)
            bolt.lineTo(w * 0.60f, h * 0.47f)
            bolt.lineTo(w * 0.50f, h * 0.47f)
            bolt.close()
            drawPath(bolt, Color.Black.copy(alpha = 0.55f))
        }

        FigureKind.WALK -> {
            person(w * 0.44f, h * 0.38f, 1.1f, accent)
            lineTo(w * 0.44f, h * 0.50f, w * 0.50f, h * 0.68f, soft)
            lineTo(w * 0.50f, h * 0.68f, w * 0.42f, h * 0.86f, soft)
            lineTo(w * 0.44f, h * 0.64f, w * 0.60f, h * 0.82f, soft)
            lineTo(w * 0.48f, h * 0.54f, w * 0.66f, h * 0.46f, soft)
            lineTo(w * 0.48f, h * 0.56f, w * 0.30f, h * 0.48f, soft)
            repeat(3) { i ->
                dot(w * (0.74f + i * 0.05f), h * (0.70f - i * 0.06f), s * 0.018f, faint)
            }
        }

        FigureKind.WATER -> {
            val drop = Path()
            drop.moveTo(w * 0.50f, h * 0.16f)
            drop.cubicTo(w * 0.64f, h * 0.36f, w * 0.70f, h * 0.48f, w * 0.70f, h * 0.58f)
            drop.cubicTo(w * 0.70f, h * 0.72f, w * 0.61f, h * 0.80f, w * 0.50f, h * 0.80f)
            drop.cubicTo(w * 0.39f, h * 0.80f, w * 0.30f, h * 0.72f, w * 0.30f, h * 0.58f)
            drop.cubicTo(w * 0.30f, h * 0.48f, w * 0.36f, h * 0.36f, w * 0.50f, h * 0.16f)
            drop.close()
            drawPath(drop, accent.copy(alpha = 0.75f))
            drawPath(drop, accent, style = Stroke(width = line))
            lineTo(w * 0.40f, h * 0.58f, w * 0.60f, h * 0.58f, Color.White.copy(alpha = 0.5f))
            lineTo(w * 0.42f, h * 0.66f, w * 0.58f, h * 0.66f, Color.White.copy(alpha = 0.35f))
        }

        FigureKind.PAUSE -> {
            drawCircle(faint, s * 0.32f, Offset(cx, cy))
            drawCircle(soft, s * 0.32f, Offset(cx, cy), style = Stroke(width = line))
            box(cx - s * 0.11f, cy - s * 0.14f, cx - s * 0.03f, cy + s * 0.14f, accent)
            box(cx + s * 0.03f, cy - s * 0.14f, cx + s * 0.11f, cy + s * 0.14f, accent)
            drawArc(
                color = soft,
                startAngle = -60f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(cx - s * 0.42f, cy - s * 0.42f),
                size = Size(s * 0.84f, s * 0.84f),
                style = Stroke(width = line, cap = StrokeCap.Round)
            )
        }

        FigureKind.BREATH -> {
            repeat(4) { i ->
                drawCircle(
                    if (i == 0) accent else faint,
                    s * (0.10f + i * 0.08f),
                    Offset(cx, cy),
                    style = Stroke(width = if (i == 0) line * 1.6f else line)
                )
            }
            dot(cx, cy, s * 0.03f, ink)
            lineTo(w * 0.10f, h * 0.86f, w * 0.90f, h * 0.86f, dim)
        }

        FigureKind.UNPLUG -> {
            drawArc(
                color = accent,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.20f, h * 0.26f),
                size = Size(w * 0.30f, h * 0.30f),
                style = Stroke(width = line * 1.2f)
            )
            lineTo(w * 0.35f, h * 0.42f, w * 0.35f, h * 0.62f, soft)
            lineTo(w * 0.65f, h * 0.42f, w * 0.65f, h * 0.62f, soft)
            lineTo(w * 0.35f, h * 0.62f, w * 0.65f, h * 0.62f, soft)
            lineTo(w * 0.50f, h * 0.62f, w * 0.50f, h * 0.82f, soft)
            lineTo(w * 0.24f, h * 0.18f, w * 0.76f, h * 0.86f, accent)
        }

        FigureKind.FENCE -> {
            repeat(5) { i ->
                val x = w * (0.16f + i * 0.17f)
                lineTo(x, h * 0.30f, x, h * 0.82f, if (i == 2) accent else soft)
            }
            lineTo(w * 0.10f, h * 0.46f, w * 0.90f, h * 0.46f, soft)
            lineTo(w * 0.10f, h * 0.66f, w * 0.90f, h * 0.66f, soft)
            dot(w * 0.50f, h * 0.20f, s * 0.035f, accent)
        }

        FigureKind.HEART -> {
            val heart = Path()
            heart.moveTo(cx, h * 0.78f)
            heart.cubicTo(w * 0.10f, h * 0.54f, w * 0.18f, h * 0.20f, cx, h * 0.36f)
            heart.cubicTo(w * 0.82f, h * 0.20f, w * 0.90f, h * 0.54f, cx, h * 0.78f)
            heart.close()
            drawPath(heart, accent.copy(alpha = 0.75f))
            drawPath(heart, accent, style = Stroke(width = line))
            val pulse = Path()
            pulse.moveTo(w * 0.22f, h * 0.52f)
            pulse.lineTo(w * 0.38f, h * 0.52f)
            pulse.lineTo(w * 0.44f, h * 0.42f)
            pulse.lineTo(w * 0.52f, h * 0.62f)
            pulse.lineTo(w * 0.58f, h * 0.50f)
            pulse.lineTo(w * 0.78f, h * 0.50f)
            drawPath(pulse, Color.White.copy(alpha = 0.7f), style = Stroke(width = line, cap = StrokeCap.Round))
        }

        FigureKind.STAIRS -> {
            val steps = 5
            repeat(steps) { i ->
                val left = w * (0.10f + i * 0.16f)
                val top = h * (0.82f - i * 0.14f)
                box(left, top, left + w * 0.16f, h * 0.82f, if (i == steps - 1) accent else faint)
                if (i < steps - 1) lineTo(left + w * 0.10f, top, left + w * 0.16f, top - h * 0.14f, soft)
            }
            arrow(w * 0.76f, h * 0.28f, w * 0.88f, h * 0.14f, accent)
        }

        FigureKind.REVIEW_ZOOM -> {
            box(w * 0.10f, h * 0.18f, w * 0.70f, h * 0.80f, faint)
            box(w * 0.10f, h * 0.18f, w * 0.70f, h * 0.80f, soft, filled = false)
            lineTo(w * 0.10f, h * 0.32f, w * 0.70f, h * 0.32f, soft)
            repeat(2) { row ->
                repeat(3) { col ->
                    val x = w * (0.17f + col * 0.17f)
                    val y = h * (0.40f + row * 0.16f)
                    box(x, y, x + w * 0.10f, y + h * 0.09f, if (row == 0 && col == 1) accent else dim)
                }
            }
            drawCircle(accent, s * 0.16f, Offset(w * 0.72f, h * 0.68f), style = Stroke(width = line * 1.5f))
            lineTo(w * 0.83f, h * 0.79f, w * 0.94f, h * 0.90f, accent)
            lineTo(w * 0.66f, h * 0.68f, w * 0.78f, h * 0.68f, soft)
        }

        FigureKind.LIGHTS -> {
            val colors = listOf(accent, soft, faint)
            repeat(3) { i ->
                val x = w * (0.24f + i * 0.26f)
                box(x - w * 0.08f, h * 0.20f, x + w * 0.08f, h * 0.62f, faint)
                box(x - w * 0.08f, h * 0.20f, x + w * 0.08f, h * 0.62f, dim, filled = false)
                dot(x, h * (0.32f + i * 0.12f), s * 0.035f, colors[i])
            }
            lineTo(w * 0.16f, h * 0.72f, w * 0.84f, h * 0.72f, dim)
        }

        FigureKind.ARROW_STEPS -> {
            repeat(4) { i ->
                val x = w * (0.14f + i * 0.18f)
                val y = h * (0.72f - i * 0.14f)
                lineTo(x, y, x, y + h * 0.12f, faint)
                arrow(x, y, x + w * 0.12f, y - h * 0.08f, if (i == 3) accent else soft)
            }
            lineTo(w * 0.10f, h * 0.84f, w * 0.90f, h * 0.84f, dim)
        }

        FigureKind.TRASH -> {
            box(w * 0.30f, h * 0.30f, w * 0.70f, h * 0.82f, faint)
            box(w * 0.30f, h * 0.30f, w * 0.70f, h * 0.82f, soft, filled = false)
            lineTo(w * 0.24f, h * 0.30f, w * 0.76f, h * 0.30f, accent)
            lineTo(w * 0.40f, h * 0.20f, w * 0.60f, h * 0.20f, accent)
            lineTo(w * 0.40f, h * 0.44f, w * 0.40f, h * 0.70f, dim)
            lineTo(w * 0.50f, h * 0.44f, w * 0.50f, h * 0.70f, dim)
            lineTo(w * 0.60f, h * 0.44f, w * 0.60f, h * 0.70f, dim)
            dot(w * 0.82f, h * 0.26f, s * 0.025f, accent)
        }

        FigureKind.HANDOFF -> {
            person(w * 0.26f, h * 0.52f, 1f, accent)
            person(w * 0.74f, h * 0.52f, 1f, soft)
            arrow(w * 0.38f, h * 0.54f, w * 0.62f, h * 0.54f, accent)
            lineTo(w * 0.30f, h * 0.72f, w * 0.46f, h * 0.66f, faint)
            lineTo(w * 0.70f, h * 0.72f, w * 0.54f, h * 0.66f, faint)
        }

        FigureKind.MEETING -> {
            drawOval(
                color = faint,
                topLeft = Offset(w * 0.20f, h * 0.50f),
                size = Size(w * 0.60f, h * 0.26f)
            )
            person(w * 0.32f, h * 0.44f, 0.8f, accent)
            person(w * 0.50f, h * 0.40f, 0.8f, soft)
            person(w * 0.68f, h * 0.44f, 0.8f, soft)
            drawCircle(accent, s * 0.07f, Offset(w * 0.82f, h * 0.24f), style = Stroke(width = line))
            lineTo(w * 0.82f, h * 0.24f, w * 0.82f, h * 0.16f, accent)
            lineTo(w * 0.82f, h * 0.24f, w * 0.87f, h * 0.26f, accent)
        }

        FigureKind.PLANE -> {
            val plane = Path()
            plane.moveTo(w * 0.10f, h * 0.48f)
            plane.lineTo(w * 0.88f, h * 0.16f)
            plane.lineTo(w * 0.56f, h * 0.82f)
            plane.lineTo(w * 0.46f, h * 0.58f)
            plane.close()
            drawPath(plane, accent.copy(alpha = 0.8f))
            lineTo(w * 0.46f, h * 0.58f, w * 0.88f, h * 0.16f, soft)
            lineTo(w * 0.30f, h * 0.62f, w * 0.18f, h * 0.70f, faint)
            lineTo(w * 0.18f, h * 0.70f, w * 0.26f, h * 0.56f, faint)
        }

        FigureKind.DOC -> {
            page(w * 0.20f, h * 0.12f, w * 0.56f, h * 0.76f)
            lineTo(w * 0.58f, h * 0.12f, w * 0.76f, h * 0.32f, soft)
            lineTo(w * 0.62f, h * 0.32f, w * 0.76f, h * 0.32f, soft)
            drawCircle(accent, s * 0.10f, Offset(w * 0.68f, h * 0.68f))
            lineTo(w * 0.63f, h * 0.68f, w * 0.67f, h * 0.72f, Color.White.copy(alpha = 0.9f))
            lineTo(w * 0.67f, h * 0.72f, w * 0.74f, h * 0.63f, Color.White.copy(alpha = 0.9f))
        }
    }
}
