package com.example.gooponthego

import com.example.gooponthego.models.GoopType
import org.junit.Assert.*
import org.junit.Test

class GoopTypeTest {

    // --- Fusion Result Tests ---

    @Test
    fun fusion_waterAndFire_returnsSteam() {
        val result = GoopType.getFusionResult(GoopType.WATER, GoopType.FIRE)
        assertEquals(GoopType.STEAM, result)
    }

    @Test
    fun fusion_fireAndWater_returnsSteam() {
        // Order should not matter
        val result = GoopType.getFusionResult(GoopType.FIRE, GoopType.WATER)
        assertEquals(GoopType.STEAM, result)
    }

    @Test
    fun fusion_electricAndNature_returnsLightningPlant() {
        val result = GoopType.getFusionResult(GoopType.ELECTRIC, GoopType.NATURE)
        assertEquals(GoopType.LIGHTNING_PLANT, result)
    }

    @Test
    fun fusion_natureAndElectric_returnsLightningPlant() {
        val result = GoopType.getFusionResult(GoopType.NATURE, GoopType.ELECTRIC)
        assertEquals(GoopType.LIGHTNING_PLANT, result)
    }

    @Test
    fun fusion_fireAndNature_returnsMagma() {
        val result = GoopType.getFusionResult(GoopType.FIRE, GoopType.NATURE)
        assertEquals(GoopType.MAGMA, result)
    }

    @Test
    fun fusion_waterAndElectric_returnsIce() {
        val result = GoopType.getFusionResult(GoopType.WATER, GoopType.ELECTRIC)
        assertEquals(GoopType.ICE, result)
    }

    @Test
    fun fusion_shadowAndShadow_returnsVoid() {
        val result = GoopType.getFusionResult(GoopType.SHADOW, GoopType.SHADOW)
        assertEquals(GoopType.VOID, result)
    }

    @Test
    fun fusion_invalidCombination_returnsNull() {
        val result = GoopType.getFusionResult(GoopType.WATER, GoopType.NATURE)
        assertNull(result)
    }

    @Test
    fun fusion_sameBasicType_returnsNull() {
        val result = GoopType.getFusionResult(GoopType.WATER, GoopType.WATER)
        assertNull(result)
    }

    @Test
    fun fusion_hybridTypes_returnsNull() {
        val result = GoopType.getFusionResult(GoopType.STEAM, GoopType.MAGMA)
        assertNull(result)
    }

    // --- Basic Types Tests ---

    @Test
    fun getBasicTypes_returnsFiveTypes() {
        val basics = GoopType.getBasicTypes()
        assertEquals(5, basics.size)
    }

    @Test
    fun getBasicTypes_containsAllBasicTypes() {
        val basics = GoopType.getBasicTypes()
        assertTrue(basics.contains(GoopType.WATER))
        assertTrue(basics.contains(GoopType.FIRE))
        assertTrue(basics.contains(GoopType.NATURE))
        assertTrue(basics.contains(GoopType.ELECTRIC))
        assertTrue(basics.contains(GoopType.SHADOW))
    }

    @Test
    fun getBasicTypes_doesNotContainHybridTypes() {
        val basics = GoopType.getBasicTypes()
        assertFalse(basics.contains(GoopType.STEAM))
        assertFalse(basics.contains(GoopType.LIGHTNING_PLANT))
        assertFalse(basics.contains(GoopType.MAGMA))
        assertFalse(basics.contains(GoopType.ICE))
        assertFalse(basics.contains(GoopType.VOID))
    }

    // --- Display Name Tests ---

    @Test
    fun displayName_basicTypes_areCorrect() {
        assertEquals("Water", GoopType.WATER.displayName)
        assertEquals("Fire", GoopType.FIRE.displayName)
        assertEquals("Nature", GoopType.NATURE.displayName)
        assertEquals("Electric", GoopType.ELECTRIC.displayName)
        assertEquals("Shadow", GoopType.SHADOW.displayName)
    }

    @Test
    fun displayName_hybridTypes_areCorrect() {
        assertEquals("Steam", GoopType.STEAM.displayName)
        assertEquals("Lightning Plant", GoopType.LIGHTNING_PLANT.displayName)
        assertEquals("Magma", GoopType.MAGMA.displayName)
        assertEquals("Ice", GoopType.ICE.displayName)
        assertEquals("Void", GoopType.VOID.displayName)
    }

    // --- Habitat Tests ---

    @Test
    fun habitat_allTypesHaveNonEmptyHabitat() {
        GoopType.entries.forEach { type ->
            assertTrue("${type.name} should have a non-empty habitat", type.habitat.isNotEmpty())
        }
    }

    // --- Color Tests ---

    @Test
    fun colors_allTypesHaveDistinctPrimaryColors() {
        val colors = GoopType.entries.map { it.primaryColor }
        assertEquals(colors.size, colors.toSet().size)
    }

    // --- Total Type Count ---

    @Test
    fun totalTypeCount_isTen() {
        assertEquals(10, GoopType.entries.size)
    }
}
