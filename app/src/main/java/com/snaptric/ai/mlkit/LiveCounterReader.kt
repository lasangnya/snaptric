package com.snaptric.ai.mlkit

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * What the viewfinder currently sees: the counter digits in the target strip, and whether the
 * scene is too dark to read reliably.
 */
data class LiveFrame(val value: String?, val tooDark: Boolean)

/**
 * Reads the counter from camera preview frames (a CameraX [ImageAnalysis.Analyzer]).
 * Frames are throttled so recognition doesn't compete with the preview.
 */
class LiveCounterReader(
    private val cropToTarget: (Bitmap) -> Bitmap,
    private val onFrame: (LiveFrame) -> Unit
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastRun = 0L

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastRun < MIN_INTERVAL_MS) {
            image.close()
            return
        }
        lastRun = now

        val tooDark = averageLuma(image) < DARK_LUMA
        val rotation = image.imageInfo.rotationDegrees
        val frame = image.toBitmap().let { raw ->
            if (rotation == 0) raw
            else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
        }
        image.close()

        recognizer.process(InputImage.fromBitmap(cropToTarget(frame), 0))
            .addOnSuccessListener { text ->
                val value = scanMeter(text.textBlocks.flatMap { block -> block.lines.map { it.toOcrLine() } }).value
                onFrame(LiveFrame(value, tooDark))
            }
            .addOnFailureListener { onFrame(LiveFrame(null, tooDark)) }
    }

    fun close() = recognizer.close()

    /** Average brightness (0-255) from a sample of the Y plane. */
    private fun averageLuma(image: ImageProxy): Int {
        val plane = image.planes.firstOrNull() ?: return 255
        val buffer = plane.buffer.duplicate()
        val size = buffer.remaining()
        if (size == 0) return 255
        var sum = 0L
        var count = 0
        var index = 0
        while (index < size) {
            sum += buffer.get(index).toInt() and 0xFF
            count++
            index += LUMA_STEP
        }
        return (sum / count).toInt()
    }

    private companion object {
        const val MIN_INTERVAL_MS = 400L
        const val DARK_LUMA = 60
        const val LUMA_STEP = 97 // sample, don't read every byte
    }
}

/**
 * Decides when the viewfinder has seen the same counter reading long enough to capture.
 */
class StableReadingDetector(private val requiredFrames: Int = 3) {
    private var last: String? = null
    private var streak = 0

    /**
     * Records the latest frame's value. Returns the value once it has been the same, with at least
     * four digits, for [requiredFrames] frames in a row; after that the streak starts over.
     */
    fun offer(value: String?): String? {
        if (value == null || value.count(Char::isDigit) < 4) {
            last = null
            streak = 0
            return null
        }
        streak = if (value == last) streak + 1 else 1
        last = value
        if (streak >= requiredFrames) {
            streak = 0
            return value
        }
        return null
    }

    fun reset() {
        last = null
        streak = 0
    }
}
