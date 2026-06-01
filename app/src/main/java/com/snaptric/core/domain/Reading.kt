package com.snaptric.core.domain

data class Reading(
    val value: String,
    val timestamp: Long,
    val source: String = "GEMMA"
)