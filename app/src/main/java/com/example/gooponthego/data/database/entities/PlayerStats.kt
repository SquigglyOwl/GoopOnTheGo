package com.example.gooponthego.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStats(
    @PrimaryKey
    val id: Int = 1, // Single row for player stats
    val playerName: String = "Trainer",
    val totalCaught: Int = 0,
    val totalEvolved: Int = 0,
    val totalFused: Int = 0,
    val dailyStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastLoginDate: Long = System.currentTimeMillis(),
    val totalExperience: Int = 0,
    val playerLevel: Int = 1
) {
    fun calculateLevel(): Int {
        return (totalExperience / 1000) + 1
    }
}
