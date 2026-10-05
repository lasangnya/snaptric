package com.snaptric.core.database

import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalUnitTest {

    @Test
    fun knownSpellings_becomeCanonicalSymbols() {
        assertEquals("m³", canonicalUnit("GAS", "m3"))
        assertEquals("kWh", canonicalUnit("ELECTRICITY", "kwh"))
        assertEquals("Liters", canonicalUnit("WATER", "litres"))
    }

    @Test
    fun missingUnit_getsTheTypeDefault() {
        assertEquals("m³", canonicalUnit("GAS", ""))
        assertEquals("kWh", canonicalUnit("ELECTRICITY", " "))
        assertEquals("m³", canonicalUnit("WATER", ""))
    }

    @Test
    fun unknownUnit_isKeptAsTyped() {
        assertEquals("units", canonicalUnit("GAS", "units"))
    }
}
