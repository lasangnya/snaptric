package com.snaptric.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.ai.litertlm.GemmaModelDownloader
import com.snaptric.ai.litertlm.GemmaTextEngine
import com.snaptric.core.database.dao.MeterDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * How much the user has stored, shown under "Your data".
 */
data class DataSummary(val properties: Int, val meters: Int, val readings: Int)

/**
 * ViewModel for the Settings screen: the optional on-device model and a summary of stored data.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    meterDao: MeterDao,
    private val downloader: GemmaModelDownloader,
    private val gemma: GemmaTextEngine
) : ViewModel() {

    /**
     * The model's install state, refreshed every second while the screen is visible so download
     * progress moves and a finished download is picked up.
     */
    val modelState: StateFlow<GemmaModelDownloader.State> = flow {
        while (true) {
            emit(downloader.currentState())
            delay(1_000)
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GemmaModelDownloader.State.NotInstalled)

    val dataSummary: StateFlow<DataSummary> = combine(
        meterDao.getAllProperties(),
        meterDao.getAllUtilities(),
        meterDao.getAllReadings()
    ) { properties, meters, readings -> DataSummary(properties.size, meters.size, readings.size) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DataSummary(0, 0, 0))

    fun downloadModel(allowMobileData: Boolean) = downloader.start(allowMobileData)

    fun cancelDownload() = downloader.cancel()

    fun deleteModel() {
        viewModelScope.launch(Dispatchers.IO) {
            gemma.close()
            downloader.delete()
        }
    }
}
