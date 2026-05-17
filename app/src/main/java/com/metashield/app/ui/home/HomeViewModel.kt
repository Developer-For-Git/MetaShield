package com.metashield.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metashield.app.data.db.entity.HistoryEntity
import com.metashield.app.data.repository.HistoryRepository
import com.metashield.app.data.repository.MetadataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    val recentHistory: StateFlow<List<HistoryEntity>> = historyRepository
        .getRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalCleanedCount: StateFlow<Int> = historyRepository
        .getHistoryCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _tipIndex = MutableStateFlow(0)
    val tipIndex: StateFlow<Int> = _tipIndex.asStateFlow()

    init {
        // Rotate tips every 8 seconds
        viewModelScope.launch {
            while (true) {
                delay(8_000)
                _tipIndex.update { it + 1 }
            }
        }
    }

    private val _panicResult = MutableSharedFlow<List<Uri>>()
    val panicResult = _panicResult.asSharedFlow()

    fun triggerPanic() {
        viewModelScope.launch {
            val uris = metadataRepository.queryRecentMedia()
            _panicResult.emit(uris)
        }
    }
}
