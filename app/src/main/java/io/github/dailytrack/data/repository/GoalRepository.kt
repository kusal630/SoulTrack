package io.github.dailytrack.data.repository

import io.github.dailytrack.data.db.dao.GoalDao
import io.github.dailytrack.data.db.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {

    fun getAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()

    suspend fun getGoalById(id: Long): GoalEntity? = goalDao.getGoalById(id)

    suspend fun insertGoal(goal: GoalEntity): Long = goalDao.insertGoal(goal)

    suspend fun updateGoal(goal: GoalEntity) = goalDao.updateGoal(goal)

    suspend fun deleteGoal(goal: GoalEntity) = goalDao.deleteGoal(goal)

    suspend fun archiveGoal(id: Long) = goalDao.archiveGoal(id)

    fun getGoalsByCategory(category: String): Flow<List<GoalEntity>> = goalDao.getGoalsByCategory(category)

    suspend fun updateGoalProgress(goalId: Long, hoursToAdd: Float): Boolean {
        val goal = goalDao.getGoalById(goalId) ?: return false
        val newCurrent = goal.currentHours + hoursToAdd
        val isCompleted = newCurrent >= goal.targetHours
        
        goalDao.updateGoal(goal.copy(
            currentHours = newCurrent,
            isCompleted = isCompleted
        ))
        return isCompleted
    }
}
