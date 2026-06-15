package com.snaptric.core.data

import com.snaptric.core.domain.Reading
import com.snaptric.core.domain.ReadingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A temporary repository implementation that stores data in memory.
 * Useful for rapid prototyping before the full database implementation is ready.
 */
class InMemoryReadingRepository : ReadingRepository {

    // Holds the most recently saved meter reading.
    private val latestReading = MutableStateFlow<Reading?>(null)
    
    // Tracks whether an image analysis process is currently active.
    private val _isAnalyzing = MutableStateFlow(false)

    /**
     * Persists the reading in memory and notifies all observers.
     */
    override suspend fun saveReading(reading: Reading) {
        latestReading.value= reading
    }

    /**
     * Returns a Flow that emits the latest reading whenever it changes.
     */
    override fun latestReading(): Flow<Reading?> = latestReading

    /**
     * Returns a Flow that emits the analysis status.
     */
    override fun isAnalyzing(): Flow<Boolean> = _isAnalyzing
    
    /**
     * Updates the global analysis state.
     */
    override fun setAnalyzing(analyzing: Boolean) {
        _isAnalyzing.value = analyzing
    }
}
