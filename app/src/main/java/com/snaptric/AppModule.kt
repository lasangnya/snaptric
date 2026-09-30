package com.snaptric

import android.content.Context
import com.snaptric.ai.insights.GemmaInsightWriter
import com.snaptric.ai.litertlm.GemmaModelLocator
import com.snaptric.ai.litertlm.GemmaTextEngine
import com.snaptric.ai.mlkit.MlKitReadingAnalyzer
import com.snaptric.core.domain.insights.InsightWriter
import com.snaptric.core.domain.insights.TemplateInsightWriter
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
     * Uses ML Kit as the underlying engine.
     */
    @Provides
    @Singleton
    fun provideMeterReadingAnalyzer(@ApplicationContext context: Context) : MeterReadingAnalyzer{
        return MlKitReadingAnalyzer(context)
    }

    /**
     * Provides the on-device Gemma engine (LiteRT-LM). It only loads a model if one is installed.
     */
    @Provides
    @Singleton
    fun provideGemmaTextEngine(@ApplicationContext context: Context) : GemmaTextEngine{
        return GemmaTextEngine(GemmaModelLocator(context), context.cacheDir)
    }

    /**
     * Provides the writer for usage summaries: Gemma rewords the calculated facts when a model is
     * installed, otherwise the built-in template is used.
     */
    @Provides
    @Singleton
    fun provideInsightWriter(gemma: GemmaTextEngine) : InsightWriter{
        return GemmaInsightWriter(gemma::generate, TemplateInsightWriter())
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
