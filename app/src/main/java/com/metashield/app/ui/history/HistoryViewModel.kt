package com.metashield.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metashield.app.data.db.entity.HistoryEntity
import com.metashield.app.data.repository.HistoryRepository
import com.metashield.app.ui.history.HistoryCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _selectedCategory = MutableStateFlow(HistoryCategory.ALL)
    val selectedCategory: StateFlow<HistoryCategory> = _selectedCategory.asStateFlow()

    fun setCategory(category: HistoryCategory) {
        _selectedCategory.value = category
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val history: StateFlow<List<HistoryEntity>> = combine(_searchQuery, _selectedCategory) { q, cat -> Pair(q, cat) }
        .debounce(300)
        .flatMapLatest { (query, category) ->
            val baseFlow = if (query.isEmpty()) historyRepository.getAllHistory()
                          else historyRepository.searchHistory(query)
            baseFlow.map { list ->
                when (category) {
                    HistoryCategory.ALL -> list
                    HistoryCategory.PHOTO -> list.filter { entry ->
                        entry.fileType.uppercase() == "PHOTO"
                    }
                    HistoryCategory.VIDEO -> list.filter { entry ->
                        entry.fileType.uppercase() == "VIDEO"
                    }
                    HistoryCategory.AUDIO -> list.filter { entry ->
                        entry.fileType.uppercase() == "AUDIO"
                    }
                    HistoryCategory.DOCS -> list.filter { entry ->
                        entry.fileType.uppercase() == "DOCUMENT"
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteEntry(entity: HistoryEntity) {
        viewModelScope.launch {
            historyRepository.deleteEntry(entity)
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val toDelete = history.value.filter { _selectedIds.value.contains(it.id) }
            toDelete.forEach { historyRepository.deleteEntry(it) }
            _selectedIds.value = emptySet()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            historyRepository.clearAll()
            _selectedIds.value = emptySet()
        }
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value
        _selectedIds.value = if (current.contains(id)) current - id else current + id
    }

    fun selectAll() {
        val allIds = history.value.map { it.id }.toSet()
        _selectedIds.value = if (_selectedIds.value.size == allIds.size) emptySet() else allIds
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun shareSelected(context: android.content.Context) {
        val toShare = history.value.filter { _selectedIds.value.contains(it.id) }
        if (toShare.isEmpty()) return
        
        val uris = ArrayList<android.net.Uri>()
        toShare.forEach { entry ->
            try {
                val pathStr = entry.outputUriString ?: entry.inputUriString
                var parsedUri = android.net.Uri.parse(pathStr)
                if (parsedUri.scheme == "file") {
                    val file = java.io.File(parsedUri.path!!)
                    parsedUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                }
                uris.add(parsedUri)
            } catch (e: Exception) {}
        }
        
        if (uris.isNotEmpty()) {
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share files"))
        }
    }

    fun shareEntry(context: android.content.Context, entry: HistoryEntity) {
        try {
            val pathStr = entry.outputUriString ?: entry.inputUriString
            var parsedUri = android.net.Uri.parse(pathStr)
            if (parsedUri.scheme == "file") {
                val file = java.io.File(parsedUri.path!!)
                parsedUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(android.content.Intent.EXTRA_STREAM, parsedUri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share file"))
        } catch (e: Exception) {}
    }

    fun exportHistory(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val currentHistory = history.value
                val sb = java.lang.StringBuilder()
                sb.append("File Name,Action Taken,Date,Fields Removed,Success\n")
                val df = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                currentHistory.forEach { entry ->
                    val dateStr = df.format(java.util.Date(entry.timestamp))
                    val successStr = if (entry.success) "Yes" else "No"
                    val safeName = entry.fileName.replace("\"", "\"\"")
                    sb.append("\"$safeName\",\"${entry.action}\",\"$dateStr\",${entry.fieldsRemoved},\"$successStr\"\n")
                }
                context.contentResolver.openOutputStream(uri)?.use { 
                    it.write(sb.toString().toByteArray()) 
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "History Exported as CSV", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
               kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Export Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
