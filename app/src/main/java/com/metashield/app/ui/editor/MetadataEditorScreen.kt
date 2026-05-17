package com.metashield.app.ui.editor

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metashield.app.data.model.MetadataField
import com.metashield.app.ui.theme.*
import com.metashield.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataEditorScreen(
    fileUri: Uri,
    onNavigateUp: () -> Unit,
    onSaved: (String, String) -> Unit,
    viewModel: MetadataEditorViewModel = hiltViewModel()
) {
    LaunchedEffect(fileUri) { viewModel.loadFile(fileUri) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigateToPreview) {
        state.navigateToPreview?.let { (inEnc, outEnc) ->
            viewModel.clearNavigation()
            onSaved(inEnc, outEnc)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddFieldDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { key, tag, value ->
                viewModel.addField(key, tag, value)
                showAddDialog = false
            }
        )
    }

    if (showGpsDialog) {
        CoordinatePickerDialog(
            onDismiss = { showGpsDialog = false },
            onSave = { lat, lon ->
                viewModel.addField("TAG_GPS_LATITUDE", "GPS Latitude", lat)
                viewModel.addField("TAG_GPS_LONGITUDE", "GPS Longitude", lon)
                showGpsDialog = false
            }
        )
    }

    CyberScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Refine Metadata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { showGpsDialog = true }) {
                        Icon(Icons.Filled.AddLocation, "Add GPS")
                    }
                    IconButton(onClick = { viewModel.undoLast() }, enabled = state.canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (!state.isLoading && state.error == null) {
                CyberActionBottomBar {
                    CyberButton(
                        onClick = { viewModel.saveFile() },
                        label = if (state.isSaving) "Applying Changes…" else "Save & Protect",
                        icon = Icons.Filled.Save,
                        gradient = listOf(CyberBlue, CyberBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        floatingActionButton = {
            if (!state.isLoading) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CyberCardShape
                ) {
                    Icon(Icons.Filled.Add, "Add")
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Filled.GppBad, null, tint = DangerRed, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Unable to edit file", style = MaterialTheme.typography.titleMedium, color = DangerRed)
                        Text(state.error!!, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                "${state.editableFields.size} Fields modified or kept",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        itemsIndexed(state.editableFields) { index, field ->
                            EditableFieldCard(
                                field = field,
                                onChange = { newValue -> viewModel.updateField(index, newValue) },
                                onDelete = { viewModel.removeField(index) }
                            )
                        }
                        item { Spacer(Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableFieldCard(
    field: MetadataField,
    onChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var value by remember(field.key) { mutableStateOf(field.value) }

    CyberCard(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    field.tag,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    onChange(it)
                },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                singleLine = false,
                maxLines = 4
            )
        }
    }
}

@Composable
private fun AddFieldDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var key by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("New Metadata Tag", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = tag, onValueChange = { tag = it },
                    label = { Text("Display Label") },
                    placeholder = { Text("e.g. Creator") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = key, onValueChange = { key = it },
                    label = { Text("Tag Key") },
                    placeholder = { Text("TAG_CREATOR") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = value, onValueChange = { value = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (key.isNotBlank()) onAdd(key.trim(), tag.ifBlank { key }.trim(), value.trim()) },
                enabled = key.isNotBlank(),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Add Tag") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CoordinatePickerDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Offline GPS Picker", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text("Enter decimal degrees (e.g. 37.7749, -122.4194)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = lat, onValueChange = { lat = it },
                    label = { Text("Latitude") },
                    placeholder = { Text("37.7749") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = lon, onValueChange = { lon = it },
                    label = { Text("Longitude") },
                    placeholder = { Text("-122.4194") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (lat.isNotBlank() && lon.isNotBlank()) onSave(lat.trim(), lon.trim())
                },
                shape = RoundedCornerShape(10.dp)
            ) { Text("Set Location") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
