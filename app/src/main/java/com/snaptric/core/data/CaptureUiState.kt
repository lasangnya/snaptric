package com.snaptric.core.data

import com.snaptric.core.domain.Reading

// UI states in the capture screen

data class CaptureUiState(
    val isLoading : Boolean = false,
    val lastReading: Reading? = null,
)