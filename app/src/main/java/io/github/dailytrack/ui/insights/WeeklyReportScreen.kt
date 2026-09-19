package io.github.dailytrack.ui.insights

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dailytrack.engine.TimeCoverage
import io.github.dailytrack.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun WeeklyReportScreen(
    weeklyData: Map<LocalDate, TimeCoverage>,
    productivityScore: Float
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProductivityScoreCard(score = productivityScore)
        }
        
        item {
            WeeklyOverviewCard(weeklyData = weeklyData)
        }
        
        item {
            DailyBreakdownCard(weeklyData = weeklyData)
        }
        
        item {
            InsightsCard(
                insights = listOf(
                    "You've been most productive on Tuesdays",
                    "Consider adding more exercise time",
                    "Your sleep schedule has been consistent"
                )
            )
        }
    }
}

@Composable
fun ProductivityScoreCard(score: Float) {
    val animatedScore by animateFloatAsState(
        targetValue = score,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "score"
    )

    val scoreColor = when {
        score >= 80 -> ProductiveColor
        score >= 60 -> ExerciseColor
        score >= 40 -> LearningColor
        else -> WastedColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = scoreColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Weekly Score",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = getScoreMessage(score),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color = Color.Gray.copy(alpha = 0.12f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = scoreColor,
                        startAngle = -90f,
                        sweepAngle = 360f * (animatedScore / 100f).coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${score.toInt()}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = scoreColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun WeeklyOverviewCard(weeklyData: Map<LocalDate, TimeCoverage>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val totalHours = weeklyData.values.sumOf { it.trackedSeconds } / 3600.0
            val productiveHours = weeklyData.values.sumOf { it.productiveSeconds + it.learningSeconds } / 3600.0
            val exerciseHours = weeklyData.values.sumOf { it.exerciseSeconds } / 3600.0
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Total", "%.1fh".format(totalHours), ProductiveColor)
                StatItem("Productive", "%.1fh".format(productiveHours), LearningColor)
                StatItem("Exercise", "%.1fh".format(exerciseHours), ExerciseColor)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DailyBreakdownCard(weeklyData: Map<LocalDate, TimeCoverage>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Daily Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val sortedData = weeklyData.entries.sortedBy { it.key }
            
            sortedData.forEach { (date, coverage) ->
                val dayName = date.format(DateTimeFormatter.ofPattern("EEE"))
                val productiveRatio = if (coverage.trackedSeconds > 0) {
                    (coverage.productiveSeconds + coverage.learningSeconds).toFloat() / coverage.trackedSeconds
                } else 0f
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(40.dp)
                    )
                    LinearProgressIndicator(
                        progress = { productiveRatio },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ProductiveColor,
                        trackColor = ProductiveColor.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "%.0f%%".format(productiveRatio * 100),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(40.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun InsightsCard(insights: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            insights.forEach { insight ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💡", modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = insight,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun getScoreMessage(score: Float): String = when {
    score >= 80 -> "Excellent work this week!"
    score >= 60 -> "Good progress, keep it up!"
    score >= 40 -> "Room for improvement"
    else -> "Try to be more productive"
}
