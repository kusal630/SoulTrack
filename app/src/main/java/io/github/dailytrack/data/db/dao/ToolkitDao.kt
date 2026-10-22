package io.github.dailytrack.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.dailytrack.data.db.entity.CompoundHabitEntity
import io.github.dailytrack.data.db.entity.CompoundLogEntity
import io.github.dailytrack.data.db.entity.LearningReviewEntity
import io.github.dailytrack.data.db.entity.ProjectEntity
import io.github.dailytrack.data.db.entity.ProjectRiskEntity
import io.github.dailytrack.data.db.entity.WeeklyReviewSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolkitDao {
    // Pre-mortem projects
    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: Long): ProjectEntity?

    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("SELECT * FROM project_risks WHERE projectId = :projectId ORDER BY isResolved ASC, dueAt ASC, id ASC")
    fun getRisks(projectId: Long): Flow<List<ProjectRiskEntity>>

    @Query("SELECT * FROM project_risks WHERE projectId = :projectId ORDER BY isResolved ASC, dueAt ASC, id ASC")
    suspend fun getRisksSync(projectId: Long): List<ProjectRiskEntity>

    @Insert
    suspend fun insertRisk(risk: ProjectRiskEntity): Long

    @Update
    suspend fun updateRisk(risk: ProjectRiskEntity)

    @Query("UPDATE project_risks SET isResolved = :resolved WHERE id = :id")
    suspend fun setRiskResolved(id: Long, resolved: Boolean)

    @Query("DELETE FROM project_risks WHERE projectId = :projectId")
    suspend fun deleteRisks(projectId: Long)

    @Query("DELETE FROM project_risks WHERE id = :id")
    suspend fun deleteRisk(id: Long)

    // Learning loop
    @Query("SELECT * FROM learning_reviews ORDER BY createdAt DESC")
    fun getLearningReviews(): Flow<List<LearningReviewEntity>>

    @Query("SELECT * FROM learning_reviews WHERE completedAt IS NULL AND (nextReviewAt IS NULL OR nextReviewAt <= :now) ORDER BY nextReviewAt ASC, createdAt ASC")
    fun getDueLearningReviews(now: Long): Flow<List<LearningReviewEntity>>

    @Query("SELECT * FROM learning_reviews WHERE completedAt IS NULL AND (nextReviewAt IS NULL OR nextReviewAt <= :now) ORDER BY nextReviewAt ASC, createdAt ASC")
    suspend fun getDueLearningReviewsSync(now: Long): List<LearningReviewEntity>

    @Insert
    suspend fun insertLearningReview(review: LearningReviewEntity): Long

    @Update
    suspend fun updateLearningReview(review: LearningReviewEntity)

    @Query("UPDATE learning_reviews SET completedAt = :completedAt WHERE id = :id")
    suspend fun completeLearningReview(id: Long, completedAt: Long)

    @Delete
    suspend fun deleteLearningReview(review: LearningReviewEntity)

    // Compounding tracker
    @Query("SELECT * FROM compound_habits WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getCompoundHabits(): Flow<List<CompoundHabitEntity>>

    @Query("SELECT * FROM compound_habits WHERE id = :id")
    suspend fun getCompoundHabit(id: Long): CompoundHabitEntity?

    @Insert
    suspend fun insertCompoundHabit(habit: CompoundHabitEntity): Long

    @Update
    suspend fun updateCompoundHabit(habit: CompoundHabitEntity)

    @Query("UPDATE compound_habits SET isArchived = 1 WHERE id = :id")
    suspend fun archiveCompoundHabit(id: Long)

    @Query("SELECT * FROM compound_logs WHERE habitId = :habitId ORDER BY epochDay ASC")
    fun getCompoundLogs(habitId: Long): Flow<List<CompoundLogEntity>>

    @Query("SELECT * FROM compound_logs WHERE habitId = :habitId AND epochDay >= :fromDay AND epochDay <= :toDay ORDER BY epochDay")
    suspend fun getCompoundLogsSync(habitId: Long, fromDay: Long, toDay: Long): List<CompoundLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompoundLog(log: CompoundLogEntity): Long

    @Query("DELETE FROM compound_logs WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteCompoundLog(habitId: Long, epochDay: Long)

    // Guided weekly review
    @Query("SELECT * FROM weekly_review_sessions ORDER BY createdAt DESC")
    fun getWeeklyReviewSessions(): Flow<List<WeeklyReviewSessionEntity>>

    @Insert
    suspend fun insertWeeklyReviewSession(session: WeeklyReviewSessionEntity): Long
}
