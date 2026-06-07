package com.snaptric.feature.properties.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.snaptric.MainDispatcherRule
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.ReadingEntity
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UtilityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val meterDao: MeterDao = mockk(relaxed = true)

    @Test
    fun `readings emits list from DAO`() = runTest(mainDispatcherRule.testDispatcher) {
        val readings = listOf(
            ReadingEntity(id = 1L, utilityId = 99L, value = 123.45, timestamp = 1700000000L, source = "Manual"),
            ReadingEntity(id = 2L, utilityId = 99L, value = 150.0, timestamp = 1700100000L, source = "MLKit")
        )
        every { meterDao.getReadingsForUtility(99L) } returns flowOf(readings)

        val savedStateHandle = SavedStateHandle(mapOf("utilityId" to 99L))
        val viewModel = UtilityViewModel(meterDao, savedStateHandle)

        viewModel.readings.test {
            assertEquals(emptyList<ReadingEntity>(), awaitItem())
            advanceUntilIdle()
            assertEquals(readings, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `readings emits empty list from DAO`() = runTest(mainDispatcherRule.testDispatcher) {
        every { meterDao.getReadingsForUtility(99L) } returns flowOf(emptyList())

        val savedStateHandle = SavedStateHandle(mapOf("utilityId" to 99L))
        val viewModel = UtilityViewModel(meterDao, savedStateHandle)

        viewModel.readings.test {
            assertEquals(emptyList<ReadingEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
