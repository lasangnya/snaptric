package com.snaptric.ai.helper

import android.content.Context
import java.io.File

/**
 * Copies a file from the app's assets folder to the internal 'files' directory.
 * This is necessary for libraries (like MediaPipe or ML Kit) that require a local file path
 * instead of an InputStream from assets.
 *
 * @param context The application context.
 * @param assetName The name of the file in the assets folder.
 * @return The File object representing the copied file in the internal storage.
 */
fun copyAssetToFiles(
    context: Context,
    assetName : String
) : File {
    val outFile = File(context.filesDir, assetName)

    // Check if the file already exists and has a reasonable size.
    // This avoids expensive re-copying on every app launch.
    // (e.g., Gemma model is approx 1.3GB)
    if (outFile.exists() && outFile.length() > 1_200_000_000) {
        return outFile
    }

    // If the file is missing or appears corrupted (too small), delete and re-copy.
    outFile.delete()

    context.assets.open(assetName).use { inputStream ->
        outFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
    return outFile
}
