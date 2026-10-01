package com.snaptric

import android.app.Application
import com.snaptric.ai.litertlm.GemmaModelDownloader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * The base Application class for Snaptric.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation, 
 * including a base class for your application that serves as the 
 * application-level dependency container.
 */
@HiltAndroidApp
class SnaptricApplication : Application() {

    @Inject
    lateinit var gemmaModelDownloader: GemmaModelDownloader

    override fun onCreate() {
        super.onCreate()
        // Picks up a model download that finished while the app wasn't running.
        Thread { gemmaModelDownloader.currentState() }.start()
    }
}
