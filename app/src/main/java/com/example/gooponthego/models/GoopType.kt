package com.example.gooponthego.models

import androidx.annotation.ColorInt

enum class GoopType(
    val displayName: String,
    @ColorInt val primaryColor: Int,
    @ColorInt val secondaryColor: Int,
    val habitat: String
) {
    WATER(
        displayName = "Water",
        primaryColor = 0xFF4FC3F7.toInt(),
        secondaryColor = 0xFF0288D1.toInt(),
        habitat = "Near water sources"
    ),
    FIRE(
        displayName = "Fire",
        primaryColor = 0xFFFF7043.toInt(),
        secondaryColor = 0xFFD84315.toInt(),
        habitat = "Warm areas"
    ),
    NATURE(
        displayName = "Nature",
        primaryColor = 0xFF81C784.toInt(),
        secondaryColor = 0xFF388E3C.toInt(),
        habitat = "Parks and plants"
    ),
    ELECTRIC(
        displayName = "Electric",
        primaryColor = 0xFFFFD54F.toInt(),
        secondaryColor = 0xFFFFA000.toInt(),
        habitat = "Near electronics"
    ),
    SHADOW(
        displayName = "Shadow",
        primaryColor = 0xFFBA68C8.toInt(),
        secondaryColor = 0xFF7B1FA2.toInt(),
        habitat = "Dark areas"
    ),
    // Hybrid types from fusion
    STEAM(
        displayName = "Steam",
        primaryColor = 0xFFB0BEC5.toInt(),
        secondaryColor = 0xFF607D8B.toInt(),
        habitat = "Misty areas"
    ),
    LIGHTNING_PLANT(
        displayName = "Lightning Plant",
        primaryColor = 0xFFAED581.toInt(),
        secondaryColor = 0xFFFFEB3B.toInt(),
        habitat = "Stormy gardens"
    ),
    MAGMA(
        displayName = "Magma",
        primaryColor = 0xFFFF5722.toInt(),
        secondaryColor = 0xFF795548.toInt(),
        habitat = "Volcanic regions"
    ),
    ICE(
        displayName = "Ice",
        primaryColor = 0xFFE1F5FE.toInt(),
        secondaryColor = 0xFF4DD0E1.toInt(),
        habitat = "Cold areas"
    ),
    VOID(
        displayName = "Void",
        primaryColor = 0xFF311B92.toInt(),
        secondaryColor = 0xFF000000.toInt(),
        habitat = "Unknown"
    );

    companion object {
        fun getFusionResult(type1: GoopType, type2: GoopType): GoopType? {
            val pair = setOf(type1, type2)
            return when {
                pair == setOf(WATER, FIRE) -> STEAM
                pair == setOf(ELECTRIC, NATURE) -> LIGHTNING_PLANT
                pair == setOf(FIRE, NATURE) -> MAGMA
                pair == setOf(WATER, ELECTRIC) -> ICE
                pair == setOf(SHADOW, SHADOW) -> VOID
                else -> null
            }
        }

        fun getBasicTypes(): List<GoopType> = listOf(WATER, FIRE, NATURE, ELECTRIC, SHADOW)
    }
}
