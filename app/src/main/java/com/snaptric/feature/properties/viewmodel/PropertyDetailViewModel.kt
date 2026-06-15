package com.snaptric.feature.properties.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the [PropertyDetailScreen].
 * Manages the specific details and utilities (meters) associated with a single property.
 */
@HiltViewModel
class PropertyDetailViewModel @Inject constructor(
    private val meterDao: MeterDao,
    savedStateHandle: SavedStateHandle // Automatically extracts 'propertyId' from the navigation route.
) : ViewModel() {
    
    // The current property ID being viewed.
    private val propertyId : Long = checkNotNull(savedStateHandle["propertyId"])

    /**
     * Observable flow of all utility meters for this specific property.
     */
    val utilities: StateFlow<List<UtilityEntity>> = meterDao.getUtilitiesForProperty(propertyId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Adds a new utility meter to the current property.
     */
    fun addUtility(type: UtilityType, unit : String, initialReading : Double, name : String?){
        viewModelScope.launch {
            meterDao.insertUtility(
                UtilityEntity(
                    propertyId = propertyId,
                    type = type,
                    unit = unit,
                    initialReading = initialReading,
                    name = name
                )
            )
        }
    }
}
