package com.metashield.app.ui.tools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.metashield.app.ui.components.*
import com.metashield.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onStripFile: (Uri) -> Unit,
    onEditMetadata: (Uri) -> Unit,
    onBatchProcess: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToTemplateSelection: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToStego: () -> Unit,
    onNavigateToIdentityMasking: (String) -> Unit,
    batchViewModel: com.metashield.app.ui.batch.BatchQueueViewModel
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val fileCache = remember { com.metashield.app.data.repository.FileCache(context) }

    var showMetadataOptions by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // File pickers
    val stripLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { scope.launch { try { onStripFile(fileCache.cacheFile(it).second) } catch (_: Exception) { onStripFile(it) } } }
    }
    val editLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { scope.launch { try { onEditMetadata(fileCache.cacheFile(it).second) } catch (_: Exception) { onEditMetadata(it) } } }
    }
    val templateBatchLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            batchViewModel.clearQueue()
            scope.launch {
                uris.forEach { uri -> try { batchViewModel.addFile(fileCache.cacheFile(uri).second) } catch (_: Exception) { batchViewModel.addFile(uri) } }
                onNavigateToTemplateSelection()
            }
        }
    }
    val batchLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            batchViewModel.clearQueue()
            scope.launch {
                uris.forEach { uri -> try { batchViewModel.addFile(fileCache.cacheFile(uri).second) } catch (_: Exception) { batchViewModel.addFile(uri) } }
                batchViewModel.setMode(com.metashield.app.ui.batch.BatchProcessingMode.STRIP)
                onBatchProcess()
            }
        }
    }
    val maskingLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { scope.launch {
            val cachedUri = try { fileCache.cacheFile(it).second } catch (_: Exception) { it }
            val enc = java.net.URLEncoder.encode(cachedUri.toString(), "UTF-8")
            onNavigateToIdentityMasking(enc)
        } }
    }

    CyberScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tools",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->

        // Metadata options bottom sheet
        if (showMetadataOptions) {
            ModalBottomSheet(
                onDismissRequest = { showMetadataOptions = false },
                sheetState       = sheetState,
                containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
                dragHandle       = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp, start = 20.dp, end = 20.dp)
                ) {
                    Text(
                        "Custom Metadata",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(vertical = 16.dp)
                    )
                    ToolSheetOption(Icons.Outlined.Edit, "Single File", "Carefully edit one file's metadata") {
                        showMetadataOptions = false
                        editLauncher.launch(arrayOf("image/*", "video/*", "audio/*"))
                    }
                    ToolSheetOption(Icons.Outlined.Layers, "Multiple Files", "Apply a template to many files at once") {
                        showMetadataOptions = false
                        templateBatchLauncher.launch(arrayOf("image/*", "video/*", "audio/*"))
                    }
                    ToolSheetOption(Icons.Outlined.Style, "Template Library", "Manage and apply metadata templates") {
                        showMetadataOptions = false
                        onNavigateToTemplates()
                    }
                }
            }
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // ── Privacy Protocols ──────────────────────────────────────────────
            item(key = "section_privacy") {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    "Privacy Protocols",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                Spacer(Modifier.height(8.dp))
                ToolsGrid(
                    modifier    = Modifier.padding(horizontal = 16.dp),
                    startIndex  = 0,
                    items       = listOf(
                        ToolItem(
                            Icons.Outlined.Face, "Identity Masking",
                            "AI-powered anonymization",
                            containerColor = { MaterialTheme.colorScheme.primaryContainer },
                            contentColor   = { MaterialTheme.colorScheme.onPrimaryContainer }
                        ) { maskingLauncher.launch(arrayOf("image/*")) },
                        ToolItem(
                            Icons.Outlined.Lock, "Stealth Vault",
                            "Plausible deniability storage",
                            containerColor = { MaterialTheme.colorScheme.tertiaryContainer },
                            contentColor   = { MaterialTheme.colorScheme.onTertiaryContainer }
                        ) { onNavigateToVault() },
                    )
                )
            }

            // ── Strip Metadata ─────────────────────────────────────────────────
            item(key = "section_strip") {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    "Strip Metadata",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                Spacer(Modifier.height(8.dp))
                ToolsGrid(
                    modifier   = Modifier.padding(horizontal = 16.dp),
                    startIndex = 2,
                    items      = listOf(
                        ToolItem(
                            Icons.Outlined.Image, "Clean Photos", "Remove EXIF from images",
                            containerColor = { MaterialTheme.colorScheme.primaryContainer },
                            contentColor   = { MaterialTheme.colorScheme.onPrimaryContainer }
                        ) { stripLauncher.launch(arrayOf("image/*")) },
                        ToolItem(
                            Icons.Outlined.Videocam, "Clean Videos", "Strip video metadata",
                            containerColor = { MaterialTheme.colorScheme.secondaryContainer },
                            contentColor   = { MaterialTheme.colorScheme.onSecondaryContainer }
                        ) { stripLauncher.launch(arrayOf("video/*")) },
                        ToolItem(
                            Icons.Outlined.AudioFile, "Clean Audio", "Remove audio tags",
                            containerColor = { MaterialTheme.colorScheme.tertiaryContainer },
                            contentColor   = { MaterialTheme.colorScheme.onTertiaryContainer }
                        ) { stripLauncher.launch(arrayOf("audio/*")) },
                        ToolItem(
                            Icons.Outlined.PictureAsPdf, "Clean Docs", "Strip PDF metadata",
                            containerColor = { MaterialTheme.colorScheme.errorContainer },
                            contentColor   = { MaterialTheme.colorScheme.onErrorContainer }
                        ) { stripLauncher.launch(arrayOf("application/pdf")) },
                    )
                )
            }

            // ── Advanced Tools ─────────────────────────────────────────────────
            item(key = "section_advanced") {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    "Advanced Tools",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                Spacer(Modifier.height(8.dp))
                ToolsGrid(
                    modifier   = Modifier.padding(horizontal = 16.dp),
                    startIndex = 6,
                    items      = listOf(
                        ToolItem(
                            Icons.Outlined.Edit, "Edit Metadata", "Custom metadata fields",
                            containerColor = { MaterialTheme.colorScheme.primaryContainer },
                            contentColor   = { MaterialTheme.colorScheme.onPrimaryContainer }
                        ) { showMetadataOptions = true },
                        ToolItem(
                            Icons.Outlined.FolderCopy, "Batch Process", "Clean multiple files",
                            containerColor = { MaterialTheme.colorScheme.secondaryContainer },
                            contentColor   = { MaterialTheme.colorScheme.onSecondaryContainer }
                        ) { batchLauncher.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf")) },
                        ToolItem(
                            Icons.Outlined.VisibilityOff, "Steganography", "Hide data in images",
                            containerColor = { MaterialTheme.colorScheme.tertiaryContainer },
                            contentColor   = { MaterialTheme.colorScheme.onTertiaryContainer }
                        ) { onNavigateToStego() },
                    )
                )
            }
            item(key = "bottom_space") { Spacer(Modifier.height(20.dp)) }
        }
    }
}

