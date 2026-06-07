package com.snaptric.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.UtilityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property : PropertyEntity) : Long

    @Query("SELECT * FROM properties")
    fun getAllProperties() : Flow<List<PropertyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtility(utility : UtilityEntity) : Long

    @Query("SELECT * FROM utility WHERE propertyId = :propertyId")
    fun getUtilitiesForProperty(propertyId : Long) : Flow<List<UtilityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: ReadingEntity)

    @Query("SELECT * FROM readings WHERE utilityId = :utilityId ORDER BY timestamp DESC")
    fun getReadingsForUtility(utilityId : Long) : Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings ORDER BY timestamp DESC")
    fun getAllReadings() : Flow<List<ReadingEntity>>
}