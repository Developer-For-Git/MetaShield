package com.metashield.app.ui.batch

import android.net.Uri
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metashield.app.data.model.BatchItem
import com.metashield.app.data.model.BatchItemStatus
import com.metashield.app.ui.theme.*
import com.metashield.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchResultsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToViewer: (Uri) -> Unit,
    onFinished: () -> Unit,
    viewModel: BatchQueueViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val finishedItems = remember(state.queue) { 
        state.queue.filter { it.status == BatchItemStatus.DONE || it.status == BatchItemStatus.ERROR } 
    }
    val completedItems = remember(finishedItems) {
        finishedItems.filter { it.status == BatchItemStatus.DONE }
    }

    CyberScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Processing Complete", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    TextButton(onClick = { viewModel.selectAll(state.selectedIds.size != completedItems.size) }) {
                        Text(if (state.selectedIds.size == completedItems.size) "Clear" else "Select All", color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (finishedItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No files were processed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            val isSuccess = completedItems.size == finishedItems.size
                            IntegritySuccessHero(
                                isSuccess = isSuccess,
                                message = if (isSuccess) "Cleanup successful" else "${completedItems.size}/${finishedItems.size} files cleaned",
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        items(finishedItems, key = { it.id }) { item ->
                            ResultItemCard(
                                item = item,
                                name = item.fileItem.name
                                    .replaceFirst(Regex("^[a-fA-F0-9\\-]{36}_"), "")
                                    .replaceFirst(Regex("^\\d+_"), ""),
                                isSelected = state.selectedIds.contains(item.id),
                                onToggle = { viewModel.toggleSelection(item.id) },
                                onClick = { item.outputUri?.let { onNavigateToViewer(it) } },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        
                        item { Spacer(Modifier.height(80.dp)) }
                    }

                    // ── Output Command Console ──────────────────────────────────
                    if (completedItems.isNotEmpty()) {
                        OutputCommandConsole(
                            onShare = { viewModel.shareSelected(context) },
                            onSave = { viewModel.saveSelectedToDevice() },
                            onFinish = onFinished,
                            isFinishedEnabled = true,
                            isActionsEnabled = state.selectedIds.isNotEmpty(),
                            isSaving = state.isSavingAll
                        )
                    } else {
                        Box(Modifier.padding(16.dp)) {
                            CyberButton(
                                onClick = onFinished,
                                label = "TERMINATE_PROTOCOL",
                                icon = Icons.Filled.Close,
                                gradient = GradientDanger,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntegritySuccessHero(isSuccess: Boolean, message: String, modifier: Modifier = Modifier) {
    val statusColor = if (isSuccess) SafeGreen else PrivacyAmber
    val infiniteTransition = rememberInfiniteTransition(label = "hero")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing)),
        label = "pulse"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing)),
        label = "scale"
    )

    CyberCard(modifier = modifier.fillMaxWidth(), containerColor = Color.Transparent) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Expanding Aura
                Box(modifier = Modifier.size(80.dp).scale(pulseScale).clip(CircleShape).background(statusColor.copy(alpha = pulseAlpha)))
                
                // Content Icon
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(statusColor.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isSuccess) Icons.Filled.GppGood else Icons.Filled.Info,
                        null,
                        tint = statusColor,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text(message, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                "VERIFIED_INTEGRITY_COMPLETED", 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Black, 
                color = statusColor.copy(0.7f),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun OutputCommandConsole(
    onShare: () -> Unit,
    onSave: () -> Unit,
    onFinish: () -> Unit,
    isFinishedEnabled: Boolean,
    isActionsEnabled: Boolean,
    isSaving: Boolean
) {
    Surface(
        color = GlassWhite.copy(0.01f),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .drawBehind {
                // Neon Path Border
                val strokeWidth = 1.5.dp.toPx()
                drawLine(
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, ObsidianCyan.copy(0.4f), CyberMagenta.copy(0.4f), Color.Transparent)),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = strokeWidth
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Segmented Action Console
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassWhite.copy(0.03f))
                    .border(1.dp, GlassWhite.copy(0.05f), RoundedCornerShape(16.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Export Segment
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(enabled = isActionsEnabled, onClick = onShare),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActionsEnabled) {
                        Box(modifier = Modifier.size(40.dp).background(Brush.radialGradient(listOf(ObsidianCyan.copy(0.08f), Color.Transparent))))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.IosShare, 
                            null, 
                            tint = if (isActionsEnabled) ObsidianCyan else TextHint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "EXPORT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = if (isActionsEnabled) TextPrimary else TextHint,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Divider
                Box(modifier = Modifier.width(1.dp).fillMaxHeight(0.5f).background(GlassWhite.copy(0.1f)))

                // Save Segment
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(enabled = isActionsEnabled && !isSaving, onClick = onSave),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActionsEnabled && !isSaving) {
                        Box(modifier = Modifier.size(40.dp).background(Brush.radialGradient(listOf(CyberGreen.copy(0.08f), Color.Transparent))))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSaving) {
                            OrbitalSpinner(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(
                                Icons.Outlined.FileDownload, 
                                null, 
                                tint = if (isActionsEnabled) CyberGreen else TextHint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (isSaving) "SAVING" else "PERSIST",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = if (isActionsEnabled) TextPrimary else TextHint,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Termination Protocol
            CyberButton(
                onClick = onFinish,
                label = "CLOSE_SANITIZATION_PROTOCOL",
                icon = Icons.Filled.VerifiedUser,
                gradient = listOf(CyberMagenta, ObsidianViolet, ObsidianIndigo),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isFinishedEnabled
            )
        }
    }
}

@Composable
private fun OrbitalSpinner(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "rot"
    )

    Canvas(modifier = modifier) {
        drawArc(
            color = CyberGreen.copy(alpha = 0.2f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        drawArc(
            color = CyberGreen,
            startAngle = rotation,
            sweepAngle = 90f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}

@Composable
private fun ResultItemCard(
    item: BatchItem,
    name: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = item.status == BatchItemStatus.DONE
    val statusColor = if (isDone) SafeGreen else DangerRed

    CyberCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = if (isSelected) statusColor.copy(0.04f) else Color.Transparent,
        borderColor = if (isSelected) statusColor.copy(0.2f) else GlassBorder.copy(0.1f)
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Node
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.5.dp, if (isSelected) statusColor else TextHint.copy(0.5f), RoundedCornerShape(6.dp))
                    .clickable { onToggle() }
                    .background(if (isSelected) statusColor.copy(0.12f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Icon(Icons.Filled.Check, null, tint = statusColor, modifier = Modifier.size(16.dp))
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.height(4.dp))
                
                if (isDone) {
                    Text(
                        "STRIPPED ${item.fieldsRemoved} TAGS // VERIFIED_OUTPUT",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = statusColor.copy(0.7f),
                        fontSize = 9.sp
                    )
                } else {
                    Text(
                        item.errorMessage?.uppercase() ?: "SANITIZATION_FAILED",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = DangerRed,
                        fontSize = 9.sp
                    )
                }
            }
            
            Icon(Icons.Filled.ChevronRight, null, tint = TextHint, modifier = Modifier.size(16.dp))
        }
    }
}
