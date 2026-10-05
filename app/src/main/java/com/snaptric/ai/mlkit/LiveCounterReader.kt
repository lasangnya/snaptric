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
data class LiveFrame(val value: String?, val tooDark: Boolean, val image: Bitmap? = null) {
    // Frames are compared by what was read, not by the image.
    override fun equals(other: Any?) = other is LiveFrame && other.value == value && other.tooDark == tooDark
    override fun hashCode() = 31 * (value?.hashCode() ?: 0) + tooDark.hashCode()
}

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

    // True while a recognition is running; frames arriving meanwhile are skipped so work can't pile up.
    @Volatile
    private var busy = false

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (busy || now - lastRun < MIN_INTERVAL_MS) {
            image.close()
            return
        }
        lastRun = now

        val tooDark = averageLuma(image) < DARK_LUMA
        val rotation = image.imageInfo.rotationDegrees
        val frame = image.toBitmap().visiblePart(image.cropRect).let { raw ->
            if (rotation == 0) raw
            else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
        }
        image.close()

        val target = cropToTarget(frame)
        busy = true
        recognizer.process(InputImage.fromBitmap(target, 0))
            .addOnSuccessListener { text -> onFrame(LiveFrame(text.toMeterScan(target).value, tooDark, target)) }
            .addOnFailureListener { onFrame(LiveFrame(null, tooDark)) }
            .addOnCompleteListener { busy = false }
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
        const val MIN_INTERVAL_MS = 250L
        const val DARK_LUMA = 60
        const val LUMA_STEP = 97 // sample, don't read every byte
    }
}

/**
 * Decides when the viewfinder has seen the same counter reading long enough to capture.
 */
class StableReadingDetector(private val requiredFrames: Int = 2) {
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

/**
 * Decides, frame by frame, whether auto-capture should take a photo now.
 *
 * It fires once the reading is steady, only while the camera is [ready] (not analysing or showing
 * the confirmation sheet), and never twice for the same value in a row, so dismissing the sheet
 * with the camera still on the meter doesn't immediately capture again.
 */
class AutoCaptureGate(private val detector: StableReadingDetector = StableReadingDetector()) {
    private var lastCaptured: String? = null

    fun shouldCapture(value: String?, ready: Boolean): Boolean {
        if (!ready) {
            detector.reset()
            return false
        }
        val stable = detector.offer(value) ?: return false
        if (stable == lastCaptured) return false
        lastCaptured = stable
        return true
    }

    fun reset() = detector.reset()
}

/**
 * The part of a camera frame that was visible on screen. CameraX reports it as the crop rect
 * when the use cases share the preview's view port; [ImageProxy.toBitmap] doesn't apply it.
 */
fun Bitmap.visiblePart(cropRect: android.graphics.Rect): Bitmap {
    if (cropRect.width() >= width && cropRect.height() >= height) return this
    val left = cropRect.left.coerceIn(0, width - 1)
    val top = cropRect.top.coerceIn(0, height - 1)
    return Bitmap.createBitmap(
        this, left, top,
        cropRect.width().coerceAtMost(width - left),
        cropRect.height().coerceAtMost(height - top)
    )
}
