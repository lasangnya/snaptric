package com.snaptric.ai.helper

import android.content.Context
import java.io.File

fun copyAssetToFiles(
    context: Context,
    assetName : String
) : File {
    val outFile = File(context.filesDir, assetName)

    // Check if file exists AND has the expected size (approx 1.3GB)
    if (outFile.exists() && outFile.length() > 1_200_000_000) {
        return outFile
    }

    // If it's broken or missing, delete and recopy
    outFile.delete()

    context.assets.open(assetName).use { inputStream ->
        outFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
    return outFile
}