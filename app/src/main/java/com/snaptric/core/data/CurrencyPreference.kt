package com.snaptric.core.data

import android.content.Context
import com.snaptric.core.domain.tariff.localCurrency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The currency new tariffs start in, chosen in Settings. Defaults to the phone's currency.
 * Existing tariffs keep their own currency; nothing is converted when this changes.
 */
interface CurrencyPreference {
    val currency: StateFlow<String>
    fun set(code: String)
}

class SharedPrefsCurrencyPreference(context: Context) : CurrencyPreference {
    private val prefs = context.getSharedPreferences("snaptric_settings", Context.MODE_PRIVATE)
    private val _currency = MutableStateFlow(prefs.getString(KEY, null) ?: localCurrency())
    override val currency: StateFlow<String> = _currency.asStateFlow()

    override fun set(code: String) {
        prefs.edit().putString(KEY, code).apply()
        _currency.value = code
    }

    private companion object {
        const val KEY = "currency"
    }
}
