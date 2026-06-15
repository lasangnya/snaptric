package com.snaptric.core.database.converter

import androidx.room.TypeConverter
import com.snaptric.core.database.entity.UtilityType

/**
 * Type converters for Room to handle non-primitive types in the database.
 */
class DatabaseConverters {
    /**
     * Converts a [UtilityType] enum to its String name for storage.
     */
    @TypeConverter
    fun FromUtilityType(value: UtilityType) = value.name

    /**
     * Reconstructs a [UtilityType] enum from its stored String name.
     */
    @TypeConverter
    fun toUtilityType(value: String) = UtilityType.valueOf(value)
}
