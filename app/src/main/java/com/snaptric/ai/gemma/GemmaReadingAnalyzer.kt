package com.snaptric.ai.gemma

import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.Reading

class GemmaReadingAnalyzer : MeterReadingAnalyzer {
    override suspend fun analyze(imageUri: android.net.Uri): Reading {
        //call gemma model here
        return Reading() // Returned the analyzed value here.
    }
}