package com.snaptric.core.domain.insights

import com.snaptric.core.database.entity.ReadingEntity
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How the usage since a meter's previous reading compares with its recent average.
 *
 * @property usedSinceLast Usage between the previous reading and the new one.
 * @property days Days between the previous reading and the new one.
 * @property averagePerDay Average daily usage over the [AVERAGE_WINDOW_MONTHS] months before the previous reading.
 */
data class UsageComparison(
    val unit: String,
    val usedSinceLast: Double,
    val days: Double,
    val averagePerDay: Double
) {
    val perDay: Double get() = usedSinceLast / days

    /** Daily usage compared with the average, as a whole percentage (positive means more). */
    val changePercent: Int get() = ((perDay - averagePerDay) / averagePerDay * 100).roundToInt()
}

/** How far back the average looks. */
const val AVERAGE_WINDOW_MONTHS = 3

private const val AVERAGE_WINDOW_DAYS = 91.0

/** History the average needs, so a couple of readings days apart don't set "usual". */
private const val MIN_AVERAGE_DAYS = 7.0

/** A gap shorter than this says little about a daily rate, so no comparison is made. */
private const val MIN_GAP_DAYS = 1.0

/** Changes within this many percent are described as usual rather than as more or less. */
private const val USUAL_BAND_PERCENT = 10

private const val MILLIS_PER_DAY = 24.0 * 60 * 60 * 1000

/**
 * Compares the usage implied by [value], taken at [timestamp], with the meter's average daily usage
 * over the [AVERAGE_WINDOW_MONTHS] months before its previous reading.
 *
 * Returns null when there isn't enough history to say what's usual, the gap since the previous
 * reading is too short, or the value went backwards. Drops inside the history (a reset meter or a
 * typo) are skipped rather than counted as negative usage.
 */
fun compareWithAverage(
    value: Double,
    timestamp: Long,
    history: List<ReadingEntity>,
    unit: String
): UsageComparison? {
    val earlier = history.filter { it.timestamp < timestamp }.sortedBy { it.timestamp }
    val previous = earlier.lastOrNull() ?: return null

    val used = value - previous.value
    val days = (timestamp - previous.timestamp) / MILLIS_PER_DAY
    if (used < 0 || days < MIN_GAP_DAYS) return null

    val windowStart = previous.timestamp - (AVERAGE_WINDOW_DAYS * MILLIS_PER_DAY).toLong()
    val window = earlier.filter { it.timestamp >= windowStart }
    val windowDays = (previous.timestamp - window.first().timestamp) / MILLIS_PER_DAY
    if (windowDays < MIN_AVERAGE_DAYS) return null

    var windowUsage = 0.0
    window.zipWithNext { older, newer ->
        val delta = newer.value - older.value
        if (delta >= 0) windowUsage += delta
    }
    if (windowUsage <= 0) return null

    return UsageComparison(
        unit = unit,
        usedSinceLast = used,
        days = days,
        averagePerDay = windowUsage / windowDays
    )
}

/**
 * The facts behind a comparison, as plain sentences. They are the only source of numbers the
 * on-device model may use.
 */
fun comparisonFacts(comparison: UsageComparison): List<String> = with(comparison) {
    val wholeDays = days.roundToInt().coerceAtLeast(1)
    listOf(
        "Used ${formatAmount(usedSinceLast)} $unit in the ${dayCount(wholeDays)} since the last reading.",
        "That's about ${formatAmount(perDay)} $unit a day.",
        "The $AVERAGE_WINDOW_MONTHS-month average is ${formatAmount(averagePerDay)} $unit a day.",
        "So this is ${changeDescription()}."
    )
}

/**
 * One short line for the save screen, used whenever the on-device model isn't.
 */
fun comparisonLine(comparison: UsageComparison): String = with(comparison) {
    "${formatAmount(perDay)} $unit a day since the last reading, ${changeDescription()} " +
        "(your $AVERAGE_WINDOW_MONTHS-month average is ${formatAmount(averagePerDay)} $unit a day)."
}

/** True when daily usage is clearly above or below the average. */
val UsageComparison.isUnusual: Boolean get() = abs(changePercent) > USUAL_BAND_PERCENT

private fun UsageComparison.changeDescription(): String = when {
    !isUnusual -> "about the same as usual"
    changePercent > 0 -> "${changePercent}% more than usual"
    else -> "${abs(changePercent)}% less than usual"
}

private fun dayCount(days: Int) = if (days == 1) "1 day" else "$days days"

/**
 * Explains how a new reading compares with the meter's usual use. Every number must come from the
 * comparison; implementations only choose the wording.
 */
interface UsageExplainer {
    suspend fun explain(comparison: UsageComparison): Insight
}

/**
 * The always-available explainer: the plain [comparisonLine].
 */
class TemplateUsageExplainer : UsageExplainer {
    override suspend fun explain(comparison: UsageComparison): Insight =
        Insight(text = comparisonLine(comparison), writtenOnDevice = false)
}
