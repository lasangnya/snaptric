package com.snaptric.core.domain.insights

import com.snaptric.core.database.entity.ReadingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageComparisonTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 200 * day

    private fun reading(value: Double, daysAgo: Long) =
        ReadingEntity(utilityId = 1, value = value, timestamp = now - daysAgo * day, source = "MLKit")

    @Test
    fun `daily usage is compared with the average before the previous reading`() {
        val history = listOf(reading(100.0, 32), reading(250.0, 17), reading(400.0, 2))

        val comparison = compareWithAverage(440.0, now, history, "kWh")!!

        assertEquals(10.0, comparison.averagePerDay, 0.001)
        assertEquals(20.0, comparison.perDay, 0.001)
        assertEquals(100, comparison.changePercent)
        assertTrue(comparison.isUnusual)
    }

    @Test
    fun `only the last three months count towards the average`() {
        // A heavy winter long ago shouldn't set "usual" for today.
        val history = listOf(reading(0.0, 300), reading(5000.0, 120), reading(5100.0, 60), reading(5200.0, 10))

        val comparison = compareWithAverage(5220.0, now, history, "kWh")!!

        assertEquals(2.0, comparison.averagePerDay, 0.001)
        assertEquals(0, comparison.changePercent)
        assertFalse(comparison.isUnusual)
    }

    @Test
    fun `drops in the history are skipped`() {
        val history = listOf(reading(100.0, 32), reading(90.0, 22), reading(190.0, 12), reading(290.0, 2))

        val comparison = compareWithAverage(300.0, now, history, "m³")!!

        assertEquals(200.0 / 30, comparison.averagePerDay, 0.001)
    }

    @Test
    fun `no comparison without enough history`() {
        assertNull(compareWithAverage(10.0, now, emptyList(), "kWh"))
        assertNull(compareWithAverage(10.0, now, listOf(reading(5.0, 3)), "kWh"))
        // Two readings only a few days apart don't say what's usual.
        assertNull(compareWithAverage(20.0, now, listOf(reading(5.0, 6), reading(10.0, 3)), "kWh"))
    }

    @Test
    fun `no comparison for a short gap or a reading that went backwards`() {
        val history = listOf(reading(100.0, 32), reading(400.0, 2))
        assertNull(compareWithAverage(401.0, history.last().timestamp + day / 2, history, "kWh"))
        assertNull(compareWithAverage(390.0, now, history, "kWh"))
    }

    @Test
    fun `template line and facts describe the change`() = kotlinx.coroutines.test.runTest {
        val more = UsageComparison(unit = "kWh", usedSinceLast = 62.0, days = 5.0, averagePerDay = 8.1)
        val less = more.copy(usedSinceLast = 20.0)

        assertEquals(
            "12.4 kWh a day since the last reading, 53% more than usual (your 3-month average is 8.1 kWh a day).",
            TemplateUsageExplainer().explain(more).text
        )
        assertTrue(comparisonLine(less).contains("51% less than usual"))
        assertEquals(
            listOf(
                "Used 62 kWh in the 5 days since the last reading.",
                "That's about 12.4 kWh a day.",
                "The 3-month average is 8.1 kWh a day.",
                "So this is 53% more than usual."
            ),
            comparisonFacts(more)
        )
    }
}
