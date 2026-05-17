package com.metashield.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * VaultSession - Manages the current authentication state and partition access.
 */
object VaultSession {
    private val _isDecoy = MutableStateFlow(false)
    val isDecoy: StateFlow<Boolean> = _isDecoy.asStateFlow()

    fun startSession(decoy: Boolean) {
        _isDecoy.value = decoy
    }

    fun endSession() {
        _isDecoy.value = false
    }
}
