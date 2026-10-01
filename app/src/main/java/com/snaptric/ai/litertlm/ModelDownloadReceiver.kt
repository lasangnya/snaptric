package com.snaptric.ai.litertlm

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Moves the Gemma model into place as soon as its download finishes, even if Settings isn't open.
 */
@AndroidEntryPoint
class ModelDownloadReceiver : BroadcastReceiver() {

    @Inject
    lateinit var downloader: GemmaModelDownloader

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val pending = goAsync()
        Thread {
            try {
                downloader.currentState()
            } finally {
                pending.finish()
            }
        }.start()
    }
}
