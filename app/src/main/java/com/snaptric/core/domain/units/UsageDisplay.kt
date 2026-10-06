package com.snaptric.core.domain.units

import com.snaptric.core.database.entity.UtilityType
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A usage amount in the meter's own unit, plus the energy it represents when that differs.
 *
 * @property estimated True when [energyKwh] used the default calorific value or correction factor.
 */
data class DualUsage(
    val amount: Double,
    val unit: String,
    val energyKwh: Double?,
    val estimated: Boolean
) {
    /** "152 m³ ≈ 1,710 kWh", or just "152 m³" when there's nothing to convert to. */
    fun text(): String {
        val primary = listOf(formatQuantity(amount), unit).filter { it.isNotBlank() }.joinToString(" ")
        return energyKwh?.let { "$primary ≈ ${formatQuantity(it)} kWh" } ?: primary
    }
}

/**
 * Describes [amount] of usage on a meter of [type] whose dial reads in [unitText]. Gas read in
 * volume or therms, and electricity read in Wh, also get their kWh equivalent.
 */
fun dualUsage(amount: Double, unitText: String, type: UtilityType, gasFactors: GasFactors = GasFactors()): DualUsage {
    val unit = MeterUnit.parse(unitText)
    val showsEnergy = unit != null && unit != MeterUnit.KILOWATT_HOURS &&
        (type == UtilityType.GAS || (type == UtilityType.ELECTRICITY && unit.dimension == Dimension.ENERGY))
    val kwh = if (showsEnergy) UnitConversions.convert(amount, unit, MeterUnit.KILOWATT_HOURS, type, gasFactors) else null
    return DualUsage(
        amount = amount,
        unit = unitText,
        energyKwh = kwh,
        estimated = kwh != null && unit?.dimension == Dimension.VOLUME && gasFactors.estimated
    )
}

/**
 * Formats a quantity with thousands separators: whole numbers from 100 up, one decimal below.
 */
fun formatQuantity(value: Double, locale: Locale = Locale.UK): String =
    if (abs(value) >= 100) String.format(locale, "%,d", value.roundToLong())
    else String.format(locale, "%.1f", value).removeSuffix(".0")
