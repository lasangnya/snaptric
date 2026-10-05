package com.snaptric.core.domain.tariff

import com.snaptric.core.domain.units.MeterUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BillTextParserTest {

    private fun assertClose(expected: Double, actual: Double?) = assertEquals(expected, actual!!, 1e-6)

    @Test
    fun ukElectricityBill() {
        val bill = BillTextParser.parse(
            """
            Your electricity charges
            Electricity 1 Jun 2026 - 30 Jun 2026
            Energy used 1,234 kWh @ 24.50p per kWh   £302.33
            Standing charge 30 days @ 53.35p per day   £16.01
            VAT @ 5%   £15.92
            """.trimIndent()
        )
        assertClose(0.245, bill.unitRate)
        assertEquals(MeterUnit.KILOWATT_HOURS, bill.billingUnit)
        assertClose(0.5335, bill.standingChargePerDay)
        assertEquals("GBP", bill.currency)
    }

    @Test
    fun ukGasBill_withConversionLine() {
        val bill = BillTextParser.parse(
            """
            Gas used: 345 m3 x 1.02264 (volume correction) x 39.2 (calorific value) ÷ 3.6 = 3,842 kWh
            Unit rate  6.24p/kWh
            Standing charge  31.43p/day
            """.trimIndent()
        )
        assertClose(0.0624, bill.unitRate)
        assertEquals(MeterUnit.KILOWATT_HOURS, bill.billingUnit)
        assertClose(0.3143, bill.standingChargePerDay)
        assertClose(39.2, bill.calorificValue)
        assertClose(1.02264, bill.volumeCorrection)
    }

    @Test
    fun labelsAndValuesOnSeparateLines() {
        val bill = BillTextParser.parse(
            """
            Calorific value
            39.4 MJ/m3
            Correction factor
            1.02264
            Unit rate (p/kWh)
            7.42
            Standing charge (p/day)
            29.11
            """.trimIndent()
        )
        assertClose(39.4, bill.calorificValue)
        assertClose(1.02264, bill.volumeCorrection)
        assertClose(0.0742, bill.unitRate)
        assertClose(0.2911, bill.standingChargePerDay)
    }

    @Test
    fun euroBill_withCommaDecimalsAndCents() {
        val bill = BillTextParser.parse(
            """
            Arbeitspreis 32,15 ct/kWh
            Grundpreis 12,50 €/Monat
            """.trimIndent()
        )
        assertClose(0.3215, bill.unitRate)
        assertEquals("EUR", bill.currency)
        assertClose(12.5 / (365.25 / 12), bill.standingChargePerDay)
    }

    @Test
    fun usBill_perThermAndMonthlyCustomerCharge() {
        val bill = BillTextParser.parse(
            """
            Natural Gas Service
            Customer Charge   $10.00
            Gas used 45 therms
            Delivery charge $1.0520 per therm   $47.34
            """.trimIndent()
        )
        assertClose(1.052, bill.unitRate)
        assertEquals(MeterUnit.THERMS, bill.billingUnit)
        assertEquals("USD", bill.currency)
        assertClose(10.0 / (365.25 / 12), bill.standingChargePerDay)
    }

    @Test
    fun waterBill_perCubicMetre() {
        val bill = BillTextParser.parse("Volumetric charge £2.0456 per m3\nStanding charge £0.12 a day")
        assertClose(2.0456, bill.unitRate)
        assertEquals(MeterUnit.CUBIC_METRES, bill.billingUnit)
        assertClose(0.12, bill.standingChargePerDay)
    }

    @Test
    fun textWithoutTariff_findsNothing() {
        val bill = BillTextParser.parse("Thank you for your payment of £120.00\nAccount number 12345678")
        assertFalse(bill.foundAnything)
        assertNull(bill.unitRate)
    }

    @Test
    fun parseNumber_handlesSeparators() {
        assertEquals(24.5, BillTextParser.parseNumber("24.50")!!, 0.0)
        assertEquals(24.5, BillTextParser.parseNumber("24,50")!!, 0.0)
        assertEquals(0.325, BillTextParser.parseNumber("0,325")!!, 0.0)
        assertEquals(1234.0, BillTextParser.parseNumber("1,234")!!, 0.0)
        assertEquals(1234.56, BillTextParser.parseNumber("1,234.56")!!, 0.0)
        assertEquals(1234.56, BillTextParser.parseNumber("1.234,56")!!, 0.0)
    }

    @Test
    fun joinIntoRows_putsTableColumnsBackTogether() {
        val text = joinIntoRows(
            listOf(
                TextLine("24.50p per kWh", left = 400, top = 102, right = 600, bottom = 122),
                TextLine("Standing charge", left = 10, top = 140, right = 200, bottom = 160),
                TextLine("Unit rate", left = 10, top = 100, right = 120, bottom = 120),
                TextLine("53.35p per day", left = 400, top = 141, right = 600, bottom = 161)
            )
        )
        assertEquals("Unit rate  24.50p per kWh\nStanding charge  53.35p per day", text)
    }
}
