package com.snaptric.ai.mlkit

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.common.InputImage.fromFilePath
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.Reading
import kotlinx.coroutines.tasks.await

class MlKitReadingAnalyzer(private val context: Context) : MeterReadingAnalyzer {
    // Initialize the ML kit text recognizer
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun analyze(bitmap: Bitmap): Reading {
        // prepare the image
        val image = InputImage.fromBitmap(bitmap, 0)

        // process the image using the Mlkit and wait for results
        val result = recognizer.process(image).await()

        // extract recognized text and keep only digits
        val readingValue = result.text.filter { it.isDigit() }

        return Reading(
            value = readingValue,
            timestamp = System.currentTimeMillis(),
            source = "MLKit"
        )
    }
}