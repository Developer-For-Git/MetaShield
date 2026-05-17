package com.metashield.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

// ═════════════════════════════════════════════════════════════════════════════
//  SONAR PULSE
//  numRings concentric circles emanate from center staggered in phase,
//  each growing and fading — like a radar / security scanner sweep.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun SonarPulse(
    color: Color,
    modifier: Modifier = Modifier,
    numRings: Int = 2,      // was 3 — 2 rings costs 1 fewer animation init on startup
    periodMs: Int = 2600
) {
    val infinite = rememberInfiniteTransition(label = "sonar")

    @Suppress("NAME_SHADOWING")
    val rings = (0 until numRings).map { i ->
        // FastForward staggers the rings' phases so they are always offset
        infinite.animateFloat(
            initialValue  = 0f,
            targetValue   = 1f,
            animationSpec = infiniteRepeatable(
                animation          = tween(periodMs, easing = LinearEasing),
                initialStartOffset = StartOffset(
                    offsetMillis = periodMs / numRings * i,
                    offsetType   = StartOffsetType.FastForward
                )
            ),
            label = "ring$i"
        )
    }

    Canvas(modifier = modifier) {
        val cx   = size.width  / 2f
        val cy   = size.height / 2f
        val minR = size.minDimension * 0.22f
        val maxR = size.minDimension * 0.62f

        rings.forEach { anim ->
            val p      = anim.value
            val radius = minR + (maxR - minR) * p
            val alpha  = (1f - p).pow(1.7f) * 0.60f
            val stroke = (1f - p) * 3.5f + 0.4f
            drawCircle(
                color  = color.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = radius,
                center = Offset(cx, cy),
                style  = Stroke(width = stroke.dp.toPx())
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  ORBITING PARTICLES
//  4 luminous dots orbit the center at different radii and speeds.
//  One goes counter-clockwise on the inner ring for visual depth.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun OrbitingParticles(
    color: Color,
    modifier: Modifier = Modifier
) {
    val t = rememberInfiniteTransition(label = "orbit")
    // Reduced from 4 particles to 2 — saves 2 animation values initializing
    // on the first composition frame (cold-start lag reduction).
    val a1 = t.animateFloat(  0f,  360f, infiniteRepeatable(tween(3400, easing = LinearEasing)), label = "a1")
    val a2 = t.animateFloat(120f,  480f, infiniteRepeatable(tween(4800, easing = LinearEasing)), label = "a2")

    Canvas(modifier = modifier) {
        val cx = size.width  / 2f
        val cy = size.height / 2f

        data class PDef(val deg: Float, val radius: Float, val dotDp: Float, val alpha: Float)

        listOf(
            PDef(a1.value, size.minDimension * 0.44f, 3.5f, 0.90f),
            PDef(a2.value, size.minDimension * 0.52f, 2.5f, 0.65f),
        ).forEach { pd ->
            val rad = pd.deg * PI.toFloat() / 180f
            val pos = Offset(cx + cos(rad) * pd.radius, cy + sin(rad) * pd.radius)
            val px  = pd.dotDp.dp.toPx()
            drawCircle(color.copy(pd.alpha * 0.18f), px * 3.2f, pos)
            drawCircle(color.copy(pd.alpha),          px,        pos)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  ODOMETER COUNTER
//  Displays an integer. When the value changes the new number slides in
//  vertically like an odometer reel / slot-machine digit.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun OdometerCounter(
    count: Int,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState   = count,
        transitionSpec = {
            val goUp = targetState > initialState
            (slideInVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
                { if (goUp) it else -it } + fadeIn(tween(200)))
                .togetherWith(
                    slideOutVertically(tween(200, easing = FastOutLinearInEasing))
                    { if (goUp) -(it / 4) else it / 4 } + fadeOut(tween(150))
                )
        },
        modifier = modifier,
        label    = "odometer"
    ) { n ->
        Text(text = "$n", style = style, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  TAP RIPPLE BOX
//  Spring-press scale + two canvas ripple rings that expand and fade from
//  the center on tap release.  Much more expressive than MD3's default ripple.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun TapRippleBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rippleColor: Color = Color.Unspecified,
    scaleDown: Float   = 0.93f,
    enabled: Boolean   = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue   = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = PressSpring,
        label         = "trScale"
    )

    val ripple     = remember { Animatable(0f) }
    var prevPress  by remember { mutableStateOf(false) }
    LaunchedEffect(isPressed) {
        if (!isPressed && prevPress && enabled) {
            ripple.snapTo(0f)
            ripple.animateTo(1f, tween(640, easing = FastOutSlowInEasing))
        }
        prevPress = isPressed
    }
    val rp = ripple.value

    val rc = if (rippleColor == Color.Unspecified)
        MaterialTheme.colorScheme.primary else rippleColor

    Box(
        modifier = modifier
            .scale(pressScale)
            .drawWithContent {
                drawContent()
                // Central glow on press
                if (isPressed && enabled)
                    drawCircle(rc.copy(alpha = 0.09f), size.minDimension * 0.6f)

                // Expanding ripple rings on release
                if (rp > 0f && rp < 1f) {
                    val r1 = size.minDimension * 0.46f + size.minDimension * 0.6f * rp
                    val a1 = ((1f - rp).pow(1.8f) * 0.42f).coerceIn(0f, 1f)
                    drawCircle(rc.copy(a1), r1,
                        style = Stroke(((1f - rp) * 3.2f + 0.3f).dp.toPx()))

                    val rp2 = (rp * 1.4f).coerceIn(0f, 1f)
                    val r2  = size.minDimension * 0.3f + size.minDimension * 0.75f * rp2
                    val a2  = ((1f - rp2).pow(2.8f) * 0.20f).coerceIn(0f, 1f)
                    drawCircle(rc.copy(a2), r2,
                        style = Stroke(((1f - rp2) * 1.5f + 0.2f).dp.toPx()))
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = enabled,
                onClick           = onClick
            ),
        content = content
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  SHIMMER BOX
//  A diagonal shimmer highlight periodically sweeps across the card surface,
//  like the premium shimmer in banking / fintech app hero cards.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    delayMs: Int = 4200,
    sweepMs: Int = 1400,
    content: @Composable BoxScope.() -> Unit
) {
    val t = rememberInfiniteTransition(label = "shimmer")
    // No `by` — store as State<Float> so .value is only read inside
    // the drawWithContent lambda (draw phase), causing zero recompositions.
    val posState = t.animateFloat(
        initialValue  = -0.7f,
        targetValue   =  1.7f,
        animationSpec = infiniteRepeatable(
            animation          = tween(sweepMs, easing = LinearEasing),
            initialStartOffset = StartOffset(delayMs, StartOffsetType.Delay)
        ),
        label = "shimPos"
    )

    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            val pos = posState.value  // draw-phase read — no recomposition
            drawRect(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.30f to Color.White.copy(alpha = 0.04f),
                        0.50f to Color.White.copy(alpha = 0.10f),
                        0.70f to Color.White.copy(alpha = 0.04f),
                        1.00f to Color.Transparent
                    ),
                    start = Offset(size.width * pos,          0f),
                    end   = Offset(size.width * (pos + 0.6f), size.height)
                ),
                size = size
            )
        },
        content = content
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  PREMIUM NAVIGATION BAR  (data holder)
//  Used by MainShell — keeps icon definitions close to this file.
// ═════════════════════════════════════════════════════════════════════════════
data class NavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// ─────────────────────────────────────────────────────────────────────────────
//  PremiumNavigationBar composable
//  Draws a spring-animated pill that slides between tab positions in Canvas.
//  Icons spring-scale on selection; label color animates with tween.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PremiumNavigationBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillColor     = MaterialTheme.colorScheme.primaryContainer
    val activeColor   = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val bgColor       = MaterialTheme.colorScheme.surfaceContainer

    Surface(
        modifier      = modifier.fillMaxWidth(),
        color         = bgColor,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .navigationBarsPadding()
        ) {
            val totalPx = constraints.maxWidth.toFloat()
            val tabPx   = totalPx / items.size
            val pillWPx = tabPx * 0.64f

            // The pill's center X slides with a low-bouncy spring
            val pillCx by animateFloatAsState(
                targetValue   = tabPx * selectedIndex + tabPx / 2f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness    = Spring.StiffnessMedium
                ),
                label = "pillSlide"
            )

            // Draw the sliding pill in Canvas (avoids any compose layout overhead)
            Canvas(modifier = Modifier.matchParentSize()) {
                val pH  = 42.dp.toPx()
                val top = (size.height - pH) / 2f
                drawRoundRect(
                    color        = pillColor,
                    topLeft      = Offset(pillCx - pillWPx / 2f, top),
                    size         = Size(pillWPx, pH),
                    cornerRadius = CornerRadius(pH / 2f)
                )
            }

            // Tab item row on top of the pill canvas
            Row(modifier = Modifier.matchParentSize()) {
                items.forEachIndexed { idx, item ->
                    val selected = idx == selectedIndex

                    val iconScale by animateFloatAsState(
                        targetValue   = if (selected) 1.22f else 0.90f,
                        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium),
                        label         = "navScale$idx"
                    )
                    val iconColor by animateColorAsState(
                        targetValue   = if (selected) activeColor else inactiveColor,
                        animationSpec = tween(220),
                        label         = "navColor$idx"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null
                            ) { onItemSelected(idx) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector        = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint               = iconColor,
                                modifier           = Modifier.scale(iconScale).size(24.dp)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text       = item.label,
                                style      = MaterialTheme.typography.labelSmall,
                                color      = iconColor,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
