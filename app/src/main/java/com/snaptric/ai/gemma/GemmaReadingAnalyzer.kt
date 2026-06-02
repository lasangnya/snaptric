package com.snaptric.ai.gemma

import android.content.Context
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

    private var llm: LlmInference? = null

    override suspend fun analyze(imageUri: android.net.Uri): Reading {

        // 1. extract raw text from the image
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val ocrText = suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { continuation.resume(it.text) }
                .addOnFailureListener { continuation.resume("") }
        }

        return withContext(Dispatchers.Default) {
            if (llm == null) {
                withContext(Dispatchers.IO) {
                    val modelFile = copyAssetToFiles(context, "gemma-2b-it-gpu-int4.bin")
                    val options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelFile.absolutePath)
                        .build()
                    llm = LlmInference.createFromOptions(context, options)
                }
            }
            // 2. Use Gemma to find the actual reading in the OCR mess
            val prompt = """
                The following text was extracted from a utility meter via OCR: "$ocrText".
                Please identify the current meter reading. 
                Output ONLY the numeric digits. 
                If you cannot find a reading, output "0000".
                """.trimIndent()
            val response = llm?.generateResponse(prompt)
            val value = response?.filter { it.isDigit() }

            Reading(value = value, timestamp = System.currentTimeMillis(), source = "GEMMA")
        }
    }

}