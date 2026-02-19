package com.example.gooponthego.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gooponthego.models.GoopType

@Entity(tableName = "fusion_recipes")
data class FusionRecipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val inputType1: GoopType,
    val inputType2: GoopType,
    val resultType: GoopType,
    val resultCreatureId: Long,
    val isDiscovered: Boolean = false
)