// ── Tool Item data class ───────────────────────────────────────────────────────
private data class ToolItem(
    val icon: ImageVector,
    val label: String,
    val description: String,
    val containerColor: @Composable () -> Color = { MaterialTheme.colorScheme.secondaryContainer },
    val contentColor: @Composable () -> Color = { MaterialTheme.colorScheme.onSecondaryContainer },
    val onClick: () -> Unit
)

@Composable
private fun ToolsGrid(
    items: List<ToolItem>,
    modifier: Modifier = Modifier,
    startIndex: Int = 0
) {
    val rows = items.chunked(2)

    // rememberSaveable: survives LazyColumn recycling.
    // Without this, stagger animations re-run every time the user scrolls past
    // this section — firing delay coroutines + state flips + AnimatedVisibility
    // transitions during the scroll frame itself.
    var hasAnimated by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { colIdx, item ->
                    val tileIdx = startIndex + rowIdx * 2 + colIdx

                    if (!hasAnimated) {
                        // First appearance: stagger-in animation
                        val visible = if (tileIdx < 5) rememberStaggeredVisible(tileIdx, staggerMs = 70L) else true
                        LaunchedEffect(visible) {
                            if (visible && tileIdx == startIndex + items.lastIndex) hasAnimated = true
                        }
                        AnimatedVisibility(
                            visible  = visible,
                            modifier = Modifier.weight(1f),
                            enter    = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.92f)
                        ) {
                            ToolTile(item = item)
                        }
                    } else {
                        // Already seen: display immediately, zero animation cost
                        Box(modifier = Modifier.weight(1f)) {
                            ToolTile(item = item)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ToolTile(item: ToolItem, modifier: Modifier = Modifier) {
    val cColor = item.containerColor()
    val nColor = item.contentColor()

    PressAnimatedBox(
        onClick   = item.onClick,
        modifier  = modifier,
        scaleDown = 0.93f
    ) {
        ElevatedCard(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    modifier     = Modifier.size(48.dp),
                    shape        = RoundedCornerShape(14.dp),
                    color        = cColor,
                    contentColor = nColor
                    // No shadowElevation: inside ElevatedCard(3dp) already
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(item.icon, null, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(item.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(item.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
            }
        }
    }
}

// ── Sheet option ───────────────────────────────────────────────────────────────
@Composable
private fun ToolSheetOption(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        modifier       = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
                Icon(icon, null, modifier = Modifier.size(22.dp))
            }
        },
        headlineContent   = { Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        colors            = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}
