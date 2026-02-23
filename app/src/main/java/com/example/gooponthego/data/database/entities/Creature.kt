package com.example.gooponthego.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gooponthego.models.GoopType

@Entity(tableName = "creatures")
data class Creature(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: GoopType,
    val rarity: Int, // 1-5 stars
    val baseHealth: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val evolutionStage: Int, // 1, 2, or 3
    val evolvesFromId: Long? = null,
    val evolvesToId: Long? = null,
    val experienceToEvolve: Int,
    val description: String,
    val imageResName: String? = null, // drawable resource name (e.g., "droplet_goop")
    val isDiscovered: Boolean = false  // true once the player has ever caught/obtained this creature
)
