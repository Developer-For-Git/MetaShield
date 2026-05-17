package com.metashield.app.ui.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metashield.app.data.model.MetadataCategory
import com.metashield.app.data.model.MetadataField
import com.metashield.app.data.model.SensitivityLevel
import com.metashield.app.data.repository.MetadataRepository
import com.metashield.app.data.repository.SettingsRepository
import com.metashield.app.domain.usecase.ReadMetadataUseCase
import com.metashield.app.domain.usecase.WriteMetadataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

data class MetadataEditorUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val fileName: String = "",
    val editableFields: List<MetadataField> = emptyList(),
    val canUndo: Boolean = false,
    val error: String? = null,
    val navigateToPreview: Pair<String, String>? = null
)

@HiltViewModel
class MetadataEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val readMetadataUseCase: ReadMetadataUseCase,
    private val writeMetadataUseCase: WriteMetadataUseCase,
    private val metadataRepository: MetadataRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetadataEditorUiState())
    val uiState: StateFlow<MetadataEditorUiState> = _uiState.asStateFlow()

    private var fileUri: Uri? = null
    private val undoStack = ArrayDeque<List<MetadataField>>()

    fun loadFile(uri: Uri) {
        fileUri = uri
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val fileItem = metadataRepository.getFileItem(uri)
                readMetadataUseCase(uri).fold(
                    onSuccess = { fields ->
                        _uiState.update { it.copy(isLoading = false, fileName = fileItem.name, editableFields = fields) }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateField(index: Int, newValue: String) {
        val current = _uiState.value.editableFields.toMutableList()
        if (index in current.indices) {
            pushUndo(current)
            current[index] = current[index].copy(value = newValue)
            _uiState.update { it.copy(editableFields = current, canUndo = true) }
        }
    }

    fun removeField(index: Int) {
        val current = _uiState.value.editableFields.toMutableList()
        if (index in current.indices) {
            pushUndo(current)
            current.removeAt(index)
            _uiState.update { it.copy(editableFields = current, canUndo = true) }
        }
    }

    fun addField(key: String, tag: String, value: String) {
        val current = _uiState.value.editableFields.toMutableList()
        pushUndo(current)
        current.add(
            MetadataField(key = key, tag = tag, value = value,
                category = MetadataCategory.CUSTOM, sensitivityLevel = SensitivityLevel.LOW)
        )
        _uiState.update { it.copy(editableFields = current, canUndo = true) }
    }

    fun undoLast() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeLast()
            _uiState.update { it.copy(editableFields = prev, canUndo = undoStack.isNotEmpty()) }
        }
    }

    fun saveFile() {
        val uri = fileUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val fileItem = metadataRepository.getFileItem(uri)
                val suffix = settingsRepository.filenameSuffix.first()
                val outputName = "${fileItem.name.substringBeforeLast('.')}${suffix}.${fileItem.name.substringAfterLast('.', "jpg")}"
                val outputFile = java.io.File(context.cacheDir, outputName)
                if (outputFile.exists()) outputFile.delete()
                outputFile.createNewFile()
                val outputUri = Uri.fromFile(outputFile)

                writeMetadataUseCase(uri, _uiState.value.editableFields, outputUri).fold(
                    onSuccess = {
                        val encIn  = URLEncoder.encode(uri.toString(), "UTF-8")
                        val encOut = URLEncoder.encode(outputUri.toString(), "UTF-8")
                        _uiState.update { it.copy(isSaving = false, navigateToPreview = encIn to encOut) }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isSaving = false, error = "Save failed: ${e.message}") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun clearNavigation() {
        _uiState.update { it.copy(navigateToPreview = null) }
    }

    private fun pushUndo(snapshot: List<MetadataField>) {
        undoStack.addLast(snapshot.map { it.copy() })
        if (undoStack.size > 30) undoStack.removeFirst()
    }
}
