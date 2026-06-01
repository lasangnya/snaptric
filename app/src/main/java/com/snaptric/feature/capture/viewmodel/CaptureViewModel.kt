package com.snaptric.feature.capture.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.ReadingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val meterReadingAnalyzer: MeterReadingAnalyzer,
    private val repository: ReadingRepository
) : ViewModel() {

    fun analyzeAndSave(uri : Uri){
        viewModelScope.launch{
            val reading = meterReadingAnalyzer.analyze(uri)
            repository.saveReading(reading)
        }
    }
}