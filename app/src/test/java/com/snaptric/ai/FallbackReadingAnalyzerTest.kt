package com.snaptric.ai

import android.graphics.Bitmap
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.Reading
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FallbackReadingAnalyzerTest {

    private val bitmap = mockk<Bitmap>(relaxed = true)
    private val primary = mockk<MeterReadingAnalyzer>()
    private val fallback = mockk<MeterReadingAnalyzer>()

    private fun reading(value: String?, source: String) = Reading(value = value, timestamp = 0L, source = source)

    @Test
    fun `clear primary result skips the fallback`() = runTest {
        coEvery { primary.analyze(bitmap) } returns reading("012345", "MLKit")

        val result = FallbackReadingAnalyzer(primary, fallback) { true }.analyze(bitmap)

        assertEquals("MLKit", result.source)
        coVerify(exactly = 0) { fallback.analyze(any()) }
    }

    @Test
    fun `unclear primary result uses the fallback when available`() = runTest {
        coEvery { primary.analyze(bitmap) } returns reading("12", "MLKit")
        coEvery { fallback.analyze(bitmap) } returns reading("01262.4", "Gemma")

        val result = FallbackReadingAnalyzer(primary, fallback) { true }.analyze(bitmap)

        assertEquals("01262.4", result.value)
        assertEquals("Gemma", result.source)
    }

    @Test
    fun `no model installed keeps the primary result`() = runTest {
        coEvery { primary.analyze(bitmap) } returns reading("", "MLKit")

        val result = FallbackReadingAnalyzer(primary, fallback) { false }.analyze(bitmap)

        assertEquals("MLKit", result.source)
        coVerify(exactly = 0) { fallback.analyze(any()) }
    }

    @Test
    fun `fallback failure keeps the primary result`() = runTest {
        coEvery { primary.analyze(bitmap) } returns reading("12", "MLKit")
        coEvery { fallback.analyze(bitmap) } throws IllegalStateException("engine failed")

        val result = FallbackReadingAnalyzer(primary, fallback) { true }.analyze(bitmap)

        assertEquals("12", result.value)
        assertEquals("MLKit", result.source)
    }
}
