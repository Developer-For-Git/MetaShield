package com.metashield.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "metashield_settings"
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val OUTPUT_FOLDER_URI = stringPreferencesKey("output_folder_uri")
        val FILENAME_SUFFIX = stringPreferencesKey("filename_suffix")
        /** SYSTEM | LIGHT | DARK */
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val DEFAULT_PROFILE_ID = longPreferencesKey("default_profile_id")
        val MAX_THREADS = intPreferencesKey("max_threads")
    }

    val outputFolderUri: Flow<String?> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[OUTPUT_FOLDER_URI] }

    val filenameSuffix: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[FILENAME_SUFFIX] ?: "_clean" }

    val themeMode: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[THEME_MODE] ?: "SYSTEM" }

    val autoBackup: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[AUTO_BACKUP] ?: false }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[APP_LOCK_ENABLED] ?: false }

    val maxThreads: Flow<Int> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[MAX_THREADS] ?: 4 }

    suspend fun setOutputFolderUri(uri: String) {
        context.dataStore.edit { it[OUTPUT_FOLDER_URI] = uri }
    }

    suspend fun setFilenameSuffix(suffix: String) {
        context.dataStore.edit { it[FILENAME_SUFFIX] = suffix }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setAutoBackup(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_BACKUP] = enabled }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setMaxThreads(threads: Int) {
        context.dataStore.edit { it[MAX_THREADS] = threads }
    }
}
