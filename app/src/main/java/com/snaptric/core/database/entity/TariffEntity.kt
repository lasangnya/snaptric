package com.snaptric.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The prices a meter is billed at from [effectiveFrom] until the next tariff starts.
 * A meter belongs to one property, so this is the rate for that meter at that property.
 *
 * @property utilityId The meter this tariff applies to.
 * @property unitRate Price per [billingUnit], in [currency] (e.g. 0.245 for 24.5p per kWh).
 * @property billingUnit The unit the rate is charged per, as a [com.snaptric.core.domain.units.MeterUnit] symbol.
 * @property standingChargePerDay Fixed daily charge, in [currency].
 * @property currency ISO 4217 code, e.g. "GBP".
 * @property effectiveFrom Start of the first day the tariff applies (Unix epoch millis).
 * @property calorificValue Gas calorific value in MJ/m³ for this period, or null if not known.
 * @property volumeCorrection Gas volume correction factor for this period, or null if not known.
 * @property source How the tariff was entered: "Manual" or "BillScan".
 */
@Entity(
    tableName = "tariffs",
    foreignKeys = [
        ForeignKey(
            entity = UtilityEntity::class,
            parentColumns = ["id"],
            childColumns = ["utilityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["utilityId"])]
)
data class TariffEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val utilityId: Long,
    val unitRate: Double,
    val billingUnit: String,
    val standingChargePerDay: Double,
    val currency: String,
    val effectiveFrom: Long,
    val calorificValue: Double? = null,
    val volumeCorrection: Double? = null,
    val source: String = "Manual"
)
