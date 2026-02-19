package com.example.gooponthego.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "player_creatures",
    foreignKeys = [
        ForeignKey(
            entity = Creature::class,
            parentColumns = ["id"],
            childColumns = ["creatureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("creatureId")]
)
data class PlayerCreature(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val creatureId: Long,
    val nickname: String? = null,
    val experience: Int = 0,
    val caughtDate: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val caughtLatitude: Double? = null,
    val caughtLongitude: Double? = null
)
