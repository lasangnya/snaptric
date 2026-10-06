package com.snaptric.core.domain.units

import com.snaptric.core.database.entity.UtilityType

/** Whether a unit measures a volume of gas or water, or an amount of energy. */
enum class Dimension { VOLUME, ENERGY }

/**
 * A unit a meter dial or a bill can be in.
 *
 * @property symbol How the unit is stored on a meter and shown in the app.
 * @property dimension Volume or energy; only gas can be converted between the two.
 * @property toBase Size of one unit in the base unit of its dimension: m³ for volume, kWh for energy.
 */
enum class MeterUnit(val symbol: String, val dimension: Dimension, val toBase: Double) {
    CUBIC_METRES("m³", Dimension.VOLUME, 1.0),
    CUBIC_FEET("ft³", Dimension.VOLUME, UnitConversions.CUBIC_METRES_PER_CUBIC_FOOT),
    HUNDRED_CUBIC_FEET("hcf", Dimension.VOLUME, UnitConversions.CUBIC_METRES_PER_CUBIC_FOOT * 100),
    LITRES("Liters", Dimension.VOLUME, 0.001),
    GALLONS("Gallons", Dimension.VOLUME, UnitConversions.CUBIC_METRES_PER_US_GALLON),
    KILOWATT_HOURS("kWh", Dimension.ENERGY, 1.0),
    WATT_HOURS("Wh", Dimension.ENERGY, 0.001),
    MEGAJOULES("MJ", Dimension.ENERGY, 1 / UnitConversions.MJ_PER_KWH),
    THERMS("therms", Dimension.ENERGY, UnitConversions.KWH_PER_THERM);

    companion object {
        /**
         * The unit a stored or typed unit string means, or null if it isn't recognised.
         * Accepts the spellings older versions of the app saved ("m3", "Liters") and common bill spellings.
         */
        fun parse(text: String?): MeterUnit? {
            val key = text?.trim()?.lowercase()?.replace(" ", "")?.removeSuffix(".") ?: return null
            return when (key) {
                "m³", "m3", "cubicmetre", "cubicmetres", "cubicmeter", "cubicmeters", "cum" -> CUBIC_METRES
                "ft³", "ft3", "cuft", "cubicfeet", "cubicfoot", "cf" -> CUBIC_FEET
                "hcf", "ccf", "100ft³", "100ft3", "100cuft", "hundredcubicfeet" -> HUNDRED_CUBIC_FEET
                "l", "liter", "liters", "litre", "litres", "ltr" -> LITRES
                "gal", "gallon", "gallons" -> GALLONS
                "kwh", "kw/h", "kwhr" -> KILOWATT_HOURS
                "wh" -> WATT_HOURS
                "mj" -> MEGAJOULES
                "therm", "therms", "thm" -> THERMS
                else -> null
            }
        }

        /** Units offered when setting up a meter of [type], most common first. */
        fun optionsFor(type: UtilityType): List<MeterUnit> = when (type) {
            UtilityType.GAS -> listOf(CUBIC_METRES, CUBIC_FEET, HUNDRED_CUBIC_FEET, KILOWATT_HOURS, THERMS)
            UtilityType.ELECTRICITY -> listOf(KILOWATT_HOURS, WATT_HOURS)
            UtilityType.WATER -> listOf(CUBIC_METRES, LITRES, GALLONS)
        }

        /** The unit assumed for a meter whose unit is missing: m³ for gas and water, kWh for electricity. */
        fun defaultFor(type: UtilityType): MeterUnit = optionsFor(type).first()
    }
}

/**
 * Gas calorific value and volume correction factor, used to turn a volume of gas into energy.
 * Both change from one billing period to the next, so they're stored with each tariff.
 *
 * @property estimated True when either value is the typical default rather than the one on the bill.
 */
data class GasFactors(
    val calorificValue: Double = UnitConversions.DEFAULT_CALORIFIC_VALUE,
    val volumeCorrection: Double = UnitConversions.DEFAULT_VOLUME_CORRECTION,
    val estimated: Boolean = true
) {
    companion object {
        /** Factors from a tariff, falling back to the defaults for whatever is missing. */
        fun of(calorificValue: Double?, volumeCorrection: Double?) = GasFactors(
            calorificValue = calorificValue ?: UnitConversions.DEFAULT_CALORIFIC_VALUE,
            volumeCorrection = volumeCorrection ?: UnitConversions.DEFAULT_VOLUME_CORRECTION,
            estimated = calorificValue == null || volumeCorrection == null
        )
    }
}

/**
 * Conversions between meter and billing units. Readings are always stored as read; anything
 * shown in another unit is derived with these.
 */
object UnitConversions {
    const val CUBIC_METRES_PER_CUBIC_FOOT = 0.0283168
    const val CUBIC_METRES_PER_US_GALLON = 0.00378541
    const val MJ_PER_KWH = 3.6
    const val KWH_PER_THERM = 29.3071

    /** Typical UK volume correction factor for temperature and pressure. */
    const val DEFAULT_VOLUME_CORRECTION = 1.02264

    /** Typical calorific value of natural gas, in MJ/m³. */
    const val DEFAULT_CALORIFIC_VALUE = 39.5

    /** kWh = m³ × volume correction × calorific value ÷ 3.6, as printed on UK gas bills. */
    fun gasCubicMetresToKwh(cubicMetres: Double, factors: GasFactors = GasFactors()): Double =
        cubicMetres * factors.volumeCorrection * factors.calorificValue / MJ_PER_KWH

    fun kwhToGasCubicMetres(kwh: Double, factors: GasFactors = GasFactors()): Double =
        kwh * MJ_PER_KWH / (factors.volumeCorrection * factors.calorificValue)

    fun cubicFeetToCubicMetres(cubicFeet: Double): Double = cubicFeet * CUBIC_METRES_PER_CUBIC_FOOT
    fun mjToKwh(mj: Double): Double = mj / MJ_PER_KWH
    fun kwhToMj(kwh: Double): Double = kwh * MJ_PER_KWH
    fun thermsToKwh(therms: Double): Double = therms * KWH_PER_THERM
    fun kwhToTherms(kwh: Double): Double = kwh / KWH_PER_THERM

    /**
     * Converts [amount] from one unit to another, or returns null when they can't be compared.
     * Volume and energy only convert into each other for gas, using [gasFactors].
     */
    fun convert(
        amount: Double,
        from: MeterUnit,
        to: MeterUnit,
        type: UtilityType,
        gasFactors: GasFactors = GasFactors()
    ): Double? {
        if (from == to) return amount
        val base = amount * from.toBase
        val inTargetDimension = when {
            from.dimension == to.dimension -> base
            type != UtilityType.GAS -> return null
            from.dimension == Dimension.VOLUME -> gasCubicMetresToKwh(base, gasFactors)
            else -> kwhToGasCubicMetres(base, gasFactors)
        }
        return inTargetDimension / to.toBase
    }
}
