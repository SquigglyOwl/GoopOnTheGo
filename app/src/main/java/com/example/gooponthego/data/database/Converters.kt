package com.example.gooponthego.data.database

import androidx.room.TypeConverter
import com.example.gooponthego.models.GoopType

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
