package com.snaptric.core.data

import app.cash.turbine.test
import com.snaptric.core.domain.Reading
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryReadingRepositoryTest {

    @Test
    fun `latestReading emits null initially`() = runTest(StandardTestDispatcher()) {
        val repository = InMemoryReadingRepository()

        repository.latestReading().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveReading then latestReading emits the saved reading`() = runTest(StandardTestDispatcher()) {
        val repository = InMemoryReadingRepository()
        val reading = Reading(value = "1234.5", timestamp = 123456789L, source = "MLKit")

        repository.saveReading(reading)

        repository.latestReading().test {
            assertEquals(reading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isAnalyzing emits false initially`() = runTest(StandardTestDispatcher()) {
        val repository = InMemoryReadingRepository()

        repository.isAnalyzing().test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setAnalyzing true then isAnalyzing emits true`() = runTest(StandardTestDispatcher()) {
        val repository = InMemoryReadingRepository()

        repository.setAnalyzing(true)

        repository.isAnalyzing().test {
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setAnalyzing false then isAnalyzing emits false`() = runTest(StandardTestDispatcher()) {
        val repository = InMemoryReadingRepository()

        repository.setAnalyzing(true)
        repository.setAnalyzing(false)

        repository.isAnalyzing().test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple saves latestReading emits the most recent one`() = runTest(StandardTestDispatcher()) {
        val repository = InMemoryReadingRepository()
        val firstReading = Reading(value = "1111.1", timestamp = 1000L, source = "MLKit")
        val secondReading = Reading(value = "2222.2", timestamp = 2000L, source = "GEMMA")

        repository.saveReading(firstReading)
        repository.saveReading(secondReading)

        repository.latestReading().test {
            assertEquals(secondReading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
