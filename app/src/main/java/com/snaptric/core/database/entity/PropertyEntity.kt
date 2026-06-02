package com.snaptric.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String?,
    val iconIdentifier: String = "ic_default_property"
)
