package com.snaptric.core.domain

import android.graphics.Bitmap

/**
 * Interface defining the contract for meter reading analysis.
 * Implementations should take an image (Bitmap) and return a structured [Reading].
 */
interface MeterReadingAnalyzer{
    /**
     * Analyzes the provided [bitmap] to detect and extract numeric meter values.
     */
    suspend fun analyze(bitmap: Bitmap) : Reading
}
