package com.snaptric.feature.capture.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.ReadingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val meterReadingAnalyzer: MeterReadingAnalyzer,
    private val repository: ReadingRepository
) : ViewModel() {

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    fun analyzeAndSave(uri: Uri, onComplete: () -> Unit){
        viewModelScope.launch{
            _isAnalyzing.value = true

            try {
                val reading = meterReadingAnalyzer.analyze(uri)
                repository.saveReading(reading)
            } catch (e: Exception) {
                Log.e("CaptureVM", "Analysis failed", e)
            } finally {
                _isAnalyzing.value = false
                onComplete()
            }
        }
    }
}