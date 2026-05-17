package com.metashield.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metashield.app.ui.components.*
import com.metashield.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToBugReport: () -> Unit,
    isRootTab: Boolean = true,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSuffixDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showSuffixDialog) {
        SuffixDialog(
            current   = state.filenameSuffix,
            onDismiss = { showSuffixDialog = false },
            onConfirm = { viewModel.setFilenameSuffix(it); showSuffixDialog = false }
        )
    }
    if (showThemeDialog) {
        ThemeDialog(
            current   = state.themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect  = { viewModel.setThemeMode(it); showThemeDialog = false }
        )
    }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.setOutputFolder(it.toString()) }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportSettings(it, context) }
    }

    CyberScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (!isRootTab) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.Filled.ArrowBack, "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    "Storage & Output",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            item {
                SettingsGroup {
                    SettingsRow(
                        icon     = Icons.Outlined.FolderOpen,
                        title    = "Destination Folder",
                        subtitle = if (state.outputFolderUri.isNullOrEmpty())
                            "App sandbox (internal)"
                        else
                            state.outputFolderUri!!.take(40) + "…",
                        onClick  = { folderLauncher.launch(null) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon     = Icons.Outlined.DriveFileRenameOutline,
                        title    = "File Naming Suffix",
                        subtitle = "Suffix: \"${state.filenameSuffix}\"",
                        onClick  = { showSuffixDialog = true }
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    "Security & Privacy",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            item {
                SettingsGroup {
                    SettingsSwitchRow(
                        icon            = Icons.Outlined.RestorePage,
                        title           = "Safety Backups",
                        subtitle        = "Keep a copy before deep cleaning",
                        checked         = state.autoBackup,
                        onCheckedChange = { viewModel.setAutoBackup(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsSwitchRow(
                        icon            = Icons.Outlined.Fingerprint,
                        title           = "Biometric Lock",
                        subtitle        = "Require identity to open app",
                        checked         = state.appLockEnabled,
                        onCheckedChange = { viewModel.setAppLockEnabled(it) }
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    "Data & Backup",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            item {
                SettingsGroup {
                    SettingsRow(
                        icon     = Icons.Outlined.FileDownload,
                        title    = "Export Settings",
                        subtitle = "Save preferences to a JSON file",
                        onClick  = { exportLauncher.launch("metashield_settings.json") }
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    "Appearance",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            item {
                SettingsGroup {
                    SettingsRow(
                        icon     = Icons.Outlined.Palette,
                        title    = "Interface Theme",
                        subtitle = when (state.themeMode) {
                            "DARK"  -> "Dark"
                            "LIGHT" -> "Light"
                            else    -> "System"
                        },
                        onClick  = { showThemeDialog = true }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            // ── About / Compliance ────────────────────────────────────────────
            item {
                AboutCard(
                    version     = "v1.9.1-STABLE",
                    onReportBug = onNavigateToBugReport,
                    onGitHub    = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Developer-For-Git"))) }
                )
            }

            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}

@Composable
private fun AboutCard(version: String, onReportBug: () -> Unit, onGitHub: () -> Unit) {
    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status chip
            AssistChip(
                onClick = {},
                label   = { Text("Compliance: Verified", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.VerifiedUser, null,
                        modifier = Modifier.size(16.dp),
                        tint     = MaterialTheme.colorScheme.primary
                    )
                }
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AboutAction(Icons.Outlined.BugReport, "Log Bug",  onReportBug)
                VerticalDivider(modifier = Modifier.height(32.dp))
                AboutAction(Icons.Outlined.GppGood,   "Privacy",  {})
                VerticalDivider(modifier = Modifier.height(32.dp))
                AboutAction(Icons.Outlined.Code,       "GitHub",   onGitHub)
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "MetaShield",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    version,
                    style      = MaterialTheme.typography.labelSmall,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AboutAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.clickable(onClick = onClick)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Surface(
                modifier     = Modifier.size(40.dp),
                shape        = RoundedCornerShape(12.dp),
                color        = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(20.dp))
                }
            }
        },
        headlineContent   = {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent   = {
            Icon(Icons.Filled.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        leadingContent = {
            Surface(
                modifier     = Modifier.size(40.dp),
                shape        = RoundedCornerShape(12.dp),
                color        = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(20.dp))
                }
            }
        },
        headlineContent   = {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent   = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

@Composable
private fun SuffixDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Filename Suffix", fontWeight = FontWeight.Bold) },
        text  = {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it },
                label         = { Text("Suffix") },
                placeholder   = { Text("e.g. _clean") },
                shape         = RoundedCornerShape(12.dp),
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ThemeDialog(current: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Appearance", fontWeight = FontWeight.Bold) },
        text  = {
            Column(Modifier.padding(top = 8.dp)) {
                listOf(
                    "SYSTEM" to "System default",
                    "DARK"   to "Dark mode",
                    "LIGHT"  to "Light mode"
                ).forEach { (key, label) ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(key) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == key,
                            onClick  = { onSelect(key) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
