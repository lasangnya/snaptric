package com.snaptric.feature.tariffs.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.TariffEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.domain.tariff.BillTextParser
import com.snaptric.core.domain.tariff.BillTextReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/** Progress of reading a bill photo. */
sealed interface BillScanState {
    data object Idle : BillScanState
    data object Reading : BillScanState
    /** The photo was read but no tariff details were recognised, or reading failed. */
    data class Failed(val message: String) : BillScanState
}

/**
 * ViewModel for the tariff screen of one meter: lists its tariffs and holds the tariff being
 * entered by hand or reviewed after scanning a bill.
 */
@HiltViewModel
class TariffViewModel @Inject constructor(
    private val meterDao: MeterDao,
    private val billTextReader: BillTextReader,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val utilityId: Long = checkNotNull(savedStateHandle["utilityId"])

    val utility: StateFlow<UtilityEntity?> = meterDao.getUtility(utilityId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** This meter's tariffs, newest first. */
    val tariffs: StateFlow<List<TariffEntity>> = meterDao.getTariffsForUtility(utilityId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _draft = MutableStateFlow<TariffDraft?>(null)

    /** The tariff being entered or reviewed, or null while the list is showing. */
    val draft: StateFlow<TariffDraft?> = _draft.asStateFlow()

    private val _scanState = MutableStateFlow<BillScanState>(BillScanState.Idle)
    val scanState: StateFlow<BillScanState> = _scanState.asStateFlow()

    /** Starts a new tariff typed in by hand. */
    fun enterManually() {
        viewModelScope.launch { _draft.value = newDraft() }
    }

    fun edit(tariff: TariffEntity) {
        _draft.value = TariffDraft.of(tariff)
    }

    fun updateDraft(transform: (TariffDraft) -> TariffDraft) {
        _draft.update { it?.let(transform) }
    }

    fun cancelDraft() {
        _draft.value = null
    }

    /** Reads a bill photographed in the app. */
    fun readBill(bitmap: Bitmap) = readBill { billTextReader.read(bitmap) }

    /** Reads a bill picked from the gallery. */
    fun readBill(uri: Uri) = readBill { billTextReader.read(uri) }

    private fun readBill(read: suspend () -> String) {
        viewModelScope.launch {
            _scanState.value = BillScanState.Reading
            val text = runCatching { read() }.getOrNull()
            if (text == null) {
                _scanState.value = BillScanState.Failed("Couldn't read that photo. Try again in better light, or enter the tariff by hand.")
                return@launch
            }
            val bill = BillTextParser.parse(text)
            if (!bill.foundAnything) {
                _scanState.value = BillScanState.Failed(
                    "No tariff details found. Photograph the part of the bill that shows the unit rate and standing charge."
                )
                return@launch
            }
            val draft = newDraft()
            _draft.value = draft.withBill(bill, meterType())
            _scanState.value = BillScanState.Idle
        }
    }

    fun dismissScanError() {
        _scanState.value = BillScanState.Idle
    }

    /** Saves the draft if it's complete. */
    fun save() {
        val meter = utility.value ?: return
        val entity = _draft.value?.toEntity(utilityId, meter.type) ?: return
        viewModelScope.launch {
            meterDao.insertTariff(entity)
            _draft.value = null
        }
    }

    fun delete(tariff: TariffEntity) {
        viewModelScope.launch {
            meterDao.deleteTariff(tariff.id)
            if (_draft.value?.id == tariff.id) _draft.value = null
        }
    }

    /** A blank tariff in the currency of the meter's latest tariff, or the phone's currency. */
    private suspend fun newDraft(): TariffDraft {
        val currency = tariffs.value.firstOrNull()?.currency ?: localCurrency()
        return TariffDraft.blank(meterType(), currency, LocalDate.now())
    }

    private suspend fun meterType(): UtilityType =
        (utility.value ?: meterDao.getUtility(utilityId).first())?.type ?: UtilityType.ELECTRICITY

    private fun localCurrency(): String =
        runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull() ?: "GBP"
}
