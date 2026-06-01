package com.snaptric.core.domain

import android.net.Uri

// Interface for the meter reading analyzer.
interface MeterReadingAnalyzer{
    suspend fun analyze(imageUri : Uri) : Reading
}