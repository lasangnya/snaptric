package com.snaptric.ai.mlkit

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.Reading
import kotlinx.coroutines.tasks.await

/**
 * An implementation of [MeterReadingAnalyzer] that uses Google ML Kit's Text Recognition.
 * This analyzer finds the meter counter among the numbers in a bitmap (see [pickMeterNumber]).
 */
class MlKitReadingAnalyzer(private val context: Context) : MeterReadingAnalyzer {
    // Initialize the ML Kit text recognizer with default Latin script options.
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Processes the given [bitmap] to extract a meter reading.
     * Uses Coroutines tasks.await() to handle the asynchronous ML Kit operation.
     */
    override suspend fun analyze(bitmap: Bitmap, fullFrame: Bitmap?): Reading {
        // Prepare the image for ML Kit.
        val image = InputImage.fromBitmap(bitmap, 0)

        // Process the image and wait for the results.
        val result = recognizer.process(image).await()

        // Pick the counter number instead of merging every digit in the frame (serials, labels, decimals).
        val scan = scanMeter(result.textBlocks.flatMap { block -> block.lines.map { it.toOcrLine() } })

        // The serial number is printed elsewhere on the faceplate, so look for it in the whole photo.
        val serialNumber = fullFrame?.let { frame ->
            val full = recognizer.process(InputImage.fromBitmap(frame, 0)).await()
            scanMeter(full.textBlocks.flatMap { block -> block.lines.map { it.toOcrLine() } }).serialNumber
        } ?: scan.serialNumber

        return Reading(
            value = scan.value.orEmpty(),
            timestamp = System.currentTimeMillis(),
            source = "MLKit",
            uncertainDigits = scan.uncertainDigits,
            serialNumber = serialNumber
        )
    }
}

/**
 * Converts an ML Kit line to an [OcrLine], keeping each character's confidence. The text is rebuilt
 * from the symbols (elements separated by spaces) so positions line up with the confidences.
 */
internal fun Text.Line.toOcrLine(): OcrLine {
    val height = boundingBox?.height() ?: 0
    val text = StringBuilder()
    val confidences = mutableListOf<Float>()
    elements.forEachIndexed { index, element ->
        if (index > 0) {
            text.append(' ')
            confidences.add(1f)
        }
        if (element.symbols.isEmpty()) {
            text.append(element.text)
            repeat(element.text.length) { confidences.add(element.confidence) }
        } else {
            element.symbols.forEach { symbol ->
                text.append(symbol.text)
                repeat(symbol.text.length) { confidences.add(symbol.confidence) }
            }
        }
    }
    return OcrLine(text.toString(), height, confidences)
}
