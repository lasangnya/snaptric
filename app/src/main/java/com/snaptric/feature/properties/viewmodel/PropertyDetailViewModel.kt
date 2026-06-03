package com.snaptric.feature.properties.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class PropertyDetailViewModel @Inject constructor(
    private val meterDao: MeterDao,
    savedStateHandle: SavedStateHandle // Automaticall catches propertyId from Navigation
) : ViewModel() {
     // Extract propertyId from navigation route
    private val propertyId : Long = checkNotNull(savedStateHandle["propertyId"])

    // Fetch utilities for this property
    val utilities: StateFlow<List<UtilityEntity>> = meterDao.getUtilitiesForProperty(propertyId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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