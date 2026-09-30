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
        utilities.firstOrNull { it.serialNumber == serialNumber }?.let {
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
