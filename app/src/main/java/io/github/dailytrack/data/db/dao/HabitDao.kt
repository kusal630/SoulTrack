package io.github.dailytrack.data.db.dao

import androidx.room.*
import io.github.dailytrack.data.db.entity.HabitEntity
import io.github.dailytrack.data.db.entity.HabitCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY sortOrder, name")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("UPDATE habits SET isArchived = 1 WHERE id = :id")
    suspend fun archiveHabit(id: Long)

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND completedDate BETWEEN :startDate AND :endDate")
    fun getCompletionsForHabit(habitId: Long, startDate: Long, endDate: Long): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE completedDate BETWEEN :startDate AND :endDate")
    fun getAllCompletionsForDateRange(startDate: Long, endDate: Long): Flow<List<HabitCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletionEntity): Long

    @Delete
    suspend fun deleteCompletion(completion: HabitCompletionEntity)

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId AND completedDate BETWEEN :startDate AND :endDate")
    suspend fun getCompletionCount(habitId: Long, startDate: Long, endDate: Long): Int

    @Query("UPDATE habits SET currentStreak = :streak, bestStreak = :bestStreak, totalCompletions = :total WHERE id = :id")
    suspend fun updateStreaks(id: Long, streak: Long, bestStreak: Long, total: Int)
}
