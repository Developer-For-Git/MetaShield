package com.metashield.app.domain.usecase

import android.net.Uri
import com.metashield.app.data.model.MetadataField
import com.metashield.app.data.repository.MetadataRepository
import javax.inject.Inject

class ReadMetadataUseCase @Inject constructor(
    private val repository: MetadataRepository
) {
    suspend operator fun invoke(uri: Uri): Result<List<MetadataField>> = runCatching {
        repository.readMetadata(uri)
    }
}
