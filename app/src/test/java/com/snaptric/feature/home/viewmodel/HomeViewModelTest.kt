package com.snaptric.feature.home.viewmodel

import app.cash.turbine.test
import com.snaptric.MainDispatcherRule
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.domain.Reading
import com.snaptric.core.domain.ReadingRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val readingRepository: ReadingRepository = mockk()
    private val meterDao: MeterDao = mockk()

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(readingRepository, meterDao)
    }

    @Test
    fun `latestRead emits reading from repository`() = runTest(mainDispatcherRule.testDispatcher) {
        val aReading = Reading(value = "12345", timestamp = 1L, source = "GEMMA")
        every { readingRepository.latestReading() } returns flowOf(aReading)
        every { readingRepository.isAnalyzing() } returns flowOf(false)
        every { meterDao.getAllProperties() } returns flowOf(emptyList())
        every { meterDao.getAllReadings() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.latestRead.test {
            assertNull(awaitItem()) // initial null
            assertEquals(aReading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `latestRead emits null when repository emits null`() = runTest(mainDispatcherRule.testDispatcher) {
        every { readingRepository.latestReading() } returns flowOf(null)
        every { readingRepository.isAnalyzing() } returns flowOf(false)
        every { meterDao.getAllProperties() } returns flowOf(emptyList())
        every { meterDao.getAllReadings() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.latestRead.test {
            assertNull(awaitItem()) // initial null, upstream also null so no re-emission
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isAnalyzing reflects repository analyzing state`() = runTest(mainDispatcherRule.testDispatcher) {
        every { readingRepository.latestReading() } returns flowOf(null)
        every { readingRepository.isAnalyzing() } returns flowOf(true)
        every { meterDao.getAllProperties() } returns flowOf(emptyList())
        every { meterDao.getAllReadings() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.isAnalyzing.test {
            assertEquals(false, awaitItem()) // initial false
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `properties emits list from meterDao`() = runTest(mainDispatcherRule.testDispatcher) {
        val property1 = PropertyEntity(id = 1L, name = "Home", address = "123 Main St")
        val property2 = PropertyEntity(id = 2L, name = "Office", address = "456 Work Ave")

        every { readingRepository.latestReading() } returns flowOf(null)
        every { readingRepository.isAnalyzing() } returns flowOf(false)
        every { meterDao.getAllProperties() } returns flowOf(listOf(property1, property2))
        every { meterDao.getAllReadings() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.properties.test {
            assertEquals(emptyList<PropertyEntity>(), awaitItem()) // initial empty
            assertEquals(listOf(property1, property2), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `properties emits empty list when dao returns empty`() = runTest(mainDispatcherRule.testDispatcher) {
        every { readingRepository.latestReading() } returns flowOf(null)
        every { readingRepository.isAnalyzing() } returns flowOf(false)
        every { meterDao.getAllProperties() } returns flowOf(emptyList())
        every { meterDao.getAllReadings() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.properties.test {
            assertEquals(emptyList<PropertyEntity>(), awaitItem()) // initial empty, upstream also empty so no re-emission
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `readings emits list from meterDao`() = runTest(mainDispatcherRule.testDispatcher) {
        val reading1 = ReadingEntity(id = 1L, utilityId = 1L, value = 123.45, timestamp = 1L, source = "GEMMA")

        every { readingRepository.latestReading() } returns flowOf(null)
        every { readingRepository.isAnalyzing() } returns flowOf(false)
        every { meterDao.getAllProperties() } returns flowOf(emptyList())
        every { meterDao.getAllReadings() } returns flowOf(listOf(reading1))

        val viewModel = createViewModel()

        viewModel.readings.test {
            assertEquals(emptyList<ReadingEntity>(), awaitItem()) // initial empty
            assertEquals(listOf(reading1), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
