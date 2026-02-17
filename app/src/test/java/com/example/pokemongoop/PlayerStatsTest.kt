package com.example.pokemongoop

import com.example.pokemongoop.data.database.entities.PlayerStats
import org.junit.Assert.*
import org.junit.Test

class PlayerStatsTest {

    // --- Level Calculation Tests ---

    @Test
    fun calculateLevel_zeroXP_returnsLevel1() {
        val stats = PlayerStats(totalExperience = 0)
        assertEquals(1, stats.calculateLevel())
    }

    @Test
    fun calculateLevel_999XP_returnsLevel1() {
        val stats = PlayerStats(totalExperience = 999)
        assertEquals(1, stats.calculateLevel())
    }

    @Test
    fun calculateLevel_1000XP_returnsLevel2() {
        val stats = PlayerStats(totalExperience = 1000)
        assertEquals(2, stats.calculateLevel())
    }

    @Test
    fun calculateLevel_2500XP_returnsLevel3() {
        val stats = PlayerStats(totalExperience = 2500)
        assertEquals(3, stats.calculateLevel())
    }

    @Test
    fun calculateLevel_10000XP_returnsLevel11() {
        val stats = PlayerStats(totalExperience = 10000)
        assertEquals(11, stats.calculateLevel())
    }

    // --- Default Values Tests ---

    @Test
    fun defaults_newPlayer_hasZeroStats() {
        val stats = PlayerStats()
        assertEquals(0, stats.totalCaught)
        assertEquals(0, stats.totalEvolved)
        assertEquals(0, stats.totalFused)
        assertEquals(0, stats.totalExperience)
    }

    @Test
    fun defaults_newPlayer_hasDefaultName() {
        val stats = PlayerStats()
        assertEquals("Trainer", stats.playerName)
    }

    @Test
    fun defaults_newPlayer_hasLevel1() {
        val stats = PlayerStats()
        assertEquals(1, stats.playerLevel)
    }

    @Test
    fun defaults_newPlayer_hasZeroStreak() {
        val stats = PlayerStats()
        assertEquals(0, stats.dailyStreak)
        assertEquals(0, stats.longestStreak)
    }

    @Test
    fun defaults_singletonId_isOne() {
        val stats = PlayerStats()
        assertEquals(1, stats.id)
    }
}
