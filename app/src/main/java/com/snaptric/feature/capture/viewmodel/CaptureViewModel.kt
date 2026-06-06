package com.snaptric.feature.capture.viewmodel

import android.graphics.Bitmap
import android.net.Uri
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

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val meterReadingAnalyzer: MeterReadingAnalyzer,
    private val meterDao: MeterDao
) : ViewModel() {

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap = _capturedBitmap.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    // hold the result from MLkit
    private val _capturedValue = MutableStateFlow<String?>(null)
    val capturedValue = _capturedValue.asStateFlow()

    // data for selectors
    val properties = meterDao.getAllProperties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Track selected property and load its meters
    val selectedPropertyId = MutableStateFlow<Long?>(null)


    @OptIn(ExperimentalCoroutinesApi::class)
    val utilities = selectedPropertyId.flatMapLatest { id ->
            if (id ==null) flowOf(emptyList())
            else meterDao.getUtilitiesForProperty(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onPropertySelected(id: Long) {
        selectedPropertyId.value = id
    }
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
    fun clearCapturedValue(){_capturedValue.value = null}
}