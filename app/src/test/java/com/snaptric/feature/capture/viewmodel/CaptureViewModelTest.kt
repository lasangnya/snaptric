package com.snaptric.feature.capture.viewmodel

import android.graphics.Bitmap
import app.cash.turbine.test
import com.snaptric.MainDispatcherRule
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.Reading
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val meterReadingAnalyzer: MeterReadingAnalyzer = mockk()
    private val meterDao: MeterDao = mockk(relaxed = true)

    private fun createViewModel(): CaptureViewModel {
        every { meterDao.getAllUtilities() } returns flowOf(emptyList())
        every { meterDao.getAllReadings() } returns flowOf(emptyList())
        return CaptureViewModel(meterReadingAnalyzer, meterDao, SavedStateHandle())
    }

    @Test
    fun `properties flow emits from MeterDao`() = runTest(mainDispatcherRule.testDispatcher) {
        val properties = listOf(
            PropertyEntity(id = 1L, name = "Home", address = "123 Main St")
        )
        every { meterDao.getAllProperties() } returns flowOf(properties)

        val viewModel = createViewModel()

        viewModel.properties.test {
            assertEquals(emptyList<PropertyEntity>(), awaitItem())
            advanceUntilIdle()
            assertEquals(properties, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPropertySelected updates utilities flow`() = runTest(mainDispatcherRule.testDispatcher) {
        val propertyId = 1L
        val utilities = listOf(
            UtilityEntity(
                id = 1L,
                propertyId = propertyId,
                type = UtilityType.ELECTRICITY,
                unit = "kWh",
                initialReading = 0.0
            )
        )
        every { meterDao.getAllProperties() } returns flowOf(emptyList())
        every { meterDao.getUtilitiesForProperty(propertyId) } returns flowOf(utilities)

        val viewModel = createViewModel()

        viewModel.utilities.test {
            assertEquals(emptyList<UtilityEntity>(), awaitItem())

            viewModel.onPropertySelected(propertyId)
            advanceUntilIdle()

            assertEquals(utilities, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analyzeAndShowDialog sets isAnalyzing true then false and sets capturedValue`() = runTest(mainDispatcherRule.testDispatcher) {
        val bitmap = mockk<Bitmap>(relaxed = true)
        val reading = Reading(value = "12345", timestamp = 0L, source = "MLKit")
        coEvery { meterReadingAnalyzer.analyze(bitmap, null) } returns reading
        every { meterDao.getAllProperties() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.isAnalyzing.test {
            assertEquals(false, awaitItem())

            viewModel.analyzeAndShowDialog(bitmap)
            advanceUntilIdle()

            assertEquals(true, awaitItem())
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.capturedValue.test {
            assertEquals("12345", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveReading calls insertReading on DAO and clears capturedValue`() = runTest(mainDispatcherRule.testDispatcher) {
        val bitmap = mockk<Bitmap>(relaxed = true)
        val reading = Reading(value = "12345", timestamp = 0L, source = "MLKit")
        coEvery { meterReadingAnalyzer.analyze(bitmap, null) } returns reading
        every { meterDao.getAllProperties() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.analyzeAndShowDialog(bitmap)
        advanceUntilIdle()

        viewModel.saveReading(value = 12345.0, utilityId = 1L)
        advanceUntilIdle()

        coVerify { meterDao.insertReading(any()) }

        viewModel.capturedValue.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearCapturedValue sets capturedValue to null`() = runTest(mainDispatcherRule.testDispatcher) {
        val bitmap = mockk<Bitmap>(relaxed = true)
        val reading = Reading(value = "12345", timestamp = 0L, source = "MLKit")
        coEvery { meterReadingAnalyzer.analyze(bitmap, null) } returns reading
        every { meterDao.getAllProperties() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.analyzeAndShowDialog(bitmap)
        advanceUntilIdle()

        viewModel.capturedValue.test {
            assertEquals("12345", awaitItem())

            viewModel.clearCapturedValue()
            advanceUntilIdle()

            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a scanned serial selects its meter and is remembered on a new meter`() = runTest(mainDispatcherRule.testDispatcher) {
        val known = UtilityEntity(id = 1L, propertyId = 2L, type = UtilityType.GAS, unit = "m³", initialReading = 0.0, serialNumber = "20481733")
        val unnamed = UtilityEntity(id = 3L, propertyId = 2L, type = UtilityType.WATER, unit = "m³", initialReading = 0.0)
        val viewModel = createViewModel()
        every { meterDao.getAllUtilities() } returns flowOf(listOf(known, unnamed))
        val bitmap = mockk<Bitmap>(relaxed = true)

        coEvery { meterReadingAnalyzer.analyze(bitmap, null) } returns
            Reading(value = "00845", timestamp = 0L, source = "MLKit", serialNumber = "20481733")
        viewModel.analyzeAndShowDialog(bitmap)
        advanceUntilIdle()

        assertEquals(known, viewModel.meterMatch.value?.utility)
        assertEquals(2L, viewModel.selectedPropertyId.value)

        // A different serial saved against the water meter is stored on it for next time.
        coEvery { meterReadingAnalyzer.analyze(bitmap, null) } returns
            Reading(value = "00120", timestamp = 0L, source = "MLKit", serialNumber = "55501234")
        viewModel.analyzeAndShowDialog(bitmap)
        advanceUntilIdle()
        viewModel.saveReading(120.0, utilityId = 3L)
        advanceUntilIdle()

        coVerify { meterDao.insertUtility(unnamed.copy(serialNumber = "55501234")) }
    }

    @Test
    fun `starting from a meter selects its property and ignores the default`() = runTest(mainDispatcherRule.testDispatcher) {
        val electric = UtilityEntity(id = 7L, propertyId = 3L, type = UtilityType.ELECTRICITY, unit = "kWh", initialReading = 0.0)
        every { meterDao.getAllUtilities() } returns flowOf(listOf(electric))
        every { meterDao.getAllReadings() } returns flowOf(emptyList())

        val viewModel = CaptureViewModel(meterReadingAnalyzer, meterDao, SavedStateHandle(mapOf("utilityId" to 7L, "propertyId" to -1L)))
        advanceUntilIdle()

        assertEquals(7L, viewModel.preselectedUtilityId)
        assertEquals(3L, viewModel.selectedPropertyId.value)
        viewModel.selectDefaultProperty(1L)
        assertEquals(3L, viewModel.selectedPropertyId.value)
    }

    @Test
    fun `an automatic match doesn't override the property the user came from`() = runTest(mainDispatcherRule.testDispatcher) {
        val gas = UtilityEntity(id = 1L, propertyId = 9L, type = UtilityType.GAS, unit = "m³", initialReading = 0.0, serialNumber = "24200264")
        every { meterDao.getAllUtilities() } returns flowOf(listOf(gas))
        every { meterDao.getAllReadings() } returns flowOf(emptyList())
        val viewModel = CaptureViewModel(meterReadingAnalyzer, meterDao, SavedStateHandle(mapOf("propertyId" to 4L)))
        val bitmap = mockk<Bitmap>(relaxed = true)
        coEvery { meterReadingAnalyzer.analyze(bitmap, null) } returns
            Reading(value = "00561", timestamp = 0L, source = "MLKit", serialNumber = "24200264")

        viewModel.analyzeAndShowDialog(bitmap)
        advanceUntilIdle()

        assertEquals(4L, viewModel.selectedPropertyId.value)
    }

    @Test
    fun `without a starting point the first property becomes the default`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.selectDefaultProperty(2L)
        assertEquals(2L, viewModel.selectedPropertyId.value)
        assertNull(viewModel.preselectedUtilityId)
    }
}
