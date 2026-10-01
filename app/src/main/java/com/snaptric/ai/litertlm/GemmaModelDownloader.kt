package com.snaptric.ai.litertlm

import android.app.DownloadManager
import android.content.Context
import androidx.core.content.edit
import androidx.core.net.toUri
import java.io.File

/**
 * Downloads the on-device Gemma model with the system DownloadManager, so the download survives
 * the app being closed and shows in the notification shade.
 *
 * The file is written under a temporary name and only renamed to `.litertlm` once complete, so
 * [GemmaModelLocator] never sees a partial model.
 */
class GemmaModelDownloader(
    private val context: Context,
    private val locator: GemmaModelLocator
) {
    sealed interface State {
        data object NotInstalled : State
        data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : State {
            val progress: Float? get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else null
        }
        data class Installed(val file: File) : State
        data object Failed : State
    }

    private val downloadManager = context.getSystemService(DownloadManager::class.java)
    private val prefs = context.getSharedPreferences("gemma_model", Context.MODE_PRIVATE)

    /**
     * Looks up the current state. When a download has just finished, this moves the file into place,
     * so it's safe (and cheap) to call from anywhere: Settings, app start, or the completion broadcast.
     */
    @Synchronized
    fun currentState(): State {
        val id = prefs.getLong(KEY_DOWNLOAD_ID, NO_DOWNLOAD)
        if (id != NO_DOWNLOAD) {
            downloadManager.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val done = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> finishDownload(localUri)
                        DownloadManager.STATUS_FAILED -> {
                            clearDownload(id)
                            // Remembered until the next attempt, so the error stays on screen.
                            prefs.edit { putBoolean(KEY_FAILED, true) }
                        }
                        else -> return State.Downloading(done, total)
                    }
                } else {
                    // The user cancelled it from the notification, or the system dropped it.
                    prefs.edit { remove(KEY_DOWNLOAD_ID) }
                }
            }
        }
        locator.findModel()?.let { return State.Installed(it) }
        return if (prefs.getBoolean(KEY_FAILED, false)) State.Failed else State.NotInstalled
    }

    /**
     * Starts downloading the default model. By default only over Wi-Fi, since it's several gigabytes.
     * Does nothing if a download is already running.
     */
    @Synchronized
    fun start(allowMobileData: Boolean = false) {
        if (currentState() is State.Downloading) return
        val dir = locator.modelDir ?: return
        File(dir, TEMP_NAME).delete()
        val request = DownloadManager.Request(MODEL_URL.toUri())
            .setTitle("Gemma model for Snaptric")
            .setDescription("On-device AI for usage summaries")
            .setDestinationInExternalFilesDir(context, GemmaModelLocator.MODEL_DIR_NAME, TEMP_NAME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(allowMobileData)
            .setAllowedOverRoaming(false)
        prefs.edit {
            putLong(KEY_DOWNLOAD_ID, downloadManager.enqueue(request))
            remove(KEY_FAILED)
        }
    }

    /** Cancels a running download and removes the partial file. */
    @Synchronized
    fun cancel() {
        val id = prefs.getLong(KEY_DOWNLOAD_ID, NO_DOWNLOAD)
        if (id != NO_DOWNLOAD) clearDownload(id)
    }

    /** Deletes the installed model to free space. */
    fun delete() {
        locator.modelDir?.listFiles { file -> file.extension == "litertlm" }?.forEach { it.delete() }
    }

    private fun finishDownload(localUri: String?) {
        prefs.edit { remove(KEY_DOWNLOAD_ID) }
        val dir = locator.modelDir ?: return
        // Use the file DownloadManager actually wrote (it may have renamed it to avoid a clash).
        val downloaded = localUri?.toUri()?.path?.let(::File)?.takeIf { it.exists() } ?: File(dir, TEMP_NAME)
        if (downloaded.exists()) downloaded.renameTo(File(dir, MODEL_FILE_NAME))
    }

    private fun clearDownload(id: Long) {
        downloadManager.remove(id) // also deletes the partial file
        prefs.edit { remove(KEY_DOWNLOAD_ID) }
    }

    companion object {
        /** Gemma 4 E2B (Apache 2.0) in LiteRT-LM format, published by Google's LiteRT community. */
        const val MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
        const val MODEL_SIZE_BYTES = 2_588_147_712L

        private const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"
        private const val TEMP_NAME = "$MODEL_FILE_NAME.download"
        private const val KEY_DOWNLOAD_ID = "download_id"
        private const val KEY_FAILED = "last_download_failed"
        private const val NO_DOWNLOAD = -1L
    }
}
