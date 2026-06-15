package com.snaptric.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
/**
 * Database entity representing a specific meter reading.
 * Tied to a [UtilityEntity] via [utilityId].
 * 
 * @property id Unique identifier for the reading.
 * @property utilityId ID of the utility (meter) this reading belongs to.
 * @property value The numeric value captured from the meter.
 * @property timestamp Time when the reading was taken (Unix epoch).
 * @property source The method used to capture the reading (e.g., "MLKit", "Gemma", "Manual").
 */
@Entity(
    tableName = "readings",
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
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val utilityId: Long,
    val value: Double,
    val timestamp : Long,
    val source: String, //Gemma, MLKit, Manual
)
