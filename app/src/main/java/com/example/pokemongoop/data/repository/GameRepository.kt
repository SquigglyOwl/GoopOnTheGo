package com.example.pokemongoop.data.repository

import com.example.pokemongoop.data.database.AppDatabase
import com.example.pokemongoop.data.database.dao.PlayerCreatureWithDetails
import com.example.pokemongoop.data.database.entities.*
import com.example.pokemongoop.models.GoopType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class GameRepository(private val database: AppDatabase) {

    // Creature operations
    fun getAllCreatures() = database.creatureDao().getAllCreatures()
    suspend fun getCreatureById(id: Long) = database.creatureDao().getCreatureById(id)
    suspend fun getBaseCreatureByType(type: GoopType) = database.creatureDao().getBaseCreatureByType(type)
    suspend fun getEvolution(creatureId: Long) = database.creatureDao().getEvolution(creatureId)

    // Player creature operations
    fun getAllPlayerCreatures() = database.playerCreatureDao().getAllPlayerCreatures()
    fun getAllPlayerCreaturesWithDetails(): Flow<List<PlayerCreatureWithDetails>> =
        database.playerCreatureDao().getAllPlayerCreaturesWithDetails()

    suspend fun getPlayerCreatureById(id: Long) = database.playerCreatureDao().getPlayerCreatureById(id)
    suspend fun getPlayerCreatureWithDetails(id: Long) =
        database.playerCreatureDao().getPlayerCreatureWithDetailsById(id)

    suspend fun catchCreature(
        creatureId: Long,
        latitude: Double? = null,
        longitude: Double? = null
    ): Long {
        val playerCreature = PlayerCreature(
            creatureId = creatureId,
            caughtLatitude = latitude,
            caughtLongitude = longitude
        )
        val id = database.playerCreatureDao().insert(playerCreature)
        database.playerStatsDao().incrementTotalCaught()
        database.playerStatsDao().addExperience(25)
        updateCatchAchievements()
        return id
    }

    suspend fun evolveCreature(playerCreatureId: Long): PlayerCreature? {
        val playerCreature = database.playerCreatureDao().getPlayerCreatureById(playerCreatureId)
            ?: return null
        val creature = database.creatureDao().getCreatureById(playerCreature.creatureId)
            ?: return null
        val evolution = database.creatureDao().getEvolution(creature.id)
            ?: return null

        if (playerCreature.experience >= creature.experienceToEvolve) {
            // Delete old creature and create evolved one
            database.playerCreatureDao().delete(playerCreature)
            val evolvedCreature = PlayerCreature(
                creatureId = evolution.id,
                nickname = playerCreature.nickname,
                experience = playerCreature.experience - creature.experienceToEvolve,
                caughtDate = playerCreature.caughtDate,
                isFavorite = playerCreature.isFavorite,
                caughtLatitude = playerCreature.caughtLatitude,
                caughtLongitude = playerCreature.caughtLongitude
            )
            database.playerCreatureDao().insert(evolvedCreature)
            database.playerStatsDao().incrementTotalEvolved()
            database.playerStatsDao().addExperience(50)
            updateEvolutionAchievements()
            return evolvedCreature
        }
        return null
    }

    suspend fun fuseCreatures(
        playerCreature1Id: Long,
        playerCreature2Id: Long
    ): PlayerCreature? {
        val pc1 = database.playerCreatureDao().getPlayerCreatureWithDetailsById(playerCreature1Id)
            ?: return null
        val pc2 = database.playerCreatureDao().getPlayerCreatureWithDetailsById(playerCreature2Id)
            ?: return null

        val recipe = database.fusionRecipeDao().findRecipe(
            pc1.creature.type,
            pc2.creature.type
        ) ?: return null

        // Delete both creatures
        database.playerCreatureDao().delete(pc1.playerCreature)
        database.playerCreatureDao().delete(pc2.playerCreature)

        // Create fused creature
        val fusedCreature = PlayerCreature(
            creatureId = recipe.resultCreatureId,
            experience = (pc1.playerCreature.experience + pc2.playerCreature.experience) / 2
        )
        val id = database.playerCreatureDao().insert(fusedCreature)

        // Mark recipe as discovered
        database.fusionRecipeDao().markDiscovered(recipe.id)

        database.playerStatsDao().incrementTotalFused()
        database.playerStatsDao().addExperience(75)
        updateFusionAchievements()

        return fusedCreature.copy(id = id)
    }

    suspend fun addExperienceToCreature(playerCreatureId: Long, amount: Int) {
        database.playerCreatureDao().addExperience(playerCreatureId, amount)
    }

    suspend fun updateNickname(playerCreatureId: Long, nickname: String?) {
        database.playerCreatureDao().updateNickname(playerCreatureId, nickname)
    }

    suspend fun toggleFavorite(playerCreatureId: Long) {
        val creature = database.playerCreatureDao().getPlayerCreatureById(playerCreatureId)
        creature?.let {
            database.playerCreatureDao().updateFavorite(playerCreatureId, !it.isFavorite)
        }
    }

    // Player stats operations
    fun getPlayerStats() = database.playerStatsDao().getPlayerStats()
    suspend fun getPlayerStatsSync() = database.playerStatsDao().getPlayerStatsSync()

    suspend fun checkAndUpdateDailyStreak() {
        val stats = database.playerStatsDao().getPlayerStatsSync() ?: return
        val now = System.currentTimeMillis()
        val lastLogin = stats.lastLoginDate

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastLogin
        val lastLoginDay = calendar.get(Calendar.DAY_OF_YEAR)
        val lastLoginYear = calendar.get(Calendar.YEAR)

        calendar.timeInMillis = now
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val thisYear = calendar.get(Calendar.YEAR)

        val daysDiff = if (thisYear == lastLoginYear) {
            today - lastLoginDay
        } else {
            // Simplified - just check if it's the next day
            1
        }

        val newStreak = when {
            daysDiff == 0 -> stats.dailyStreak // Same day
            daysDiff == 1 -> stats.dailyStreak + 1 // Consecutive day
            else -> 1 // Streak broken
        }

        database.playerStatsDao().updateStreak(newStreak, now)
    }

    // Achievement operations
    fun getAllAchievements() = database.achievementDao().getAllAchievements()
    fun getIncompleteAchievements() = database.achievementDao().getIncompleteAchievements()

    private suspend fun updateCatchAchievements() {
        val totalCaught = database.playerCreatureDao().getTotalCount()
        updateAchievementProgress(1, totalCaught) // First Catch
        updateAchievementProgress(2, totalCaught) // Collector (10)
        updateAchievementProgress(3, totalCaught) // Goop Master (50)

        // Check for all types
        var typesCaught = 0
        for (type in GoopType.getBasicTypes()) {
            if (database.playerCreatureDao().getCountByType(type) > 0) {
                typesCaught++
            }
        }
        updateAchievementProgress(9, typesCaught)
    }

    private suspend fun updateEvolutionAchievements() {
        val stats = database.playerStatsDao().getPlayerStatsSync() ?: return
        updateAchievementProgress(4, stats.totalEvolved) // First Evolution
        updateAchievementProgress(5, stats.totalEvolved) // Evolution Expert
    }

    private suspend fun updateFusionAchievements() {
        val stats = database.playerStatsDao().getPlayerStatsSync() ?: return
        updateAchievementProgress(6, stats.totalFused) // Mad Scientist
    }

    private suspend fun updateAchievementProgress(achievementId: Long, progress: Int) {
        val achievement = database.achievementDao().getAchievementById(achievementId) ?: return
        if (achievement.isCompleted) return

        database.achievementDao().updateProgress(achievementId, progress)
        if (progress >= achievement.targetProgress) {
            database.achievementDao().markCompleted(achievementId)
            database.playerStatsDao().addExperience(achievement.rewardExperience)
        }
    }

    // Daily challenge operations
    fun getActiveChallenges() = database.dailyChallengeDao().getActiveChallenges()

    suspend fun generateDailyChallenges() {
        database.dailyChallengeDao().deleteExpiredChallenges()

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val expiration = calendar.timeInMillis

        val challenges = listOf(
            DailyChallenge(
                title = "Catch 3 Goops",
                description = "Catch any 3 Goop creatures today",
                challengeType = ChallengeType.CATCH_ANY,
                targetCount = 3,
                rewardExperience = 50,
                expirationDate = expiration
            ),
            DailyChallenge(
                title = "Water Hunter",
                description = "Catch 2 Water type Goops",
                challengeType = ChallengeType.CATCH_TYPE,
                targetType = GoopType.WATER,
                targetCount = 2,
                rewardExperience = 75,
                expirationDate = expiration
            ),
            DailyChallenge(
                title = "Evolution Time",
                description = "Evolve 1 creature",
                challengeType = ChallengeType.EVOLVE,
                targetCount = 1,
                rewardExperience = 100,
                expirationDate = expiration
            )
        )

        database.dailyChallengeDao().insertAll(challenges)
    }

    suspend fun updateChallengeProgress(challengeType: ChallengeType, goopType: GoopType? = null) {
        val challenges = database.dailyChallengeDao()
            .getActiveChallenges()

        // This is simplified - in a real app you'd collect the flow
    }

    // Fusion recipe operations
    fun getDiscoveredRecipes() = database.fusionRecipeDao().getDiscoveredRecipes()
    suspend fun findFusionRecipe(type1: GoopType, type2: GoopType) =
        database.fusionRecipeDao().findRecipe(type1, type2)
}
