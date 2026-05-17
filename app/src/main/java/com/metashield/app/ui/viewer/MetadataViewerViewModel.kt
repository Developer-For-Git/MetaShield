package com.metashield.app.ui.viewer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metashield.app.data.model.MetadataField
import com.metashield.app.data.repository.MetadataRepository
import com.metashield.app.domain.usecase.ReadMetadataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MetadataViewerUiState(
    val isLoading: Boolean = false,
    val fields: List<MetadataField> = emptyList(),
    val fileName: String = "",
    val fileType: com.metashield.app.data.model.FileType = com.metashield.app.data.model.FileType.UNKNOWN,
    val searchQuery: String = "",
    val privacyScore: Int = 100,
    val error: String? = null
)

@HiltViewModel
class MetadataViewerViewModel @Inject constructor(
    private val readMetadataUseCase: com.metashield.app.domain.usecase.ReadMetadataUseCase,
    val repository: com.metashield.app.data.repository.MetadataRepository,
    val vaultRepository: com.metashield.app.data.repository.VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetadataViewerUiState())
    val uiState: StateFlow<MetadataViewerUiState> = _uiState.asStateFlow()

    private var allFields: List<MetadataField> = emptyList()

    fun loadMetadata(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val fileItem = repository.getFileItem(uri)
                readMetadataUseCase(uri).fold(
                    onSuccess = { fields ->
                        allFields = fields
                        _uiState.update { it.copy(
                            isLoading = false,
                            fields = fields,
                            fileName = fileItem.name,
                            fileType = fileItem.fileType,
                            privacyScore = calculateScore(fields)
                        ) }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to read metadata") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isEmpty()) allFields
            else allFields.filter {
                it.tag.contains(query, ignoreCase = true) || it.value.contains(query, ignoreCase = true)
            }
            state.copy(searchQuery = query, fields = filtered)
        }
    }

    private fun calculateScore(fields: List<MetadataField>): Int {
        val impact = fields.sumOf { it.privacyImpact }
        return (100 - impact).coerceIn(0, 100)
    }
}
