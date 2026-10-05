package com.snaptric.core.domain.insights

import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.UtilityEntity

/**
 * The meter a new scan most likely belongs to, and why.
 */
data class MeterMatch(val utility: UtilityEntity, val reason: Reason) {
    enum class Reason { SERIAL_NUMBER, CLOSEST_READING }
}

/**
 * Picks the meter a scan belongs to, so the user doesn't have to.
 *
 * A serial number seen in the photo that matches a saved meter wins. Otherwise, with several
 * meters, the scan goes to the meter whose latest reading is closest below the new value, since
 * meters only count up. Returns null when there's nothing to go on.
 */
fun matchMeter(
    serialNumber: String?,
    value: Double?,
    utilities: List<UtilityEntity>,
    readings: List<ReadingEntity>
): MeterMatch? {
    if (serialNumber != null) {
        utilities.firstOrNull { sameSerial(it.serialNumber, serialNumber) }?.let {
            return MeterMatch(it, MeterMatch.Reason.SERIAL_NUMBER)
        }
    }
    if (value == null || utilities.size < 2) return null

    val latestByMeter = readings.groupBy { it.utilityId }
        .mapValues { (_, meterReadings) -> meterReadings.maxBy { it.timestamp }.value }

    return utilities
        .mapNotNull { utility -> latestByMeter[utility.id]?.let { latest -> utility to value - latest } }
        .filter { (_, gap) -> gap >= 0 }
        .minByOrNull { (_, gap) -> gap }
        ?.let { (utility, _) -> MeterMatch(utility, MeterMatch.Reason.CLOSEST_READING) }
}

/** Digits a serial is matched on. Prefixes and letters are often misread, so only the tail counts. */
private const val SERIAL_MATCH_DIGITS = 8
private const val MIN_SERIAL_MATCH_DIGITS = 6

/**
 * Whether two serial numbers read from photos belong to the same meter. OCR often garbles letters
 * and the first characters (e.g. "7 HTLO0 2420 0264" vs "024200264"), so only the last digits count.
 */
fun sameSerial(a: String?, b: String?): Boolean {
    val tailA = a?.filter(Char::isDigit)?.takeLast(SERIAL_MATCH_DIGITS) ?: return false
    val tailB = b?.filter(Char::isDigit)?.takeLast(SERIAL_MATCH_DIGITS) ?: return false
    return tailA.length >= MIN_SERIAL_MATCH_DIGITS && tailA == tailB
}
