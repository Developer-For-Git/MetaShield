package com.metashield.app.processing

import android.net.Uri
import com.metashield.app.data.model.MetadataField
import com.metashield.app.data.model.RemovalOptions

/**
 * Common interface for all file-type-specific metadata processors.
 * All implementations must be non-destructive: original files are never modified.
 * Output is always written to outputUri.
 */
interface MetadataProcessor {

    /**
     * Read all metadata fields from the given URI.
     * Returns an empty list if the file has no readable metadata.
     */
    suspend fun read(uri: Uri): List<MetadataField>

    /**
     * Strip metadata according to the provided [RemovalOptions].
     * Writes the cleaned file to [outputUri].
     * @return number of metadata fields removed
     */
    suspend fun strip(uri: Uri, options: RemovalOptions, outputUri: Uri): Int

    /**
     * Write (inject) a set of custom metadata [fields] into the file.
     * Writes the result to [outputUri].
     * @return true if successful
     */
    suspend fun write(uri: Uri, fields: List<MetadataField>, outputUri: Uri): Boolean
}
