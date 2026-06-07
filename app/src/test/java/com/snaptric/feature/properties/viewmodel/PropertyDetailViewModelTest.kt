package com.snaptric.feature.properties.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.snaptric.MainDispatcherRule
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import io.mockk.coVerify
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
class PropertyDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val meterDao: MeterDao = mockk(relaxed = true)

    @Test
    fun `utilities emits list from DAO`() = runTest(mainDispatcherRule.testDispatcher) {
        val utilities = listOf(
            UtilityEntity(id = 1L, propertyId = 42L, type = UtilityType.ELECTRICITY, unit = "kWh", initialReading = 100.0, name = "Main Meter"),
            UtilityEntity(id = 2L, propertyId = 42L, type = UtilityType.WATER, unit = "m3", initialReading = 50.0, name = null)
        )
        every { meterDao.getUtilitiesForProperty(42L) } returns flowOf(utilities)

        val savedStateHandle = SavedStateHandle(mapOf("propertyId" to 42L))
        val viewModel = PropertyDetailViewModel(meterDao, savedStateHandle)

        viewModel.utilities.test {
            assertEquals(emptyList<UtilityEntity>(), awaitItem())
            advanceUntilIdle()
            assertEquals(utilities, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `utilities emits empty list from DAO`() = runTest(mainDispatcherRule.testDispatcher) {
        every { meterDao.getUtilitiesForProperty(42L) } returns flowOf(emptyList())

        val savedStateHandle = SavedStateHandle(mapOf("propertyId" to 42L))
        val viewModel = PropertyDetailViewModel(meterDao, savedStateHandle)

        viewModel.utilities.test {
            assertEquals(emptyList<UtilityEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addUtility calls insertUtility on DAO`() = runTest(mainDispatcherRule.testDispatcher) {
        every { meterDao.getUtilitiesForProperty(42L) } returns flowOf(emptyList())

        val savedStateHandle = SavedStateHandle(mapOf("propertyId" to 42L))
        val viewModel = PropertyDetailViewModel(meterDao, savedStateHandle)

        viewModel.addUtility(
            type = UtilityType.GAS,
            unit = "m3",
            initialReading = 25.0,
            name = "Heating"
        )

        advanceUntilIdle()
        coVerify { meterDao.insertUtility(any()) }
    }
}
