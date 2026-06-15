package com.snaptric.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.domain.ReadingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the [HomeScreen].
 * Aggregates data from multiple sources to provide a summary of latest activities and properties.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    readingRepository: ReadingRepository,
    meterDao: MeterDao,
) : ViewModel() {
    
    /**
     * Observable flow of the single most recent reading processed by the app.
     */
    val latestRead = readingRepository.latestReading()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Observable flow of the current image analysis status.
     */
    val isAnalyzing = readingRepository.isAnalyzing()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Observable flow of all properties configured in the system.
     */
    val properties = meterDao.getAllProperties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Observable flow of the entire history of meter readings.
     */
    val readings = meterDao.getAllReadings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

