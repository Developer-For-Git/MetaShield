package com.metashield.app.processing

import com.metashield.app.data.model.FileType
import com.metashield.app.processing.audio.AudioMetadataProcessor
import com.metashield.app.processing.document.DocumentMetadataProcessor
import com.metashield.app.processing.photo.PhotoMetadataProcessor
import com.metashield.app.processing.video.VideoMetadataProcessor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessorFactory @Inject constructor(
    private val photoProcessor: PhotoMetadataProcessor,
    private val videoProcessor: VideoMetadataProcessor,
    private val audioProcessor: AudioMetadataProcessor,
    private val documentProcessor: DocumentMetadataProcessor
) {
    fun getProcessor(fileType: FileType): MetadataProcessor? = when (fileType) {
        FileType.PHOTO -> photoProcessor
        FileType.VIDEO -> videoProcessor
        FileType.AUDIO -> audioProcessor
        FileType.DOCUMENT -> documentProcessor
        FileType.UNKNOWN -> null
    }
}
