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
