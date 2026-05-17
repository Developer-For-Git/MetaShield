package com.metashield.app.data.model

data class RemovalOptions(
    val removeLocation: Boolean = true,
    val removeCamera: Boolean = false,
    val removeTimestamps: Boolean = false,
    val removeDevice: Boolean = true,
    val removeCopyright: Boolean = false,
    val removeTechnical: Boolean = false,
    val removeCustom: Boolean = false,
    val removeAll: Boolean = false,
    // Preserve codec, resolution, duration even when stripping
    val preserveEssentials: Boolean = true,
    // Replace real timestamps with 2000-01-01 instead of deleting
    val anonymizeTimestamps: Boolean = false,
    // Overwrite source file instead of creating a copy
    val overwriteOriginal: Boolean = false,
    // Replace real GPS with decoy location
    val spoofLocation: Boolean = false,
    // Replace device make/model with generic labels
    val spoofDevice: Boolean = false,
    // Remove ultrasonic watermarks from audio
    // Remove ultrasonic watermarks from audio
    val removeWatermarks: Boolean = false,
    // Deep Hash Anonymizer
    val mutateHash: Boolean = false,
    // Temporal Drift (Anonymize time while preserving order)
    val useTemporalDrift: Boolean = false,
    val driftOffsetMinutes: Int = 0
) {
    companion object {
        val STRIP_ALL = RemovalOptions(
            removeLocation = true, removeCamera = true, removeTimestamps = true,
            removeDevice = true, removeCopyright = true, removeTechnical = false,
            removeCustom = true, removeAll = true, preserveEssentials = true
        )

        val STRIP_LOCATION_ONLY = RemovalOptions(
            removeLocation = true
        )

        val PRIVACY_SAFE = RemovalOptions(
            removeLocation = true, removeDevice = true, removeTimestamps = false,
            preserveEssentials = true
        )

        val SOCIAL_SHARE = RemovalOptions(
            removeLocation = true, removeDevice = true, removeTimestamps = true,
            removeCopyright = false, preserveEssentials = true
        )

        // Social Profiles
        val PROFILE_WHATSAPP = RemovalOptions(
            removeLocation = true, removeDevice = false, removeTimestamps = true,
            preserveEssentials = true, mutateHash = true
        )
        val PROFILE_DISCORD = RemovalOptions(
            removeLocation = true, removeDevice = true, removeTimestamps = true,
            preserveEssentials = true, spoofDevice = true, mutateHash = true
        )
        val PROFILE_STEALTH = RemovalOptions(
            removeLocation = true, removeCamera = true, removeTimestamps = true,
            removeDevice = true, removeCopyright = true, removeTechnical = true,
            removeCustom = true, removeAll = true, preserveEssentials = false,
            spoofLocation = true, spoofDevice = true, anonymizeTimestamps = true,
            mutateHash = true, useTemporalDrift = true
        )
    }
}
