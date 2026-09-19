package io.github.dailytrack.data.db.dao

import androidx.room.*
import io.github.dailytrack.data.db.entity.AchievementEntity
import io.github.dailytrack.data.db.entity.UserStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY category, id")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1")
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievementById(id: Long): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity): Long

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("UPDATE achievements SET currentValue = :value WHERE id = :id")
    suspend fun updateProgress(id: Long, value: Int)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedAt = :timestamp WHERE id = :id")
    suspend fun unlockAchievement(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: UserStatsEntity)

    @Update
    suspend fun updateUserStats(stats: UserStatsEntity)
}
