package com.snaptric.feature.tariffs.viewmodel

import com.snaptric.core.database.entity.TariffEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.domain.tariff.BillTextParser
import com.snaptric.core.domain.tariff.ParsedBill
import com.snaptric.core.domain.tariff.billingUnitsFor
import com.snaptric.core.domain.tariff.isKnownCurrency
import com.snaptric.core.domain.units.MeterUnit
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** A tariff field that can be filled in from a bill. */
enum class TariffField { UNIT_RATE, BILLING_UNIT, STANDING_CHARGE, CURRENCY, CALORIFIC_VALUE, VOLUME_CORRECTION }

/**
 * A tariff being entered or reviewed. Numbers are kept as typed so the form can show them back.
 *
 * @property id The tariff being edited, or 0 for a new one.
 * @property fromBill Fields that were read from a bill and need the user to check them.
 * @property scanned True when the draft started from a bill scan.
 */
data class TariffDraft(
    val id: Long = 0,
    val unitRate: String = "",
    val billingUnit: String,
    val standingCharge: String = "",
    val currency: String,
    val effectiveFrom: LocalDate,
    val calorificValue: String = "",
    val volumeCorrection: String = "",
    val fromBill: Set<TariffField> = emptySet(),
    val scanned: Boolean = false
) {
    val unitRateValue: Double? get() = BillTextParser.parseNumber(unitRate.trim())?.takeIf { it > 0 }
    val standingChargeValue: Double? get() = BillTextParser.parseNumber(standingCharge.trim())?.takeIf { it >= 0 }
    val calorificValueValue: Double? get() = BillTextParser.parseNumber(calorificValue.trim())
    val volumeCorrectionValue: Double? get() = BillTextParser.parseNumber(volumeCorrection.trim())

    val currencyValid: Boolean get() = isKnownCurrency(currency.trim())
    val calorificValueValid: Boolean get() = calorificValue.isBlank() || calorificValueValue?.let { it in 20.0..60.0 } == true
    val volumeCorrectionValid: Boolean get() = volumeCorrection.isBlank() || volumeCorrectionValue?.let { it in 0.8..1.2 } == true

    val isValid: Boolean
        get() = unitRateValue != null && standingChargeValue != null && currencyValid &&
            MeterUnit.parse(billingUnit) != null && calorificValueValid && volumeCorrectionValid

    /** The tariff to save for [utilityId], or null while the draft is incomplete. */
    fun toEntity(utilityId: Long, type: UtilityType, zone: ZoneId = ZoneId.systemDefault()): TariffEntity? {
        if (!isValid) return null
        val isGas = type == UtilityType.GAS
        return TariffEntity(
            id = id,
            utilityId = utilityId,
            unitRate = unitRateValue!!,
            billingUnit = MeterUnit.parse(billingUnit)!!.symbol,
            standingChargePerDay = standingChargeValue!!,
            currency = currency.trim().uppercase(Locale.ROOT),
            effectiveFrom = effectiveFrom.atStartOfDay(zone).toInstant().toEpochMilli(),
            calorificValue = if (isGas) calorificValueValue else null,
            volumeCorrection = if (isGas) volumeCorrectionValue else null,
            source = if (scanned) SOURCE_BILL_SCAN else SOURCE_MANUAL
        )
    }

    /**
     * Fills in everything found on a bill, marking those fields for review. Fields the bill didn't
     * show keep their current value. A billing unit that doesn't suit the meter type is ignored.
     */
    fun withBill(bill: ParsedBill, type: UtilityType): TariffDraft {
        val found = mutableSetOf<TariffField>()
        fun <T> take(value: T?, field: TariffField): T? = value?.also { found += field }
        val unit = bill.billingUnit?.takeIf { it in billingUnitsFor(type) }
        val isGas = type == UtilityType.GAS
        return copy(
            unitRate = take(bill.unitRate, TariffField.UNIT_RATE)?.let(::formatNumber) ?: unitRate,
            billingUnit = take(unit, TariffField.BILLING_UNIT)?.symbol ?: billingUnit,
            standingCharge = take(bill.standingChargePerDay, TariffField.STANDING_CHARGE)?.let(::formatNumber) ?: standingCharge,
            currency = take(bill.currency, TariffField.CURRENCY) ?: currency,
            calorificValue = (if (isGas) take(bill.calorificValue, TariffField.CALORIFIC_VALUE) else null)?.let(::formatNumber) ?: calorificValue,
            volumeCorrection = (if (isGas) take(bill.volumeCorrection, TariffField.VOLUME_CORRECTION) else null)?.let(::formatNumber) ?: volumeCorrection,
            fromBill = found,
            scanned = true
        )
    }

    companion object {
        const val SOURCE_MANUAL = "Manual"
        const val SOURCE_BILL_SCAN = "BillScan"

        /** A blank tariff for a meter of [type], starting [today]. */
        fun blank(type: UtilityType, currency: String, today: LocalDate) = TariffDraft(
            billingUnit = billingUnitsFor(type).first().symbol,
            currency = currency,
            effectiveFrom = today
        )

        /** A draft for editing an existing tariff. */
        fun of(tariff: TariffEntity, zone: ZoneId = ZoneId.systemDefault()) = TariffDraft(
            id = tariff.id,
            unitRate = formatNumber(tariff.unitRate),
            billingUnit = tariff.billingUnit,
            standingCharge = formatNumber(tariff.standingChargePerDay),
            currency = tariff.currency,
            effectiveFrom = java.time.Instant.ofEpochMilli(tariff.effectiveFrom).atZone(zone).toLocalDate(),
            calorificValue = tariff.calorificValue?.let(::formatNumber).orEmpty(),
            volumeCorrection = tariff.volumeCorrection?.let(::formatNumber).orEmpty(),
            scanned = tariff.source == SOURCE_BILL_SCAN
        )

        /** Up to six decimals without trailing zeros: 0.245, 1.02264, 39.5. */
        fun formatNumber(value: Double): String =
            String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
    }
}
