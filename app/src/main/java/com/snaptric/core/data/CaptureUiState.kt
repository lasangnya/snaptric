package com.snaptric.core.data

import com.snaptric.core.domain.Reading

/**
 * Represents the UI state for the Capture screen.
 * @property isLoading Indicates if a background process (like AI analysis) is running.
 * @property lastReading The most recently captured reading, if any.
 */
data class CaptureUiState(
    val isLoading : Boolean = false,
    val lastReading: Reading? = null,
)
