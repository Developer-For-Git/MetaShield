package com.metashield.app.data.model

import android.net.Uri

enum class BatchItemStatus {
    PENDING, PROCESSING, DONE, ERROR, PAUSED
}

data class BatchItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileItem: FileItem,
    val status: BatchItemStatus = BatchItemStatus.PENDING,
    val progress: Float = 0f,
    val errorMessage: String? = null,
    val outputUri: Uri? = null,
    val fieldsRemoved: Int = 0,
    val originalFieldsCount: Int = 0
)

data class BatchProgress(
    val total: Int,
    val completed: Int,
    val failed: Int,
    val currentFileName: String = "",
    val estimatedSecondsRemaining: Long = 0L,
    val isPaused: Boolean = false,
    val isCancelled: Boolean = false
) {
    val overallProgress: Float
        get() = if (total == 0) 0f else (completed + failed).toFloat() / total.toFloat()

    val isFinished: Boolean
        get() = total > 0 && completed + failed >= total && !isCancelled
}
