package com.metashield.app.di

import android.content.Context
import androidx.room.Room
import com.metashield.app.data.db.AppDatabase
import com.metashield.app.data.db.dao.HistoryDao
import com.metashield.app.data.db.dao.RemovalProfileDao
import com.metashield.app.data.db.dao.TemplateDao
import com.metashield.app.data.db.dao.VaultDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "metashield.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideTemplateDao(db: AppDatabase): TemplateDao = db.templateDao()

    @Provides
    fun provideRemovalProfileDao(db: AppDatabase): RemovalProfileDao = db.removalProfileDao()

    @Provides
    fun provideVaultDao(db: AppDatabase): VaultDao = db.vaultDao()
}
