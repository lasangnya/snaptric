package com.snaptric.core.database.converter

import androidx.room.TypeConverter
import com.snaptric.core.database.entity.UtilityType

class DatabaseConverters {
    @TypeConverter
    fun FromUtilityType(value: UtilityType) = value.name

    @TypeConverter
    fun toUtilityType(value: String) = UtilityType.valueOf(value)
}