package com.snaptric.ai.litertlm

import android.content.Context
import java.io.File

/**
 * Finds an on-device Gemma model in LiteRT-LM format (`.litertlm`).
 *
 * Models are over a gigabyte, so they are never bundled in the APK. During development, push one with:
 * `adb push gemma.litertlm /sdcard/Android/data/com.snaptric/files/models/`
 */
class GemmaModelLocator(private val context: Context) {

    /** Directory the app looks in for models; no storage permission is needed for it. */
    val modelDir: File?
        get() = context.getExternalFilesDir(MODEL_DIR_NAME)

    /** The model to use, or null when no valid model has been installed. */
    fun findModel(): File? =
        modelDir
            ?.listFiles { file -> file.isFile && file.extension == "litertlm" && hasLiteRtLmHeader(file) }
            ?.maxByOrNull { it.lastModified() }

    companion object {
        const val MODEL_DIR_NAME = "models"
    }
}

private val LITERTLM_MAGIC = "LITERTLM".toByteArray(Charsets.US_ASCII)

/**
 * True when [file] starts with the LiteRT-LM header magic.
 *
 * LiteRT-LM 0.17.1 aborts the whole process (instead of throwing) when handed a file that isn't a
 * model, so anything else in the models folder must be filtered out before it reaches the engine.
 */
fun hasLiteRtLmHeader(file: File): Boolean =
    try {
        file.inputStream().use { input ->
            val header = ByteArray(LITERTLM_MAGIC.size)
            input.read(header) == header.size && header.contentEquals(LITERTLM_MAGIC)
        }
    } catch (e: java.io.IOException) {
        false
    }
