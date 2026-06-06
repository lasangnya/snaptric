package com.snaptric.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


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

enum class UtilityType{GAS, ELECTRICITY, WATER}