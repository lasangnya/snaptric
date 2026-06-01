package com.snaptric.core.data

import com.snaptric.core.domain.Reading
import com.snaptric.core.domain.ReadingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// Reading class holding functions for meter readings
class InMemoryReadingRepository : ReadingRepository {

    private val latestReading = MutableStateFlow<Reading?>(null)

    override suspend fun saveReading(reading: Reading) {
        latestReading.value= reading
    }

    override fun latestReading(): Flow<Reading?> = latestReading
}