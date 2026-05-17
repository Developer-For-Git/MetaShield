package com.metashield.app.ui.scanner

import android.content.Context
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ScannerUiState(
    val isScanning: Boolean = false,
    val totalPhotosFound: Int = 0,
    val photosWithGps: Int = 0,
    val photosWithDevice: Int = 0,
    val privacyScore: Int = 100,
    val scannedCount: Int = 0,
    val currentFileName: String? = null
)

@HiltViewModel
class PrivacyScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun startScan() {
        if (_uiState.value.isScanning) return
        
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = ScannerUiState(isScanning = true)
            
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA
            )
            // Get up to 500 recent photos so we don't block the UI forever
            val cursor = context.contentResolver.query(
                uri, projection, null, null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )

            var localWithGps = 0
            var localWithDevice = 0
            var localScanned = 0
            var localTotal = 0

            cursor?.use { c ->
                localTotal = minOf(c.count, 500)
                val dataColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                while (c.moveToNext() && localScanned < 500) {
                    val path = c.getString(dataColumn)
                    val name = path.substringAfterLast('/')
                    
                    try {
                        val exif = ExifInterface(path)
                        val lat = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                        val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                        
                        if (!lat.isNullOrEmpty()) {
                            localWithGps++
                        }
                        if (!make.isNullOrEmpty()) {
                            localWithDevice++
                        }
                    } catch (e: Exception) {
                        // ignore unreadable files
                    }
                    
                    localScanned++
                    
                    // Add a tiny delay to make the "radar" feel like it's processing
                    if (localScanned < 50) kotlinx.coroutines.delay(100)
                    else if (localScanned < 200) kotlinx.coroutines.delay(30)
                    
                    if (localScanned % 5 == 0) {
                        updateScore(localTotal, localScanned, localWithGps, localWithDevice, name)
                    }
                }
            }
            
            // Final update
            updateScore(localTotal, localScanned, localWithGps, localWithDevice, null, scanning = false)
        }
    }

    private suspend fun updateScore(total: Int, scanned: Int, withGps: Int, withDevice: Int, fileName: String? = null, scanning: Boolean = true) {
        withContext(Dispatchers.Main) {
            val gpsRatio = if (scanned > 0) withGps.toFloat() / scanned else 0f
            val deviceRatio = if (scanned > 0) withDevice.toFloat() / scanned else 0f
            
            // Weight GPS heavily (60 points) and Device moderately (40 points)
            val penalty = (gpsRatio * 60) + (deviceRatio * 40)
            val currentScore = (100 - penalty).toInt().coerceIn(0, 100)
            
            _uiState.value = _uiState.value.copy(
                isScanning = scanning,
                totalPhotosFound = total,
                scannedCount = scanned,
                photosWithGps = withGps,
                photosWithDevice = withDevice,
                privacyScore = currentScore,
                currentFileName = fileName
            )
        }
    }
}
