package com.snaptric.feature.home.viewmodel

import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.UtilityEntity

/**
 * A reading enriched with the meter and property it belongs to, ready for display on Home.
 *
 * @property delta Change since the previous reading of the same meter, or null for its first reading.
 */
data class HomeReadingItem(
    val reading: ReadingEntity,
    val utility: UtilityEntity?,
    val propertyName: String?,
    val delta: Double?
)

/**
 * Joins readings with their meter and property, newest first, and computes each reading's
 * change against the previous reading of the same meter (never across different meters).
 */
fun buildHomeReadingItems(
    readings: List<ReadingEntity>,
    utilities: List<UtilityEntity>,
    properties: List<PropertyEntity>
): List<HomeReadingItem> {
    val utilitiesById = utilities.associateBy { it.id }
    val propertyNamesById = properties.associate { it.id to it.name }
    val newestFirst = readings.sortedByDescending { it.timestamp }
    val previousByReadingId = newestFirst
        .groupBy { it.utilityId }
        .values
        .flatMap { perMeter -> perMeter.zipWithNext() }
        .associate { (newer, older) -> newer.id to older }

    return newestFirst.map { reading ->
        val utility = utilitiesById[reading.utilityId]
        HomeReadingItem(
            reading = reading,
            utility = utility,
            propertyName = utility?.let { propertyNamesById[it.propertyId] },
            delta = previousByReadingId[reading.id]?.let { reading.value - it.value }
        )
    }
}

/**
 * One meter for the "Your meters" row on Home, including meters that have no readings yet.
 *
 * @property latest The meter's newest reading, or null if it has never been read.
 */
data class MeterSummary(
    val utility: UtilityEntity,
    val propertyName: String?,
    val latest: ReadingEntity?
)

/**
 * Every meter with its newest reading, grouped by property (in name order), then by utility type.
 */
fun buildMeterSummaries(
    readings: List<ReadingEntity>,
    utilities: List<UtilityEntity>,
    properties: List<PropertyEntity>
): List<MeterSummary> {
    val propertyNamesById = properties.associate { it.id to it.name }
    val latestByMeter = readings.groupBy { it.utilityId }.mapValues { (_, list) -> list.maxBy { it.timestamp } }
    return utilities
        .map { MeterSummary(it, propertyNamesById[it.propertyId], latestByMeter[it.id]) }
        .sortedWith(compareBy({ it.propertyName.orEmpty().lowercase() }, { it.utility.type.ordinal }, { it.utility.name.orEmpty() }))
}

/** Days after which a meter is shown as due for a new reading. */
const val READING_DUE_AFTER_DAYS = 31

/**
 * Whole days since [timestamp], counted in calendar days in [zone].
 */
fun daysSince(timestamp: Long, now: Long = System.currentTimeMillis(), zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long {
    val then = java.time.Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return java.time.temporal.ChronoUnit.DAYS.between(then, today).coerceAtLeast(0)
}
