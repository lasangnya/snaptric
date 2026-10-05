package com.snaptric.feature.home.viewmodel

import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeReadingItemTest {

    private val properties = listOf(PropertyEntity(id = 1, name = "Home", address = null))
    private val electricity = UtilityEntity(id = 1, propertyId = 1, type = UtilityType.ELECTRICITY, unit = "kWh", initialReading = 0.0)
    private val water = UtilityEntity(id = 2, propertyId = 1, type = UtilityType.WATER, unit = "m³", initialReading = 0.0)

    private fun reading(id: Long, utilityId: Long, value: Double, timestamp: Long) =
        ReadingEntity(id = id, utilityId = utilityId, value = value, timestamp = timestamp, source = "Manual")

    @Test
    fun `items are newest first and joined with meter and property`() {
        val items = buildHomeReadingItems(
            readings = listOf(reading(1, 1, 100.0, 1_000), reading(2, 1, 110.0, 2_000)),
            utilities = listOf(electricity),
            properties = properties
        )

        assertEquals(listOf(2L, 1L), items.map { it.reading.id })
        assertEquals(electricity, items.first().utility)
        assertEquals("Home", items.first().propertyName)
    }

    @Test
    fun `delta compares against the previous reading of the same meter only`() {
        val items = buildHomeReadingItems(
            readings = listOf(
                reading(1, 1, 100.0, 1_000),
                reading(2, 2, 5.0, 2_000),
                reading(3, 1, 112.5, 3_000)
            ),
            utilities = listOf(electricity, water),
            properties = properties
        ).associateBy { it.reading.id }

        assertEquals(12.5, items.getValue(3).delta!!, 0.0001)
        assertNull(items.getValue(2).delta) // first water reading
        assertNull(items.getValue(1).delta) // first electricity reading
    }

    @Test
    fun `reading for an unknown meter has no meter or property`() {
        val item = buildHomeReadingItems(
            readings = listOf(reading(1, 99, 1.0, 1_000)),
            utilities = emptyList(),
            properties = properties
        ).single()

        assertNull(item.utility)
        assertNull(item.propertyName)
    }

    @Test
    fun `meter summaries include meters without readings, grouped by property`() {
        val office = PropertyEntity(id = 2, name = "Apartment", address = null)
        val officeGas = UtilityEntity(id = 5, propertyId = 2, type = UtilityType.GAS, unit = "m³", initialReading = 0.0)
        val summaries = buildMeterSummaries(
            readings = listOf(reading(1, 1, 100.0, 1_000), reading(2, 1, 110.0, 2_000)),
            utilities = listOf(water, electricity, officeGas),
            properties = properties + office
        )

        assertEquals(listOf(5L, 1L, 2L), summaries.map { it.utility.id }) // Apartment first; then electricity before water
        assertEquals(2L, summaries[1].latest?.id)
        assertNull(summaries[2].latest)
    }

    @Test
    fun `days since counts calendar days`() {
        val zone = java.time.ZoneOffset.UTC
        val now = java.time.LocalDateTime.of(2026, 10, 5, 0, 30).toInstant(zone).toEpochMilli()
        val lateYesterday = java.time.LocalDateTime.of(2026, 10, 4, 23, 50).toInstant(zone).toEpochMilli()
        assertEquals(0L, daysSince(now, now, zone))
        assertEquals(1L, daysSince(lateYesterday, now, zone))
    }
}
