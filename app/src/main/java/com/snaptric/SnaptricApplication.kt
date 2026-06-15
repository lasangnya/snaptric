package com.snaptric

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * The base Application class for Snaptric.
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation, 
 * including a base class for your application that serves as the 
 * application-level dependency container.
 */
@HiltAndroidApp
class SnaptricApplication : Application()
