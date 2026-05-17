package com.metashield.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── Obsidian Shield Shape Tokens ──────────────────────────────────────────────

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small  = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large  = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Named convenience shapes
val CardShape        = RoundedCornerShape(20.dp)   // Glass cards
val ButtonShape      = RoundedCornerShape(14.dp)   // Primary buttons
val ChipShape        = RoundedCornerShape(50.dp)   // Pills / chips
val DialogShape      = RoundedCornerShape(28.dp)   // Dialogs & bottom sheets
val InputShape       = RoundedCornerShape(14.dp)   // Text fields
val BadgeShape       = RoundedCornerShape(50.dp)   // Status badges
val IconContShape    = RoundedCornerShape(14.dp)   // Icon containers

// Legacy aliases for backward compat during migration
val CyberCardShape   = CardShape
val CyberButtonShape = ButtonShape
