package com.snaptric.core.domain

import android.graphics.Bitmap
import android.net.Uri

// Interface for the meter reading analyzer.
interface MeterReadingAnalyzer{
    suspend fun analyze(bitmap: Bitmap) : Reading
}