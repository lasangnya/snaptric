package com.snaptric.core.domain.tariff

import com.snaptric.core.database.entity.TariffEntity
import com.snaptric.core.database.entity.UtilityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffCostTest {

    private fun tariff(
        rate: Double = 0.25,
        unit: String = "kWh",
        standing: Double = 0.50,
        from: Long = 0,
        cv: Double? = null,
        correction: Double? = null
    ) = TariffEntity(
        utilityId = 1, unitRate = rate, billingUnit = unit, standingChargePerDay = standing,
        currency = "GBP", effectiveFrom = from, calorificValue = cv, volumeCorrection = correction
    )

    @Test
    fun electricity_kwhTimesRatePlusStandingCharge() {
        val cost = estimateCost(100.0, "kWh", UtilityType.ELECTRICITY, tariff(), days = 10)!!
        assertEquals(25.0, cost.usageCost, 1e-9)
        assertEquals(5.0, cost.standingCharge, 1e-9)
        assertEquals(30.0, cost.total, 1e-9)
        assertFalse(cost.estimated)
    }

    @Test
    fun gasInCubicMetres_billedPerKwh_usesTariffFactors() {
        val cost = estimateCost(100.0, "m³", UtilityType.GAS, tariff(rate = 0.06, cv = 39.5, correction = 1.02264), days = 0)!!
        assertEquals(1122.0633 * 0.06, cost.usageCost, 0.001)
        assertFalse(cost.estimated)
    }

    @Test
    fun gasWithoutCalorificValue_isEstimated() {
        val cost = estimateCost(100.0, "m3", UtilityType.GAS, tariff(rate = 0.06), days = 0)!!
        assertTrue(cost.estimated)
    }

    @Test
    fun gasBilledPerTherm() {
        val cost = estimateCost(10.0, "therms", UtilityType.GAS, tariff(rate = 1.50, unit = "therms"), days = 0)!!
        assertEquals(15.0, cost.usageCost, 1e-9)
    }

    @Test
    fun incompatibleUnits_haveNoCost() {
        assertNull(estimateCost(10.0, "m³", UtilityType.WATER, tariff(unit = "kWh"), days = 1))
        assertNull(estimateCost(10.0, "widgets", UtilityType.GAS, tariff(), days = 1))
    }

    @Test
    fun activeAt_picksLatestStartedTariff() {
        val old = tariff(rate = 0.20, from = 100)
        val new = tariff(rate = 0.30, from = 200)
        val future = tariff(rate = 0.40, from = 300)
        val tariffs = listOf(new, future, old)
        assertEquals(old, tariffs.activeAt(150))
        assertEquals(new, tariffs.activeAt(250))
        assertNull(tariffs.activeAt(50))
    }

    @Test
    fun formatting() {
        assertEquals("£1,234.50", formatMoney(1234.5, "GBP"))
        assertEquals("€0.33", formatMoney(0.325, "EUR"))
        assertEquals("CHF 3.00", formatMoney(3.0, "chf"))
        assertEquals("24.50p/kWh", formatRate(0.245, "GBP", "kWh"))
        assertEquals("$0.1534/kWh", formatRate(0.1534, "USD", "kWh"))
    }

    @Test
    fun needsGasFactors_onlyWhenVolumeMeetsEnergy() {
        assertTrue(needsGasFactors(UtilityType.GAS, "m³", "kWh"))
        assertFalse(needsGasFactors(UtilityType.GAS, "m³", "m³"))
        assertFalse(needsGasFactors(UtilityType.ELECTRICITY, "kWh", "kWh"))
    }
}
