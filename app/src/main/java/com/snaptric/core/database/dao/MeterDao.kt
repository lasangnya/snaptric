package com.snaptric.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
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
     * Uses an upsert rather than REPLACE: REPLACE deletes the old row first, which
     * would CASCADE-delete all of the property's utilities and readings.
     * Returns the new row id, or -1 when an existing property was updated.
     */
    @Upsert
    suspend fun insertProperty(property : PropertyEntity) : Long

    /**
     * Retrieves all properties, sorted by their creation in the database.
     */
    @Query("SELECT * FROM properties")
    fun getAllProperties() : Flow<List<PropertyEntity>>

    /**
     * Adds or updates a specific meter/utility (e.g., "Electricity Meter") to a property.
     * Upsert for the same reason as [insertProperty]: keeps the meter's readings on update.
     * Returns the new row id, or -1 when an existing utility was updated.
     */
    @Upsert
    suspend fun insertUtility(utility : UtilityEntity) : Long

    /**
     * Retrieves all meters associated with a specific property ID.
     */
    @Query("SELECT * FROM utility WHERE propertyId = :propertyId")
    fun getUtilitiesForProperty(propertyId : Long) : Flow<List<UtilityEntity>>

    /**
     * Retrieves every meter across all properties.
     */
    @Query("SELECT * FROM utility")
    fun getAllUtilities() : Flow<List<UtilityEntity>>

    /**
     * Persists a newly captured meter reading.
     */
    @Upsert
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

    /**
     * Observes one meter, or null once it has been deleted.
     */
    @Query("SELECT * FROM utility WHERE id = :utilityId")
    fun getUtility(utilityId: Long): Flow<UtilityEntity?>

    /**
     * Removes a meter; its readings are removed with it (CASCADE).
     */
    @Query("DELETE FROM utility WHERE id = :utilityId")
    suspend fun deleteUtility(utilityId: Long)

    /**
     * Removes a single reading.
     */
    @Query("DELETE FROM readings WHERE id = :readingId")
    suspend fun deleteReading(readingId: Long)
}
