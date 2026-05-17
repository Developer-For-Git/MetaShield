package com.metashield.app.ui.batch

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metashield.app.data.model.BatchItem
import com.metashield.app.data.model.BatchItemStatus
import com.metashield.app.data.repository.FileCache
import com.metashield.app.ui.components.*
import com.metashield.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchQueueScreen(
    onNavigateUp: () -> Unit,
    onNavigateToResults: () -> Unit,
    viewModel: BatchQueueViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val fileCache = remember { FileCache(context) }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val cached = uris.mapNotNull { uri -> try { fileCache.cacheFile(uri).second } catch (_: Exception) { uri } }
                viewModel.addFiles(cached)
            }
        }
    }

    LaunchedEffect(state.progress.isFinished, state.isRunning) {
        if (state.progress.isFinished && !state.isRunning && state.queue.isNotEmpty()) {
            onNavigateToResults()
        }
    }

    CyberScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "SHIELD QUEUE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = 2.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (state.isRunning) ObsidianCyan else ObsidianIndigo.copy(0.4f)))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (state.isRunning) "ACTIVE_PROTOCOL_v4.2" else "QUEUE_IDLE_v1.0",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary.copy(0.6f),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextSecondary) } },
                actions = {
                    if (state.queue.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearQueue() }, enabled = !state.isRunning) {
                            Icon(Icons.Outlined.DeleteSweep, "Clear", tint = DangerRed.copy(alpha = 0.7f))
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (state.queue.isNotEmpty()) {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(Brush.verticalGradient(listOf(SpaceVoid.copy(0.1f), SpaceVoid)))
                        .drawBehind {
                            // Neon Path Border
                            val strokeWidth = 1.5.dp.toPx()
                            drawLine(
                                brush = Brush.horizontalGradient(listOf(Color.Transparent, ObsidianCyan.copy(0.4f), ObsidianPink.copy(0.4f), Color.Transparent)),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                strokeWidth = strokeWidth
                            )
                        }
                ) {
                    Column(modifier = Modifier.padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)) {
                        if (state.isRunning) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "INJECTING_PAYLOAD...",
                                        style = MaterialTheme.typography.labelSmall, 
                                        fontFamily = FontFamily.Monospace,
                                        color = ObsidianCyan,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        "${state.progress.completed + state.progress.failed}/${state.progress.total}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { if (state.progress.total > 0) (state.progress.completed + state.progress.failed).toFloat() / state.progress.total else 0f },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = ObsidianCyan, 
                                    trackColor = SpaceRaised
                                )
                                Spacer(Modifier.height(16.dp))
                                CyberButton(
                                    onClick = { viewModel.cancelBatch() }, 
                                    label = "CANCEL_BATCH",
                                    gradient = listOf(DangerRed, DangerRed.copy(0.7f)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Add Action Segment
                                Box(
                                    modifier = Modifier
                                        .weight(0.4f)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(GlassWhite.copy(0.04f))
                                        .border(1.dp, GlassWhite.copy(0.08f), RoundedCornerShape(18.dp))
                                        .clickable { pickerLauncher.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf")) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Add, null, tint = ObsidianCyan, modifier = Modifier.size(24.dp))
                                }

                                // Batch Injection Segment
                                Box(
                                    modifier = Modifier
                                        .weight(0.6f)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Brush.linearGradient(GradientPrimary))
                                        .clickable { viewModel.startBatch() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Surface Shimmer
                                    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
                                    val shimmerX by infiniteTransition.animateFloat(
                                        initialValue = -100f, targetValue = 500f,
                                        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
                                        label = "x"
                                    )
                                    Box(modifier = Modifier.fillMaxSize().drawBehind {
                                        drawRect(
                                            brush = Brush.linearGradient(
                                                0f to Color.Transparent, 
                                                0.5f to Color.White.copy(0.12f), 
                                                1f to Color.Transparent,
                                                start = androidx.compose.ui.geometry.Offset(shimmerX, 0f),
                                                end = androidx.compose.ui.geometry.Offset(shimmerX + 100f, size.height)
                                            )
                                        )
                                    })

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Shield, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "INJECT_SHIELD",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.queue.isEmpty()) {
            // ── Empty state ───────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Animated shield core
                    val infiniteTransition = rememberInfiniteTransition(label = "empty")
                    val orbitRotation by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
                        label = "orbit"
                    )
                    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                        // Orbital rings
                        Box(modifier = Modifier.size(120.dp).rotate(orbitRotation).border(1.dp, Brush.sweepGradient(listOf(ObsidianCyan.copy(0.4f), Color.Transparent)), CircleShape))
                        Box(modifier = Modifier.size(140.dp).rotate(-orbitRotation * 1.5f).border(1.dp, Brush.sweepGradient(listOf(ObsidianPink.copy(0.2f), Color.Transparent)), CircleShape))
                        
                        GlassIconBox(icon = Icons.Outlined.Shield, gradient = GradientPrimary, size = 80.dp, iconSize = 40.dp, shape = CircleShape)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Task Queue is Empty", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Black)
                        Text("Add files to initialize metadata sanitization", style = MaterialTheme.typography.bodySmall, color = TextSecondary.copy(0.6f))
                    }
                    CyberButton(
                        onClick = { pickerLauncher.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf")) },
                        label = "ADD_FILES",
                        icon = Icons.Outlined.Add,
                        modifier = Modifier.width(180.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.queue, key = { it.id }) { item ->
                    BatchFileCard(item = item, onRemove = { viewModel.removeFile(item.id) })
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun BatchFileCard(item: BatchItem, onRemove: () -> Unit) {
    val iconAndColor = when (item.fileItem.fileType.name) {
        "PHOTO"    -> Pair(Icons.Outlined.Image, ObsidianCyan)
        "VIDEO"    -> Pair(Icons.Outlined.Videocam, ObsidianViolet)
        "AUDIO"    -> Pair(Icons.Outlined.AudioFile, ObsidianPink)
        "DOCUMENT" -> Pair(Icons.Outlined.PictureAsPdf, DangerRed)
        else       -> Pair(Icons.AutoMirrored.Outlined.InsertDriveFile, ObsidianIndigo)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "card")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(14.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Identity Glow Icon
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(32.dp).background(Brush.radialGradient(listOf(iconAndColor.second.copy(alpha = 0.15f * glowPulse), Color.Transparent))))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(iconAndColor.second.copy(alpha = 0.05f))
                            .border(1.dp, iconAndColor.second.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(iconAndColor.first, null, tint = iconAndColor.second, modifier = Modifier.size(22.dp))
                    }
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.fileItem.name, 
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = TextPrimary, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val statusText = when (item.status) {
                        BatchItemStatus.DONE       -> "SANITIZED_SUCCESS"
                        BatchItemStatus.ERROR      -> "PROTOCOL_FAILURE"
                        BatchItemStatus.PROCESSING -> "SCRUBBING_IDENTITY..."
                        BatchItemStatus.PAUSED     -> "PROCESS_SUSPENDED"
                        else                       -> "PAYLOAD: ${(item.fileItem.size / 1024)} KB"
                    }
                    
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = when(item.status) {
                            BatchItemStatus.DONE -> SafeGreen
                            BatchItemStatus.ERROR -> DangerRed
                            BatchItemStatus.PROCESSING -> ObsidianCyan
                            else -> TextSecondary.copy(0.7f)
                        },
                        letterSpacing = 0.5.sp
                    )
                }
                
                // Action/Status Icon
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                    when (item.status) {
                        BatchItemStatus.PENDING -> {
                            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Close, "Remove", tint = TextHint, modifier = Modifier.size(16.dp))
                            }
                        }
                        BatchItemStatus.DONE -> Icon(Icons.Filled.CheckCircle, null, tint = SafeGreen.copy(0.7f), modifier = Modifier.size(20.dp))
                        BatchItemStatus.ERROR -> Icon(Icons.Filled.Error, null, tint = DangerRed.copy(0.7f), modifier = Modifier.size(20.dp))
                        BatchItemStatus.PROCESSING -> {
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f, targetValue = 360f,
                                animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
                                label = "rot"
                            )
                            Icon(Icons.AutoMirrored.Filled.RotateRight, null, tint = ObsidianCyan, modifier = Modifier.size(20.dp).rotate(rotation))
                        }
                        else -> {}
                    }
                }
            }
            
            // Progress Track
            if (item.status == BatchItemStatus.PROCESSING) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = ObsidianCyan.copy(0.6f), 
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
