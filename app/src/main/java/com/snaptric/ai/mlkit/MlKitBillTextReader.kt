package com.snaptric.ai.mlkit

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.snaptric.core.domain.tariff.BillTextReader
import com.snaptric.core.domain.tariff.TextLine
import com.snaptric.core.domain.tariff.joinIntoRows
import kotlinx.coroutines.tasks.await

/**
 * [BillTextReader] backed by ML Kit text recognition, which runs entirely on the device.
 */
class MlKitBillTextReader(private val context: Context) : BillTextReader {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun read(bitmap: Bitmap): String =
        recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().toRows()

    override suspend fun read(uri: Uri): String =
        recognizer.process(InputImage.fromFilePath(context, uri)).await().toRows()
}

/** Rebuilds the page's rows from ML Kit's blocks, so table labels and values stay together. */
private fun Text.toRows(): String {
    val lines = textBlocks.flatMap { it.lines }.mapNotNull { line ->
        val box = line.boundingBox ?: return@mapNotNull null
        TextLine(line.text, box.left, box.top, box.right, box.bottom)
    }
    return if (lines.isEmpty()) text else joinIntoRows(lines)
}
