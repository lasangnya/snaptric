package com.snaptric

import android.content.Context
import com.snaptric.ai.FallbackReadingAnalyzer
import com.snaptric.ai.litertlm.GemmaModelLocator
import com.snaptric.ai.litertlm.GemmaReadingAnalyzer
import com.snaptric.ai.mlkit.MlKitReadingAnalyzer
import com.snaptric.core.domain.MeterReadingAnalyzer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * AppModule provides global dependencies for the application using Hilt.
 * These dependencies are shared across the entire app lifetime.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule{

    /**
     * Provides the analyzer responsible for extracting text from meter images.
     * ML Kit reads every photo; when it can't find a clear number and a Gemma model is installed,
     * Gemma (via LiteRT-LM) takes a second look on-device.
     */
    @Provides
    @Singleton
    fun provideMeterReadingAnalyzer(@ApplicationContext context: Context) : MeterReadingAnalyzer{
        val gemma = GemmaReadingAnalyzer(GemmaModelLocator(context), context.cacheDir)
        return FallbackReadingAnalyzer(
            primary = MlKitReadingAnalyzer(context),
            fallback = gemma,
            isFallbackAvailable = gemma::isAvailable
        )
    }

    /**
     * Provides a global CoroutineScope for long-running or background tasks
     * that should not be tied to a specific UI lifecycle.
     */
    @Provides
    @Singleton
    fun provideApplicationScope() : CoroutineScope{
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
