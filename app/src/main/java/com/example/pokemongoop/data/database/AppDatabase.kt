package com.example.pokemongoop.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pokemongoop.data.database.dao.*
import com.example.pokemongoop.data.database.entities.*
import com.example.pokemongoop.models.GoopType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Creature::class,
        PlayerCreature::class,
        Achievement::class,
        PlayerStats::class,
        DailyChallenge::class,
        FusionRecipe::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creatureDao(): CreatureDao
    abstract fun playerCreatureDao(): PlayerCreatureDao
    abstract fun achievementDao(): AchievementDao
    abstract fun playerStatsDao(): PlayerStatsDao
    abstract fun dailyChallengeDao(): DailyChallengeDao
    abstract fun fusionRecipeDao(): FusionRecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pokemongoop_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database)
                }
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database)
                }
            }
        }

        private suspend fun populateDatabase(database: AppDatabase) {
            // Initialize player stats
            database.playerStatsDao().insert(PlayerStats())

            // Create base creatures for each type
            val creatures = createInitialCreatures()
            database.creatureDao().insertAll(creatures)

            // Create fusion recipes
            val recipes = createFusionRecipes()
            database.fusionRecipeDao().insertAll(recipes)

            // Create initial achievements
            val achievements = createInitialAchievements()
            database.achievementDao().insertAll(achievements)
        }

        private fun createInitialCreatures(): List<Creature> {
            val creatures = mutableListOf<Creature>()
            var id = 1L

            // Water line
            creatures.add(Creature(id++, "Droplet Goop", GoopType.WATER, 1, 30, 10, 15, 1, null, 2, 100, "A small water droplet that bounces around happily.", "droplet_goop"))
            creatures.add(Creature(id++, "Aqua Goop", GoopType.WATER, 2, 50, 20, 25, 2, 1, 3, 300, "A flowing water spirit with enhanced powers.", "aqua_goop"))
            creatures.add(Creature(id++, "Tsunami Goop", GoopType.WATER, 3, 80, 35, 40, 3, 2, null, 0, "A powerful wave spirit that commands the tides.", "tsunami_goop"))

            // Fire line
            creatures.add(Creature(id++, "Ember Goop", GoopType.FIRE, 1, 25, 15, 10, 1, null, 5, 100, "A tiny flame that flickers with curiosity.", "ember_goop"))
            creatures.add(Creature(id++, "Blaze Goop", GoopType.FIRE, 2, 45, 30, 20, 2, 4, 6, 300, "A fiery spirit burning with determination.", "blaze_goop"))
            creatures.add(Creature(id++, "Inferno Goop", GoopType.FIRE, 3, 70, 50, 35, 3, 5, null, 0, "An unstoppable force of pure fire energy.", "inferno_goop"))

            // Nature line
            creatures.add(Creature(id++, "Sprout Goop", GoopType.NATURE, 1, 35, 12, 18, 1, null, 8, 100, "A seedling spirit growing in the sunlight.", "sprout_goop"))
            creatures.add(Creature(id++, "Leaf Goop", GoopType.NATURE, 2, 55, 22, 30, 2, 7, 9, 300, "A leafy creature connected to nature.", "leaf_goop"))
            creatures.add(Creature(id++, "Forest Goop", GoopType.NATURE, 3, 85, 38, 50, 3, 8, null, 0, "An ancient spirit of the deep woods.", "forest_goop"))

            // Electric line
            creatures.add(Creature(id++, "Spark Goop", GoopType.ELECTRIC, 1, 28, 18, 8, 1, null, 11, 100, "A tiny electric spark full of energy.", "spark_goop"))
            creatures.add(Creature(id++, "Volt Goop", GoopType.ELECTRIC, 2, 48, 35, 18, 2, 10, 12, 300, "A charged spirit crackling with power.", "volt_goop"))
            creatures.add(Creature(id++, "Thunder Goop", GoopType.ELECTRIC, 3, 75, 55, 30, 3, 11, null, 0, "A legendary storm spirit of immense power.", "thunder_goop"))

            // Shadow line
            creatures.add(Creature(id++, "Shade Goop", GoopType.SHADOW, 1, 32, 14, 12, 1, null, 14, 100, "A mysterious shadow that lurks in darkness.", "shade_goop"))
            creatures.add(Creature(id++, "Phantom Goop", GoopType.SHADOW, 2, 52, 28, 24, 2, 13, 15, 300, "A ghostly presence from the shadow realm.", "phantom_goop"))
            creatures.add(Creature(id++, "Void Goop", GoopType.SHADOW, 3, 78, 45, 42, 3, 14, null, 0, "An entity from the deepest void.", "void_goop"))

            // Hybrid creatures (from fusion)
            creatures.add(Creature(id++, "Steam Goop", GoopType.STEAM, 2, 60, 28, 28, 2, null, null, 0, "A misty fusion of water and fire.", "steam_goop"))
            creatures.add(Creature(id++, "Lightning Bloom", GoopType.LIGHTNING_PLANT, 2, 58, 32, 25, 2, null, null, 0, "A shocking plant hybrid crackling with energy.", "lightning_bloom_goop"))
            creatures.add(Creature(id++, "Magma Goop", GoopType.MAGMA, 2, 65, 40, 35, 2, null, null, 0, "A molten fusion of fire and earth.", "magma_goop"))
            creatures.add(Creature(id, "Frost Goop", GoopType.ICE, 2, 55, 25, 32, 2, null, null, 0, "A frozen spirit of water and lightning.", "frost_goop"))

            return creatures
        }

        private fun createFusionRecipes(): List<FusionRecipe> {
            return listOf(
                FusionRecipe(1, GoopType.WATER, GoopType.FIRE, GoopType.STEAM, 16),
                FusionRecipe(2, GoopType.ELECTRIC, GoopType.NATURE, GoopType.LIGHTNING_PLANT, 17),
                FusionRecipe(3, GoopType.FIRE, GoopType.NATURE, GoopType.MAGMA, 18),
                FusionRecipe(4, GoopType.WATER, GoopType.ELECTRIC, GoopType.ICE, 19)
            )
        }

        private fun createInitialAchievements(): List<Achievement> {
            return listOf(
                Achievement(1, "First Catch", "Catch your first Goop creature", "catch_1", 1, 0, false, null, 50, AchievementCategory.COLLECTION),
                Achievement(2, "Collector", "Catch 10 Goop creatures", "catch_10", 10, 0, false, null, 100, AchievementCategory.COLLECTION),
                Achievement(3, "Goop Master", "Catch 50 Goop creatures", "catch_50", 50, 0, false, null, 500, AchievementCategory.COLLECTION),
                Achievement(4, "First Evolution", "Evolve a creature for the first time", "evolve_1", 1, 0, false, null, 100, AchievementCategory.EVOLUTION),
                Achievement(5, "Evolution Expert", "Evolve 10 creatures", "evolve_10", 10, 0, false, null, 300, AchievementCategory.EVOLUTION),
                Achievement(6, "Mad Scientist", "Perform your first fusion", "fuse_1", 1, 0, false, null, 150, AchievementCategory.EVOLUTION),
                Achievement(7, "Explorer", "Discover 5 different habitat zones", "explore_5", 5, 0, false, null, 200, AchievementCategory.EXPLORATION),
                Achievement(8, "Dedicated Trainer", "Login for 7 consecutive days", "streak_7", 7, 0, false, null, 250, AchievementCategory.DAILY),
                Achievement(9, "Type Specialist", "Catch at least one of each basic type", "all_types", 5, 0, false, null, 300, AchievementCategory.COLLECTION),
                Achievement(10, "Challenge Champion", "Complete 10 daily challenges", "challenge_10", 10, 0, false, null, 200, AchievementCategory.DAILY)
            )
        }
    }
}
