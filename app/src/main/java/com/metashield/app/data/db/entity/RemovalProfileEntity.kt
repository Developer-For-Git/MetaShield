package com.metashield.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "removal_profiles")
data class RemovalProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val removeLocation: Boolean = true,
    val removeCamera: Boolean = false,
    val removeTimestamps: Boolean = false,
    val removeDevice: Boolean = true,
    val removeCopyright: Boolean = false,
    val removeTechnical: Boolean = false,
    val removeAll: Boolean = false,
    val preserveEssentials: Boolean = true,
    val anonymizeTimestamps: Boolean = false,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
