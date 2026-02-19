package com.example.gooponthego.data.database.dao

import androidx.room.*
import com.example.gooponthego.data.database.entities.Achievement
import com.example.gooponthego.data.database.entities.AchievementCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievementById(id: Long): Achievement?

    @Query("SELECT * FROM achievements WHERE isCompleted = 1")
    fun getCompletedAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE isCompleted = 0")
    fun getIncompleteAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE category = :category")
    fun getAchievementsByCategory(category: AchievementCategory): Flow<List<Achievement>>

    @Query("SELECT COUNT(*) FROM achievements WHERE isCompleted = 1")
    suspend fun getCompletedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: Achievement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<Achievement>)

    @Update
    suspend fun update(achievement: Achievement)

    @Query("UPDATE achievements SET currentProgress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int)

    @Query("""
        UPDATE achievements
        SET isCompleted = 1, completedDate = :completedDate
        WHERE id = :id
    """)
    suspend fun markCompleted(id: Long, completedDate: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(achievement: Achievement)
}
