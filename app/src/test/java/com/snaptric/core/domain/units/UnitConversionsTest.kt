package com.snaptric.core.domain.units

import com.snaptric.core.database.entity.UtilityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitConversionsTest {

    @Test
    fun gasVolumeToKwh_usesCorrectionAndCalorificValue() {
        // 100 m³ × 1.02264 × 39.5 ÷ 3.6 = 1,122.06 kWh
        assertEquals(1122.0633, UnitConversions.gasCubicMetresToKwh(100.0), 0.001)
        val billed = GasFactors(calorificValue = 39.2, volumeCorrection = 1.02264, estimated = false)
        assertEquals(1113.5416, UnitConversions.gasCubicMetresToKwh(100.0, billed), 0.001)
    }

    @Test
    fun kwhToGasVolume_isTheInverse() {
        val kwh = UnitConversions.gasCubicMetresToKwh(152.0)
        assertEquals(152.0, UnitConversions.kwhToGasCubicMetres(kwh), 1e-9)
    }

    @Test
    fun simpleFactors() {
        assertEquals(2.83168, UnitConversions.cubicFeetToCubicMetres(100.0), 1e-9)
        assertEquals(10.0, UnitConversions.mjToKwh(36.0), 1e-9)
        assertEquals(36.0, UnitConversions.kwhToMj(10.0), 1e-9)
        assertEquals(29.3071, UnitConversions.thermsToKwh(1.0), 1e-9)
        assertEquals(1.0, UnitConversions.kwhToTherms(29.3071), 1e-9)
    }

    @Test
    fun convert_volumeUnits() {
        assertEquals(2.83168, UnitConversions.convert(1.0, MeterUnit.HUNDRED_CUBIC_FEET, MeterUnit.CUBIC_METRES, UtilityType.GAS)!!, 1e-9)
        assertEquals(1000.0, UnitConversions.convert(1.0, MeterUnit.CUBIC_METRES, MeterUnit.LITRES, UtilityType.WATER)!!, 1e-9)
        assertEquals(264.172, UnitConversions.convert(1.0, MeterUnit.CUBIC_METRES, MeterUnit.GALLONS, UtilityType.WATER)!!, 0.001)
    }

    @Test
    fun convert_cubicFeetToKwh_goesThroughCubicMetres() {
        val expected = UnitConversions.gasCubicMetresToKwh(28.3168)
        assertEquals(expected, UnitConversions.convert(1000.0, MeterUnit.CUBIC_FEET, MeterUnit.KILOWATT_HOURS, UtilityType.GAS)!!, 1e-6)
    }

    @Test
    fun convert_thermsAndWattHours() {
        assertEquals(29.3071, UnitConversions.convert(1.0, MeterUnit.THERMS, MeterUnit.KILOWATT_HOURS, UtilityType.GAS)!!, 1e-9)
        assertEquals(1.5, UnitConversions.convert(1500.0, MeterUnit.WATT_HOURS, MeterUnit.KILOWATT_HOURS, UtilityType.ELECTRICITY)!!, 1e-9)
    }

    @Test
    fun convert_volumeToEnergy_onlyForGas() {
        assertNull(UnitConversions.convert(1.0, MeterUnit.CUBIC_METRES, MeterUnit.KILOWATT_HOURS, UtilityType.WATER))
    }

    @Test
    fun parse_acceptsOldAndBillSpellings() {
        assertEquals(MeterUnit.CUBIC_METRES, MeterUnit.parse("m3"))
        assertEquals(MeterUnit.CUBIC_METRES, MeterUnit.parse(" M³ "))
        assertEquals(MeterUnit.KILOWATT_HOURS, MeterUnit.parse("kwh"))
        assertEquals(MeterUnit.LITRES, MeterUnit.parse("Liters"))
        assertEquals(MeterUnit.LITRES, MeterUnit.parse("litres"))
        assertEquals(MeterUnit.HUNDRED_CUBIC_FEET, MeterUnit.parse("CCF"))
        assertEquals(MeterUnit.THERMS, MeterUnit.parse("therm"))
        assertNull(MeterUnit.parse("bananas"))
        assertNull(MeterUnit.parse(""))
    }

    @Test
    fun defaults_perType() {
        assertEquals(MeterUnit.CUBIC_METRES, MeterUnit.defaultFor(UtilityType.GAS))
        assertEquals(MeterUnit.KILOWATT_HOURS, MeterUnit.defaultFor(UtilityType.ELECTRICITY))
        assertEquals(MeterUnit.CUBIC_METRES, MeterUnit.defaultFor(UtilityType.WATER))
    }

    @Test
    fun gasFactors_estimatedWhenEitherIsMissing() {
        assertTrue(GasFactors.of(null, 1.02264).estimated)
        assertTrue(GasFactors.of(39.3, null).estimated)
        assertFalse(GasFactors.of(39.3, 1.02264).estimated)
        assertEquals(39.5, GasFactors.of(null, null).calorificValue, 0.0)
    }

    @Test
    fun dualUsage_gasInCubicMetres_showsEstimatedKwh() {
        val usage = dualUsage(152.0, "m³", UtilityType.GAS)
        assertEquals("152 m³ ≈ 1,706 kWh", usage.text())
        assertTrue(usage.estimated)
    }

    @Test
    fun dualUsage_gasWithBilledFactors_isNotEstimated() {
        val usage = dualUsage(152.0, "m³", UtilityType.GAS, GasFactors.of(39.5, 1.02264))
        assertFalse(usage.estimated)
    }

    @Test
    fun dualUsage_sameUnitOrWater_showsOneUnit() {
        assertEquals("1,234 kWh", dualUsage(1234.0, "kWh", UtilityType.ELECTRICITY).text())
        assertEquals("12.5 m³", dualUsage(12.5, "m³", UtilityType.WATER).text())
        assertNull(dualUsage(12.5, "m³", UtilityType.WATER).energyKwh)
    }

    @Test
    fun dualUsage_thermsAreNotEstimated() {
        val usage = dualUsage(10.0, "therms", UtilityType.GAS)
        assertEquals(293.071, usage.energyKwh!!, 1e-6)
        assertFalse(usage.estimated)
    }
}
