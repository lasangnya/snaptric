package com.snaptric.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
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