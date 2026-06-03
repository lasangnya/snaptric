package com.snaptric.feature.properties.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.PropertyEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class PropertyViewModel @Inject constructor(
    private val meterDao: MeterDao
) : ViewModel() {

     val properties: StateFlow<List<PropertyEntity>> = meterDao.getAllProperties()
         .stateIn(
             scope = viewModelScope,
             started = SharingStarted.WhileSubscribed(5000),
             initialValue = emptyList()
         )

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