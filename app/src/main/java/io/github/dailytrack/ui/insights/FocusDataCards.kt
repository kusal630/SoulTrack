package io.github.dailytrack.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dailytrack.engine.EisenhowerQuadrant
import io.github.dailytrack.engine.FocusInsightReport
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.NeutralColor
import io.github.dailytrack.ui.theme.ProductiveColor
import io.github.dailytrack.ui.theme.WastedColor

@Composable
fun FocusDataInsightsCard(report: FocusInsightReport) {
    if (!report.hasFocusData && report.comparisons.isEmpty() && report.distraction.totalCycles == 0) {
        return
    }
    SurfaceCard {
        SectionTitle(
            title = "Focus patterns",
            subtitle = "Built from your completed blocks and task estimates"
        )

        if (report.bestHours.isNotEmpty()) {
            Text(
                "Your best focus hours",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                report.bestHours.forEach { hour ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ProductiveColor.copy(alpha = 0.12f))
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "%02d:00".format(hour.hour),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ProductiveColor
                        )
                        Text(
                            "${hour.focusedMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (report.quadrantSplit.totalMinutes > 0) {
            Text(
                "Where your time went",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                EisenhowerQuadrant.entries.forEach { quadrant ->
                    val share = report.quadrantSplit.share(quadrant)
                    if (share > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(share)
                                .fillMaxWidth()
                                .background(quadrantColor(quadrant))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EisenhowerQuadrant.entries.forEach { quadrant ->
                    val share = report.quadrantSplit.share(quadrant)
                    if (share > 0f) {
                        Text(
                            "${quadrantShortLabel(quadrant)} ${(share * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = quadrantColor(quadrant)
                        )
                    }
                }
            }
            report.quadrantSplit.dominant?.let { dominant ->
                Text(
                    "Most of your time is in ${dominant.label}. " + dominant.nudge,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (report.comparisons.isNotEmpty()) {
            Text(
                "Estimate vs reality",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            report.comparisons.take(3).forEach { comparison ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        comparison.title,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        comparison.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (comparison.deltaMinutes > 15) {
                            ExerciseColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            report.averageErrorMinutes?.let { average ->
                val direction = if (average >= 0) "over" else "under"
                Text(
                    "On average you run ${kotlin.math.abs(average)}m $direction your estimates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        val distraction = report.distraction
        if (distraction.totalCycles > 0) {
            Text(
                "When you quit",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                distraction.label,
                style = MaterialTheme.typography.bodySmall,
                color = if (distraction.earlyQuitRate >= 0.4f) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            distraction.averageQuitAtMinutes?.let { minutes ->
                Text(
                    "Average early stop: $minutes minutes into a block.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun quadrantColor(quadrant: EisenhowerQuadrant): Color = when (quadrant) {
    EisenhowerQuadrant.DO_FIRST -> ProductiveColor
    EisenhowerQuadrant.SCHEDULE -> ExerciseColor
    EisenhowerQuadrant.DELEGATE -> NeutralColor
    EisenhowerQuadrant.DELETE -> WastedColor
}

private fun quadrantShortLabel(quadrant: EisenhowerQuadrant): String = when (quadrant) {
    EisenhowerQuadrant.DO_FIRST -> "Q1"
    EisenhowerQuadrant.SCHEDULE -> "Q2"
    EisenhowerQuadrant.DELEGATE -> "Q3"
    EisenhowerQuadrant.DELETE -> "Q4"
}

private val EisenhowerQuadrant.nudge: String
    get() = when (this) {
        EisenhowerQuadrant.DO_FIRST -> "Keep a buffer so urgent work does not crowd out planning."
        EisenhowerQuadrant.SCHEDULE -> "This is the quadrant that compounds. Protect it."
        EisenhowerQuadrant.DELEGATE -> "Can any of this be handed off or batched?"
        EisenhowerQuadrant.DELETE -> "Try deleting one item from this quadrant next week."
    }
