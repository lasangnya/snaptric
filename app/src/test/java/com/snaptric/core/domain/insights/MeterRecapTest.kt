package com.snaptric.core.domain.insights

import com.snaptric.core.database.entity.PropertyEntity
import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

class MeterRecapTest {

    private val zone = ZoneOffset.UTC
    private val home = PropertyEntity(id = 1, name = "Home", address = null)
    private val electricity = UtilityEntity(id = 1, propertyId = 1, type = UtilityType.ELECTRICITY, unit = "kWh", initialReading = 0.0)
    private var nextId = 1L

    private fun reading(date: String, value: Double, utilityId: Long = 1) = ReadingEntity(
        id = nextId++,
        utilityId = utilityId,
        value = value,
        timestamp = LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli(),
        source = "Manual"
    )

    private fun recaps(readings: List<ReadingEntity>, today: String) =
        buildMeterRecaps(readings, listOf(electricity), listOf(home), LocalDate.parse(today), zone)

    @Test
    fun `usage is split by the month of the later reading and forecast to month end`() {
        val recap = recaps(
            listOf(
                reading("2026-08-31", 1000.0),
                reading("2026-09-30", 1310.0), // 310 in September
                reading("2026-10-10", 1410.0) // 100 in October, by day 10
            ),
            today = "2026-10-12"
        ).single()

        assertEquals(100.0, recap.usedSoFar, 0.001)
        assertEquals(310.0, recap.lastMonth!!, 0.001)
        assertEquals(310.0, recap.forecast!!, 0.001) // 10 a day since Sep 30, plus 21 days left
        assertEquals(0, recap.changePercent)
        assertEquals("Home", recap.propertyName)
    }

    @Test
    fun `no forecast from readings taken minutes apart`() {
        val start = reading("2026-10-10", 1000.0)
        val recap = recaps(listOf(start, start.copy(id = 99, value = 1010.0, timestamp = start.timestamp + 60_000)), today = "2026-10-10").single()

        assertNull(recap.forecast)
    }

    @Test
    fun `no forecast once the month is over`() {
        val recap = recaps(
            listOf(reading("2026-10-01", 1000.0), reading("2026-10-31", 1300.0)),
            today = "2026-10-31"
        ).single()

        assertEquals(300.0, recap.usedSoFar, 0.001)
        assertNull(recap.forecast)
    }

    @Test
    fun `no forecast from under three days of history`() {
        val recap = recaps(
            listOf(reading("2026-09-30", 1000.0), reading("2026-10-02", 1020.0)),
            today = "2026-10-02"
        ).single()

        assertNull(recap.forecast)
        assertNull(recap.changePercent)
    }

    @Test
    fun `a drop in value is not counted as usage`() {
        val recap = recaps(
            listOf(reading("2026-10-01", 1000.0), reading("2026-10-05", 900.0), reading("2026-10-08", 950.0)),
            today = "2026-10-08"
        ).single()

        assertEquals(50.0, recap.usedSoFar, 0.001)
    }

    @Test
    fun `meters without a reading this month are left out`() {
        assertTrue(recaps(listOf(reading("2026-09-01", 1.0), reading("2026-09-20", 5.0)), today = "2026-10-12").isEmpty())
    }

    @Test
    fun `facts spell out usage, forecast and change`() {
        val recap = recaps(
            listOf(reading("2026-08-31", 1000.0), reading("2026-09-30", 1250.0), reading("2026-10-10", 1350.0)),
            today = "2026-10-12"
        ).single()

        assertEquals(
            "Electricity at Home: 100 kWh used so far in October. On track for about 310 kWh. " +
                "That's 24% more than September (250 kWh).",
            recapFacts(listOf(recap), Locale.ENGLISH).single()
        )
    }

    @Test
    fun `amounts are rounded for display`() {
        assertEquals("1234", formatAmount(1234.4))
        assertEquals("12.5", formatAmount(12.46))
        assertEquals("12", formatAmount(12.0))
    }

    @Test
    fun `monthly usage sums deltas into the later reading's month and skips drops`() {
        val usage = monthlyUsage(
            listOf(
                reading("2026-08-31", 1000.0),
                reading("2026-09-15", 1150.0),
                reading("2026-09-30", 1300.0),
                reading("2026-10-05", 1290.0), // drop: skipped
                reading("2026-10-20", 1350.0)
            ),
            zone
        )

        assertEquals(300.0, usage.getValue(java.time.YearMonth.of(2026, 9)), 0.001)
        assertEquals(60.0, usage.getValue(java.time.YearMonth.of(2026, 10)), 0.001)
        assertEquals(listOf(java.time.YearMonth.of(2026, 9), java.time.YearMonth.of(2026, 10)), usage.keys.toList())
    }
}
