package com.snaptric.core.domain

import kotlinx.coroutines.flow.Flow

/**
 * Interface defining the contract for managing meter reading data and analysis state.
 */
interface ReadingRepository{
    /**
     * Persists a [Reading] object.
     */
    suspend fun saveReading(reading: Reading)
    
    /**
     * Returns a [Flow] that emits the most recently captured reading.
     */
    fun latestReading() : Flow<Reading?>

    /**
     * Returns a [Flow] indicating if an analysis is currently in progress.
     */
    fun isAnalyzing() : Flow<Boolean>
    
    /**
     * Updates the current analysis status.
     */
    fun setAnalyzing(analyzing : Boolean)
}
