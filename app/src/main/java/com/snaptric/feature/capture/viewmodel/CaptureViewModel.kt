package com.snaptric.feature.capture.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.insights.MeterMatch
import com.snaptric.core.domain.insights.matchMeter
import com.snaptric.core.domain.insights.sameSerial
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val meterDao: MeterDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * The meter the user started scanning from (e.g. its card on Home or its own screen), if any.
     * It's pre-selected in the confirmation sheet and wins over automatic matching.
     */
    val preselectedUtilityId: Long? = savedStateHandle.get<Long>(ARG_UTILITY_ID)?.takeIf { it > 0 }

    // The property the user started from, if any; otherwise the pre-selected meter's property.
    private val preselectedPropertyId: Long? = savedStateHandle.get<Long>(ARG_PROPERTY_ID)?.takeIf { it > 0 }

    // The bitmap captured from the camera, used for display in the confirmation dialog.
    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap = _capturedBitmap.asStateFlow()

    // Flag indicating if an analysis process is currently running.
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    // Which engine produced the current value (e.g. "MLKit" or "Gemma"), saved with the reading.
    private var capturedSource: String = "MLKit"

    // The serial number seen in the photo, remembered on the chosen meter when it has none yet.
    private var capturedSerial: String? = null

    // Digit positions the analyzer was unsure about, highlighted for the user to check.
    private val _uncertainDigits = MutableStateFlow<Set<Int>>(emptySet())
    val uncertainDigits = _uncertainDigits.asStateFlow()

    // The meter this scan most likely belongs to, pre-selected in the confirmation sheet.
    private val _meterMatch = MutableStateFlow<MeterMatch?>(null)
    val meterMatch = _meterMatch.asStateFlow()

    // The string result returned from the AI analyzer.
    private val _capturedValue = MutableStateFlow<String?>(null)
    val capturedValue = _capturedValue.asStateFlow()

    // Observable list of all properties to allow the user to select the destination for the reading.
    val properties = meterDao.getAllProperties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks the user-selected property ID.
    val selectedPropertyId = MutableStateFlow(preselectedPropertyId)

    init {
        if (preselectedUtilityId != null && preselectedPropertyId == null) {
            viewModelScope.launch {
                meterDao.getAllUtilities().first().firstOrNull { it.id == preselectedUtilityId }?.let {
                    selectedPropertyId.value = it.propertyId
                }
            }
        }
    }

    /**
     * Selects [id] as the default property, unless the user came from a specific property or meter.
     */
    fun selectDefaultProperty(id: Long) {
        if (selectedPropertyId.value == null && preselectedUtilityId == null) selectedPropertyId.value = id
    }

    // Observable list of utilities (meters) filtered by the selected property.
    @OptIn(ExperimentalCoroutinesApi::class)
    val utilities = selectedPropertyId.flatMapLatest { id ->
            if (id ==null) flowOf(emptyList())
            else meterDao.getUtilitiesForProperty(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // The meter chosen in the confirmation sheet; its history feeds the pre-save sanity check.
    private val selectedUtilityId = MutableStateFlow<Long?>(null)

    // Earlier readings of the selected meter.
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedUtilityHistory = selectedUtilityId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else meterDao.getReadingsForUtility(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Records which meter is selected in the confirmation sheet.
     */
    fun onUtilitySelected(id: Long?) {
        selectedUtilityId.value = id
    }

    /**
     * Updates the selected property and triggers a refresh of the utilities list.
     */
    fun onPropertySelected(id: Long) {
        selectedPropertyId.value = id
    }
    
    /**
     * Triggers the AI analysis on the provided [bitmap] (the counter strip), using [fullFrame] to
     * look for the serial number. Then works out which meter the reading most likely belongs to.
     */
    fun analyzeAndShowDialog(bitmap: Bitmap, fullFrame: Bitmap? = null) {
        _capturedBitmap.value =bitmap
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val reading = meterReadingAnalyzer.analyze(bitmap, fullFrame)
                capturedSource = reading.source
                capturedSerial = reading.serialNumber
                _uncertainDigits.value = reading.uncertainDigits
                val match = matchMeter(
                    serialNumber = reading.serialNumber,
                    value = reading.value?.toDoubleOrNull(),
                    utilities = meterDao.getAllUtilities().first(),
                    readings = meterDao.getAllReadings().first()
                )
                _meterMatch.value = match
                // Where the user started from wins over automatic matching.
                if (preselectedUtilityId == null && preselectedPropertyId == null) {
                    match?.let { selectedPropertyId.value = it.utility.propertyId }
                }
                _capturedValue.value = reading.value
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Persists the confirmed reading to the database. If the photo showed a serial number and the
     * meter doesn't have one yet, it's saved on the meter so future scans match automatically.
     */
    fun saveReading(value : Double, utilityId : Long){
        viewModelScope.launch {
            meterDao.insertReading(
                ReadingEntity(
                    utilityId = utilityId,
                    value = value,
                    timestamp = System.currentTimeMillis(),
                    source = capturedSource
                )
            )
            capturedSerial?.let { serial ->
                val meters = meterDao.getAllUtilities().first()
                val meter = meters.firstOrNull { it.id == utilityId }
                if (meter != null && meter.serialNumber == null && meters.none { sameSerial(it.serialNumber, serial) }) {
                    meterDao.insertUtility(meter.copy(serialNumber = serial))
                }
            }
            _capturedValue.value = null // reset to close the dialog
        }
    }

    /**
     * Clears the current captured value to dismiss the confirmation dialog.
     */
    fun clearCapturedValue(){_capturedValue.value = null}

    companion object {
        const val ARG_UTILITY_ID = "utilityId"
        const val ARG_PROPERTY_ID = "propertyId"
    }
}
