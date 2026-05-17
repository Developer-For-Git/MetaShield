package com.metashield.app.domain.usecase

import android.net.Uri
import com.metashield.app.data.db.entity.HistoryEntity
import com.metashield.app.data.model.MetadataField
import com.metashield.app.data.repository.HistoryRepository
import com.metashield.app.data.repository.MetadataRepository
import javax.inject.Inject

class WriteMetadataUseCase @Inject constructor(
    private val metadataRepository: MetadataRepository,
    private val historyRepository: HistoryRepository,
    private val readMetadataUseCase: ReadMetadataUseCase
) {
    private val gson = com.google.gson.Gson()

    suspend operator fun invoke(
        uri: Uri,
        fields: List<MetadataField>,
        outputUri: Uri
    ): Result<Boolean> = runCatching {
        val fileItem = metadataRepository.getFileItem(uri)
        
        // Capture Before Snapshot
        val metadataBefore = readMetadataUseCase(uri).getOrDefault(emptyList())
        val metadataBeforeJson = gson.toJson(metadataBefore)

        val success = metadataRepository.writeMetadata(uri, fields, outputUri)

        if (success) {
            // Capture After Snapshot
            val metadataAfter = readMetadataUseCase(outputUri).getOrDefault(emptyList())
            val metadataAfterJson = gson.toJson(metadataAfter)

            historyRepository.addEntry(
                HistoryEntity(
                    fileName = fileItem.name,
                    action = "EDIT",
                    inputUriString = uri.toString(),
                    outputUriString = outputUri.toString(),
                    fieldsRemoved = 0,
                    fileType = fileItem.fileType.name,
                    fileSizeBefore = fileItem.size,
                    success = true,
                    metadataBeforeJson = metadataBeforeJson,
                    metadataAfterJson = metadataAfterJson
                )
            )
        }

        success
    }
}
