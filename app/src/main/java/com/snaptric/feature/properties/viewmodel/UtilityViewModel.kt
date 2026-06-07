package com.snaptric.feature.properties.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.ReadingEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class UtilityViewModel @Inject constructor(
    meterDao: MeterDao,
    savedStateHandle: SavedStateHandle
) : ViewModel(){
    private val utilityId : Long = checkNotNull(savedStateHandle["utilityId"])

    val readings : StateFlow<List<ReadingEntity>> = meterDao.getReadingsForUtility(utilityId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
}