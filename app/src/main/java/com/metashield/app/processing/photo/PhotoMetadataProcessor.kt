package com.metashield.app.processing.photo

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.metashield.app.data.model.MetadataCategory
import com.metashield.app.data.model.MetadataField
import com.metashield.app.data.model.RemovalOptions
import com.metashield.app.data.model.SensitivityLevel
import com.metashield.app.processing.MetadataProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoMetadataProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) : MetadataProcessor {

    // ── Tag categorization ────────────────────────────────────────────────────

    private val locationTags = setOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_MEASURE_MODE
    )

    private val cameraTags = setOf(
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_LENS_SPECIFICATION,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_APERTURE_VALUE,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_SHUTTER_SPEED_VALUE,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
        ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
        ExifInterface.TAG_METERING_MODE,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_EXPOSURE_PROGRAM,
        ExifInterface.TAG_SUBJECT_DISTANCE
    )

    private val timestampTags = setOf(
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_OFFSET_TIME,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP
    )

    private val deviceTags = setOf(
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_CAMERA_OWNER_NAME,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_SERIAL_NUMBER,
        ExifInterface.TAG_IMAGE_UNIQUE_ID,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_MAKER_NOTE
    )

    private val copyrightTags = setOf(
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_ARTIST
    )

    private val technicalTags = setOf(
        ExifInterface.TAG_IMAGE_WIDTH,
        ExifInterface.TAG_IMAGE_LENGTH,
        ExifInterface.TAG_BITS_PER_SAMPLE,
        ExifInterface.TAG_COMPRESSION,
        ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION,
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.TAG_SAMPLES_PER_PIXEL,
        ExifInterface.TAG_X_RESOLUTION,
        ExifInterface.TAG_Y_RESOLUTION,
        ExifInterface.TAG_RESOLUTION_UNIT,
        ExifInterface.TAG_COLOR_SPACE,
        ExifInterface.TAG_PIXEL_X_DIMENSION,
        ExifInterface.TAG_PIXEL_Y_DIMENSION,
        ExifInterface.TAG_EXIF_VERSION,
        ExifInterface.TAG_LIGHT_SOURCE,
        ExifInterface.TAG_SCENE_CAPTURE_TYPE
    )

    // ── Read ─────────────────────────────────────────────────────────────────

    override suspend fun read(uri: Uri): List<MetadataField> = withContext(Dispatchers.IO) {
        val fields = mutableListOf<MetadataField>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val exif = ExifInterface(inputStream)
            readGroup(exif, locationTags,  MetadataCategory.LOCATION,   SensitivityLevel.HIGH,   fields)
            readGroup(exif, cameraTags,    MetadataCategory.CAMERA,     SensitivityLevel.MEDIUM, fields)
            readGroup(exif, timestampTags, MetadataCategory.TIMESTAMPS, SensitivityLevel.MEDIUM, fields)
            readGroup(exif, deviceTags,    MetadataCategory.DEVICE,     SensitivityLevel.HIGH,   fields)
            readGroup(exif, copyrightTags, MetadataCategory.COPYRIGHT,  SensitivityLevel.LOW,    fields)
            readGroup(exif, technicalTags, MetadataCategory.TECHNICAL,  SensitivityLevel.LOW,    fields)
        }
        fields
    }

    private fun readGroup(
        exif: ExifInterface,
        tags: Set<String>,
        category: MetadataCategory,
        sensitivity: SensitivityLevel,
        dest: MutableList<MetadataField>
    ) {
        for (tag in tags) {
            val value = exif.getAttribute(tag)
            if (!value.isNullOrEmpty()) {
                dest.add(
                    MetadataField(
                        key = tag,
                        tag = formatTagName(tag),
                        value = value,
                        category = category,
                        sensitivityLevel = sensitivity,
                        isSensitive = sensitivity == SensitivityLevel.HIGH,
                        isEditable = true
                    )
                )
            }
        }
    }

    // ── Strip ─────────────────────────────────────────────────────────────────

    override suspend fun strip(uri: Uri, options: RemovalOptions, outputUri: Uri): Int =
        withContext(Dispatchers.IO) {
            val tempFile = makeTempFile("strip")
            try {
                // Copy original to temp
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { out -> input.copyTo(out) }
                }

                val exif = ExifInterface(tempFile.absolutePath)
                val tagsToRemove = mutableSetOf<String>()

                if (options.removeAll || options.removeLocation)  tagsToRemove += locationTags
                if (options.removeAll || options.removeCamera)    tagsToRemove += cameraTags
                if (options.removeAll || options.removeTimestamps) tagsToRemove += timestampTags
                if (options.removeAll || options.removeDevice)    tagsToRemove += deviceTags
                if (options.removeAll || options.removeCopyright) tagsToRemove += copyrightTags

                // Count and remove
                var removed = 0
                for (tag in tagsToRemove) {
                    val v = exif.getAttribute(tag)
                    if (!v.isNullOrEmpty()) {
                        exif.setAttribute(tag, null)
                        removed++
                    }
                }

                // Spoofing: Location Decoy
                if (options.spoofLocation) {
                    // Randomly select between a few famous locations
                    val decoys = listOf(
                        Triple("40.785091", "-73.968285", "Central Park, NYC"), // Central Park
                        Triple("48.858372", "2.294481", "Eiffel Tower, Paris"),   // Eiffel Tower
                        Triple("35.689487", "139.691711", "Tokyo"),               // Tokyo
                        Triple("-33.856784", "151.215297", "Sydney Opera House")  // Sydney
                    )
                    val (lat, lon, _) = decoys.random()
                    
                    // Convert to HMS format for ExifInterface
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, formatLatLonToExif(lat.toDouble()))
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (lat.toDouble() >= 0) "N" else "S")
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, formatLatLonToExif(lon.toDouble()))
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (lon.toDouble() >= 0) "E" else "W")
                    exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, "GPS")
                    exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, "2000:01:01")
                    exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, "00:00:00")
                }

                // Spoofing: Device Masking
                if (options.spoofDevice) {
                    exif.setAttribute(ExifInterface.TAG_MAKE, "Generic")
                    exif.setAttribute(ExifInterface.TAG_MODEL, "Digital Camera")
                    exif.setAttribute(ExifInterface.TAG_SOFTWARE, "MetaShield v1.0")
                }

                // Timestamp anonymization
                if (options.anonymizeTimestamps && !options.removeTimestamps && !options.useTemporalDrift) {
                    val neutral = "2000:01:01 00:00:00"
                    for (tag in timestampTags) {
                        if (!exif.getAttribute(tag).isNullOrEmpty()) {
                            exif.setAttribute(tag, neutral)
                        }
                    }
                }

                // Temporal Drift (Anonymize but preserve relative timelines)
                if (options.useTemporalDrift && !options.removeTimestamps) {
                    applyTemporalDrift(exif, options.driftOffsetMinutes)
                }

                exif.saveAttributes()

                // Write to output
                context.contentResolver.openOutputStream(outputUri)?.use { out ->
                    tempFile.inputStream().use { it.copyTo(out) }
                    
                    if (options.mutateHash) {
                        out.write(kotlin.random.Random.nextBytes(4))
                    }
                }

                removed
            } finally {
                tempFile.delete()
            }
        }

    // ── Write ─────────────────────────────────────────────────────────────────

    override suspend fun write(uri: Uri, fields: List<MetadataField>, outputUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            val tempFile = makeTempFile("write")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { out -> input.copyTo(out) }
                }

                val exif = ExifInterface(tempFile.absolutePath)
                for (field in fields) {
                    exif.setAttribute(field.key, field.value)
                }
                exif.saveAttributes()

                context.contentResolver.openOutputStream(outputUri)?.use { out ->
                    tempFile.inputStream().use { it.copyTo(out) }
                }
                true
            } catch (e: Exception) {
                false
            } finally {
                tempFile.delete()
            }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeTempFile(prefix: String): File =
        File(context.cacheDir, "metashield_${prefix}_${System.currentTimeMillis()}.jpg")

    private fun formatTagName(tag: String): String =
        tag.replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }

    private fun applyTemporalDrift(exif: ExifInterface, offsetMinutes: Int) {
        val format = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
        for (tag in timestampTags) {
            val current = exif.getAttribute(tag)
            if (!current.isNullOrEmpty()) {
                runCatching {
                    val date = format.parse(current)
                    if (date != null) {
                        val cal = Calendar.getInstance().apply { 
                            time = date
                            add(Calendar.MINUTE, offsetMinutes)
                        }
                        exif.setAttribute(tag, format.format(cal.time))
                    }
                }
            }
        }
    }

    private fun formatLatLonToExif(coordinate: Double): String {
        val absolute = Math.abs(coordinate)
        val degrees = absolute.toInt()
        val minutes = ((absolute - degrees) * 60).toInt()
        val seconds = ((absolute - degrees - minutes / 60.0) * 3600 * 1000).toInt()
        return "$degrees/1,$minutes/1,$seconds/1000"
    }
}
