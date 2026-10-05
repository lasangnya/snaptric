package com.snaptric.core.domain.insights

import com.snaptric.core.database.entity.ReadingEntity
import com.snaptric.core.database.entity.UtilityEntity
import com.snaptric.core.database.entity.UtilityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeterMatchTest {

    private val electricity = UtilityEntity(id = 1, propertyId = 1, type = UtilityType.ELECTRICITY, unit = "kWh", initialReading = 0.0, serialNumber = "20481733")
    private val gas = UtilityEntity(id = 2, propertyId = 1, type = UtilityType.GAS, unit = "m³", initialReading = 0.0)
    private val water = UtilityEntity(id = 3, propertyId = 2, type = UtilityType.WATER, unit = "m³", initialReading = 0.0)
    private val meters = listOf(electricity, gas, water)

    private val readings = listOf(
        ReadingEntity(id = 1, utilityId = 1, value = 1262.4, timestamp = 2, source = "MLKit"),
        ReadingEntity(id = 2, utilityId = 2, value = 845.0, timestamp = 2, source = "MLKit"),
        ReadingEntity(id = 3, utilityId = 3, value = 120.0, timestamp = 2, source = "MLKit"),
        ReadingEntity(id = 4, utilityId = 3, value = 90.0, timestamp = 1, source = "MLKit")
    )

    @Test
    fun `a known serial number picks its meter`() {
        val match = matchMeter("20481733", value = 130.0, utilities = meters, readings = readings)
        assertEquals(MeterMatch(electricity, MeterMatch.Reason.SERIAL_NUMBER), match)
    }

    @Test
    fun `without a serial the meter just below the value wins`() {
        assertEquals(gas, matchMeter(null, 851.2, meters, readings)?.utility)
        assertEquals(water, matchMeter(null, 125.5, meters, readings)?.utility)
    }

    @Test
    fun `an unknown serial falls back to the closest reading`() {
        val match = matchMeter("99999999", 851.2, meters, readings)
        assertEquals(MeterMatch(gas, MeterMatch.Reason.CLOSEST_READING), match)
    }

    @Test
    fun `nothing to go on returns null`() {
        assertNull(matchMeter(null, 50.0, meters, readings)) // below every meter
        assertNull(matchMeter(null, null, meters, readings))
        assertNull(matchMeter(null, 900.0, listOf(gas), readings)) // only one meter
    }

    @Test
    fun `serials match on their last digits despite OCR noise`() {
        assertEquals(true, sameSerial("0024200264", "024200264"))
        assertEquals(true, sameSerial("7 HTL00 2420 0264", "0024200264"))
        assertEquals(false, sameSerial("0024200264", "0024200265"))
        assertEquals(false, sameSerial("123", "123"))
    }
}
