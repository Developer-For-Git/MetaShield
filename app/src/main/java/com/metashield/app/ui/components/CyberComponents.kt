package com.metashield.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metashield.app.ui.theme.*
import kotlinx.coroutines.delay

// ═════════════════════════════════════════════════════════════════════════════
//  ANIMATION UTILITIES
// ═════════════════════════════════════════════════════════════════════════════

/** Spring physics used for all interactive press animations. Feels bouncy but snappy. */
val PressSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness    = Spring.StiffnessMedium
)

/** Softer spring for card-level animations (larger elements). */
val CardSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness    = Spring.StiffnessMediumLow
)

/**
 * Remembers a staggered entrance visibility flag.
 * [index] controls how long this item waits before appearing.
 * Capped at index 6 to avoid long delays on big lists.
 */
@Composable
fun rememberStaggeredVisible(index: Int, staggerMs: Long = 65L): Boolean {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(minOf(index, 6) * staggerMs)
        visible = true
    }
    return visible
}

/**
 * Wraps any composable with spring-press scale + dual canvas ripple rings.
 * Delegates to [TapRippleBox] so all interactive surfaces share the same
 * premium canvas effect.
 */
@Composable
fun PressAnimatedBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scaleDown: Float = 0.93f,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    TapRippleBox(
        onClick    = onClick,
        modifier   = modifier,
        scaleDown  = scaleDown,
        enabled    = enabled,
        content    = content
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  CORE COMPONENTS
// ═════════════════════════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────────────────────────────────
//  CyberScaffold  →  MD3 Scaffold
//  contentWindowInsets = WindowInsets(0) — system insets consumed by MainShell.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CyberScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier             = modifier.fillMaxSize(),
        topBar               = topBar,
        bottomBar            = bottomBar,
        floatingActionButton = floatingActionButton,
        containerColor       = MaterialTheme.colorScheme.background,
        contentColor         = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets  = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        content(padding)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CyberCard  →  Premium ElevatedCard with spring press
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    @Suppress("UNUSED_PARAMETER") borderColor: Color = Color.Unspecified,   // legacy compat
    containerColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier  = modifier,
        shape     = shape,
        colors    = CardDefaults.elevatedCardColors(
            containerColor = if (containerColor != Color.Unspecified && containerColor != Color.Transparent)
                containerColor else MaterialTheme.colorScheme.surfaceContainer,
            contentColor   = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CyberButton  →  Pill Button with spring press bounce
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CyberButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    @Suppress("UNUSED_PARAMETER") gradient: List<Color> = emptyList(),
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
) {
    // Shared interactionSource so the spring-scale tracks the Button's own press state.
    // DO NOT wrap Button in TapRippleBox — Button consumes all touch events itself and
    // the outer clickable would never fire.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed && enabled) 0.93f else 1f,
        animationSpec = PressSpring,
        label         = "btnScale"
    )

    Button(
        onClick           = onClick,
        modifier          = modifier.scale(scale),
        enabled           = enabled,
        shape             = RoundedCornerShape(50.dp),
        interactionSource = interactionSource,
        contentPadding    = contentPadding
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}


// ─────────────────────────────────────────────────────────────────────────────
//  CyberIconButton  →  Spring-press FilledTonalIconButton
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CyberIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") color: Color = Color.Unspecified,  // legacy compat
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed && enabled) 0.82f else 1f,
        animationSpec = PressSpring,
        label         = "iconBtnScale"
    )
    FilledTonalIconButton(
        onClick           = onClick,
        enabled           = enabled,
        interactionSource = interactionSource,
        modifier          = modifier.scale(scale)
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  AnimatedIconButton — spring-press plain IconButton (for TopAppBar use)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.80f else 1f,
        animationSpec = PressSpring,
        label         = "topBarIconScale"
    )
    IconButton(
        onClick           = onClick,
        interactionSource = interactionSource,
        modifier          = modifier.scale(scale)
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  StatusBadge  →  Premium tonal pill badge
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier     = modifier,
        shape        = RoundedCornerShape(50.dp),
        color        = color.copy(alpha = 0.15f),
        contentColor = color,
        border       = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text      = text,
            style     = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier  = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  GradientText
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GradientText(
    text: String,
    @Suppress("UNUSED_PARAMETER") gradient: List<Color> = emptyList(),
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
    modifier: Modifier = Modifier
) {
    Text(text = text, style = style, color = MaterialTheme.colorScheme.primary, modifier = modifier)
}

// ─────────────────────────────────────────────────────────────────────────────
//  GlassIconBox  →  Tonal icon container with persistent breathing pulse
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlassIconBox(
    icon: ImageVector,
    @Suppress("UNUSED_PARAMETER") gradient: List<Color> = emptyList(),  // legacy compat
    size: Dp = 52.dp,
    iconSize: Dp = 26.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    animated: Boolean = false
) {
    // Only create the infinite transition when breathing is actually needed.
    val breatheScaleState = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "breathe")
        infiniteTransition.animateFloat(
            initialValue  = 1f, targetValue = 1.06f,
            animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label         = "breatheScale"
        )
    } else null

    Surface(
        // graphicsLayer reads .value in draw phase — no recomposition on
        // animation frames even when animated = true.
        modifier        = Modifier.size(size).graphicsLayer {
            val s = breatheScaleState?.value ?: 1f
            scaleX = s; scaleY = s
        },
        shape           = shape,
        color           = containerColor,
        contentColor    = contentColor,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(iconSize))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PulsingDot  —  animated status dot
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PulsingDot(color: Color, size: Dp = 10.dp, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    // No `by` — State<Float> references read in draw phase only (zero recompositions)
    val alphaState = infiniteTransition.animateFloat(
        initialValue  = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "alpha"
    )
    val scaleState = infiniteTransition.animateFloat(
        initialValue  = 0.85f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "dotScale"
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val s = scaleState.value   // draw-phase read
                scaleX = s; scaleY = s
            }
            .clip(CircleShape)
            .drawBehind {
                drawCircle(color.copy(alpha = alphaState.value))  // draw-phase read
            }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  SectionHeader  →  Premium tonal pill label with slide-in entrance
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Surface(
        modifier     = modifier,
        shape        = RoundedCornerShape(50.dp),
        color        = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text         = title.uppercase(),
            style        = MaterialTheme.typography.labelSmall,
            fontWeight   = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier     = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SwitchOption  →  MD3 ListItem toggle row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SwitchOption(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier          = modifier.fillMaxWidth(),
        headlineContent   = { Text(title, style = MaterialTheme.typography.bodyMedium) },
        supportingContent = { if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        trailingContent   = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        colors            = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  CyberActionBottomBar
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CyberActionBottomBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BottomAppBar(
        modifier       = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        content()
    }
}
