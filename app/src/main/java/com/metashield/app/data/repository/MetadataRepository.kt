package com.metashield.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.metashield.app.data.model.FileItem
import com.metashield.app.data.model.FileType
import com.metashield.app.data.model.MetadataField
import com.metashield.app.data.model.RemovalOptions
import com.metashield.app.processing.ProcessorFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processorFactory: ProcessorFactory
) {
    suspend fun readMetadata(uri: Uri): List<MetadataField> = withContext(Dispatchers.IO) {
        val fileItem = getFileItem(uri)
        val processor = processorFactory.getProcessor(fileItem.fileType)
        processor?.read(uri) ?: emptyList()
    }

    suspend fun stripMetadata(uri: Uri, options: RemovalOptions, outputUri: Uri): Int =
        withContext(Dispatchers.IO) {
            val fileItem = getFileItem(uri)
            val processor = processorFactory.getProcessor(fileItem.fileType)
            processor?.strip(uri, options, outputUri) ?: 0
        }

    suspend fun writeMetadata(uri: Uri, fields: List<MetadataField>, outputUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            val fileItem = getFileItem(uri)
            val processor = processorFactory.getProcessor(fileItem.fileType)
            processor?.write(uri, fields, outputUri) ?: false
        }

    suspend fun saveToPublicStorage(uri: Uri, fileItem: FileItem): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileItem.name)
                put(MediaStore.MediaColumns.MIME_TYPE, fileItem.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val folder = when (fileItem.fileType) {
                        FileType.PHOTO -> Environment.DIRECTORY_PICTURES
                        FileType.VIDEO -> Environment.DIRECTORY_MOVIES
                        else           -> Environment.DIRECTORY_DOWNLOADS
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/MetaShield")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collection = when (fileItem.fileType) {
                FileType.PHOTO -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                FileType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else           -> MediaStore.Files.getContentUri("external")
            }

            val destinationUri = resolver.insert(collection, contentValues) 
                ?: throw Exception("Could not create MediaStore entry")

            try {
                resolver.openInputStream(uri)?.use { input ->
                    resolver.openOutputStream(destinationUri)?.use { output ->
                        input.copyTo(output)
                    }
                } ?: throw Exception("Could not open input stream")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(destinationUri, contentValues, null, null)
                }
                destinationUri
            } catch (e: Exception) {
                resolver.delete(destinationUri, null, null)
                throw e
            }
        }
    }

    fun queryRecentMedia(): List<Uri> {
        val uris = mutableListOf<Uri>()
        val twentyFourHoursAgo = (System.currentTimeMillis() - 86400000) / 1000
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DATE_ADDED} > ?"
        val selectionArgs = arrayOf(twentyFourHoursAgo.toString())

        // Images
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs, null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) {
                uris.add(Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getString(idColumn)))
            }
        }

        // Video
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs, null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) {
                uris.add(Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cursor.getString(idColumn)))
            }
        }

        return uris
    }

    fun getFileItem(uri: Uri): FileItem {
        var name = "unknown"
        var size = 0L
        var mimeType: String? = null

        when (uri.scheme) {
            // ── file:// URI (from FileCache or local file) ────────────────
            "file" -> {
                val file = File(uri.path!!)
                name = file.name
                    .replaceFirst(Regex("^[a-fA-F0-9\\-]{36}_"), "")
                    .replaceFirst(Regex("^\\d+_"), "")
                size = file.length()
                val ext = file.extension.lowercase()
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            }

            // ── content:// URI (from SAF / share sheet) ───────────────────
            else -> {
                val contentResolver = context.contentResolver
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: "unknown"
                        if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                    }
                }
                mimeType = contentResolver.getType(uri)
            }
        }

        val ext = name.substringAfterLast('.', "")
        val fileType = when {
            mimeType != null -> FileType.fromMimeType(mimeType)
            ext.isNotEmpty() -> FileType.fromExtension(ext)
            else             -> FileType.UNKNOWN
        }

        return FileItem(uri = uri, name = name, size = size, mimeType = mimeType, fileType = fileType)
    }
}

