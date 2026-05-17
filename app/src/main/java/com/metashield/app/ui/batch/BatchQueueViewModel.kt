package com.metashield.app.ui.batch

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metashield.app.data.model.*
import com.metashield.app.data.repository.MetadataRepository
import com.metashield.app.data.repository.SettingsRepository
import com.metashield.app.domain.usecase.ReadMetadataUseCase
import com.metashield.app.domain.usecase.StripMetadataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.metashield.app.data.db.entity.TemplateEntity

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.net.URLEncoder
import javax.inject.Inject
import com.metashield.app.domain.usecase.WriteMetadataUseCase
import android.widget.Toast

enum class BatchProcessingMode {
    STRIP, APPLY_TEMPLATE
}


data class BatchQueueUiState(
    val queue: List<BatchItem> = emptyList(),
    val progress: BatchProgress = BatchProgress(0, 0, 0),
    val isRunning: Boolean = false,
    val isSavingAll: Boolean = false,
    val saveAllSuccess: Boolean = false,
    val completedItems: List<BatchItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val error: String? = null,
    val mode: BatchProcessingMode = BatchProcessingMode.STRIP,
    val templateToApply: TemplateEntity? = null
)

@HiltViewModel
class BatchQueueViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stripMetadataUseCase: StripMetadataUseCase,
    private val writeMetadataUseCase: WriteMetadataUseCase,
    private val readMetadataUseCase: ReadMetadataUseCase,
    private val metadataRepository: MetadataRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BatchQueueUiState())
    val uiState: StateFlow<BatchQueueUiState> = _uiState.asStateFlow()

    private var batchJob: Job? = null

    suspend fun addFile(uri: Uri) {
        val fileItem = metadataRepository.getFileItem(uri)
        val originalFields = readMetadataUseCase(uri).getOrDefault(emptyList())
        val item = BatchItem(
            fileItem = fileItem,
            originalFieldsCount = originalFields.size
        )
        _uiState.update { it.copy(queue = it.queue + item) }
    }

    suspend fun addFiles(uris: List<Uri>) {
        val newItems = uris.map { uri ->
            val fileItem = metadataRepository.getFileItem(uri)
            val originalFields = readMetadataUseCase(uri).getOrDefault(emptyList())
            BatchItem(
                fileItem = fileItem,
                originalFieldsCount = originalFields.size
            )
        }
        _uiState.update { it.copy(queue = it.queue + newItems) }
    }

    fun removeFile(id: String) {
        _uiState.update { it.copy(queue = it.queue.filter { item -> item.id != id }) }
    }

    fun clearQueue() {
        _uiState.update { 
            it.copy(
                queue = emptyList(), 
                progress = BatchProgress(0, 0, 0),
                selectedIds = emptySet(),
                saveAllSuccess = false,
                isSavingAll = false,
                mode = BatchProcessingMode.STRIP,
                templateToApply = null,
                error = null
            ) 
        }
    }

    fun setMode(mode: BatchProcessingMode, template: TemplateEntity? = null) {
        _uiState.update { it.copy(mode = mode, templateToApply = template) }
    }

    fun startBatch() {
        if (_uiState.value.isRunning) return
        val queue = _uiState.value.queue
        if (queue.isEmpty()) return

        val state = _uiState.value
        _uiState.update { it.copy(isRunning = true, progress = BatchProgress(queue.size, 0, 0)) }
        com.metashield.app.service.BatchForegroundService.start(context)

        batchJob = viewModelScope.launch {
            val options = RemovalOptions.STRIP_ALL
            var completed = 0
            var failed = 0

            // Pre-process template fields if applying
            val templateFields = state.templateToApply?.let {
                val array = com.google.gson.Gson().fromJson(it.fieldsJson, Array<MetadataField>::class.java)
                array?.toList()
            } ?: emptyList()

            // Update all to PENDING
            _uiState.update { s ->
                s.copy(queue = s.queue.map { it.copy(status = BatchItemStatus.PENDING) })
            }

            for (item in queue) {
                if (!isActive) break

                // Check for pause
                while (_uiState.value.progress.isPaused && isActive) {
                    delay(200)
                }

                updateItemStatus(item.id, BatchItemStatus.PROCESSING)
                _uiState.update { s ->
                    s.copy(progress = s.progress.copy(currentFileName = item.fileItem.name))
                }

                // Notify service
                val updateIntent = Intent(context, com.metashield.app.service.BatchForegroundService::class.java).apply {
                    action = com.metashield.app.service.BatchForegroundService.ACTION_UPDATE
                    putExtra("CURRENT", completed + failed)
                    putExtra("TOTAL", state.progress.total)
                    putExtra("FILENAME", item.fileItem.name)
                }
                context.startService(updateIntent)

                try {
                    val uri = item.fileItem.uri
                    val fileItem = item.fileItem
                    val outputName = "${fileItem.name.substringBeforeLast('.')}_processed.${fileItem.name.substringAfterLast('.', "jpg")}"
                    val outputFile = File(context.cacheDir, outputName)
                    if (outputFile.exists()) outputFile.delete()
                    outputFile.createNewFile()
                    val outputUri = Uri.fromFile(outputFile)

                    val result = if (state.mode == BatchProcessingMode.STRIP) {
                        stripMetadataUseCase(uri, options, outputUri)
                    } else {
                        writeMetadataUseCase(uri, templateFields, outputUri).map { if (it) templateFields.size else 0 }
                    }

                    result.fold(
                        onSuccess = { count ->
                            completed++
                            updateItemStatus(item.id, BatchItemStatus.DONE, outputUri = outputUri, fieldsRemoved = if (state.mode == BatchProcessingMode.STRIP) count else 0)
                        },
                        onFailure = { e ->
                            failed++
                            updateItemStatus(item.id, BatchItemStatus.ERROR, errorMessage = e.message)
                        }
                    )
                } catch (e: Exception) {
                    failed++
                    updateItemStatus(item.id, BatchItemStatus.ERROR, errorMessage = e.message)
                }

                _uiState.update { s ->
                    s.copy(progress = s.progress.copy(completed = completed, failed = failed))
                }
            }

            _uiState.update { it.copy(isRunning = false, progress = it.progress.copy(currentFileName = "")) }

            // Notify done
            val doneIntent = Intent(context, com.metashield.app.service.BatchForegroundService::class.java).apply {
                action = com.metashield.app.service.BatchForegroundService.ACTION_DONE
                putExtra("PROCESSED", completed)
                putExtra("FAILED", failed)
            }
            context.startService(doneIntent)
        }
    }

    fun toggleSelection(id: String) {
        _uiState.update { state ->
            val newSelected = if (state.selectedIds.contains(id)) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(selectedIds = newSelected)
        }
    }

    fun selectAll(select: Boolean) {
        _uiState.update { state ->
            val newSelected = if (select) {
                state.queue.filter { it.status == BatchItemStatus.DONE }.map { it.id }.toSet()
            } else {
                emptySet()
            }
            state.copy(selectedIds = newSelected)
        }
    }

    fun shareSelected(context: android.content.Context) {
        val state = _uiState.value
        val itemsToShare = state.queue.filter { state.selectedIds.contains(it.id) && it.outputUri != null }
        if (itemsToShare.isEmpty()) return

        val uris = ArrayList<Uri>()
        itemsToShare.forEach { item ->
            try {
                val file = java.io.File(item.outputUri!!.path!!)
                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                uris.add(contentUri)
            } catch (_: Exception) {}
        }

        if (uris.isNotEmpty()) {
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share selected files"))
        }
    }

    fun saveSelectedToDevice() {
        if (_uiState.value.isSavingAll) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingAll = true, error = null, saveAllSuccess = false) }
            val state = _uiState.value
            val itemsToSave = state.queue.filter { state.selectedIds.contains(it.id) && it.status == BatchItemStatus.DONE && it.outputUri != null }

            if (itemsToSave.isEmpty()) {
                _uiState.update { it.copy(isSavingAll = false) }
                return@launch
            }

            var allSaved = true
            itemsToSave.forEach { item ->
                val result = metadataRepository.saveToPublicStorage(item.outputUri!!, item.fileItem)
                if (result.isFailure) allSaved = false
            }

            _uiState.update { it.copy(isSavingAll = false, saveAllSuccess = allSaved) }
            
            if (allSaved) {
                Toast.makeText(context, "Saved successfully to device", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Some files failed to save", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveAllToDevice() {
        if (_uiState.value.isSavingAll) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingAll = true, error = null, saveAllSuccess = false) }
            val successfulItems = _uiState.value.queue.filter { it.status == BatchItemStatus.DONE && it.outputUri != null }
            
            if (successfulItems.isEmpty()) {
                _uiState.update { it.copy(isSavingAll = false) }
                return@launch
            }

            var allSaved = true
            successfulItems.forEach { item ->
                val result = metadataRepository.saveToPublicStorage(item.outputUri!!, item.fileItem)
                if (result.isFailure) allSaved = false
            }

            _uiState.update { 
                it.copy(
                    isSavingAll = false, 
                    saveAllSuccess = allSaved,
                    error = if (!allSaved) "Some files failed to save" else null
                ) 
            }
            if (allSaved) {
                Toast.makeText(context, "All files saved successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun pauseBatch() {
        _uiState.update { it.copy(progress = it.progress.copy(isPaused = true)) }
    }

    fun resumeBatch() {
        _uiState.update { it.copy(progress = it.progress.copy(isPaused = false)) }
    }

    fun cancelBatch() {
        batchJob?.cancel()
        _uiState.update { state ->
            state.copy(
                isRunning = false,
                queue = state.queue.map {
                    if (it.status == BatchItemStatus.PROCESSING || it.status == BatchItemStatus.PENDING)
                        it.copy(status = BatchItemStatus.PAUSED)
                    else it
                },
                progress = state.progress.copy(isCancelled = true, isPaused = false)
            )
        }
        com.metashield.app.service.BatchForegroundService.stop(context)
    }

    private fun updateItemStatus(
        id: String,
        status: BatchItemStatus,
        outputUri: Uri? = null,
        fieldsRemoved: Int = 0,
        errorMessage: String? = null
    ) {
        _uiState.update { state ->
            state.copy(queue = state.queue.map { item ->
                if (item.id == id) item.copy(
                    status = status, outputUri = outputUri,
                    fieldsRemoved = fieldsRemoved, errorMessage = errorMessage
                )
                else item
            })
        }
    }
}
