package com.snaptric.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.UtilityEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing all meter-related database entities.
 */
@Dao
interface MeterDao {
    /**
     * Adds or updates a property (e.g., "Main Home", "Rental Apartment").
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property : PropertyEntity) : Long

    /**
     * Retrieves all properties, sorted by their creation in the database.
     */
    @Query("SELECT * FROM properties")
    fun getAllProperties() : Flow<List<PropertyEntity>>

    /**
     * Adds or updates a specific meter/utility (e.g., "Electricity Meter") to a property.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUtility(utility : UtilityEntity) : Long

    /**
     * Retrieves all meters associated with a specific property ID.
     */
    @Query("SELECT * FROM utility WHERE propertyId = :propertyId")
    fun getUtilitiesForProperty(propertyId : Long) : Flow<List<UtilityEntity>>

    /**
     * Persists a newly captured meter reading.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: ReadingEntity)

    /**
     * Retrieves reading history for a specific meter, from newest to oldest.
     */
    @Query("SELECT * FROM readings WHERE utilityId = :utilityId ORDER BY timestamp DESC")
    fun getReadingsForUtility(utilityId : Long) : Flow<List<ReadingEntity>>

    /**
     * Retrieves the entire reading history across all properties and meters.
     */
    @Query("SELECT * FROM readings ORDER BY timestamp DESC")
    fun getAllReadings() : Flow<List<ReadingEntity>>

    /**
     * Removes a property and its configuration from the database.
     */
    @Query("DELETE FROM properties WHERE id = :propertyId")
    suspend fun deleteProperty(propertyId: Long)
}
