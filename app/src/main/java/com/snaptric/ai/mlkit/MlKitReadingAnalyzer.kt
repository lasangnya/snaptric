package com.snaptric.ai.mlkit

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.Reading
import kotlinx.coroutines.tasks.await

class MlKitReadingAnalyzer(private val context: Context) : MeterReadingAnalyzer {
    // Initialize the ML kit text recognizer
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun analyze(imageUri: Uri): Reading {
        // prepare the image
        val image = InputImage.fromFilePath(context, imageUri)
        // process the image using the Mlkit and wait for results
        val result = recognizer.process(image).await()
        // extract numbers from the recognized text
        // filter for 4 to 8 consecutive digits
        val fullText = result.text
        val meterRegex = Regex("\\d{4,8}")
        val match = meterRegex.find(fullText)
        val readingValue = match?.value ?: ""

        return Reading(
            value = readingValue,
            timestamp = System.currentTimeMillis(),
            source = "MLKit"
        )
    }
}