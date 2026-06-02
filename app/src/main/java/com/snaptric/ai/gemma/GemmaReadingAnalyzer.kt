package com.snaptric.ai.gemma

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.snaptric.ai.helper.copyAssetToFiles
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.Reading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class GemmaReadingAnalyzer(private val context: Context) : MeterReadingAnalyzer {

    private val llm by lazy {
        val modelFile = copyAssetToFiles(context, "gemma-2b-it-gpu-int4.bin")
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelFile.absolutePath)
            .setMaxTokens(1024)
            .build()
        LlmInference.createFromOptions(context, options)
    }

    override suspend fun analyze(imageUri: android.net.Uri): Reading {

        // 1. extract raw text from the image
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val ocrText = suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { continuation.resume(it.text) }
                .addOnFailureListener { continuation.resume("") }
        }
        val cleanedOcr = ocrText.lines()
            .map { it.trim() }
            .filter { line ->
                val digitCount = line.count { it.isDigit() }
                val isTechnical = line.contains("m/h") || line.contains("bar") || line.contains("°C")
                val isDate = line.contains("2026") || line.contains("at")

                // Keep lines with 4+ digits that aren't dates or technical specs
                digitCount > 3 && !isTechnical && !isDate
            }
            .joinToString("\n")
        Log.d("ocrtext", cleanedOcr)

        return withContext(Dispatchers.Default) {
            // 2. Use Gemma to find the actual reading in the OCR mess
            val prompt = """
                Identify the meter reading from the OCR text below.
    
                ### EXAMPLES ###
                OCR: "Pmax 0,1 bar\n001 22 34 5\nEN 1359" -> Result: 00122345
                OCR: "12142MIO\n5. Feb 2026\n007 35 05 4\nQmin 0,04" -> Result: 00735054

                ### ACTUAL OCR DATA TO PROCESS ###
                $cleanedOcr

                ### INSTRUCTIONS ###
                - Output ONLY the numeric reading.
                - Do NOT include any text from the examples above.
                - Ignore serial numbers and phone numbers.
                - Look for the 8-digit consumption counter (like 00735054).
                - Result:
                """.trimIndent()
                val response = llm.generateResponse(prompt)
                val value = response?.filter { it.isDigit() }

            Reading(value = value, timestamp = System.currentTimeMillis(), source = "GEMMA")
        }
    }

}