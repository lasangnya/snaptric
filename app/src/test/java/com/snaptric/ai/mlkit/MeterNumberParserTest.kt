package com.snaptric.ai.mlkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeterNumberParserTest {

    @Test
    fun `tallest counter-length number wins over a smaller serial number`() {
        val lines = listOf(
            OcrLine("S/N 20481733", height = 18),
            OcrLine("01262", height = 64),
            OcrLine("kWh", height = 22)
        )
        assertEquals("01262", pickMeterNumber(lines))
    }

    @Test
    fun `decimal point is kept`() {
        assertEquals("01262.4", pickMeterNumber(listOf(OcrLine("01262.4", height = 60))))
    }

    @Test
    fun `decimal comma becomes a point`() {
        assertEquals("1262.4", pickMeterNumber(listOf(OcrLine("1262,4 m3", height = 60))))
    }

    @Test
    fun `gaps between counter drums are joined`() {
        assertEquals("012624", pickMeterNumber(listOf(OcrLine("0 1 2 6 2 4", height = 60))))
    }

    @Test
    fun `letters misread for digits are fixed inside a number`() {
        assertEquals("01262", pickMeterNumber(listOf(OcrLine("O1262", height = 60))))
        assertEquals("10384", pickMeterNumber(listOf(OcrLine("I0384", height = 60))))
    }

    @Test
    fun `ordinary words are left alone`() {
        assertEquals("0512", pickMeterNumber(listOf(OcrLine("BOSS 0512", height = 60))))
    }

    @Test
    fun `short labels lose to a counter even when taller`() {
        val lines = listOf(OcrLine("50 Hz", height = 80), OcrLine("004571", height = 50))
        assertEquals("004571", pickMeterNumber(lines))
    }

    @Test
    fun `no digits returns null`() {
        assertNull(pickMeterNumber(listOf(OcrLine("kWh", height = 40))))
    }
}
