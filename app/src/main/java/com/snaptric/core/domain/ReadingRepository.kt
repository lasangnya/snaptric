package com.snaptric.core.domain

import kotlinx.coroutines.flow.Flow

// Repository for meter readings
interface ReadingRepository{
    suspend fun saveReading(reading: Reading)
    fun latestReading() : Flow<Reading?>

    fun isAnalyzing() : Flow<Boolean>
    fun setAnalyzing(analyzing : Boolean)
}