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

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PieChartSlice(
    val value: Float,
    val color: Color,
    val label: String
)

@Composable
fun PieChart(
    slices: List<PieChartSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = 20.dp,
    centerContent: @Composable (() -> Unit)? = null
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = this.size
            val stroke = strokeWidth.toPx()
            val radius = (canvasSize.minDimension - stroke) / 2
            val topLeft = Offset(
                (canvasSize.width - radius * 2) / 2,
                (canvasSize.height - radius * 2) / 2
            )
            val arcSize = Size(radius * 2, radius * 2)

            if (total == 0f) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            } else {
                var startAngle = -90f
                for (slice in slices) {
                    if (slice.value <= 0f) continue
                    val sweepAngle = (slice.value / total) * 360f * animatedProgress.value
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    startAngle += (slice.value / total) * 360f
                }
            }
        }

        if (centerContent != null) {
            centerContent()
        }
    }
}

@Composable
fun PieChartLegend(
    slices: List<PieChartSlice>,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (slice in slices) {
            if (slice.value <= 0f) continue
            val pct = if (total > 0) (slice.value / total * 100).toInt() else 0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawCircle(color = slice.color)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = slice.label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = slice.color
                )
            }
        }
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 12.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    centerContent: @Composable (() -> Unit)? = null
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = this.size
            val stroke = strokeWidth.toPx()
            val radius = (canvasSize.minDimension - stroke) / 2
            val topLeft = Offset(
                (canvasSize.width - radius * 2) / 2,
                (canvasSize.height - radius * 2) / 2
            )
            val arcSize = Size(radius * 2, radius * 2)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        if (centerContent != null) {
            centerContent()
        }
    }
}

@Composable
fun MiniPieChart(
    slices: List<PieChartSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Canvas(modifier = modifier.size(size)) {
        val stroke = 4.dp.toPx()
        val radius = (size.toPx() - stroke) / 2
        val topLeft = Offset(
            (this.size.width - radius * 2) / 2,
            (this.size.height - radius * 2) / 2
        )
        val arcSize = Size(radius * 2, radius * 2)

        if (total == 0f) {
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )
        } else {
            var startAngle = -90f
            for (slice in slices) {
                if (slice.value <= 0f) continue
                val sweepAngle = (slice.value / total) * 360f
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                startAngle += sweepAngle
            }
        }
    }
}
