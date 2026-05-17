package com.metashield.app.data.model

enum class MetadataCategory(val displayName: String, val baseWeight: Int) {
    LOCATION("Location", 50),
    CAMERA("Camera", 10),
    TIMESTAMPS("Timestamps", 15),
    DEVICE("Device", 25),
    COPYRIGHT("Copyright", 5),
    TECHNICAL("Technical", 2),
    AUDIO("Audio", 10),
    CUSTOM("Custom", 5),
    UNKNOWN("Unknown", 0)
}

enum class SensitivityLevel(val weightMultiplier: Float) {
    HIGH(1.0f),    // GPS, device ID, serial number
    MEDIUM(0.5f),  // Timestamps, software version
    LOW(0.1f)      // Technical/codec info
}

data class MetadataField(
    val key: String,
    val tag: String,
    val value: String,
    val category: MetadataCategory,
    val sensitivityLevel: SensitivityLevel = SensitivityLevel.LOW,
    val isSensitive: Boolean = sensitivityLevel != SensitivityLevel.LOW,
    val isEditable: Boolean = true
) {
    /** Calculates the reduction in privacy score caused by this field. */
    val privacyImpact: Int
        get() = (category.baseWeight * sensitivityLevel.weightMultiplier).toInt()
}
