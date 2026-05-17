package com.metashield.app.ui.history

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metashield.app.ui.components.CyberCard
import com.metashield.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataComparisonScreen(
    historyId: Long,
    onNavigateUp: () -> Unit,
    viewModel: MetadataComparisonViewModel = hiltViewModel()
) {
    LaunchedEffect(historyId) {
        viewModel.loadEntry(historyId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Metadata Evolution") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, color = DangerRed, modifier = Modifier.align(Alignment.Center))
                else -> Column(Modifier.fillMaxSize()) {
                    ComparisonHeader(state)
                    
                    if (state.diffs.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No metadata changes detected.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            items(state.diffs) { diff ->
                                DiffRow(diff)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonHeader(state: ComparisonUiState) {
    val entry = state.historyEntry ?: return
    CyberCard(
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(entry.fileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.action,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "${state.diffs.size} Changes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DiffRow(diff: MetadataDiff) {
    Column(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        Text(
            diff.tag,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(4.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Before
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (diff.isRemoved || diff.isModified) DangerRed.copy(alpha = 0.1f) else Color.Transparent)
                    .padding(8.dp)
            ) {
                Text(
                    diff.beforeValue ?: "<empty>",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = if (diff.isRemoved) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    maxLines = 3
                )
            }
            
            Icon(
                Icons.Filled.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 4.dp).size(16.dp)
            )
            
            // After
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (diff.isAdded || diff.isModified) SafeGreen.copy(alpha = 0.1f) else Color.Transparent)
                    .padding(8.dp)
            ) {
                Text(
                    diff.afterValue ?: "<stripped>",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = if (diff.isAdded) SafeGreen else if (diff.isRemoved) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else SafeGreen,
                    lineHeight = 16.sp,
                    maxLines = 3
                )
            }
        }
        Divider(Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    }
}
