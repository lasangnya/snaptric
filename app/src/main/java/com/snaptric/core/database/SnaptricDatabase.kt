package com.snaptric.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.snaptric.core.database.converter.DatabaseConverters
import com.snaptric.core.database.dao.MeterDao

@Database(
    entities = [
        com.snaptric.core.database.entity.PropertyEntity::class,
        com.snaptric.core.database.entity.UtilityEntity::class,
        com.snaptric.core.database.entity.ReadingEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class SnaptricDatabase : RoomDatabase() {
    abstract fun meterDao(): MeterDao
}