package com.snaptric.core.domain.insights

import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.UtilityEntity
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * This month's usage for one meter, calculated from its saved readings.
 *
 * @property usedSoFar Usage this month, up to [lastReadingDate].
 * @property forecast Projected usage for the whole month, or null when there is too little data
 *   or the month is already complete.
 * @property lastMonth Usage in the previous month, or null when it isn't known.
 */
data class MeterRecap(
    val utility: UtilityEntity,
    val propertyName: String?,
    val month: YearMonth,
    val usedSoFar: Double,
    val lastReadingDate: LocalDate,
    val forecast: Double?,
    val lastMonth: Double?
) {
    /** Forecast compared with last month, as a whole percentage (positive means more), or null. */
    val changePercent: Int?
        get() {
            val projected = forecast ?: return null
            val previous = lastMonth?.takeIf { it > 0 } ?: return null
            return ((projected - previous) / previous * 100).roundToInt()
        }
}

/** Days of history a forecast needs, so two readings minutes apart don't produce a wild projection. */
private const val MIN_DAYS_FOR_FORECAST = 3.0

private const val MILLIS_PER_DAY = 24.0 * 60 * 60 * 1000

private fun ReadingEntity.localDate(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

/**
 * Usage per calendar month for one meter's readings. Usage between two consecutive readings is
 * counted in the month of the later reading; drops in value (a reset meter or a typo) are skipped.
 */
fun monthlyUsage(readings: List<ReadingEntity>, zone: ZoneId = ZoneId.systemDefault()): Map<YearMonth, Double> {
    val usage = sortedMapOf<YearMonth, Double>()
    readings.sortedBy { it.timestamp }.zipWithNext { older, newer ->
        val delta = newer.value - older.value
        if (delta >= 0) usage.merge(YearMonth.from(newer.localDate(zone)), delta, Double::plus)
    }
    return usage
}

/**
 * Builds a recap for every meter that has usage this month.
 *
 * Usage between two consecutive readings is counted in the month of the later reading. Drops in
 * value (a reset meter or a typo) are ignored rather than counted as negative usage. The forecast
 * extends the daily rate over the days left in the month; a finished month has no forecast.
 */
fun buildMeterRecaps(
    readings: List<ReadingEntity>,
    utilities: List<UtilityEntity>,
    properties: List<PropertyEntity>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): List<MeterRecap> {
    val month = YearMonth.from(today)
    val propertyNames = properties.associate { it.id to it.name }
    val readingsByMeter = readings.groupBy { it.utilityId }

    return utilities.mapNotNull { utility ->
        val sorted = readingsByMeter[utility.id].orEmpty().sortedBy { it.timestamp }
        val usageByMonth = monthlyUsage(sorted, zone)
        // The time span this month's usage covers: from the reading before the first one this month
        // to the latest one. Used to work out a daily rate.
        var coveredFrom: Long? = null
        var coveredTo: ReadingEntity? = null

        sorted.zipWithNext { older, newer ->
            if (YearMonth.from(newer.localDate(zone)) == month) {
                if (coveredFrom == null) coveredFrom = older.timestamp
                coveredTo = newer
            }
        }

        val latest = coveredTo ?: return@mapNotNull null
        val lastDate = latest.localDate(zone)
        val usedSoFar = usageByMonth[month] ?: 0.0
        val coveredDays = (latest.timestamp - (coveredFrom ?: latest.timestamp)) / MILLIS_PER_DAY
        val remainingDays = month.lengthOfMonth() - lastDate.dayOfMonth
        val forecast = if (coveredDays >= MIN_DAYS_FOR_FORECAST && remainingDays > 0) {
            usedSoFar + usedSoFar / coveredDays * remainingDays
        } else {
            null
        }

        MeterRecap(
            utility = utility,
            propertyName = propertyNames[utility.propertyId],
            month = month,
            usedSoFar = usedSoFar,
            lastReadingDate = lastDate,
            forecast = forecast,
            lastMonth = usageByMonth[month.minusMonths(1)]
        )
    }
}

/**
 * Rounds a usage amount for display: whole units from 100 up, one decimal below.
 */
fun formatAmount(value: Double): String =
    if (abs(value) >= 100) value.roundToInt().toString()
    else String.format(Locale.US, "%.1f", value).removeSuffix(".0")

/** "Electricity at Home", or the meter's own name when it has one. */
fun MeterRecap.meterLabel(): String {
    val meter = utility.name?.takeIf { it.isNotBlank() }
        ?: utility.type.name.lowercase().replaceFirstChar { it.uppercase() }
    return listOfNotNull(meter, propertyName).joinToString(" at ")
}
