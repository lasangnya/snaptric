package com.snaptric.core.domain.insights

import com.snaptric.core.database.entity.ReadingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingCheckTest {

    private val day = 24L * 60 * 60 * 1000

    private fun reading(dayNumber: Long, value: Double) =
        ReadingEntity(id = dayNumber, utilityId = 1, value = value, timestamp = dayNumber * day, source = "Manual")

    // 14 kWh a day for 30 days, last reading 1262.4 on day 30.
    private val history = listOf(reading(0, 842.4), reading(30, 1262.4))

    @Test
    fun `normal usage is plausible`() {
        assertEquals(ReadingCheck.Plausible, checkReading(1290.0, 32 * day, history))
    }

    @Test
    fun `a lower value than last time is flagged`() {
        assertEquals(ReadingCheck.LowerThanPrevious(1262.4), checkReading(1200.0, 32 * day, history))
    }

    @Test
    fun `a missed decimal is flagged with the corrected value`() {
        val check = checkReading(12904.0, 32 * day, history)

        assertTrue(check is ReadingCheck.UnusuallyHigh)
        check as ReadingCheck.UnusuallyHigh
        assertEquals(1290.4, check.suggestion!!, 0.0001)
        assertEquals(14.0, check.typicalPerDay, 0.0001)
    }

    @Test
    fun `a huge jump with no sensible decimal has no suggestion`() {
        val check = checkReading(1900.0, 31 * day, history) as ReadingCheck.UnusuallyHigh
        assertNull(check.suggestion)
    }

    @Test
    fun `first or second reading of a meter is never flagged`() {
        assertEquals(ReadingCheck.Plausible, checkReading(99999.0, 1 * day, emptyList()))
        assertEquals(ReadingCheck.Plausible, checkReading(99999.0, 1 * day, listOf(reading(0, 10.0))))
    }

    @Test
    fun `readings taken minutes apart don't create a tiny daily rate`() {
        val closeTogether = listOf(
            ReadingEntity(id = 1, utilityId = 1, value = 100.0, timestamp = 0, source = "Manual"),
            ReadingEntity(id = 2, utilityId = 1, value = 100.5, timestamp = 60_000, source = "Manual")
        )
        assertEquals(ReadingCheck.Plausible, checkReading(130.0, 10 * day, closeTogether))
    }
}
