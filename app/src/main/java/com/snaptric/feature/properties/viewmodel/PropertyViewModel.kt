package com.snaptric.feature.properties.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.PropertyEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the [PropertiesScreen].
 * Handles the listing and addition of properties.
 */
@HiltViewModel
class PropertyViewModel @Inject constructor(
    private val meterDao: MeterDao
) : ViewModel() {

    /**
     * Observable flow of all available properties.
     */
     val properties: StateFlow<List<PropertyEntity>> = meterDao.getAllProperties()
         .stateIn(
             scope = viewModelScope,
             started = SharingStarted.WhileSubscribed(5000),
             initialValue = emptyList()
         )

    /**
     * Creates and persists a new property record.
     */
    fun addProperty(name : String, address : String?, iconIdentifier: String?){
        viewModelScope.launch {
            val newProperty = PropertyEntity(
                name = name,
                address = address,
                iconIdentifier = iconIdentifier.toString()
            )
            meterDao.insertProperty(newProperty)
        }
    }
}
