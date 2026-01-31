package com.example.pokemongoop.data.database

import androidx.room.TypeConverter
import com.example.pokemongoop.models.GoopType

class Converters {
    @TypeConverter
    fun fromGoopType(value: GoopType): String {
        return value.name
    }

    @TypeConverter
    fun toGoopType(value: String): GoopType {
        return GoopType.valueOf(value)
    }
}
