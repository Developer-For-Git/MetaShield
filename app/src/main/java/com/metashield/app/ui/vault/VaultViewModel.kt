package com.metashield.app.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metashield.app.data.db.entity.VaultEntity
import com.metashield.app.data.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultUiState(
    val entries: List<VaultEntity> = emptyList(),
    val isLocked: Boolean = true,
    val isDecoyMode: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val vaultRepository: VaultRepository,
    private val metadataRepository: com.metashield.app.data.repository.MetadataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private var entriesJob: kotlinx.coroutines.Job? = null

    init {
        observeVault(false)
    }

    private fun observeVault(isDecoy: Boolean) {
        entriesJob?.cancel()
        entriesJob = viewModelScope.launch {
            vaultRepository.getVaultEntries(isDecoy).collect { entries ->
                _uiState.update { it.copy(entries = entries, isDecoyMode = isDecoy) }
            }
        }
    }

    fun unlock(isDecoy: Boolean = false) {
        observeVault(isDecoy)
        _uiState.update { it.copy(isLocked = false, isDecoyMode = isDecoy) }
    }

    fun lock() {
        _uiState.update { it.copy(isLocked = true) }
    }

    fun deleteEntry(entry: VaultEntity) {
        viewModelScope.launch {
            vaultRepository.deleteFromVault(entry)
        }
    }

    fun exportToGallery(entry: VaultEntity) {
        viewModelScope.launch {
            val file = vaultRepository.getVaultFile(entry) ?: return@launch
            val uri = android.net.Uri.fromFile(file)
            val fileItem = metadataRepository.getFileItem(uri)
            
            val result = metadataRepository.saveToPublicStorage(uri, fileItem)
            result.fold(
                onSuccess = {
                    android.widget.Toast.makeText(context, "Exported to Gallery", android.widget.Toast.LENGTH_SHORT).show()
                },
                onFailure = { e ->
                    android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    fun shareFile(entry: VaultEntity) {
        viewModelScope.launch {
            val file = vaultRepository.getVaultFile(entry) ?: return@launch
            try {
                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = entry.mimeType ?: "*/*"
                    putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = android.content.Intent.createChooser(shareIntent, "Share protected file").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Share failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
