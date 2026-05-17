package com.metashield.app.domain.usecase

import android.net.Uri
import com.metashield.app.data.db.entity.HistoryEntity
import com.metashield.app.data.model.RemovalOptions
import com.metashield.app.data.repository.HistoryRepository
import com.metashield.app.data.repository.MetadataRepository
import javax.inject.Inject

class StripMetadataUseCase @Inject constructor(
    private val metadataRepository: MetadataRepository,
    private val historyRepository: HistoryRepository,
    private val readMetadataUseCase: ReadMetadataUseCase
) {
    private val gson = com.google.gson.Gson()

    suspend operator fun invoke(
        uri: Uri,
        options: RemovalOptions,
        outputUri: Uri
    ): Result<Int> = runCatching {
        val fileItem = metadataRepository.getFileItem(uri)
        
        // Capture Before Snapshot
        val metadataBefore = readMetadataUseCase(uri).getOrDefault(emptyList())
        val metadataBeforeJson = gson.toJson(metadataBefore)

        val fieldsRemoved = metadataRepository.stripMetadata(uri, options, outputUri)

        // Capture After Snapshot
        val metadataAfter = readMetadataUseCase(outputUri).getOrDefault(emptyList())
        val metadataAfterJson = gson.toJson(metadataAfter)

        val action = when {
            options.removeAll -> "STRIP_ALL"
            options.removeLocation && !options.removeCamera && !options.removeDevice -> "STRIP_LOCATION"
            else -> "STRIP_SELECTIVE"
        }

        historyRepository.addEntry(
            HistoryEntity(
                fileName = fileItem.name,
                action = action,
                inputUriString = uri.toString(),
                outputUriString = outputUri.toString(),
                fieldsRemoved = fieldsRemoved,
                fileType = fileItem.fileType.name,
                fileSizeBefore = fileItem.size,
                success = true,
                metadataBeforeJson = metadataBeforeJson,
                metadataAfterJson = metadataAfterJson
            )
        )

        fieldsRemoved
    }
}
