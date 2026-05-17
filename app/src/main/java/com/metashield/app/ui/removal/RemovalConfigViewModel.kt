package com.metashield.app.ui.removal

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metashield.app.data.model.RemovalOptions
import com.metashield.app.data.repository.MetadataRepository
import com.metashield.app.data.repository.SettingsRepository
import com.metashield.app.domain.usecase.StripMetadataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

data class RemovalConfigUiState(
    val options: RemovalOptions = RemovalOptions.STRIP_ALL,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val navigateToPreview: Pair<String, String>? = null,
    val fileType: com.metashield.app.data.model.FileType = com.metashield.app.data.model.FileType.UNKNOWN,
    val currentMetadata: List<com.metashield.app.data.model.MetadataField> = emptyList(),
    val potentialScore: Int = 100
)

@HiltViewModel
class RemovalConfigViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stripMetadataUseCase: StripMetadataUseCase,
    private val readMetadataUseCase: com.metashield.app.domain.usecase.ReadMetadataUseCase,
    private val metadataRepository: MetadataRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemovalConfigUiState())
    val uiState: StateFlow<RemovalConfigUiState> = _uiState.asStateFlow()

    private var fileUri: Uri? = null

    fun setFile(uri: Uri) {
        fileUri = uri
        viewModelScope.launch {
            try {
                val item = metadataRepository.getFileItem(uri)
                val metadata = readMetadataUseCase(uri).getOrDefault(emptyList())
                _uiState.update { it.copy(
                    fileType = item.fileType,
                    currentMetadata = metadata,
                    potentialScore = calculatePotentialScore(metadata, _uiState.value.options)
                ) }
            } catch (_: Exception) {}
        }
    }

    fun applyPreset(options: RemovalOptions) {
        _uiState.update { it.copy(
            options = options,
            potentialScore = calculatePotentialScore(it.currentMetadata, options)
        ) }
    }

    fun updateOptions(options: RemovalOptions) {
        _uiState.update { it.copy(
            options = options,
            potentialScore = calculatePotentialScore(it.currentMetadata, options)
        ) }
    }

    fun processFile() {
        val uri = fileUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val fileItem = metadataRepository.getFileItem(uri)
                val suffix = settingsRepository.filenameSuffix.first()

                // Create output file in app's cache dir (fallback - ideally use SAF output folder)
                val outputName = "${fileItem.name.substringBeforeLast('.')}${suffix}.${fileItem.name.substringAfterLast('.', "jpg")}"
                val outputFile = java.io.File(context.cacheDir, outputName)
                if (outputFile.exists()) outputFile.delete()
                outputFile.createNewFile()

                val outputUri = Uri.fromFile(outputFile)

                stripMetadataUseCase(uri, _uiState.value.options, outputUri).fold(
                    onSuccess = {
                        val encIn  = URLEncoder.encode(uri.toString(), "UTF-8")
                        val encOut = URLEncoder.encode(outputUri.toString(), "UTF-8")
                        _uiState.update { it.copy(isProcessing = false, navigateToPreview = encIn to encOut) }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isProcessing = false, error = "Failed: ${e.message}") }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun clearNavigation() {
        _uiState.update { it.copy(navigateToPreview = null) }
    }

    private fun calculatePotentialScore(
        fields: List<com.metashield.app.data.model.MetadataField>,
        options: RemovalOptions
    ): Int {
        // Find which fields WILL remain after stripping
        val remainingImpact = fields.sumOf { field ->
            val willBeRemoved = when (field.category) {
                com.metashield.app.data.model.MetadataCategory.LOCATION -> options.removeLocation || options.removeAll
                com.metashield.app.data.model.MetadataCategory.DEVICE -> options.removeDevice || options.removeAll
                com.metashield.app.data.model.MetadataCategory.TIMESTAMPS -> options.removeTimestamps || options.removeAll
                com.metashield.app.data.model.MetadataCategory.CAMERA -> options.removeCamera || options.removeAll
                com.metashield.app.data.model.MetadataCategory.COPYRIGHT -> options.removeCopyright || options.removeAll
                com.metashield.app.data.model.MetadataCategory.TECHNICAL -> options.removeTechnical || options.removeAll
                com.metashield.app.data.model.MetadataCategory.CUSTOM -> options.removeCustom || options.removeAll
                else -> options.removeAll
            }
            if (willBeRemoved) 0 else field.privacyImpact
        }
        return (100 - remainingImpact).coerceIn(0, 100)
    }
}
