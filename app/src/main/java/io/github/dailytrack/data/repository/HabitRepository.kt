package io.github.dailytrack.data.repository

import io.github.dailytrack.data.db.dao.HabitDao
import io.github.dailytrack.data.db.entity.HabitEntity
import io.github.dailytrack.data.db.entity.HabitCompletionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.ZoneId

class HabitRepository(private val habitDao: HabitDao) {

    fun getAllHabits(): Flow<List<HabitEntity>> = habitDao.getAllHabits()

    suspend fun getHabitById(id: Long): HabitEntity? = habitDao.getHabitById(id)

    suspend fun insertHabit(habit: HabitEntity): Long = habitDao.insertHabit(habit)

    suspend fun updateHabit(habit: HabitEntity) = habitDao.updateHabit(habit)

    suspend fun deleteHabit(habit: HabitEntity) = habitDao.deleteHabit(habit)

    suspend fun archiveHabit(id: Long) = habitDao.archiveHabit(id)

    fun getCompletionsForHabit(habitId: Long, startDate: LocalDate, endDate: LocalDate): Flow<List<HabitCompletionEntity>> {
        val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return habitDao.getCompletionsForHabit(habitId, startMillis, endMillis)
    }

    fun getAllCompletionsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HabitCompletionEntity>> {
        val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return habitDao.getAllCompletionsForDateRange(startMillis, endMillis)
    }

    suspend fun completeHabit(habitId: Long, notes: String = ""): Boolean {
        val today = LocalDate.now()
        val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val existingCompletion = habitDao.getCompletionCount(habitId, todayMillis, todayMillis + 86400000)
        if (existingCompletion > 0) return false

        val completion = HabitCompletionEntity(
            habitId = habitId,
            completedDate = todayMillis,
            notes = notes
        )
        habitDao.insertCompletion(completion)

        val habit = habitDao.getHabitById(habitId) ?: return false
        val newStreak = calculateCurrentStreak(habitId)
        val bestStreak = maxOf(habit.bestStreak, newStreak)
        val total = habit.totalCompletions + 1

        habitDao.updateStreaks(habitId, newStreak, bestStreak, total)
        return true
    }

    suspend fun uncompleteHabit(habitId: Long): Boolean {
        val today = LocalDate.now()
        val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val completions = habitDao.getCompletionsForHabit(habitId, todayMillis, todayMillis + 86400000).first()
        val completion = completions.firstOrNull() ?: return false
        
        habitDao.deleteCompletion(completion)

        val habit = habitDao.getHabitById(habitId) ?: return false
        val newStreak = calculateCurrentStreak(habitId)
        val total = maxOf(0, habit.totalCompletions - 1)

        habitDao.updateStreaks(habitId, newStreak, habit.bestStreak, total)
        return true
    }

    private suspend fun calculateCurrentStreak(habitId: Long): Long {
        var streak = 0L
        var currentDate = LocalDate.now()
        
        while (true) {
            val dateMillis = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val count = habitDao.getCompletionCount(habitId, dateMillis, dateMillis + 86400000)
            if (count > 0) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }
}
