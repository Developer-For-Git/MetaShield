package com.metashield.app.data.db.entity

import androidx.room.*

@Entity(tableName = "vault")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val mimeType: String?,
    val fileType: String,
    val internalPath: String, // Absolute path in filesDir
    val originalSize: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = true, // To support future non-encrypted vault if needed
    val isDecoy: Boolean = false // Plausible deniability partition
)
