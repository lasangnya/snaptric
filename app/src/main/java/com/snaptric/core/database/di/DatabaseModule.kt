package com.snaptric.core.database.di

import android.content.Context
import androidx.room3.Room
import com.snaptric.core.database.SnaptricDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context) : SnaptricDatabase{
        return Room.databaseBuilder(
            context,
            SnaptricDatabase::class.java,
            "snaptric_database"
        ).build()
    }

    @Provides
    fun provideMeterDao(db : SnaptricDatabase) = db.meterDao()
}