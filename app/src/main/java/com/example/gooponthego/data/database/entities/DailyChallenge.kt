package com.example.gooponthego.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gooponthego.models.GoopType

@Entity(tableName = "daily_challenges")
data class DailyChallenge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val challengeType: ChallengeType,
    val targetType: GoopType? = null, // For type-specific challenges
    val targetCount: Int,
    val currentProgress: Int = 0,
    val isCompleted: Boolean = false,
    val rewardExperience: Int,
    val dateCreated: Long = System.currentTimeMillis(),
    val expirationDate: Long
)

enum class ChallengeType {
    CATCH_ANY,
    CATCH_TYPE,
    EVOLVE,
    FUSE,
    SCAN_LOCATIONS
}
