package com.snaptric.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.domain.ReadingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Viewmodel for the Home screen
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val readingRepository: ReadingRepository,
    private val meterDao: MeterDao,
) : ViewModel() {
    val latestRead = readingRepository.latestReading()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // observe the analyzing state
    val isAnalyzing = readingRepository.isAnalyzing()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val properties = meterDao.getAllProperties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readings = meterDao.getAllReadings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
