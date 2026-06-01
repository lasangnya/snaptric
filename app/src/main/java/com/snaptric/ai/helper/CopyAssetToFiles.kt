package com.snaptric.ai.helper

import android.content.Context
import java.io.File

fun copyAssetToFiles(
    context: Context,
    assetName : String
) : File {
    val outFile = File(context.filesDir, assetName)
    if(!outFile.exists()){
        context.assets.open(assetName).use { inputStream ->
            outFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
    return outFile
}