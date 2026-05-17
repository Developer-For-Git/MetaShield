package com.metashield.app.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metashield.app.data.db.entity.HistoryEntity
import com.metashield.app.ui.components.*
import com.metashield.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStripFile: (Uri) -> Unit,
    @Suppress("UNUSED_PARAMETER") onEditMetadata: (Uri) -> Unit,
    onBatchProcess: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToTemplates: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToTemplateSelection: () -> Unit,
    onNavigateToVault: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onNavigateToStego: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToTools: () -> Unit,
    sharedUris: List<Uri> = emptyList(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    batchViewModel: com.metashield.app.ui.batch.BatchQueueViewModel = hiltViewModel()
) {
    val recentHistory by homeViewModel.recentHistory.collectAsStateWithLifecycle()
    val tipIndex      by homeViewModel.tipIndex.collectAsStateWithLifecycle()
    val context        = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        homeViewModel.panicResult.collect { uris ->
            if (uris.isNotEmpty()) {
                batchViewModel.clearQueue()
                batchViewModel.addFiles(uris)
                batchViewModel.setMode(com.metashield.app.ui.batch.BatchProcessingMode.STRIP)
                onBatchProcess()
            } else {
                android.widget.Toast.makeText(context, "No recent media found in the last 24h", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val fileCache = remember { com.metashield.app.data.repository.FileCache(context) }

    LaunchedEffect(sharedUris) {
        if (sharedUris.isNotEmpty()) {
            if (sharedUris.size > 1) {
                batchViewModel.clearQueue()
                sharedUris.forEach { uri ->
                    try { batchViewModel.addFile(fileCache.cacheFile(uri).second) }
                    catch (_: Exception) { try { batchViewModel.addFile(uri) } catch (_: Exception) {} }
                }
                batchViewModel.setMode(com.metashield.app.ui.batch.BatchProcessingMode.STRIP)
                onBatchProcess()
            } else {
                try { onStripFile(fileCache.cacheFile(sharedUris.first()).second) }
                catch (_: Exception) { onStripFile(sharedUris.first()) }
            }
        }
    }

    val panicLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) homeViewModel.triggerPanic()
        else android.widget.Toast.makeText(context, "Permissions required", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun handlePanic() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
            homeViewModel.triggerPanic()
        else panicLauncher.launch(perms)
    }

    Scaffold(
        modifier             = Modifier.fillMaxSize(),
        containerColor       = MaterialTheme.colorScheme.background,
        contentWindowInsets  = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Plain TopAppBar — MediumTopAppBar with exitUntilCollapsedScrollBehavior
            // intercepted and re-laid out on EVERY scroll frame, causing a layout
            // pass that competed with LazyColumn scroll. Single TopAppBar is free.
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier     = Modifier.size(34.dp),
                            shape        = RoundedCornerShape(10.dp),
                            color        = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            // No shadowElevation — saves 1 off-screen GPU render pass
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Shield, null, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("MetaShield", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    AnimatedIconButton(Icons.Outlined.Info, "About", { showAboutDialog = true })
                    AnimatedIconButton(Icons.Outlined.Settings, "Settings", onNavigateToSettings)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        val historyCount by homeViewModel.totalCleanedCount.collectAsStateWithLifecycle()

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "dashboard_hub") {
                DashboardHub(
                    cleanedCount = historyCount,
                    tip          = privacyTips[tipIndex % privacyTips.size],
                    onScanClick  = onNavigateToScanner,
                    modifier     = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            item(key = "security_tools") {
                SectionHeader("Security Tools",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                Spacer(Modifier.height(12.dp))
                FeatureGrid(
                    onPhotoClick = onNavigateToTools,
                    onVideoClick = onNavigateToTools,
                    onDocsClick  = onNavigateToTools,
                    onVaultClick = onNavigateToVault,
                    modifier     = Modifier.padding(horizontal = 16.dp)
                )
            }
            item(key = "nuclear_panic") {
                Spacer(Modifier.height(24.dp))
                NuclearPanicCard(onPanic = { handlePanic() },
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (recentHistory.isNotEmpty()) {
                item(key = "recent_header") {
                    Spacer(Modifier.height(24.dp))
                    SectionHeader("Recent Activity",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                    Spacer(Modifier.height(12.dp))
                }
                // Use itemsIndexed so stagger index is O(1), not O(n) via indexOf
                itemsIndexed(recentHistory, key = { _, entry -> entry.id }) { idx, entry ->
                    val visible = rememberStaggeredVisible(idx)
                    AnimatedVisibility(visible,
                        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { (it * 0.25f).toInt() }) {
                        HistoryRow(
                            entry    = entry,
                            onTap    = { onStripFile(Uri.parse(entry.outputUriString ?: entry.inputUriString)) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("About MetaShield", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MetaShield v1.0\nProtecting your digital footprint by stripping sensitive metadata from your media and files.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "DEVELOPER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Developer-For-Git",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(16.dp))
                            
                            CyberButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Developer-For-Git"))
                                    context.startActivity(intent)
                                },
                                label = "Visit GitHub",
                                icon = Icons.Filled.Code,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SUPPORT THE PROJECT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Scan this QR code to support our development efforts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = com.metashield.app.R.drawable.support_qr),
                                    contentDescription = "Support QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    }
}

// ── DashboardHub ─────────────────────────────────────────────────────────────

@Composable
private fun DashboardHub(
    cleanedCount: Int,
    tip: String,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(visible,
        enter = fadeIn(tween(420)) + slideInVertically(tween(420, easing = FastOutSlowInEasing)) { (it * 0.12f).toInt() }
    ) {
        // Shimmer sweeps over the entire hero card
        ShimmerBox(modifier = modifier.fillMaxWidth()) {
            ElevatedCard(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(28.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HeroShieldSection()

                    Spacer(Modifier.height(20.dp))

                    // Odometer counter — slides vertically when count changes
                    OdometerCounter(
                        count    = cleanedCount,
                        style    = MaterialTheme.typography.displayMedium
                    )
                    Text(
                        "Files Sanitized",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    AssistChip(
                        onClick = {},
                        label   = { Text("Privacy Integrity Verified", style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = { Icon(Icons.Filled.VerifiedUser, null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor          = MaterialTheme.colorScheme.primaryContainer,
                            leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            labelColor              = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = null
                    )

                    Spacer(Modifier.height(16.dp))

                    // Privacy tip card
                    Surface(
                        modifier     = Modifier.fillMaxWidth(),
                        shape        = RoundedCornerShape(16.dp),
                        color        = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.TipsAndUpdates, null,
                                modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text(tip, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // CTA — TapRippleBox comes through CyberButton
                    CyberButton(
                        onClick  = onScanClick,
                        label    = "Run Privacy Audit",
                        icon     = Icons.Outlined.ContentPasteSearch,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ── Hero Shield Section (Sonar + Orbiting Particles + Floating Icon) ──────────

@Composable
private fun HeroShieldSection() {
    val infinite = rememberInfiniteTransition(label = "hero")

    // State<Float> without `by` — values read only in draw-phase lambdas
    val floatY = infinite.animateFloat(
        -5f, 5f,
        infiniteRepeatable(tween(2600, easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)), RepeatMode.Reverse),
        label = "floatY"
    )
    val arcRot = infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "arcRot"
    )

    // Defer ring+particle animations 250 ms so the first frame renders clean.
    // On cold start the main thread already handles: Compose layout, Room query,
    // Hilt DI setup, navigation, stagger animations — adding 4 more animation
    // inits to frame 1 causes visible jank. Shield icon renders immediately.
    var animationsReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250)
        animationsReady = true
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier         = Modifier
            .size(170.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        contentAlignment = Alignment.Center
    ) {
        // Only enter composition after first frame — keeps startup lightweight
        if (animationsReady) {
            SonarPulse(color = primaryColor, modifier = Modifier.matchParentSize())
            OrbitingParticles(color = primaryColor, modifier = Modifier.matchParentSize())
        }

        // Shield renders immediately (only 2 animation values, not 4)
        Surface(
            modifier        = Modifier
                .size(92.dp)
                .graphicsLayer { translationY = floatY.value * density },
            shape           = RoundedCornerShape(28.dp),
            color           = MaterialTheme.colorScheme.primaryContainer,
            contentColor    = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier
                    .size(78.dp)
                    .graphicsLayer { rotationZ = arcRot.value }
                ) {
                    drawArc(primaryColor.copy(0.22f), 0f, 270f, false,
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(primaryColor.copy(0.08f), 280f, 60f, false,
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                }
                Icon(Icons.Filled.Shield, null, modifier = Modifier.size(42.dp))
            }
        }
    }
}

// ── Feature Grid ──────────────────────────────────────────────────────────────

private data class TileData(
    val title: String, val subtitle: String, val icon: ImageVector,
    val cColor: @Composable () -> Color, val nColor: @Composable () -> Color,
    val onClick: () -> Unit
)

@Composable
private fun FeatureGrid(
    onPhotoClick: () -> Unit, onVideoClick: () -> Unit,
    onDocsClick: () -> Unit, onVaultClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tiles = listOf(
        TileData("Clean Photos",  "EXIF & location",   Icons.Outlined.Image,
            { MaterialTheme.colorScheme.primaryContainer },          { MaterialTheme.colorScheme.onPrimaryContainer },   onPhotoClick),
        TileData("Clean Videos",  "Video metadata",    Icons.Outlined.VideoFile,
            { MaterialTheme.colorScheme.tertiaryContainer },         { MaterialTheme.colorScheme.onTertiaryContainer },  onVideoClick),
        TileData("Clean Docs",    "PDF & documents",   Icons.Outlined.Description,
            { MaterialTheme.colorScheme.secondaryContainer },        { MaterialTheme.colorScheme.onSecondaryContainer }, onDocsClick),
        TileData("Ghost Vault",   "Encrypted storage", Icons.Outlined.EnhancedEncryption,
            { MaterialTheme.colorScheme.surfaceContainerHighest },   { MaterialTheme.colorScheme.onSurface },            onVaultClick),
    )

    // rememberSaveable survives recomposition AND LazyColumn item recycling.
    // Once true, tiles skip their enter animation on scroll-back — the #1 cause
    // of lag when scrolling up/down past this grid.
    var hasAnimated by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { colIdx, tile ->
                    val idx = rowIdx * 2 + colIdx

                    if (!hasAnimated) {
                        // First time only: stagger-in animation
                        val visible = rememberStaggeredVisible(idx, 70L)
                        LaunchedEffect(visible) { if (visible && idx == tiles.lastIndex) hasAnimated = true }
                        AnimatedVisibility(
                            visible  = visible,
                            modifier = Modifier.weight(1f),
                            enter    = fadeIn(tween(280)) + scaleIn(tween(280), 0.90f)
                        ) {
                            TapRippleBox(
                                onClick     = tile.onClick,
                                rippleColor = tile.cColor(),
                                modifier    = Modifier.fillMaxWidth()
                            ) {
                                FeatureTileContent(tile)
                            }
                        }
                    } else {
                        // Already animated: render directly, zero overhead on scroll
                        Box(modifier = Modifier.weight(1f)) {
                            TapRippleBox(
                                onClick     = tile.onClick,
                                rippleColor = tile.cColor(),
                                modifier    = Modifier.fillMaxWidth()
                            ) {
                                FeatureTileContent(tile)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FeatureTileContent(tile: TileData) {
    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Surface(
                modifier     = Modifier.size(48.dp),
                shape        = RoundedCornerShape(14.dp),
                color        = tile.cColor(),
                contentColor = tile.nColor()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(tile.icon, null, Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(tile.title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            Text(tile.subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Emergency Card ─────────────────────────────────────────────────────────────

@Composable
private fun NuclearPanicCard(onPanic: () -> Unit, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(visible,
        enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { (it * 0.1f).toInt() }) {
        ElevatedCard(
            modifier  = modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.errorContainer),
            elevation = CardDefaults.elevatedCardElevation(3.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Surface(
                    shape        = RoundedCornerShape(50.dp),
                    color        = MaterialTheme.colorScheme.error.copy(0.15f),
                    contentColor = MaterialTheme.colorScheme.error
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Emergency Protocol", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NuclearButton(onPanic)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Nuclear Panic", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(4.dp))
                        Text("Strip all metadata from media in the last 24h. Long-press to activate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(0.75f),
                            lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NuclearButton(onPanic: () -> Unit) {
    var isPressing by remember { mutableStateOf(false) }
    val progress   by animateFloatAsState(if (isPressing) 1f else 0f, tween(1800), label = "nuke")
    val infinite    = rememberInfiniteTransition(label = "nukePulse")
    // State<Float> without `by` — values read in draw phase lambdas below
    val pulseScale = infinite.animateFloat(1f, 1.13f,
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ps")
    val glowAlpha  = infinite.animateFloat(0.08f, 0.32f,
        infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "ga")

    val errorColor = MaterialTheme.colorScheme.error

    LaunchedEffect(progress) { if (progress >= 1f) { onPanic(); isPressing = false } }

    Box(
        modifier = Modifier
            .size(76.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent()
                        if (e.type == PointerEventType.Press)   isPressing = true
                        if (e.type == PointerEventType.Release) isPressing = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Pulsing glow ring — scale + alpha read in graphicsLayer/drawBehind (draw phase)
        Box(
            modifier = Modifier
                .size(70.dp)
                .graphicsLayer {
                    val s = if (isPressing) 1f else pulseScale.value  // draw-phase read
                    scaleX = s; scaleY = s
                }
                .clip(CircleShape)
                .drawBehind {
                    drawCircle(errorColor.copy(alpha = if (isPressing) 0.3f else glowAlpha.value))  // draw-phase read
                }
        )

        FilledIconButton(
            onClick  = {},
            modifier = Modifier.size(56.dp)
                .graphicsLayer { scaleX = if (isPressing) 0.88f else 1f; scaleY = if (isPressing) 0.88f else 1f },
            colors   = IconButtonDefaults.filledIconButtonColors(
                containerColor = errorColor,
                contentColor   = MaterialTheme.colorScheme.onError)
        ) {
            CircularProgressIndicator(
                progress   = { progress }, modifier = Modifier.fillMaxSize(),
                color      = MaterialTheme.colorScheme.onError, strokeWidth = 3.dp,
                trackColor = Color.Transparent, strokeCap = StrokeCap.Round)
            Icon(Icons.Filled.GppBad, null,
                Modifier.size(22.dp).alpha(if (isPressing && (progress * 10).toInt() % 2 == 0) 0.5f else 1f))
        }
    }
}

// ── History Row ────────────────────────────────────────────────────────────────

@Composable
private fun HistoryRow(entry: HistoryEntity, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val statusColor = if (entry.success) SafeGreen else DangerRed
    TapRippleBox(onClick = onTap, modifier = modifier.fillMaxWidth(), rippleColor = statusColor) {
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.elevatedCardElevation(2.dp)) {
            ListItem(
                leadingContent = {
                    Surface(
                        modifier     = Modifier.size(38.dp),
                        shape        = RoundedCornerShape(10.dp),
                        color        = statusColor.copy(0.12f),
                        contentColor = statusColor
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(if (entry.success) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                null, Modifier.size(20.dp))
                        }
                    }
                },
                headlineContent = {
                    Text(entry.fileName, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    Text("${entry.action} · ${entry.fieldsRemoved} fields stripped",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingContent = { StatusBadge(if (entry.success) "Clean" else "Failed", statusColor) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedIconButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    com.metashield.app.ui.components.AnimatedIconButton(onClick, icon, desc)
}

private val privacyTips = listOf(
    "Photos may contain your home GPS coordinates in EXIF — strip before sharing!",
    "Your device model and serial number are embedded in every photo you take.",
    "EXIF timestamps can reveal your daily routine — remove before sharing.",
    "Camera metadata can fingerprint your device across social platforms.",
    "Use Batch Process to clean entire albums in one tap.",
    "Content creators: add copyright fields to protect your work.",
)
