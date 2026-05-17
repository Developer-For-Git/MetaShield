package com.metashield.app.ui.history

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metashield.app.data.db.entity.HistoryEntity
import com.metashield.app.ui.components.*
import com.metashield.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onNavigateUp: () -> Unit,
    onFileSelected: (Uri) -> Unit,
    onCompare: (Long) -> Unit,
    isRootTab: Boolean = true,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportHistory(it, context) } }

    CyberScaffold(
        topBar = {
            if (selectedIds.isNotEmpty()) {
                TopAppBar(
                    title = {
                        Text(
                            "${selectedIds.size} selected",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Filled.Close, "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Outlined.SelectAll, "Select All")
                        }
                        IconButton(onClick = { viewModel.shareSelected(context) }) {
                            Icon(Icons.Outlined.Share, "Share")
                        }
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Outlined.DeleteSweep, "Delete",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            "History",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (!isRootTab) {
                            IconButton(onClick = onNavigateUp) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { exportLauncher.launch("metashield_history.csv") }) {
                            Icon(Icons.Outlined.FileDownload, "Export")
                        }
                        IconButton(onClick = { viewModel.clearAll() }) {
                            Icon(Icons.Outlined.DeleteSweep, "Clear All",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── MD3 Search Bar ────────────────────────────────────────────────
            OutlinedTextField(
                value           = searchQuery,
                onValueChange   = { viewModel.setSearchQuery(it) },
                modifier        = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder     = { Text("Search activity log…") },
                leadingIcon     = { Icon(Icons.Outlined.Search, null) },
                trailingIcon    = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape           = RoundedCornerShape(28.dp),
                singleLine      = true,
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // ── FilterChip category tabs ──────────────────────────────────────
            HistoryCategoryTabs(
                selected = selectedCategory,
                onSelect = { viewModel.setCategory(it) }
            )

            if (history.isEmpty()) {
                // Empty state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick  = {},
                            modifier = Modifier.size(80.dp),
                            enabled  = false
                        ) {
                            Icon(
                                Icons.Outlined.History, null,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Text(
                            "No Activity Yet",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Cleaned files will appear here after metadata processing.",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(horizontal = 40.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(
                        items = history,
                        key   = { _, entry -> entry.id }   // stable identity = no full-list recompose
                    ) { index, entry ->
                        // Only animate first 5 items; the rest appear instantly to avoid
                        // stalling scroll when the list is long.
                        val visible = if (index < 5) rememberStaggeredVisible(index, staggerMs = 55L) else true
                        AnimatedVisibility(
                            visible = visible,
                            enter   = fadeIn(tween(220)) +
                                      slideInVertically(tween(220)) { (it * 0.15f).toInt() }
                        ) {
                            HistoryListItem(
                                entry      = entry,
                                isSelected = selectedIds.contains(entry.id),
                                onTap      = {
                                    if (selectedIds.isNotEmpty()) viewModel.toggleSelection(entry.id)
                                    else onFileSelected(Uri.parse(entry.outputUriString ?: entry.inputUriString))
                                },
                                onLongPress = { viewModel.toggleSelection(entry.id) },
                                onCompare   = { onCompare(entry.id) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryListItem(
    entry: HistoryEntity,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onCompare: () -> Unit
) {
    val statusColor = if (entry.success) SafeGreen else DangerRed
    val dateStr = remember(entry.timestamp) {
        java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(entry.timestamp))
    }

    // Single combinedClickable — no outer PressAnimatedBox wrapper.
    // Wrapping with a second clickable fought for gesture ownership and caused
    // the list to feel janky / unresponsive mid-scroll.
    ElevatedCard(
        modifier  = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .then(
                if (isSelected) Modifier.border(
                    2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)
                ) else Modifier
            ),
        colors    = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            leadingContent = {
                Surface(
                    shape        = RoundedCornerShape(8.dp),
                    color        = statusColor.copy(alpha = 0.15f),
                    contentColor = statusColor,
                    modifier     = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (entry.success) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            null, modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            headlineContent = {
                Text(entry.fileName, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text("${entry.action} · ${entry.fieldsRemoved} fields · $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingContent = {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val iconScale by animateFloatAsState(
                    targetValue   = if (isPressed) 0.80f else 1f,
                    animationSpec = PressSpring,
                    label         = "compareScale"
                )
                IconButton(
                    onClick           = onCompare,
                    modifier          = Modifier.size(36.dp).scale(iconScale),
                    interactionSource = interactionSource
                ) {
                    Icon(Icons.Outlined.Compare, null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

// ── History Category ───────────────────────────────────────────────────────────
enum class HistoryCategory(val label: String) {
    ALL("All"),
    PHOTO("Photos"),
    VIDEO("Videos"),
    AUDIO("Audio"),
    DOCS("Docs")
}

// ── FilterChip category tabs ───────────────────────────────────────────────────
@Composable
fun HistoryCategoryTabs(
    selected: HistoryCategory,
    onSelect: (HistoryCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        HistoryCategory.values().forEach { cat ->
            FilterChip(
                selected  = cat == selected,
                onClick   = { onSelect(cat) },
                label     = { Text(cat.label) }
            )
        }
    }
}
