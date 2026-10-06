package com.snaptric.core.domain.tariff

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CurrenciesTest {

    @Test
    fun commonCurrenciesComeFirst() {
        val codes = currencyOptions(locale = Locale.UK).map { it.code }
        assertEquals(listOf("GBP", "EUR", "USD"), codes.take(3))
        assertTrue(codes.size > COMMON_CURRENCIES.size)
    }

    @Test
    fun searchMatchesCodeOrName() {
        assertEquals("EUR", currencyOptions("eur", Locale.UK).first().code)
        assertTrue(currencyOptions("rupee", Locale.UK).any { it.code == "LKR" })
        assertTrue(currencyOptions("zzzz", Locale.UK).isEmpty())
    }

    @Test
    fun knownCurrency() {
        assertTrue(isKnownCurrency("gbp"))
        assertFalse(isKnownCurrency("XYZ"))
        assertFalse(isKnownCurrency("GB"))
    }

    @Test
    fun localCurrency_followsRegion() {
        assertEquals("GBP", localCurrency(Locale.UK))
        assertEquals("EUR", localCurrency(Locale.GERMANY))
        assertEquals("GBP", localCurrency(Locale.ENGLISH)) // no region
    }
}
