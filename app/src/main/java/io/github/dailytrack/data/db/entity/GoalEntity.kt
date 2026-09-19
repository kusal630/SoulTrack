package io.github.dailytrack.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "PRODUCTIVE", // PRODUCTIVE, EXERCISE, LEARNING, SLEEP, SOCIAL
    val targetType: String = "WEEKLY", // DAILY, WEEKLY, MONTHLY
    val targetHours: Float = 10f,
    val currentHours: Float = 0f,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)
