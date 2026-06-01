package com.snaptric.ai.gemma

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.snaptric.ai.helper.copyAssetToFiles
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.Reading
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GemmaReadingAnalyzer(private val context: Context) : MeterReadingAnalyzer {

    val modelFile = copyAssetToFiles(context, "gemma-2b-it-gpu-int4.bin") // Copy the model from assets to files and get the file reference.

    val options = LlmInference.LlmInferenceOptions.builder()
        .setModelPath(modelFile.absolutePath) // Set the model path to the copied file.
        .setMaxTokens(128)
        .build()

    val llm = LlmInference.createFromOptions(context, options) // Create the LLM inference instance with the specified options.

    override suspend fun analyze(imageUri: android.net.Uri): Reading {
        // 1. extract raw text from the image
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val ocrText = suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { continuation.resume(it.text) }
                .addOnFailureListener { continuation.resume("") }
        }

        // 2. Use Gemma to find the actual reading in the OCR mess
        val prompt = "Extract only the numeric meter reading from this text: \"$ocrText\". Output numbers only."
        val response = llm.generateResponse(prompt)
        val value = response.filter { it.isDigit() }

        return Reading(value = value, timestamp = System.currentTimeMillis(), source = "GEMMA")
    }
}