package com.snaptric.ai.mlkit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.core.graphics.get
import androidx.core.graphics.scale
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.snaptric.core.domain.MeterReadingAnalyzer
import com.snaptric.core.domain.Reading
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
        // Read the counter strip and, in parallel, the whole photo (for the serial number, which is
        // printed elsewhere on the faceplate).
        val (scan, fullScan) = coroutineScope {
            val strip = async { recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().toMeterScan(bitmap) }
            // The serial is printed large enough to read from a reduced copy, which is much faster.
            val full = fullFrame?.let { frame ->
                async {
                    val small = frame.scaledToMaxSide(SERIAL_PASS_MAX_SIDE)
                    recognizer.process(InputImage.fromBitmap(small, 0)).await().toMeterScan(small)
                }
            }
            strip.await() to full?.await()
        }
        val serialNumber = fullScan?.serialNumber ?: scan.serialNumber

        return Reading(
            value = scan.value.orEmpty(),
            timestamp = System.currentTimeMillis(),
            source = "MLKit",
            uncertainDigits = scan.uncertainDigits,
            serialNumber = serialNumber
        )
    }
}

/** Longest side of the copy of the whole photo used to look for the serial number. */
private const val SERIAL_PASS_MAX_SIDE = 1600

/** Picks the counter, uncertain digits and serial number from a recognition result. */
internal fun Text.toMeterScan(source: Bitmap): MeterScan =
    scanMeter(textBlocks.flatMap { block -> block.lines.map { it.toOcrLine(source) } })

/**
 * Converts an ML Kit line to an [OcrLine], keeping each character's confidence and position. The
 * text is rebuilt from the symbols (elements separated by spaces) so positions line up. With the
 * [source] image, it also finds where a red section (decimal wheels) starts on the line.
 */
internal fun Text.Line.toOcrLine(source: Bitmap? = null): OcrLine {
    // Text height: the box's short side, so a vertical label doesn't look like huge text.
    val height = boundingBox?.let { minOf(it.width(), it.height()) } ?: 0
    val text = StringBuilder()
    val confidences = mutableListOf<Float>()
    val centers = mutableListOf<Float>()
    elements.forEachIndexed { index, element ->
        if (index > 0) {
            text.append(' ')
            confidences.add(1f)
            centers.add(element.boundingBox?.left?.toFloat() ?: centers.lastOrNull() ?: 0f)
        }
        if (element.symbols.isEmpty()) {
            text.append(element.text)
            val box = element.boundingBox
            element.text.indices.forEach { i ->
                confidences.add(element.confidence)
                centers.add(box?.let { it.left + (i + 0.5f) * it.width() / element.text.length } ?: 0f)
            }
        } else {
            element.symbols.forEach { symbol ->
                text.append(symbol.text)
                repeat(symbol.text.length) {
                    confidences.add(symbol.confidence)
                    centers.add(symbol.boundingBox?.exactCenterX() ?: 0f)
                }
            }
        }
    }
    return OcrLine(
        text = text.toString(),
        height = height,
        charConfidences = confidences,
        charCenters = centers,
        decimalFromX = source?.let { bitmap -> boundingBox?.let { box -> redSectionStart(bitmap, box) } }
    )
}

/**
 * Finds where a red section starts on a counter, scanning the middle of [box] in [bitmap].
 * Returns null unless the red part runs to the right end and covers a meaningful share of it,
 * as the red decimal wheels on gas and water meters do.
 */
internal fun redSectionStart(bitmap: Bitmap, box: Rect): Float? {
    val left = box.left.coerceIn(0, bitmap.width - 1)
    val right = box.right.coerceIn(left + 1, bitmap.width)
    val top = (box.top + box.height() / 4).coerceIn(0, bitmap.height - 1)
    val bottom = (box.bottom - box.height() / 4).coerceIn(top + 1, bitmap.height)
    val width = right - left
    if (width < 8) return null

    val rows = (bottom - top).coerceAtMost(12)
    val red = BooleanArray(width) { column ->
        var redPixels = 0
        for (r in 0 until rows) {
            val y = top + r * (bottom - top) / rows
            val pixel = bitmap[left + column, y]
            val (rr, g, b) = Triple(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
            if (rr > 90 && rr > g * 1.5f && rr > b * 1.5f) redPixels++
        }
        redPixels * 3 >= rows // at least a third of the column is red
    }

    // Walk left from the right edge while the columns are mostly red.
    var start = width
    var gap = 0
    for (column in width - 1 downTo 0) {
        if (red[column]) {
            start = column
            gap = 0
        } else if (++gap > width / 20) {
            break
        }
    }
    val redShare = width - start
    return if (redShare >= width / 8 && redShare <= width * 2 / 3) (left + start).toFloat() else null
}

private fun Bitmap.scaledToMaxSide(maxSide: Int): Bitmap {
    val longest = maxOf(width, height)
    if (longest <= maxSide) return this
    val scale = maxSide.toFloat() / longest
    return scale((width * scale).toInt(), (height * scale).toInt())
}
