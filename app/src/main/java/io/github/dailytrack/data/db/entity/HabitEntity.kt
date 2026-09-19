package io.github.dailytrack.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val icon: String = "⭐",
    val color: String = "#10B981",
    val frequency: String = "DAILY", // DAILY, WEEKLY, MONTHLY
    val targetCount: Int = 1,
    val currentStreak: Long = 0,
    val bestStreak: Long = 0,
    val totalCompletions: Int = 0,
    val lastCompletedDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(tableName = "habit_completions")
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val completedDate: Long,
    val notes: String = ""
)
