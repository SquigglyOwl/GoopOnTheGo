package com.example.pokemongoop

import com.example.pokemongoop.data.database.entities.Creature
import com.example.pokemongoop.models.GoopType
import org.junit.Assert.*
import org.junit.Test

class CreatureTest {

    // --- Rarity Catch Rate Tests ---
    // Mirrors the catch rate logic in AROverlayView

    private fun getCatchRate(rarity: Int): Float {
        return when (rarity) {
            1 -> 0.70f
            2 -> 0.55f
            3 -> 0.40f
            4 -> 0.25f
            5 -> 0.15f
            else -> 0.50f
        }
    }

    @Test
    fun catchRate_commonRarity_is70Percent() {
        assertEquals(0.70f, getCatchRate(1), 0.001f)
    }

    @Test
    fun catchRate_uncommonRarity_is55Percent() {
        assertEquals(0.55f, getCatchRate(2), 0.001f)
    }

    @Test
    fun catchRate_rareRarity_is40Percent() {
        assertEquals(0.40f, getCatchRate(3), 0.001f)
    }

    @Test
    fun catchRate_epicRarity_is25Percent() {
        assertEquals(0.25f, getCatchRate(4), 0.001f)
    }

    @Test
    fun catchRate_legendaryRarity_is15Percent() {
        assertEquals(0.15f, getCatchRate(5), 0.001f)
    }

    @Test
    fun catchRate_invalidRarity_is50Percent() {
        assertEquals(0.50f, getCatchRate(0), 0.001f)
        assertEquals(0.50f, getCatchRate(6), 0.001f)
    }

    @Test
    fun catchRate_higherRarity_hasLowerRate() {
        for (r in 1..4) {
            assertTrue(getCatchRate(r) > getCatchRate(r + 1))
        }
    }

    // --- Creature Entity Tests ---

    @Test
    fun creature_stage1_hasNoEvolvesFrom() {
        val creature = Creature(
            name = "Droplet",
            type = GoopType.WATER,
            rarity = 1,
            baseHealth = 40,
            baseAttack = 25,
            baseDefense = 30,
            evolutionStage = 1,
            evolvesFromId = null,
            evolvesToId = 2,
            experienceToEvolve = 100,
            description = "A small water goop"
        )
        assertNull(creature.evolvesFromId)
        assertNotNull(creature.evolvesToId)
    }

    @Test
    fun creature_stage3_hasNoEvolvesTo() {
        val creature = Creature(
            name = "Tsunami",
            type = GoopType.WATER,
            rarity = 3,
            baseHealth = 80,
            baseAttack = 65,
            baseDefense = 70,
            evolutionStage = 3,
            evolvesFromId = 2,
            evolvesToId = null,
            experienceToEvolve = 0,
            description = "A massive water goop"
        )
        assertNotNull(creature.evolvesFromId)
        assertNull(creature.evolvesToId)
    }

    @Test
    fun creature_rarityRange_isValid() {
        for (rarity in 1..5) {
            val creature = Creature(
                name = "Test",
                type = GoopType.FIRE,
                rarity = rarity,
                baseHealth = 50,
                baseAttack = 50,
                baseDefense = 50,
                evolutionStage = 1,
                experienceToEvolve = 100,
                description = "Test creature"
            )
            assertTrue(creature.rarity in 1..5)
        }
    }

    @Test
    fun creature_evolutionStage_rangesFrom1To3() {
        val stages = listOf(1, 2, 3)
        stages.forEach { stage ->
            val creature = Creature(
                name = "Test",
                type = GoopType.FIRE,
                rarity = 1,
                baseHealth = 50,
                baseAttack = 50,
                baseDefense = 50,
                evolutionStage = stage,
                experienceToEvolve = 100,
                description = "Test"
            )
            assertTrue(creature.evolutionStage in 1..3)
        }
    }
}
