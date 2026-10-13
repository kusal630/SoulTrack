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


package io.github.dailytrack.ui.gamification

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dailytrack.data.db.entity.AchievementEntity
import io.github.dailytrack.data.db.entity.UserStatsEntity
import io.github.dailytrack.ui.components.EmptyState
import io.github.dailytrack.ui.components.ScreenList
import io.github.dailytrack.ui.components.SectionTitle
import io.github.dailytrack.ui.components.StatTile
import io.github.dailytrack.ui.components.SurfaceCard
import io.github.dailytrack.ui.theme.Dimens
import io.github.dailytrack.ui.theme.ExerciseColor
import io.github.dailytrack.ui.theme.LearningColor
import io.github.dailytrack.ui.theme.ProductiveColor

@Composable
fun GamificationScreen(
    userStats: UserStatsEntity?,
    achievements: List<AchievementEntity>
) {
    val level = userStats?.level ?: 1
    val xp = userStats?.totalXp ?: 0L
    val unlockedCount = achievements.count { it.isUnlocked }

    ScreenList(
        title = "Achievements",
        subtitle = "Your level, XP and milestones",
        onBack = null
    ) {
        item {
            LevelHeroCard(level = level, xp = xp)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                StatTile(
                    label = "Hours",
                    value = "%.0f".format(userStats?.totalHoursTracked ?: 0f),
                    icon = Icons.Default.Timer,
                    tint = ProductiveColor,
                    caption = "Tracked",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Habits",
                    value = "${userStats?.totalHabitsCompleted ?: 0}",
                    icon = Icons.Default.CheckCircle,
                    tint = LearningColor,
                    caption = "Completed",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Goals",
                    value = "${userStats?.totalGoalsAchieved ?: 0}",
                    icon = Icons.Default.Flag,
                    tint = ExerciseColor,
                    caption = "Achieved",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionTitle(
                title = "Achievements",
                subtitle = if (achievements.isEmpty()) {
                    "Your first badge is waiting"
                } else {
                    "$unlockedCount of ${achievements.size} unlocked"
                },
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (achievements.isEmpty()) {
            item {
                EmptyState(
                    title = "No achievements yet",
                    message = "Complete habits, hit goals and log focused time — the first badge is closer than you think.",
                    icon = Icons.Default.EmojiEvents,
                    tint = ExerciseColor
                )
            }
        } else {
            items(achievements.chunked(2), key = { chunk -> chunk.first().id }) { chunk ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                    AchievementTile(
                        achievement = chunk[0],
                        modifier = Modifier.weight(1f)
                    )
                    if (chunk.size > 1) {
                        AchievementTile(
                            achievement = chunk[1],
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(4.dp)) }
    }
}

@Composable
private fun LevelHeroCard(level: Int, xp: Long) {
    val xpForNextLevel = (level * 1000L).coerceAtLeast(1L)
    val progress = ((xp % xpForNextLevel).toFloat() / xpForNextLevel).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Level",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$level",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = ProductiveColor,
                    lineHeight = 60.sp
                )
                Text(
                    text = "${xp % xpForNextLevel} / $xpForNextLevel XP to next level",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Dimens.CardGap))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color = ProductiveColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
            Spacer(modifier = Modifier.width(Dimens.CardGap))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ExerciseColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = ExerciseColor,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun AchievementTile(
    achievement: AchievementEntity,
    modifier: Modifier = Modifier
) {
    val isUnlocked = achievement.isUnlocked
    val accent: Color = ExerciseColor
    val progress = if (achievement.targetValue > 0) {
        achievement.currentValue.toFloat() / achievement.targetValue
    } else {
        0f
    }

    SurfaceCard(
        modifier = modifier,
        containerColor = if (isUnlocked) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isUnlocked) {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        contentPadding = PaddingValues(Dimens.CardPadding)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isUnlocked) accent.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = achievement.icon,
                style = MaterialTheme.typography.titleLarge,
                color = if (isUnlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(Dimens.CardGap))
        Text(
            text = achievement.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isUnlocked) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = achievement.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (!isUnlocked) {
            Spacer(modifier = Modifier.height(Dimens.TitleGap))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.outlineVariant,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        } else {
            Spacer(modifier = Modifier.height(Dimens.TitleGap))
            Text(
                text = "+${achievement.xpReward} XP",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}
