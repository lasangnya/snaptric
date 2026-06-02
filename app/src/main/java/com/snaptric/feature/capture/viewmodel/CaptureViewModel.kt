package com.snaptric.feature.capture.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.ReadingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val meterReadingAnalyzer: MeterReadingAnalyzer,
    private val repository: ReadingRepository,
    private val externalScope: CoroutineScope
) : ViewModel() {

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    fun analyzeAndSave(uri: Uri, onComplete: () -> Unit){
        externalScope.launch{
            repository.setAnalyzing(true)
            try {
                val reading = meterReadingAnalyzer.analyze(uri)
                repository.saveReading(reading)
            } finally {
                repository.setAnalyzing(false)
            }
        }
    }
}