package com.snaptric.feature.capture.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.domain.MeterReadingAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the [CaptureScreen].
 * Manages the camera capture state, image analysis via AI, and saving the results to the database.
 */
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val meterReadingAnalyzer: MeterReadingAnalyzer,
    private val meterDao: MeterDao
) : ViewModel() {

    // The bitmap captured from the camera, used for display in the confirmation dialog.
    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap = _capturedBitmap.asStateFlow()

    // Flag indicating if an analysis process is currently running.
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    // The string result returned from the AI analyzer.
    private val _capturedValue = MutableStateFlow<String?>(null)
    val capturedValue = _capturedValue.asStateFlow()

    // Observable list of all properties to allow the user to select the destination for the reading.
    val properties = meterDao.getAllProperties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks the user-selected property ID.
    val selectedPropertyId = MutableStateFlow<Long?>(null)

    // Observable list of utilities (meters) filtered by the selected property.
    @OptIn(ExperimentalCoroutinesApi::class)
    val utilities = selectedPropertyId.flatMapLatest { id ->
            if (id ==null) flowOf(emptyList())
            else meterDao.getUtilitiesForProperty(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Updates the selected property and triggers a refresh of the utilities list.
     */
    fun onPropertySelected(id: Long) {
        selectedPropertyId.value = id
    }
    
    /**
     * Triggers the AI analysis on the provided [bitmap].
     * Updates the [_isAnalyzing] and [_capturedValue] states.
     */
    fun analyzeAndShowDialog(bitmap: Bitmap) {
        _capturedBitmap.value =bitmap
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val reading = meterReadingAnalyzer.analyze(bitmap)
                _capturedValue.value = reading.value
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Persists the confirmed reading to the database.
     */
    fun saveReading(value : Double, utilityId : Long){
        viewModelScope.launch {
            meterDao.insertReading(
                ReadingEntity(
                    utilityId = utilityId,
                    value = value,
                    timestamp = System.currentTimeMillis(),
                    source = "MLKit"
                )
            )
            _capturedValue.value = null // reset to close the dialog
        }
    }
    
    /**
     * Clears the current captured value to dismiss the confirmation dialog.
     */
    fun clearCapturedValue(){_capturedValue.value = null}
}
