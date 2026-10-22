package io.github.dailytrack.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A project created through the pre-mortem flow. */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    /** "Imagine it failed in six months. Why?" answered by the user. */
    val failureScenario: String = "",
    val milestoneAt: Long? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** One named way the project could fail, plus the prevention task it created. */
@Entity(
    tableName = "project_risks",
    indices = [Index("projectId"), Index("preventionTodoId")]
)
data class ProjectRiskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val text: String,
    val preventionTodoId: Long? = null,
    val dueAt: Long? = null,
    val isResolved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** One completed pass of the recall -> compare -> explain loop. */
@Entity(
    tableName = "learning_reviews",
    indices = [Index("nextReviewAt"), Index("completedAt")]
)
data class LearningReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topic: String,
    val recallText: String = "",
    val missedPoints: String = "",
    val explanation: String = "",
    val stillUnclear: Boolean = false,
    val stage: Int = 3,
    val attempt: Int = 0,
    val todoId: Long? = null,
    val sessionId: Long? = null,
    val nextReviewAt: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** A skill or habit tracked with a tiny daily increment (the 1% rule). */
@Entity(tableName = "compound_habits")
data class CompoundHabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String = "reps",
    val dailyIncrement: Double = 1.0,
    val dailyPercent: Double = 1.0,
    val startValue: Double = 0.0,
    val currentValue: Double = 0.0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastLogDate: Long? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "compound_logs",
    indices = [Index("habitId"), Index(value = ["habitId", "epochDay"], unique = true)]
)
data class CompoundLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val epochDay: Long,
    val amount: Double,
    val createdAt: Long = System.currentTimeMillis()
)

/** A guided five-minute weekly review that also sets up the next week. */
@Entity(tableName = "weekly_review_sessions")
data class WeeklyReviewSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekStart: Long,
    val doneText: String = "",
    val deleteText: String = "",
    val failedText: String = "",
    val lessonText: String = "",
    val nextWeekFocus: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
