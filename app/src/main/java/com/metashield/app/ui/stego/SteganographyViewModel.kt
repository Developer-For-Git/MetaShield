package com.metashield.app.ui.stego

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metashield.app.util.SteganographyUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class StegoUiState(
    val selectedUri: Uri? = null,
    val message: String = "",
    val password: String = "",
    val isProcessing: Boolean = false,
    val decodedMessage: String? = null,
    val resultUri: Uri? = null,
    val error: String? = null
)

@HiltViewModel
class SteganographyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metadataRepository: com.metashield.app.data.repository.MetadataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StegoUiState())
    val uiState: StateFlow<StegoUiState> = _uiState.asStateFlow()

    fun selectUri(uri: Uri) {
        _uiState.update { it.copy(selectedUri = uri, decodedMessage = null, resultUri = null, error = null) }
    }

    fun updateMessage(msg: String) {
        _uiState.update { it.copy(message = msg) }
    }

    fun updatePassword(pw: String) {
        _uiState.update { it.copy(password = pw) }
    }

    fun encode() {
        val uri = _uiState.value.selectedUri ?: return
        val msg = _uiState.value.message
        val pw = _uiState.value.password.takeIf { it.isNotEmpty() }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { 
                        BitmapFactory.decodeStream(it)
                    }
                } ?: throw Exception("Failed to load image")

                val resultBitmap = withContext(Dispatchers.Default) {
                    SteganographyUtils.encode(bitmap, msg, pw)
                }

                val resultFile = File(context.cacheDir, "pixel_armor_${System.currentTimeMillis()}.png")
                withContext(Dispatchers.IO) {
                    FileOutputStream(resultFile).use { out ->
                        resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }

                _uiState.update { it.copy(isProcessing = false, resultUri = Uri.fromFile(resultFile)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = "Encoding failed: ${e.message}") }
            }
        }
    }

    fun decode() {
        val uri = _uiState.value.selectedUri ?: return
        val pw = _uiState.value.password.takeIf { it.isNotEmpty() }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { 
                        BitmapFactory.decodeStream(it)
                    }
                } ?: throw Exception("Failed to load image")

                val message = withContext(Dispatchers.Default) {
                    SteganographyUtils.decode(bitmap, pw)
                }

                _uiState.update { it.copy(isProcessing = false, decodedMessage = message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = "Decoding failed: ${e.message}") }
            }
        }
    }

    fun saveToDevice() {
        val uri = _uiState.value.resultUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val fileItem = metadataRepository.getFileItem(uri)
                val result = metadataRepository.saveToPublicStorage(uri, fileItem)
                result.fold(
                    onSuccess = {
                        android.widget.Toast.makeText(context, "Saved to Gallery", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(error = "Save failed: ${e.message}") }
                    }
                )
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun shareImage() {
        val uri = _uiState.value.resultUri ?: return
        viewModelScope.launch {
            try {
                val file = File(uri.path!!)
                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = android.content.Intent.createChooser(shareIntent, "Share protected image").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Share failed: ${e.message}") }
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(resultUri = null, decodedMessage = null, error = null) }
    }
}
