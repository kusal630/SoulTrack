package io.github.dailytrack.ui.gamification

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dailytrack.data.db.entity.AchievementEntity
import io.github.dailytrack.data.db.entity.UserStatsEntity
import io.github.dailytrack.ui.theme.*

@Composable
fun GamificationScreen(
    userStats: UserStatsEntity?,
    achievements: List<AchievementEntity>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            UserStatsCard(stats = userStats)
        }
        
        item {
            LevelProgressCard(level = userStats?.level ?: 1, xp = userStats?.totalXp ?: 0)
        }
        
        item {
            Text(
                text = "Achievements",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(achievements, key = { it.id }) { achievement ->
            AchievementCard(achievement = achievement)
        }
    }
}

@Composable
fun UserStatsCard(stats: UserStatsEntity?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ProductiveColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Your Stats",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Hours", "%.0f".format(stats?.totalHoursTracked ?: 0f), ProductiveColor)
                StatItem("Habits", "${stats?.totalHabitsCompleted ?: 0}", LearningColor)
                StatItem("Goals", "${stats?.totalGoalsAchieved ?: 0}", ExerciseColor)
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
fun LevelProgressCard(level: Int, xp: Long) {
    val xpForNextLevel = level * 1000L
    val progress = (xp % xpForNextLevel).toFloat() / xpForNextLevel

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Level $level",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ProductiveColor
                )
                Text(
                    text = "${xp % xpForNextLevel} / $xpForNextLevel XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = ProductiveColor,
                trackColor = ProductiveColor.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun AchievementCard(achievement: AchievementEntity) {
    val isUnlocked = achievement.isUnlocked
    val scale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) ProductiveColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) ProductiveColor else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = achievement.icon,
                    fontSize = 24.sp,
                    color = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isUnlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { achievement.currentValue.toFloat() / achievement.targetValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = ProductiveColor,
                        trackColor = ProductiveColor.copy(alpha = 0.2f)
                    )
                }
            }
            if (isUnlocked) {
                Text(
                    text = "+${achievement.xpReward} XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = ProductiveColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
