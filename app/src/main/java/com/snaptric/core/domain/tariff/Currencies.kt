package com.snaptric.core.domain.tariff

import java.util.Currency
import java.util.Locale

/** A currency the user can pick, e.g. GBP "British Pound". */
data class CurrencyOption(val code: String, val name: String)

/** Shown first in the picker; the rest follow alphabetically. */
val COMMON_CURRENCIES = listOf("GBP", "EUR", "USD", "CAD", "AUD", "NZD", "CHF", "SEK", "NOK", "DKK", "PLN", "INR", "LKR", "JPY", "ZAR", "AED", "SGD")

/**
 * Every currency the phone knows, common ones first, filtered by [query] against the code or name.
 */
fun currencyOptions(query: String = "", locale: Locale = Locale.getDefault()): List<CurrencyOption> {
    val all = Currency.getAvailableCurrencies()
        .map { CurrencyOption(it.currencyCode, it.getDisplayName(locale)) }
        .sortedWith(compareBy({ it.code !in COMMON_CURRENCIES }, { COMMON_CURRENCIES.indexOf(it.code) }, { it.name }))
    val q = query.trim()
    if (q.isEmpty()) return all
    return all.filter { it.code.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true) }
}

/** True for a currency code the phone recognises, e.g. "GBP". */
fun isKnownCurrency(code: String): Boolean =
    code.length == 3 && runCatching { Currency.getInstance(code.uppercase(Locale.ROOT)) }.isSuccess

/** The phone's own currency, or GBP when the region has none. */
fun localCurrency(locale: Locale = Locale.getDefault()): String =
    runCatching { Currency.getInstance(locale).currencyCode }.getOrNull() ?: "GBP"
