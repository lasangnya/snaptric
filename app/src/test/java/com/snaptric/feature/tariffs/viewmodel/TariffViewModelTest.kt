package com.snaptric.feature.tariffs.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import com.snaptric.MainDispatcherRule
import com.snaptric.core.data.CurrencyPreference
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.TariffEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.domain.tariff.BillTextReader
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TariffViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val meterDao: MeterDao = mockk(relaxed = true)
    private val reader: BillTextReader = mockk()
    private val currencyPreference = object : CurrencyPreference {
        override val currency = MutableStateFlow("EUR")
        override fun set(code: String) { currency.value = code }
    }
    private val gas = UtilityEntity(id = 5, propertyId = 1, type = UtilityType.GAS, unit = "m³", initialReading = 0.0)

    @Before
    fun setUp() {
        every { meterDao.getUtility(5) } returns flowOf(gas)
        every { meterDao.getTariffsForUtility(5) } returns flowOf(emptyList())
    }

    private fun viewModel() = TariffViewModel(meterDao, reader, currencyPreference, SavedStateHandle(mapOf("utilityId" to 5L)))

    @Test
    fun scannedBill_opensReviewAndSavesOnlyWhenConfirmed() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { reader.read(any<Bitmap>()) } returns "Unit rate 6.24p/kWh\nStanding charge 31.43p/day\nCalorific value 39.2"
        val vm = viewModel()
        backgroundScope.launch { vm.utility.collect {} }
        backgroundScope.launch { vm.tariffs.collect {} }

        vm.readBill(mockk<Bitmap>())
        advanceUntilIdle()

        val draft = vm.draft.value!!
        assertTrue(draft.scanned)
        assertEquals("0.0624", draft.unitRate)
        assertEquals("0.3143", draft.standingCharge)
        assertEquals("39.2", draft.calorificValue)
        assertEquals(BillScanState.Idle, vm.scanState.value)
        coVerify(exactly = 0) { meterDao.insertTariff(any()) }

        vm.updateDraft { it.copy(currency = "GBP") }
        vm.save()
        advanceUntilIdle()

        val saved = slot<TariffEntity>()
        coVerify { meterDao.insertTariff(capture(saved)) }
        assertEquals(5, saved.captured.utilityId)
        assertEquals(0.0624, saved.captured.unitRate, 1e-9)
        assertEquals("BillScan", saved.captured.source)
        assertNull(vm.draft.value)
    }

    @Test
    fun billWithoutTariff_reportsFailure() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { reader.read(any<Bitmap>()) } returns "Thank you for your payment"
        val vm = viewModel()

        vm.readBill(mockk<Bitmap>())
        advanceUntilIdle()

        assertTrue(vm.scanState.value is BillScanState.Failed)
        assertNull(vm.draft.value)
    }

    @Test
    fun unreadablePhoto_reportsFailure() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { reader.read(any<Bitmap>()) } throws IllegalStateException("ML Kit failed")
        val vm = viewModel()

        vm.readBill(mockk<Bitmap>())
        advanceUntilIdle()

        assertTrue(vm.scanState.value is BillScanState.Failed)
    }

    @Test
    fun manualTariff_startsInTheSettingsCurrency() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = viewModel()
        currencyPreference.set("LKR")

        vm.enterManually()
        advanceUntilIdle()

        assertEquals("LKR", vm.draft.value!!.currency)
    }

    @Test
    fun scannedBill_currencyOverridesTheSetting() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { reader.read(any<Bitmap>()) } returns "Unit rate 6.24p/kWh"
        val vm = viewModel()

        vm.readBill(mockk<Bitmap>())
        advanceUntilIdle()

        assertEquals("GBP", vm.draft.value!!.currency)
    }
}
