package com.snaptric.core.domain

import android.graphics.Bitmap

// Interface for the meter reading analyzer.
interface MeterReadingAnalyzer{
    suspend fun analyze(bitmap: Bitmap) : Reading
}