package com.example.gooponthego.data.database.dao

import androidx.room.*
import com.example.gooponthego.data.database.entities.FusionRecipe
import com.example.gooponthego.models.GoopType
import kotlinx.coroutines.flow.Flow

@Dao
interface FusionRecipeDao {
    @Query("SELECT * FROM fusion_recipes")
    fun getAllRecipes(): Flow<List<FusionRecipe>>

    @Query("SELECT * FROM fusion_recipes WHERE isDiscovered = 1")
    fun getDiscoveredRecipes(): Flow<List<FusionRecipe>>

    @Query("""
        SELECT * FROM fusion_recipes
        WHERE (inputType1 = :type1 AND inputType2 = :type2)
           OR (inputType1 = :type2 AND inputType2 = :type1)
    """)
    suspend fun findRecipe(type1: GoopType, type2: GoopType): FusionRecipe?

    @Query("SELECT * FROM fusion_recipes WHERE id = :id")
    suspend fun getRecipeById(id: Long): FusionRecipe?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: FusionRecipe): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<FusionRecipe>)

    @Update
    suspend fun update(recipe: FusionRecipe)

    @Query("UPDATE fusion_recipes SET isDiscovered = 1 WHERE id = :id")
    suspend fun markDiscovered(id: Long)

    @Delete
    suspend fun delete(recipe: FusionRecipe)
}
