package com.metashield.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metashield.app.data.db.entity.HistoryEntity
import com.metashield.app.data.model.MetadataField
import com.metashield.app.data.repository.HistoryRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComparisonUiState(
    val isLoading: Boolean = false,
    val historyEntry: HistoryEntity? = null,
    val beforeFields: List<MetadataField> = emptyList(),
    val afterFields: List<MetadataField> = emptyList(),
    val diffs: List<MetadataDiff> = emptyList(),
    val error: String? = null
)

data class MetadataDiff(
    val tag: String,
    val beforeValue: String?,
    val afterValue: String?,
    val isRemoved: Boolean = false,
    val isAdded: Boolean = false,
    val isModified: Boolean = false
)

@HiltViewModel
class MetadataComparisonViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComparisonUiState())
    val uiState: StateFlow<ComparisonUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    fun loadEntry(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val entry = historyRepository.getHistoryById(id)
                if (entry == null) {
                    _uiState.update { it.copy(isLoading = false, error = "History entry not found") }
                    return@launch
                }

                val beforeArray = gson.fromJson(entry.metadataBeforeJson ?: "[]", Array<MetadataField>::class.java) ?: emptyArray()
                val afterArray = gson.fromJson(entry.metadataAfterJson ?: "[]", Array<MetadataField>::class.java) ?: emptyArray()
                val before = beforeArray.toList()
                val after = afterArray.toList()
                
                val diffs = calculateDiffs(before, after)

                _uiState.update { it.copy(
                    isLoading = false,
                    historyEntry = entry,
                    beforeFields = before,
                    afterFields = after,
                    diffs = diffs
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load comparison") }
            }
        }
    }

    private fun calculateDiffs(before: List<MetadataField>, after: List<MetadataField>): List<MetadataDiff> {
        val result = mutableListOf<MetadataDiff>()
        val beforeMap = before.associateBy { it.key }
        val afterMap = after.associateBy { it.key }

        val allKeys = (beforeMap.keys + afterMap.keys).distinct()

        for (key in allKeys) {
            val b = beforeMap[key]
            val a = afterMap[key]

            when {
                b != null && a == null -> {
                    result.add(MetadataDiff(b.tag, b.value, null, isRemoved = true))
                }
                b == null && a != null -> {
                    result.add(MetadataDiff(a.tag, null, a.value, isAdded = true))
                }
                b != null && a != null && b.value != a.value -> {
                    result.add(MetadataDiff(b.tag, b.value, a.value, isModified = true))
                }
                // If they are the same, we might not show them in the "diff" view, 
                // but let's keep them for a "full comparison" toggle later if needed.
            }
        }
        return result
    }
}
