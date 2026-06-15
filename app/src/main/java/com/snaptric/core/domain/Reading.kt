package com.snaptric.core.domain

/**
 * A domain-level data model representing a raw meter reading.
 * Used for intermediate processing before being saved to the database.
 * 
 * @property value The detected value as a string (may contain non-numeric characters before cleaning).
 * @property timestamp The time the reading was processed.
 * @property source The engine used for detection (defaulting to "GEMMA").
 */
data class Reading(
    val value: String?,
    val timestamp: Long,
    val source: String = "GEMMA"
)
