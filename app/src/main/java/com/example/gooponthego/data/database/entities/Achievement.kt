package com.example.gooponthego.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val iconName: String,
    val targetProgress: Int,
    val currentProgress: Int = 0,
    val isCompleted: Boolean = false,
    val completedDate: Long? = null,
    val rewardExperience: Int = 0,
    val category: AchievementCategory
)

enum class AchievementCategory {
    COLLECTION,
    EVOLUTION,
    EXPLORATION,
    DAILY,
    SPECIAL
}
