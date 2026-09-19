package io.github.dailytrack.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val icon: String,
    val category: String, // STREAK, TIME, HABIT, GOAL
    val targetValue: Int,
    val currentValue: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val xpReward: Int = 100
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey
    val id: Long = 1,
    val totalXp: Long = 0,
    val level: Int = 1,
    val totalHoursTracked: Float = 0f,
    val totalHabitsCompleted: Int = 0,
    val totalGoalsAchieved: Int = 0,
    val longestStreak: Long = 0,
    val achievementsUnlocked: Int = 0
)
