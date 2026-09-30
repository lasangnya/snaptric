package com.snaptric.core.domain.insights

import com.snaptric.core.database.entity.ReadingEntity
import java.math.BigDecimal

/**
 * The result of checking a new reading against a meter's history before it is saved.
 */
sealed interface ReadingCheck {
    /** Nothing unusual, or not enough history to judge. */
    data object Plausible : ReadingCheck

    /** The new value is below the previous reading. */
    data class LowerThanPrevious(val previous: Double) : ReadingCheck

    /**
     * Usage since the last reading is far above this meter's usual daily rate.
     *
     * @property suggestion The same digits with a decimal point that gives normal usage, if one does.
     */
    data class UnusuallyHigh(
        val usedSinceLast: Double,
        val days: Double,
        val typicalPerDay: Double,
        val suggestion: Double?
    ) : ReadingCheck
}

/** How many times the usual daily rate counts as "unusually high". */
private const val HIGH_FACTOR = 5.0

/** History a usual daily rate needs, so a pair of readings minutes apart doesn't set it. */
private const val MIN_HISTORY_DAYS = 1.0

/** Short gaps inflate the rate, so the gap since the last reading counts as at least this long. */
private const val MIN_GAP_DAYS = 1.0

private const val MILLIS_PER_DAY = 24.0 * 60 * 60 * 1000

/**
 * Checks [value], taken at [timestamp], against the earlier readings of the same meter.
 *
 * Catches the two common capture mistakes: a reading that goes backwards, and a missed decimal
 * point, which makes usage look ten or more times higher than usual. For the second it suggests
 * the decimal placement that brings usage back in line.
 */
fun checkReading(value: Double, timestamp: Long, history: List<ReadingEntity>): ReadingCheck {
    val earlier = history.filter { it.timestamp <= timestamp }.sortedBy { it.timestamp }
    val previous = earlier.lastOrNull() ?: return ReadingCheck.Plausible

    if (value < previous.value) return ReadingCheck.LowerThanPrevious(previous.value)

    val first = earlier.first()
    val historyDays = (previous.timestamp - first.timestamp) / MILLIS_PER_DAY
    val historyUsage = previous.value - first.value
    if (historyDays < MIN_HISTORY_DAYS || historyUsage <= 0) return ReadingCheck.Plausible

    val typicalPerDay = historyUsage / historyDays
    val days = maxOf((timestamp - previous.timestamp) / MILLIS_PER_DAY, MIN_GAP_DAYS)
    val used = value - previous.value
    if (used / days <= typicalPerDay * HIGH_FACTOR) return ReadingCheck.Plausible

    val suggestion = (1..3)
        .map { decimals -> BigDecimal.valueOf(value).movePointLeft(decimals).toDouble() }
        .firstOrNull { candidate ->
            candidate >= previous.value && (candidate - previous.value) / days <= typicalPerDay * HIGH_FACTOR
        }

    return ReadingCheck.UnusuallyHigh(
        usedSinceLast = used,
        days = days,
        typicalPerDay = typicalPerDay,
        suggestion = suggestion
    )
}
