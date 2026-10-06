package com.snaptric.core.domain.tariff

import com.snaptric.core.database.entity.TariffEntity
import com.snaptric.core.database.entity.UtilityType
import com.snaptric.core.domain.units.GasFactors
import com.snaptric.core.domain.units.MeterUnit
import com.snaptric.core.domain.units.UnitConversions
import java.util.Locale

/**
 * What some usage costs under a tariff.
 *
 * @property estimated True when gas volume was turned into energy with a default calorific value
 *   or correction factor.
 */
data class CostEstimate(
    val usageCost: Double,
    val standingCharge: Double,
    val currency: String,
    val estimated: Boolean
) {
    val total: Double get() = usageCost + standingCharge
}

/** The tariff in force at [timestamp]: the latest one that had started by then. */
fun List<TariffEntity>.activeAt(timestamp: Long): TariffEntity? =
    filter { it.effectiveFrom <= timestamp }.maxByOrNull { it.effectiveFrom }

/** The gas factors a tariff carries, with defaults for any that weren't entered. */
val TariffEntity.gasFactors: GasFactors get() = GasFactors.of(calorificValue, volumeCorrection)

/**
 * Cost of [usage] read on a meter of [type] in [meterUnit], plus [days] of standing charge.
 * Returns null when the meter's unit can't be converted to the tariff's billing unit.
 */
fun estimateCost(
    usage: Double,
    meterUnit: String,
    type: UtilityType,
    tariff: TariffEntity,
    days: Int
): CostEstimate? {
    val from = MeterUnit.parse(meterUnit) ?: return null
    val to = MeterUnit.parse(tariff.billingUnit) ?: return null
    val factors = tariff.gasFactors
    val billed = UnitConversions.convert(usage, from, to, type, factors) ?: return null
    return CostEstimate(
        usageCost = billed * tariff.unitRate,
        standingCharge = days.coerceAtLeast(0) * tariff.standingChargePerDay,
        currency = tariff.currency,
        estimated = from.dimension != to.dimension && factors.estimated
    )
}

/** "£48.20", "€12.05", "$7.10", or "CHF 3.00" for currencies without a well-known symbol. */
fun formatMoney(amount: Double, currency: String): String {
    val number = String.format(Locale.UK, "%,.2f", amount)
    return when (currency.uppercase()) {
        "GBP" -> "£$number"
        "EUR" -> "€$number"
        "USD", "CAD", "AUD", "NZD" -> "$$number"
        else -> "${currency.uppercase()} $number"
    }
}

/** "24.50p/kWh"-style rate: shown in pence or cents when under one pound or euro. */
fun formatRate(rate: Double, currency: String, billingUnit: String): String {
    val minor = when (currency.uppercase()) {
        "GBP" -> "p"
        "EUR" -> "c"
        else -> null
    }
    val price = when {
        minor != null && rate < 1 -> String.format(Locale.UK, "%.2f", rate * 100) + minor
        // Sub-unit rates such as $0.1534 need more than two decimals.
        rate < 1 -> formatMoney(0.0, currency).replace("0.00", String.format(Locale.UK, "%.4f", rate))
        else -> formatMoney(rate, currency)
    }
    return "$price/$billingUnit"
}

/** Units a tariff for a meter of [type] can be billed per, most common first. */
fun billingUnitsFor(type: UtilityType): List<MeterUnit> = when (type) {
    UtilityType.GAS -> listOf(MeterUnit.KILOWATT_HOURS, MeterUnit.CUBIC_METRES, MeterUnit.THERMS, MeterUnit.HUNDRED_CUBIC_FEET)
    UtilityType.ELECTRICITY -> listOf(MeterUnit.KILOWATT_HOURS)
    UtilityType.WATER -> listOf(MeterUnit.CUBIC_METRES, MeterUnit.LITRES, MeterUnit.GALLONS)
}

/**
 * True when costing a gas meter read in [meterUnit] on a tariff billed per [billingUnit] needs the
 * calorific value and correction factor (volume on one side, energy on the other).
 */
fun needsGasFactors(type: UtilityType, meterUnit: String, billingUnit: String): Boolean {
    if (type != UtilityType.GAS) return false
    val from = MeterUnit.parse(meterUnit)?.dimension ?: return true
    val to = MeterUnit.parse(billingUnit)?.dimension ?: return true
    return from != to
}
