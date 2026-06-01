package com.snaptric.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.domain.ReadingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Viewmodel for the Home screen
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val readingRepository: ReadingRepository,
) : ViewModel() {
    val latestRead = readingRepository.latestReading()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}