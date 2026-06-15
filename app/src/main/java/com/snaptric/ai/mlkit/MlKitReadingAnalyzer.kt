package com.snaptric.ai.mlkit

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.Reading
import kotlinx.coroutines.tasks.await

/**
 * An implementation of [MeterReadingAnalyzer] that uses Google ML Kit's Text Recognition.
 * This analyzer extracts numeric sequences from a bitmap.
 */
class MlKitReadingAnalyzer(private val context: Context) : MeterReadingAnalyzer {
    // Initialize the ML Kit text recognizer with default Latin script options.
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Processes the given [bitmap] to extract a meter reading.
     * Uses Coroutines tasks.await() to handle the asynchronous ML Kit operation.
     */
    override suspend fun analyze(bitmap: Bitmap): Reading {
        // Prepare the image for ML Kit.
        val image = InputImage.fromBitmap(bitmap, 0)

        // Process the image and wait for the results.
        val result = recognizer.process(image).await()

        // Extract recognized text and keep only digits to find the meter value.
        val readingValue = result.text.filter { it.isDigit() }

        return Reading(
            value = readingValue,
            timestamp = System.currentTimeMillis(),
            source = "MLKit"
        )
    }
}
