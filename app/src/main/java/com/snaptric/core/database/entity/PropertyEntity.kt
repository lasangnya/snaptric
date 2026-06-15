package com.snaptric.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Database entity representing a physical property (e.g., "Home", "Office").
 * @property id Unique identifier for the property.
 * @property name User-defined name for the property.
 * @property address Optional physical address.
 * @property iconIdentifier Identifier for the icon displayed in the UI.
 */
@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String?,
    val iconIdentifier: String = "ic_default_property"
)

