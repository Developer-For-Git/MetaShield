package com.metashield.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── MetaShield Material You Color Schemes ─────────────────────────────────────

private val ShieldDarkScheme = darkColorScheme(
    primary                = md_primary_dark,
    onPrimary              = md_on_primary_dark,
    primaryContainer       = md_primary_container_dark,
    onPrimaryContainer     = md_on_primary_container_dark,

    secondary              = md_secondary_dark,
    onSecondary            = md_on_secondary_dark,
    secondaryContainer     = md_secondary_container_dark,
    onSecondaryContainer   = md_on_secondary_container_dark,

    tertiary               = md_tertiary_dark,
    onTertiary             = md_on_tertiary_dark,
    tertiaryContainer      = md_tertiary_container_dark,
    onTertiaryContainer    = md_on_tertiary_container_dark,

    error                  = md_error_dark,
    onError                = md_on_error_dark,
    errorContainer         = md_error_container_dark,
    onErrorContainer       = md_on_error_container_dark,

    background             = md_background_dark,
    onBackground           = md_on_background_dark,

    surface                = md_surface_dark,
    onSurface              = md_on_surface_dark,
    surfaceVariant         = md_surface_variant_dark,
    onSurfaceVariant       = md_on_surface_variant_dark,
    surfaceContainer       = md_surface_container_dark,
    surfaceContainerHigh   = md_surface_container_high_dark,
    surfaceContainerHighest = md_surface_container_highest_dark,
    surfaceContainerLow    = md_surface_container_low_dark,
    surfaceContainerLowest = md_surface_container_lowest_dark,

    inverseSurface         = md_inverse_surface_dark,
    inverseOnSurface       = md_inverse_on_surface_dark,
    inversePrimary         = md_primary_light,

    outline                = md_outline_dark,
    outlineVariant         = md_outline_variant_dark,
)

private val ShieldLightScheme = lightColorScheme(
    primary                = md_primary_light,
    onPrimary              = md_on_primary_light,
    primaryContainer       = md_primary_container_light,
    onPrimaryContainer     = md_on_primary_container_light,

    secondary              = md_secondary_light,
    onSecondary            = md_on_secondary_light,
    secondaryContainer     = md_secondary_container_light,
    onSecondaryContainer   = md_on_secondary_container_light,

    tertiary               = md_tertiary_light,
    onTertiary             = md_on_tertiary_light,
    tertiaryContainer      = md_tertiary_container_light,
    onTertiaryContainer    = md_on_tertiary_container_light,

    error                  = md_error_light,
    onError                = md_on_error_light,
    errorContainer         = md_error_container_light,
    onErrorContainer       = md_on_error_container_light,

    background             = md_background_light,
    onBackground           = md_on_background_light,

    surface                = md_surface_light,
    onSurface              = md_on_surface_light,
    surfaceVariant         = md_surface_variant_light,
    onSurfaceVariant       = md_on_surface_variant_light,
    surfaceContainer       = md_surface_container_light,
    surfaceContainerHigh   = md_surface_container_high_light,
    surfaceContainerHighest = md_surface_container_highest_light,

    inverseSurface         = md_inverse_surface_light,
    inverseOnSurface       = md_inverse_on_surface_light,
    inversePrimary         = md_primary_dark,

    outline                = md_outline_light,
    outlineVariant         = md_outline_variant_light,
)

@Composable
fun MetaShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,           // Material You — enabled on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ShieldDarkScheme
        else      -> ShieldLightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = Shapes,
        content     = content
    )
}
