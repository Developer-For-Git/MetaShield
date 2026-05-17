package com.metashield.app.data.model

import android.net.Uri

enum class FileType {
    PHOTO, VIDEO, AUDIO, DOCUMENT, UNKNOWN;

    companion object {
        fun fromMimeType(mimeType: String?): FileType = when {
            mimeType?.startsWith("image/") == true -> PHOTO
            mimeType?.startsWith("video/") == true -> VIDEO
            mimeType?.startsWith("audio/") == true -> AUDIO
            mimeType?.startsWith("application/pdf") == true -> DOCUMENT
            mimeType?.contains("office") == true -> DOCUMENT
            mimeType?.contains("msword") == true -> DOCUMENT
            else -> UNKNOWN
        }

        fun fromExtension(ext: String): FileType = when (ext.lowercase()) {
            "jpg", "jpeg", "png", "heic", "webp", "raw", "bmp", "tiff", "tif", "dng" -> PHOTO
            "mp4", "mov", "avi", "mkv", "3gp", "webm", "flv", "m4v" -> VIDEO
            "mp3", "flac", "aac", "ogg", "wav", "m4a", "opus", "wma" -> AUDIO
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx" -> DOCUMENT
            else -> UNKNOWN
        }
    }
}

data class FileItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String?,
    val fileType: FileType = FileType.fromMimeType(mimeType),
    val fields: List<MetadataField> = emptyList()
)
