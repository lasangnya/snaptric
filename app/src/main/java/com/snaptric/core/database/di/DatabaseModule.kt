package com.snaptric.core.database.di

import android.content.Context
import androidx.room.Room
import com.snaptric.core.database.SnaptricDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides the singleton instance of the [SnaptricDatabase].
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context) : SnaptricDatabase{
        return Room.databaseBuilder(
            context,
            SnaptricDatabase::class.java,
            "snaptric_database"
        ).build()
    }

    /**
     * Provides the [MeterDao] for performing database operations on meters and readings.
     */
    @Provides
    fun provideMeterDao(db : SnaptricDatabase) = db.meterDao()
}
