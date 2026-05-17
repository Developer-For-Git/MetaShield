package com.metashield.app.ui.update

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.metashield.app.data.model.UpdateInfo
import com.metashield.app.data.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Download state machine ────────────────────────────────────────────────────
sealed class DownloadState {
    object Idle        : DownloadState()
    object Downloading : DownloadState()
    object Installing  : DownloadState()
    object Failed      : DownloadState()
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    app: Application,
    private val updateRepository: UpdateRepository
) : AndroidViewModel(app) {

    private val _updateInfo     = MutableStateFlow<UpdateInfo?>(null)
    private val _isPostponed    = MutableStateFlow(false)
    private val _downloadState  = MutableStateFlow<DownloadState>(DownloadState.Idle)
    private val _downloadProgress = MutableStateFlow(0)   // 0–100

    private var activeDownloadId = -1L

    /**
     * Null = hide dialog. Non-null = show dialog.
     * The dialog hides while update is postponed and reappears after 10 minutes.
     */
    val pendingUpdate: StateFlow<UpdateInfo?> =
        combine(_updateInfo, _isPostponed, _downloadState) { info, postponed, dl ->
            // Keep dialog visible while downloading/installing even if user hit postpone
            if (postponed && dl == DownloadState.Idle) null else info
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val downloadState:    StateFlow<DownloadState> = _downloadState
    val downloadProgress: StateFlow<Int>           = _downloadProgress

    // ── Check for update on app start ─────────────────────────────────────────
    fun checkForUpdate() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val current = try {
                ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0.0.0"
            } catch (_: Exception) { "0.0.0" }
            _updateInfo.value = updateRepository.checkForUpdate(current)
        }
    }

    // ── Postpone for exactly 10 minutes ──────────────────────────────────────
    fun postponeUpdate() {
        _isPostponed.value = true
        viewModelScope.launch {
            delay(10L * 60L * 1_000L)          // 10 minutes
            _isPostponed.value = false
        }
    }

    // ── Start in-app APK download via DownloadManager ────────────────────────
    fun startDownload(info: UpdateInfo) {
        val ctx  = getApplication<Application>()
        val mgr  = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        _downloadState.value   = DownloadState.Downloading
        _downloadProgress.value = 0

        val fileName = "MetaShield-${info.version}.apk"
        val request  = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("MetaShield ${info.version}")
            .setDescription("Downloading update…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")

        activeDownloadId = mgr.enqueue(request)

        // Poll DownloadManager every 500 ms for progress / completion
        viewModelScope.launch {
            while (_downloadState.value == DownloadState.Downloading) {
                val cursor = mgr.query(DownloadManager.Query().setFilterById(activeDownloadId))
                if (cursor.moveToFirst()) {
                    val status    = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val bytesDown = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal= cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            _downloadProgress.value = 100
                            _downloadState.value    = DownloadState.Installing
                            val apkUri = mgr.getUriForDownloadedFile(activeDownloadId)
                            triggerInstall(ctx, apkUri)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            _downloadState.value = DownloadState.Failed
                        }
                        else -> {
                            if (bytesTotal > 0L)
                                _downloadProgress.value = ((bytesDown * 100L) / bytesTotal).toInt()
                        }
                    }
                }
                cursor.close()
                delay(500L)
            }
        }
    }

    // ── Launch system package installer ──────────────────────────────────────
    private fun triggerInstall(context: Context, apkUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        runCatching { context.startActivity(intent) }
        _downloadState.value = DownloadState.Idle
    }

    // ── Retry after a failed download ────────────────────────────────────────
    fun retryDownload() {
        _downloadState.value = DownloadState.Idle
        _downloadProgress.value = 0
    }
}
