package com.example.gooponthego.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.example.gooponthego.data.database.entities.Creature
import com.example.gooponthego.data.database.entities.PlayerCreature
import com.example.gooponthego.models.GoopType
import kotlinx.coroutines.flow.Flow

data class PlayerCreatureWithDetails(
    @Embedded val playerCreature: PlayerCreature,
    @Relation(
        parentColumn = "creatureId",
        entityColumn = "id"
    )
    val creature: Creature
)

@Dao
interface PlayerCreatureDao {
    @Query("SELECT * FROM player_creatures")
    fun getAllPlayerCreatures(): Flow<List<PlayerCreature>>

    @Transaction
    @Query("SELECT * FROM player_creatures ORDER BY caughtDate DESC")
    fun getAllPlayerCreaturesWithDetails(): Flow<List<PlayerCreatureWithDetails>>

    @Transaction
    @Query("SELECT * FROM player_creatures ORDER BY caughtDate DESC")
    suspend fun getAllPlayerCreaturesWithDetailsSync(): List<PlayerCreatureWithDetails>

    @Query("SELECT * FROM player_creatures WHERE id = :id")
    suspend fun getPlayerCreatureById(id: Long): PlayerCreature?

    @Transaction
    @Query("SELECT * FROM player_creatures WHERE id = :id")
    suspend fun getPlayerCreatureWithDetailsById(id: Long): PlayerCreatureWithDetails?

    @Query("SELECT * FROM player_creatures WHERE isFavorite = 1")
    fun getFavoriteCreatures(): Flow<List<PlayerCreature>>

    @Query("""
        SELECT COUNT(*) FROM player_creatures pc
        INNER JOIN creatures c ON pc.creatureId = c.id
        WHERE c.type = :type
    """)
    suspend fun getCountByType(type: GoopType): Int

    @Query("SELECT COUNT(*) FROM player_creatures")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM player_creatures WHERE creatureId = :creatureId")
    suspend fun getPlayerCreaturesByCreatureId(creatureId: Long): List<PlayerCreature>

    @Query("SELECT COUNT(*) FROM player_creatures WHERE creatureId = :creatureId")
    suspend fun getCountByCreatureId(creatureId: Long): Int

    @Query("SELECT COUNT(DISTINCT creatureId) FROM player_creatures")
    suspend fun getUniqueCreatureCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playerCreature: PlayerCreature): Long

    @Update
    suspend fun update(playerCreature: PlayerCreature)

    @Delete
    suspend fun delete(playerCreature: PlayerCreature)

    @Query("UPDATE player_creatures SET experience = experience + :amount WHERE id = :id")
    suspend fun addExperience(id: Long, amount: Int)

    @Query("""
        UPDATE player_creatures SET experience = experience + :amount
        WHERE creatureId IN (SELECT id FROM creatures WHERE type = :type)
    """)
    suspend fun addXpToCreaturesByType(type: GoopType, amount: Int)

    @Query("UPDATE player_creatures SET nickname = :nickname WHERE id = :id")
    suspend fun updateNickname(id: Long, nickname: String?)

    @Query("UPDATE player_creatures SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
}
