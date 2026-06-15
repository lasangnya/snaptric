package com.snaptric.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


/**
 * Database entity representing a specific utility meter (e.g., Gas, Electric, Water).
 * Tied to a [PropertyEntity] via [propertyId].
 * 
 * @property id Unique identifier for the utility.
 * @property propertyId ID of the property this utility is installed in.
 * @property type The type of utility (Gas, Electricity, or Water).
 * @property unit The measurement unit (e.g., "kWh", "m³").
 * @property initialReading The starting value of the meter when added to the app.
 * @property name Optional descriptive name (e.g., "Main Floor Meter").
 */
@Entity(
    tableName = "utility",
    foreignKeys = [
        ForeignKey(
            entity = PropertyEntity::class,
            parentColumns = ["id"],
            childColumns = ["propertyId"],
            onDelete = ForeignKey.CASCADE // if property is deleted, delete its utilities
        )
    ],
    indices = [Index(value = ["propertyId"])] // for faster lookups by propertyId
    )
data class UtilityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val propertyId: Long, // foreign key to PropertyEntity
    val type: UtilityType, // e.g., "Electricity", "Water", "Gas
    val unit: String, // m3, kwh, l etc...
    val initialReading : Double,
    val name : String? = null // optional e.g - Main Meter, Solar Meter etc...
)

/**
 * Supported utility types in the application.
 */
enum class UtilityType{GAS, ELECTRICITY, WATER}
