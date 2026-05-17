package com.metashield.app.processing.audio

import android.content.Context
import android.net.Uri
import com.metashield.app.data.model.MetadataCategory
import com.metashield.app.data.model.MetadataField
import com.metashield.app.data.model.RemovalOptions
import com.metashield.app.data.model.SensitivityLevel
import com.metashield.app.processing.MetadataProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.FileOutputStream
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioMetadataProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) : MetadataProcessor {

    init {
        // Suppress verbose JAudioTagger logs
        Logger.getLogger("org.jaudiotagger").level = Level.SEVERE
    }

    /** Maps FieldKey → (displayName, category) */
    private val fieldMap = mapOf(
        FieldKey.TITLE         to ("Title"         to MetadataCategory.AUDIO),
        FieldKey.ARTIST        to ("Artist"        to MetadataCategory.AUDIO),
        FieldKey.ALBUM         to ("Album"         to MetadataCategory.AUDIO),
        FieldKey.ALBUM_ARTIST  to ("Album Artist"  to MetadataCategory.AUDIO),
        FieldKey.YEAR          to ("Year"          to MetadataCategory.TIMESTAMPS),
        FieldKey.TRACK         to ("Track Number"  to MetadataCategory.AUDIO),
        FieldKey.TRACK_TOTAL   to ("Track Total"   to MetadataCategory.AUDIO),
        FieldKey.DISC_NO       to ("Disc Number"   to MetadataCategory.AUDIO),
        FieldKey.DISC_TOTAL    to ("Disc Total"    to MetadataCategory.AUDIO),
        FieldKey.GENRE         to ("Genre"         to MetadataCategory.AUDIO),
        FieldKey.LYRICS        to ("Lyrics"        to MetadataCategory.AUDIO),
        FieldKey.COMMENT       to ("Comment"       to MetadataCategory.AUDIO),
        FieldKey.COMPOSER      to ("Composer"      to MetadataCategory.AUDIO),
        FieldKey.COPYRIGHT     to ("Copyright"     to MetadataCategory.COPYRIGHT),
        FieldKey.RECORD_LABEL  to ("Label"         to MetadataCategory.COPYRIGHT),
        FieldKey.ENCODER       to ("Encoder"       to MetadataCategory.TECHNICAL),
        FieldKey.BPM           to ("BPM"           to MetadataCategory.AUDIO),
        FieldKey.URL_OFFICIAL_ARTIST_SITE to ("Artist URL" to MetadataCategory.AUDIO)
    )

    override suspend fun read(uri: Uri): List<MetadataField> = withContext(Dispatchers.IO) {
        val fields = mutableListOf<MetadataField>()
        val tempFile = copyToTemp(uri) ?: return@withContext fields
        try {
            val audioFile = AudioFileIO.read(tempFile)
            val tag = audioFile.tag ?: return@withContext fields

            for ((key, info) in fieldMap) {
                runCatching {
                    val value = tag.getFirst(key)
                    if (value.isNotEmpty()) {
                        val (displayName, category) = info
                        val sensitivity = when (key) {
                            FieldKey.ENCODER -> SensitivityLevel.MEDIUM
                            else -> SensitivityLevel.LOW
                        }
                        fields += MetadataField(
                            key = key.name,
                            tag = displayName,
                            value = value,
                            category = category,
                            sensitivityLevel = sensitivity,
                            isSensitive = false,
                            isEditable = true
                        )
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            tempFile.delete()
        }
        fields
    }

    override suspend fun strip(uri: Uri, options: RemovalOptions, outputUri: Uri): Int =
        withContext(Dispatchers.IO) {
            val tempFile = copyToTemp(uri) ?: return@withContext 0
            var removed = 0
            try {
                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tag ?: run {
                    // No tag — just copy
                    copyToOutput(tempFile, outputUri, options.mutateHash)
                    return@withContext 0
                }

                if (options.removeAll) {
                    for (key in fieldMap.keys) {
                        runCatching {
                            val existing = tag.getFirst(key)
                            if (existing.isNotEmpty()) {
                                tag.deleteField(key)
                                removed++
                            }
                        }
                    }
                } else {
                    if (options.removeCopyright) {
                        for (key in listOf(FieldKey.COPYRIGHT, FieldKey.RECORD_LABEL)) {
                            runCatching { tag.deleteField(key); removed++ }
                        }
                    }
                    if (options.removeTimestamps) {
                        runCatching { tag.deleteField(FieldKey.YEAR); removed++ }
                    }
                    if (options.removeDevice || options.removeWatermarks) {
                        runCatching { 
                            if (options.spoofDevice) {
                                tag.setField(FieldKey.ENCODER, "Generic Audio Encoder")
                            } else {
                                tag.deleteField(FieldKey.ENCODER)
                            }
                            removed++ 
                        }
                    }
                    if (options.removeWatermarks) {
                        // Aggressively remove all non-essential tags that could be used for fingerprinting
                        val essential = setOf(FieldKey.TITLE.name, FieldKey.ARTIST.name, FieldKey.ALBUM.name)
                        val allFields = tag.fields
                        for (field in allFields) {
                            runCatching {
                                val fieldId = field.id
                                if (fieldId !in essential) {
                                    tag.deleteField(fieldId)
                                    removed++
                                }
                            }
                        }
                        // Note: actual ultrasonic frequency removal (~18kHz+) requires 
                        // full PCM decoding and a Low-Pass Filter, which is slated for v2.
                    }
                }

                AudioFileIO.write(audioFile)
                copyToOutput(tempFile, outputUri, options.mutateHash)
            } catch (_: Exception) {
                // Fallback: raw copy
                context.contentResolver.openInputStream(uri)?.use { input ->
                    context.contentResolver.openOutputStream(outputUri)?.use { out -> 
                        input.copyTo(out) 
                        if (options.mutateHash) {
                            out.write(kotlin.random.Random.nextBytes(4))
                        }
                    }
                }
            } finally {
                tempFile.delete()
            }
            removed
        }

    override suspend fun write(uri: Uri, fields: List<MetadataField>, outputUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            val tempFile = copyToTemp(uri) ?: return@withContext false
            try {
                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tag ?: audioFile.createDefaultTag()

                for (field in fields) {
                    runCatching {
                        val fieldKey = FieldKey.valueOf(field.key)
                        tag.setField(fieldKey, field.value)
                    }
                }

                AudioFileIO.write(audioFile)
                copyToOutput(tempFile, outputUri, false) // Write method does not use hash mutator
                true
            } catch (_: Exception) {
                false
            } finally {
                tempFile.delete()
            }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun copyToTemp(uri: Uri): File? {
        return try {
            val ext = uri.path?.substringAfterLast('.', "mp3") ?: "mp3"
            val temp = File(context.cacheDir, "metashield_audio_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { input.copyTo(it) }
            }
            temp
        } catch (_: Exception) { null }
    }

    private fun copyToOutput(source: File, outputUri: Uri, mutateHash: Boolean) {
        context.contentResolver.openOutputStream(outputUri)?.use { out ->
            source.inputStream().use { it.copyTo(out) }
            if (mutateHash) {
                out.write(kotlin.random.Random.nextBytes(4))
            }
        }
    }
}
