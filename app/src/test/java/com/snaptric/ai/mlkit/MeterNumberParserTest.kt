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

    @Test
    fun `labelled serial number is read separately from the counter`() {
        val scan = scanMeter(
            listOf(
                OcrLine("S/N 20481733", height = 18),
                OcrLine("01262", height = 64)
            )
        )
        assertEquals("01262", scan.value)
        assertEquals("20481733", scan.serialNumber)
    }

    @Test
    fun `short unlabelled numbers are not taken as a serial`() {
        assertNull(scanMeter(listOf(OcrLine("230V 50Hz", height = 18), OcrLine("01262", height = 64))).serialNumber)
    }

    @Test
    fun `low-confidence digits are flagged at their position in the value`() {
        // "0126 2,4": the '6' (index 3) and the decimal '4' are uncertain; the space is dropped.
        val line = OcrLine(
            text = "0126 2,4",
            height = 60,
            charConfidences = listOf(0.95f, 0.9f, 0.9f, 0.4f, 1f, 0.9f, 0.9f, 0.3f)
        )
        val scan = scanMeter(listOf(line))
        assertEquals("01262.4", scan.value)
        assertEquals(setOf(3, 6), scan.uncertainDigits)
    }

    @Test
    fun `all-zero confidences are treated as unknown`() {
        val line = OcrLine("01262", height = 60, charConfidences = List(5) { 0f })
        assertEquals(emptySet<Int>(), scanMeter(listOf(line)).uncertainDigits)
    }
}
