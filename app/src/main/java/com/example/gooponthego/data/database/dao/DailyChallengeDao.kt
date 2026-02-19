package com.example.gooponthego.data.database.dao

import androidx.room.*
import com.example.gooponthego.data.database.entities.DailyChallenge
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyChallengeDao {
    @Query("SELECT * FROM daily_challenges WHERE expirationDate > :currentTime")
    fun getActiveChallenges(currentTime: Long = System.currentTimeMillis()): Flow<List<DailyChallenge>>

    @Query("SELECT * FROM daily_challenges WHERE id = :id")
    suspend fun getChallengeById(id: Long): DailyChallenge?

    @Query("SELECT * FROM daily_challenges WHERE isCompleted = 0 AND expirationDate > :currentTime")
    fun getIncompleteChallenges(currentTime: Long = System.currentTimeMillis()): Flow<List<DailyChallenge>>

    @Query("SELECT COUNT(*) FROM daily_challenges WHERE isCompleted = 1 AND dateCreated > :startOfDay")
    suspend fun getCompletedTodayCount(startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM daily_challenges WHERE isCompleted = 1 AND expirationDate > :currentTime")
    suspend fun getCompletedActiveCount(currentTime: Long = System.currentTimeMillis()): Int

    @Query("SELECT COUNT(*) FROM daily_challenges WHERE expirationDate > :currentTime")
    suspend fun getActiveChallengeCount(currentTime: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM daily_challenges WHERE isCompleted = 0 AND expirationDate > :currentTime")
    suspend fun getActiveIncompleteChallengesSync(currentTime: Long = System.currentTimeMillis()): List<DailyChallenge>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(challenge: DailyChallenge): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(challenges: List<DailyChallenge>)

    @Update
    suspend fun update(challenge: DailyChallenge)

    @Query("UPDATE daily_challenges SET currentProgress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int)

    @Query("UPDATE daily_challenges SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Query("DELETE FROM daily_challenges WHERE expirationDate < :currentTime")
    suspend fun deleteExpiredChallenges(currentTime: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(challenge: DailyChallenge)
}
