package com.snaptric.feature.capture.viewmodel

import android.graphics.Bitmap
import app.cash.turbine.test
import com.snaptric.MainDispatcherRule
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.Reading
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
        return CaptureViewModel(meterReadingAnalyzer, meterDao)
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
        coEvery { meterReadingAnalyzer.analyze(bitmap) } returns reading
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
        coEvery { meterReadingAnalyzer.analyze(bitmap) } returns reading
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
        coEvery { meterReadingAnalyzer.analyze(bitmap) } returns reading
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
}
