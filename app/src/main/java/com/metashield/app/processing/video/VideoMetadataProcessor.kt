package com.metashield.app.processing.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.metashield.app.data.model.MetadataCategory
import com.metashield.app.data.model.MetadataField
import com.metashield.app.data.model.RemovalOptions
import com.metashield.app.data.model.SensitivityLevel
import com.metashield.app.processing.MetadataProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoMetadataProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) : MetadataProcessor {

    override suspend fun read(uri: Uri): List<MetadataField> = withContext(Dispatchers.IO) {
        val fields = mutableListOf<MetadataField>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            // ── Technical ───────────────────────────────────────────────────
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.let {
                fields += field("Width", "Width", it, MetadataCategory.TECHNICAL, SensitivityLevel.LOW)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.let {
                fields += field("Height", "Height", it, MetadataCategory.TECHNICAL, SensitivityLevel.LOW)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                val seconds = it.toLongOrNull()?.let { ms -> ms / 1000 } ?: it
                fields += field("Duration", "Duration", "${seconds}s", MetadataCategory.TECHNICAL, SensitivityLevel.LOW)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.let {
                fields += field("Bitrate", "Bitrate", "$it bps", MetadataCategory.TECHNICAL, SensitivityLevel.LOW)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.let {
                fields += field("MIMEType", "MIME Type", it, MetadataCategory.TECHNICAL, SensitivityLevel.LOW)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)?.let {
                fields += field("NumTracks", "Track Count", it, MetadataCategory.TECHNICAL, SensitivityLevel.LOW)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.let {
                fields += field("Rotation", "Rotation", "$it°", MetadataCategory.TECHNICAL, SensitivityLevel.LOW)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.let {
                fields += field("FrameCount", "Frame Count", it, MetadataCategory.TECHNICAL, SensitivityLevel.LOW)
            }

            // ── Timestamps ──────────────────────────────────────────────────
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)?.let {
                fields += field("Date", "Creation Date", it, MetadataCategory.TIMESTAMPS, SensitivityLevel.MEDIUM)
            }

            // ── Location ────────────────────────────────────────────────────
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)?.let {
                fields += field("Location", "GPS Location", it, MetadataCategory.LOCATION, SensitivityLevel.HIGH, isSensitive = true)
            }

            // ── Copyright / Author ──────────────────────────────────────────
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                fields += field("Artist", "Artist", it, MetadataCategory.COPYRIGHT, SensitivityLevel.LOW)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                fields += field("Title", "Title", it, MetadataCategory.TECHNICAL, SensitivityLevel.LOW)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)?.let {
                fields += field("Author", "Author", it, MetadataCategory.DEVICE, SensitivityLevel.MEDIUM)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER)?.let {
                fields += field("Writer", "Writer", it, MetadataCategory.COPYRIGHT, SensitivityLevel.LOW)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                fields += field("Album", "Album", it, MetadataCategory.AUDIO, SensitivityLevel.LOW)
            }

        } catch (e: Exception) {
            // Return whatever we could extract
        } finally {
            retriever.release()
        }

        // TODO Phase 2: Add MP4Parser for full atom-level metadata (e.g. ©too, ©xyz, udta)
        fields
    }

    override suspend fun strip(uri: Uri, options: RemovalOptions, outputUri: Uri): Int =
        withContext(Dispatchers.IO) {
            // TODO Phase 2: Implement full atom-level stripping with MP4Parser.
            // For now: copy the file unchanged and return stub count.
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    context.contentResolver.openOutputStream(outputUri)?.use { out ->
                        input.copyTo(out)
                        if (options.mutateHash) {
                            out.write(kotlin.random.Random.nextBytes(4))
                        }
                    }
                }
            } catch (_: Exception) {}
            0
        }

    override suspend fun write(uri: Uri, fields: List<MetadataField>, outputUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            // TODO Phase 2: Implement write with MP4Parser
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    context.contentResolver.openOutputStream(outputUri)?.use { out ->
                        input.copyTo(out)
                    }
                }
                true
            } catch (_: Exception) {
                false
            }
        }

    private fun field(
        key: String, tag: String, value: String,
        category: MetadataCategory, sensitivity: SensitivityLevel,
        isSensitive: Boolean = sensitivity == SensitivityLevel.HIGH
    ) = MetadataField(key, tag, value, category, sensitivity, isSensitive)
}
