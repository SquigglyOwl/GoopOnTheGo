package com.example.gooponthego.data.database.dao

import androidx.room.*
import com.example.gooponthego.data.database.entities.PlayerStats
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerStatsDao {
    @Query("SELECT * FROM player_stats WHERE id = 1")
    fun getPlayerStats(): Flow<PlayerStats?>

    @Query("SELECT * FROM player_stats WHERE id = 1")
    suspend fun getPlayerStatsSync(): PlayerStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playerStats: PlayerStats)

    @Update
    suspend fun update(playerStats: PlayerStats)

    @Query("UPDATE player_stats SET totalCaught = totalCaught + 1 WHERE id = 1")
    suspend fun incrementTotalCaught()

    @Query("UPDATE player_stats SET totalEvolved = totalEvolved + 1 WHERE id = 1")
    suspend fun incrementTotalEvolved()

    @Query("UPDATE player_stats SET totalFused = totalFused + 1 WHERE id = 1")
    suspend fun incrementTotalFused()

    @Query("UPDATE player_stats SET totalExperience = totalExperience + :amount WHERE id = 1")
    suspend fun addExperience(amount: Int)

    @Query("""
        UPDATE player_stats
        SET dailyStreak = :streak,
            longestStreak = CASE WHEN :streak > longestStreak THEN :streak ELSE longestStreak END,
            lastLoginDate = :loginDate
        WHERE id = 1
    """)
    suspend fun updateStreak(streak: Int, loginDate: Long)

    @Query("UPDATE player_stats SET playerName = :name WHERE id = 1")
    suspend fun updatePlayerName(name: String)
}
