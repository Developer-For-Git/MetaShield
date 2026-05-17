package com.metashield.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies content:// URIs into app-internal cache so that file access
 * doesn't depend on the temporary SAF/picker permission persisting
 * across navigation destinations.
 *
 * This is the standard Android pattern: copy once while the permission
 * is alive, then work exclusively with the local File / file:// URI.
 */
@Singleton
class FileCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cacheDir = File(context.cacheDir, "picker_files").also { it.mkdirs() }

    /**
     * Copies the content URI into app cache and returns a Pair of
     * (display name, cached file:// URI).
     *
     * Must be called while the caller still has read access to [sourceUri].
     */
    suspend fun cacheFile(sourceUri: Uri): Pair<String, Uri> = withContext(Dispatchers.IO) {
        // Resolve the display name from the content provider
        var displayName = "file_${System.currentTimeMillis()}"
        context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) displayName = cursor.getString(idx) ?: displayName
            }
        }

        // Unique-ify to avoid collisions
        val cachedFile = File(cacheDir, "${java.util.UUID.randomUUID()}_$displayName")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            cachedFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Cannot read $sourceUri")

        displayName to Uri.fromFile(cachedFile)
    }

    /** Clean up old cached files to free storage */
    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
