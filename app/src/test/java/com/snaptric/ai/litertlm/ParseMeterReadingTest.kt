package com.snaptric.ai.litertlm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseMeterReadingTest {

    @Test
    fun `plain number is returned as is`() {
        assertEquals("12345.6", parseMeterReading("12345.6"))
    }

    @Test
    fun `number is extracted from surrounding words`() {
        assertEquals("01262.4", parseMeterReading("The reading is 01262.4 kWh."))
    }

    @Test
    fun `comma decimal separator becomes a dot`() {
        assertEquals("1262.4", parseMeterReading("1262,4"))
    }

    @Test
    fun `spaces between digit groups are removed`() {
        assertEquals("0012345", parseMeterReading("0012 345"))
    }

    @Test
    fun `longest number wins over short labels`() {
        assertEquals("98765", parseMeterReading("Meter 2: 98765"))
    }

    @Test
    fun `no digits returns null`() {
        assertNull(parseMeterReading("NONE"))
    }
}
