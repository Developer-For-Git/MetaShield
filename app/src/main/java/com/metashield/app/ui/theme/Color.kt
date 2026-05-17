package com.metashield.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── MetaShield Material You "Shield Teal" Palette ────────────────────────────
// Seed color: #006874 (Deep Teal)
// Generated via Material Theme Builder

// ── Primary (Teal) ─────────────────────────────────────────────────────────────
val md_primary_dark            = Color(0xFF4FD8EB)   // onPrimary: SpaceCadet
val md_on_primary_dark         = Color(0xFF00363D)
val md_primary_container_dark  = Color(0xFF004F58)
val md_on_primary_container_dark = Color(0xFF97F0FF)

val md_primary_light           = Color(0xFF006874)
val md_on_primary_light        = Color.White
val md_primary_container_light = Color(0xFF97F0FF)
val md_on_primary_container_light = Color(0xFF001F24)

// ── Secondary ──────────────────────────────────────────────────────────────────
val md_secondary_dark          = Color(0xFFB1CBD0)
val md_on_secondary_dark       = Color(0xFF1C3438)
val md_secondary_container_dark = Color(0xFF334B4F)
val md_on_secondary_container_dark = Color(0xFFCDE7EC)

val md_secondary_light         = Color(0xFF4A6267)
val md_on_secondary_light      = Color.White
val md_secondary_container_light = Color(0xFFCDE7EC)
val md_on_secondary_container_light = Color(0xFF051F23)

// ── Tertiary ───────────────────────────────────────────────────────────────────
val md_tertiary_dark           = Color(0xFFB8C4EA)
val md_on_tertiary_dark        = Color(0xFF222E4D)
val md_tertiary_container_dark = Color(0xFF394465)
val md_on_tertiary_container_dark = Color(0xFFD8E0FF)

val md_tertiary_light          = Color(0xFF525E7D)
val md_on_tertiary_light       = Color.White
val md_tertiary_container_light = Color(0xFFD8E0FF)
val md_on_tertiary_container_light = Color(0xFF0E1B37)

// ── Error ──────────────────────────────────────────────────────────────────────
val md_error_dark              = Color(0xFFFFB4AB)
val md_on_error_dark           = Color(0xFF690005)
val md_error_container_dark    = Color(0xFF93000A)
val md_on_error_container_dark = Color(0xFFFFDAD6)

val md_error_light             = Color(0xFFBA1A1A)
val md_on_error_light          = Color.White
val md_error_container_light   = Color(0xFFFFDAD6)
val md_on_error_container_light = Color(0xFF410002)

// ── Neutral Surfaces (Dark) ────────────────────────────────────────────────────
val md_background_dark         = Color(0xFF0E1415)
val md_on_background_dark      = Color(0xFFDDE3E5)
val md_surface_dark            = Color(0xFF0E1415)
val md_on_surface_dark         = Color(0xFFDDE3E5)
val md_surface_variant_dark    = Color(0xFF3F484A)
val md_on_surface_variant_dark = Color(0xFFBFC8CA)
val md_surface_container_dark          = Color(0xFF1A2122)
val md_surface_container_high_dark     = Color(0xFF242B2D)
val md_surface_container_highest_dark  = Color(0xFF2F3637)
val md_surface_container_low_dark      = Color(0xFF161D1E)
val md_surface_container_lowest_dark   = Color(0xFF090F10)
val md_inverse_surface_dark    = Color(0xFFDDE3E5)
val md_inverse_on_surface_dark = Color(0xFF252B2C)
val md_outline_dark            = Color(0xFF899294)
val md_outline_variant_dark    = Color(0xFF3F484A)

// ── Neutral Surfaces (Light) ───────────────────────────────────────────────────
val md_background_light        = Color(0xFFFAFDFD)
val md_on_background_light     = Color(0xFF191C1D)
val md_surface_light           = Color(0xFFF7FAFB)
val md_on_surface_light        = Color(0xFF191C1D)
val md_surface_variant_light   = Color(0xFFDBE4E6)
val md_on_surface_variant_light = Color(0xFF3F484A)
val md_surface_container_light         = Color(0xFFEBEEEF)
val md_surface_container_high_light    = Color(0xFFE5E8EA)
val md_surface_container_highest_light = Color(0xFFDFE2E4)
val md_inverse_surface_light   = Color(0xFF2D3132)
val md_inverse_on_surface_light = Color(0xFFEFF1F2)
val md_outline_light           = Color(0xFF6F797A)
val md_outline_variant_light   = Color(0xFFBFC8CA)

// ── Semantic Signals (unchanged, used throughout app) ─────────────────────────
val SafeGreen      = Color(0xFF1B9C6A)   // Success / clean
val DangerRed      = Color(0xFFBA1A1A)   // Danger / threat (aligned with MD3 error)
val PrivacyAmber   = Color(0xFFB25E00)   // Warning / partial

// ── Backward-compat aliases — keeps un-migrated screens compiling ─────────────
val ObsidianIndigo         = md_primary_light
val ObsidianCyan           = md_primary_dark
val ObsidianViolet         = md_tertiary_light
val ObsidianPink           = md_tertiary_dark
val SpaceVoid              = md_background_dark
val SpaceDeep              = md_surface_dark
val SpaceCard              = md_surface_container_dark
val SpaceRaised            = md_surface_container_high_dark
val GlassWhite             = Color(0xFFFFFFFF)
val GlassBorder            = md_outline_dark
val TextPrimary            = md_on_background_dark
val TextSecondary          = md_on_surface_variant_dark
val TextHint               = md_outline_dark
val LightBackground        = md_background_light
val LightSurface           = md_surface_light
val LightSurfaceVariant    = md_surface_variant_light
val LightOnSurface         = md_on_surface_light
val LightOutline           = md_outline_light
val LightPrimary           = md_primary_light
val LightOnPrimary         = md_on_primary_light

val GradientPrimary  = listOf(md_primary_light, md_primary_dark)
val GradientViolet   = listOf(md_tertiary_light, md_primary_light)
val GradientDanger   = listOf(DangerRed, md_error_dark)
val GradientSuccess  = listOf(SafeGreen, md_primary_dark)
val GradientWarning  = listOf(PrivacyAmber, DangerRed)

// Further compat aliases
val CyberGreen    = SafeGreen
val CyberMagenta  = md_tertiary_dark
val CyberBlue     = md_primary_dark
val CyberYellow   = PrivacyAmber
val CyberRed      = DangerRed
val CyberVoid     = SpaceVoid
val CyberSurface  = SpaceDeep
val CyberSurfaceVariant = SpaceCard
val CyberOutline  = md_outline_dark
val CyberGlitch   = listOf(md_tertiary_dark, md_primary_dark)
val CyberMatrix   = listOf(SafeGreen, Color(0xFF1A5C2A))
val CyberWarning  = listOf(PrivacyAmber, DangerRed)

// MD3 aliases used in Theme.kt
val DarkBackground       = md_background_dark
val DarkSurface          = md_surface_dark
val DarkSurfaceVariant   = md_surface_variant_dark
val DarkOnSurface        = md_on_surface_dark
val DarkOutline          = md_outline_dark
