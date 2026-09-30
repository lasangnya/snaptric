package com.snaptric.ai

import android.graphics.Bitmap
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.Reading
import kotlin.coroutines.cancellation.CancellationException

/**
 * Tries the fast [primary] analyzer first and only asks the slower [fallback] when the primary
 * result looks unusable and the fallback is available. Any fallback failure keeps the primary result,
 * so the user can still correct the value by hand.
 */
class FallbackReadingAnalyzer(
    private val primary: MeterReadingAnalyzer,
    private val fallback: MeterReadingAnalyzer,
    private val isFallbackAvailable: () -> Boolean
) : MeterReadingAnalyzer {

    override suspend fun analyze(bitmap: Bitmap): Reading {
        val first = primary.analyze(bitmap)
        if (first.isUsable() || !isFallbackAvailable()) return first

        return try {
            fallback.analyze(bitmap).takeIf { it.isUsable() } ?: first
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            first
        }
    }

    private fun Reading.isUsable(): Boolean =
        (value?.count(Char::isDigit) ?: 0) >= MIN_DIGITS

    private companion object {
        // Real meter counters have at least four digits; fewer usually means OCR missed the counter.
        const val MIN_DIGITS = 4
    }
}
