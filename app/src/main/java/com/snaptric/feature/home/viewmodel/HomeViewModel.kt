package com.snaptric.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.database.dao.MeterDao
import dagger.hilt.android.lifecycle.HiltViewModel
import com.snaptric.core.domain.insights.Insight
import com.snaptric.core.domain.insights.InsightWriter
import com.snaptric.core.domain.insights.buildMeterRecaps
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for the [HomeScreen].
 * Aggregates data from multiple sources to provide a summary of latest activities and properties.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    meterDao: MeterDao,
    insightWriter: InsightWriter,
) : ViewModel() {
    
    /**
     * Observable flow of all properties configured in the system.
     */
    val properties = meterDao.getAllProperties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Observable flow of the entire history of meter readings.
     */
    val readings = meterDao.getAllReadings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Readings joined with their meter and property, newest first, with per-meter deltas.
     */
    val readingItems = combine(
        meterDao.getAllReadings(),
        meterDao.getAllUtilities(),
        meterDao.getAllProperties(),
        ::buildHomeReadingItems
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * This month's usage summary, or null until at least one meter has usage this month.
     * Numbers are calculated here; the writer (Gemma on-device, or a template) only words them.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val insight: StateFlow<Insight?> = combine(
        meterDao.getAllReadings(),
        meterDao.getAllUtilities(),
        meterDao.getAllProperties()
    ) { readings, utilities, properties ->
        buildMeterRecaps(readings, utilities, properties, LocalDate.now())
    }
        .distinctUntilChanged()
        .mapLatest { recaps -> if (recaps.isEmpty()) null else insightWriter.write(recaps) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
