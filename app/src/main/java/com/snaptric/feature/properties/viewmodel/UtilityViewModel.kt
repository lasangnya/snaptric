package com.snaptric.feature.properties.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.UtilityEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the [UtilityScreen].
 * Provides the historical reading data for a specific meter.
 */
@HiltViewModel
class UtilityViewModel @Inject constructor(
    private val meterDao: MeterDao,
    savedStateHandle: SavedStateHandle // Automatically extracts 'utilityId' from the navigation route.
) : ViewModel(){
    
    // The current utility (meter) ID being viewed.
    private val utilityId : Long = checkNotNull(savedStateHandle["utilityId"])

    /**
     * Observable flow of the reading history for this specific utility.
     */
    val readings : StateFlow<List<ReadingEntity>> = meterDao.getReadingsForUtility(utilityId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    /**
     * The meter being viewed, or null once it has been deleted.
     */
    val utility: StateFlow<UtilityEntity?> = meterDao.getUtility(utilityId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    /**
     * Saves a reading typed in by hand, timestamped now.
     */
    fun addManualReading(value: Double) {
        viewModelScope.launch {
            meterDao.insertReading(
                ReadingEntity(utilityId = utilityId, value = value, timestamp = System.currentTimeMillis(), source = "Manual")
            )
        }
    }

    /**
     * Corrects the value of an existing reading, keeping its time.
     */
    fun updateReading(reading: ReadingEntity, value: Double) {
        viewModelScope.launch { meterDao.insertReading(reading.copy(value = value)) }
    }

    fun deleteReading(reading: ReadingEntity) {
        viewModelScope.launch { meterDao.deleteReading(reading.id) }
    }

    /**
     * Saves changes to the meter's name, unit or starting value.
     */
    fun updateUtility(updated: UtilityEntity) {
        viewModelScope.launch { meterDao.insertUtility(updated) }
    }

    /**
     * Deletes the meter and all of its readings.
     */
    fun deleteUtility() {
        viewModelScope.launch { meterDao.deleteUtility(utilityId) }
    }
}
