package com.snaptric.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.domain.ReadingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

// Viewmodel for the Home screen

class HomeViewModel(
    private val readingRepository: ReadingRepository,
) : ViewModel() {
    val latestRead = readingRepository.latestReading()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}