package com.snaptric.core.domain.tariff

import android.graphics.Bitmap
import android.net.Uri

/**
 * Reads the printed text of a bill photo on the device, row by row (see [joinIntoRows]).
 */
interface BillTextReader {
    suspend fun read(bitmap: Bitmap): String
    suspend fun read(uri: Uri): String
}
