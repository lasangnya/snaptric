package com.snaptric.feature.tariffs.viewmodel

import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.domain.tariff.ParsedBill
import com.snaptric.core.domain.units.MeterUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class TariffDraftTest {

    private val blank = TariffDraft.blank(UtilityType.GAS, "GBP", LocalDate.of(2026, 10, 1))

    @Test
    fun blankDraft_isIncomplete() {
        assertFalse(blank.isValid)
        assertNull(blank.toEntity(1, UtilityType.GAS))
        assertEquals("kWh", blank.billingUnit)
    }

    @Test
    fun completeDraft_becomesTariff() {
        val draft = blank.copy(unitRate = "0,0624", standingCharge = "0.3143", calorificValue = "39.2", volumeCorrection = "1.02264")
        val tariff = draft.toEntity(7, UtilityType.GAS, ZoneOffset.UTC)!!
        assertEquals(7, tariff.utilityId)
        assertEquals(0.0624, tariff.unitRate, 0.0)
        assertEquals(0.3143, tariff.standingChargePerDay, 0.0)
        assertEquals(39.2, tariff.calorificValue!!, 0.0)
        assertEquals(1.02264, tariff.volumeCorrection!!, 0.0)
        assertEquals(LocalDate.of(2026, 10, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), tariff.effectiveFrom)
        assertEquals("Manual", tariff.source)
    }

    @Test
    fun implausibleCalorificValue_blocksSaving() {
        assertFalse(blank.copy(unitRate = "0.06", standingCharge = "0.3", calorificValue = "392").isValid)
    }

    @Test
    fun gasFactors_areDroppedForOtherMeters() {
        val draft = TariffDraft.blank(UtilityType.ELECTRICITY, "GBP", LocalDate.of(2026, 10, 1))
            .copy(unitRate = "0.245", standingCharge = "0.5", calorificValue = "39.2")
        assertNull(draft.toEntity(1, UtilityType.ELECTRICITY)!!.calorificValue)
    }

    @Test
    fun withBill_fillsFoundFieldsAndMarksThem() {
        val bill = ParsedBill(unitRate = 0.0624, billingUnit = MeterUnit.KILOWATT_HOURS, calorificValue = 39.2, currency = "GBP")
        val draft = blank.withBill(bill, UtilityType.GAS)
        assertEquals("0.0624", draft.unitRate)
        assertEquals("39.2", draft.calorificValue)
        assertEquals("", draft.standingCharge)
        assertEquals(setOf(TariffField.UNIT_RATE, TariffField.BILLING_UNIT, TariffField.CALORIFIC_VALUE, TariffField.CURRENCY), draft.fromBill)
        assertTrue(draft.scanned)
        // Still needs the user to add the standing charge before it can be saved.
        assertFalse(draft.isValid)
        assertEquals("BillScan", draft.copy(standingCharge = "0.31").toEntity(1, UtilityType.GAS)!!.source)
    }

    @Test
    fun withBill_ignoresBillingUnitThatDoesNotSuitTheMeter() {
        val electricity = TariffDraft.blank(UtilityType.ELECTRICITY, "GBP", LocalDate.of(2026, 10, 1))
        val draft = electricity.withBill(ParsedBill(unitRate = 1.2, billingUnit = MeterUnit.THERMS), UtilityType.ELECTRICITY)
        assertEquals("kWh", draft.billingUnit)
        assertFalse(TariffField.BILLING_UNIT in draft.fromBill)
    }
}
