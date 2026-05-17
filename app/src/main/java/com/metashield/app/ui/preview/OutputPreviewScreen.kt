package com.metashield.app.ui.preview

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextOverflow
import com.metashield.app.ui.components.*
import com.metashield.app.ui.theme.*
import com.metashield.app.data.model.MetadataField
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutputPreviewScreen(
    inputUri: Uri,
    outputUri: Uri,
    onNavigateHome: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: OutputPreviewViewModel = hiltViewModel()
) {
    LaunchedEffect(inputUri, outputUri) { viewModel.loadPreview(inputUri, outputUri) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var compareMode by remember { mutableStateOf("Summary") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Output Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    TextButton(onClick = onNavigateHome) {
                        Text("FINISH", color = CyberGreen, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                CyberCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    borderColor = CyberOutline.copy(alpha = 0.2f),
                    containerColor = CyberVoid.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Share Icon Action
                        CyberIconButton(
                            onClick = {
                                try {
                                    val file = File(outputUri.path!!)
                                    val contentUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = context.contentResolver.getType(contentUri) ?: "*/*"
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share clean file"))
                                } catch (_: Exception) {}
                            },
                            icon = Icons.Outlined.Share,
                            contentDescription = "Share",
                            color = CyberBlue
                        )

                        // Main Save Action
                        CyberButton(
                            onClick = { viewModel.saveToDevice(outputUri) },
                            label = if (state.saveSuccess) "Saved!" else "Save to Gallery",
                            icon = if (state.saveSuccess) Icons.Filled.CheckCircle else Icons.Outlined.FileDownload,
                            modifier = Modifier.weight(1f),
                            enabled = !state.isSaving,
                            gradient = if (state.saveSuccess) listOf(SafeGreen, SafeGreen) else listOf(CyberGreen, CyberGreen)
                        )

                        // Vault Icon Action
                        CyberIconButton(
                            onClick = { viewModel.moveToVault(outputUri) },
                            icon = if (state.vaultSuccess) Icons.Filled.Shield else Icons.Outlined.Security,
                            contentDescription = "To Vault",
                            color = if (state.vaultSuccess) SafeGreen else CyberMagenta,
                            enabled = !state.isVaulting && !state.vaultSuccess
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SafeGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = SafeGreen, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Processing Complete!", fontWeight = FontWeight.Bold, color = SafeGreen)
                            Text(
                                "${state.fieldsRemoved} metadata fields removed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Segmented Button or TabRow for Compare Mode
            item {
                CyberCard(shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        listOf("Summary", "Before", "After").forEach { mode ->
                            val isSelected = compareMode == mode
                            val bg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                            val txtColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(bg)
                                    .clickable { compareMode = mode }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(mode, fontWeight = FontWeight.Bold, color = txtColor)
                            }
                        }
                    }
                }
            }

            if (compareMode == "Summary") {
                // Before/After comparison
                item {
                Text("Metadata Summary", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard("Before", state.beforeFieldCount, MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
                    SummaryCard("After", state.afterFieldCount, SafeGreen.copy(alpha = 0.1f), Modifier.weight(1f))
                }
            }

            // File size
            if (state.outputFileName.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Output File", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.outputFileName, fontWeight = FontWeight.Medium)
                            Row {
                                Icon(Icons.Filled.FolderOpen, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Saved to app cache (use Share to export)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            } else if (compareMode == "Before") {
                if (state.beforeFields.isEmpty()) {
                    item { Text("No metadata found", modifier = Modifier.padding(16.dp)) }
                } else {
                    items(state.beforeFields.size) { i ->
                        SimpleFieldRow(state.beforeFields[i])
                    }
                }
            } else if (compareMode == "After") {
                if (state.afterFields.isEmpty()) {
                    item { Text("Cleaned completely. No metadata.", modifier = Modifier.padding(16.dp)) }
                } else {
                    items(state.afterFields.size) { i ->
                        SimpleFieldRow(state.afterFields[i])
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, count: Int, backgroundColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("$count", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("fields", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SimpleFieldRow(field: MetadataField) {
    CyberCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(field.tag, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            Text(field.value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
}
