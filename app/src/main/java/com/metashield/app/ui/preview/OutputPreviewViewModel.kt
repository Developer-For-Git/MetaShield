package com.metashield.app.ui.preview

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metashield.app.data.model.FileItem
import com.metashield.app.data.model.FileType
import com.metashield.app.data.repository.MetadataRepository
import com.metashield.app.domain.usecase.ReadMetadataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class OutputPreviewUiState(
    val isLoading: Boolean = false,
    val beforeFieldCount: Int = 0,
    val afterFieldCount: Int = 0,
    val fieldsRemoved: Int = 0,
    val beforeFields: List<com.metashield.app.data.model.MetadataField> = emptyList(),
    val afterFields: List<com.metashield.app.data.model.MetadataField> = emptyList(),
    val outputFileName: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isVaulting: Boolean = false,
    val vaultSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OutputPreviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val readMetadataUseCase: ReadMetadataUseCase,
    private val metadataRepository: MetadataRepository,
    private val vaultRepository: com.metashield.app.data.repository.VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OutputPreviewUiState())
    val uiState: StateFlow<OutputPreviewUiState> = _uiState.asStateFlow()

    fun loadPreview(inputUri: Uri, outputUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val beforeFields = readMetadataUseCase(inputUri).getOrElse { emptyList() }
                val afterFields  = readMetadataUseCase(outputUri).getOrElse { emptyList() }
                val fileItem = metadataRepository.getFileItem(outputUri)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        beforeFieldCount = beforeFields.size,
                        afterFieldCount  = afterFields.size,
                        fieldsRemoved    = beforeFields.size - afterFields.size,
                        beforeFields     = beforeFields,
                        afterFields      = afterFields,
                        outputFileName   = fileItem.name
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun saveToDevice(outputUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, saveSuccess = false) }
            val fileItem = metadataRepository.getFileItem(outputUri)
            val result = metadataRepository.saveToPublicStorage(outputUri, fileItem)
            
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, error = "Save failed: ${e.message}") }
                }
            )
        }
    }

    fun moveToVault(outputUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isVaulting = true, error = null, vaultSuccess = false) }
            val fileItem = metadataRepository.getFileItem(outputUri)
            val result = vaultRepository.moveToVault(
                uri = outputUri,
                fileName = fileItem.name,
                mimeType = fileItem.mimeType,
                fileType = fileItem.fileType.name,
                originalSize = fileItem.size
            )
            
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isVaulting = false, vaultSuccess = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isVaulting = false, error = "Vaulting failed: ${e.message}") }
                }
            )
        }
    }
}
