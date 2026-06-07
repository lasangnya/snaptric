package com.snaptric.feature.properties.viewmodel

import app.cash.turbine.test
import com.snaptric.MainDispatcherRule
import com.snaptric.core.database.dao.MeterDao
import com.snaptric.core.database.entity.PropertyEntity
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
class PropertyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val meterDao: MeterDao = mockk(relaxed = true)

    @Test
    fun `properties emits list from DAO`() = runTest(mainDispatcherRule.testDispatcher) {
        val properties = listOf(
            PropertyEntity(id = 1L, name = "Home", address = "123 Main St", iconIdentifier = "ic_home"),
            PropertyEntity(id = 2L, name = "Office", address = "456 Work Ave", iconIdentifier = "ic_office")
        )
        every { meterDao.getAllProperties() } returns flowOf(properties)

        val viewModel = PropertyViewModel(meterDao)

        viewModel.properties.test {
            assertEquals(emptyList<PropertyEntity>(), awaitItem())
            advanceUntilIdle()
            assertEquals(properties, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `properties emits empty list from DAO`() = runTest(mainDispatcherRule.testDispatcher) {
        every { meterDao.getAllProperties() } returns flowOf(emptyList())

        val viewModel = PropertyViewModel(meterDao)

        viewModel.properties.test {
            assertEquals(emptyList<PropertyEntity>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addProperty calls insertProperty on DAO`() = runTest(mainDispatcherRule.testDispatcher) {
        every { meterDao.getAllProperties() } returns flowOf(emptyList())

        val viewModel = PropertyViewModel(meterDao)

        viewModel.addProperty(name = "Cabin", address = "Lake Rd", iconIdentifier = "ic_cabin")

        advanceUntilIdle()
        coVerify { meterDao.insertProperty(any()) }
    }
}
