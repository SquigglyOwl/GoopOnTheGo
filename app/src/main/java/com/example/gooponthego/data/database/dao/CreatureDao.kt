package com.example.gooponthego.data.database.dao

import androidx.room.*
import com.example.gooponthego.data.database.entities.Creature
import com.example.gooponthego.models.GoopType
import kotlinx.coroutines.flow.Flow

@Dao
interface CreatureDao {
    @Query("SELECT * FROM creatures")
    fun getAllCreatures(): Flow<List<Creature>>

    @Query("SELECT * FROM creatures WHERE id = :id")
    suspend fun getCreatureById(id: Long): Creature?

    @Query("SELECT * FROM creatures WHERE type = :type")
    fun getCreaturesByType(type: GoopType): Flow<List<Creature>>

    @Query("SELECT * FROM creatures WHERE evolutionStage = 1")
    fun getBaseCreatures(): Flow<List<Creature>>

    @Query("SELECT * FROM creatures WHERE type = :type AND evolutionStage = 1")
    suspend fun getBaseCreatureByType(type: GoopType): Creature?

    @Query("SELECT * FROM creatures WHERE evolvesFromId = :creatureId")
    suspend fun getEvolution(creatureId: Long): Creature?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(creature: Creature): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(creatures: List<Creature>)

    @Update
    suspend fun update(creature: Creature)

    @Delete
    suspend fun delete(creature: Creature)

    @Query("SELECT COUNT(*) FROM creatures")
    suspend fun getCreatureCount(): Int

    @Query("SELECT * FROM creatures ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomCreature(): Creature?
}
