package com.metashield.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    /** Action: STRIP_ALL | STRIP_SELECTIVE | STRIP_LOCATION | EDIT | BATCH | SAFE_SHARE */
    val action: String,
    val inputUriString: String,
    val outputUriString: String? = null,
    val fieldsRemoved: Int = 0,
    val fileType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileSizeBefore: Long = 0L,
    val fileSizeAfter: Long = 0L,
    val success: Boolean = true,
    val errorMessage: String? = null,
    
    // Time Machine Snapshots
    val metadataBeforeJson: String? = null,
    val metadataAfterJson: String? = null
)
