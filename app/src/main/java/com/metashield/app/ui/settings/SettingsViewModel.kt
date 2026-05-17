package com.metashield.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metashield.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import javax.inject.Inject

data class SettingsUiState(
    val outputFolderUri: String? = null,
    val filenameSuffix: String = "_clean",
    val themeMode: String = "SYSTEM",
    val autoBackup: Boolean = false,
    val appLockEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.outputFolderUri,
        settingsRepository.filenameSuffix,
        settingsRepository.themeMode,
        settingsRepository.autoBackup,
        settingsRepository.appLockEnabled
    ) { folder, suffix, theme, backup, lock ->
        SettingsUiState(
            outputFolderUri = folder,
            filenameSuffix = suffix,
            themeMode = theme,
            autoBackup = backup,
            appLockEnabled = lock
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setOutputFolder(uri: String)    = viewModelScope.launch { settingsRepository.setOutputFolderUri(uri) }
    fun setFilenameSuffix(suffix: String) = viewModelScope.launch { settingsRepository.setFilenameSuffix(suffix) }
    fun setThemeMode(mode: String)      = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setAutoBackup(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAutoBackup(enabled) }
    fun setAppLockEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setAppLockEnabled(enabled) }

    fun exportSettings(uri: android.net.Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonStr = com.google.gson.Gson().toJson(uiState.value)
                context.contentResolver.openOutputStream(uri)?.use { 
                    it.write(jsonStr.toByteArray()) 
                }
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Settings exported successfully", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
