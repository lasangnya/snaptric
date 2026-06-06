package com.snaptric.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String?,
    val iconIdentifier: String = "ic_default_property"
)
