package com.metashield.app.ui.vault

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metashield.app.data.db.entity.VaultEntity
import com.metashield.app.ui.components.*
import com.metashield.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onNavigateUp: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val executor = remember { Executors.newSingleThreadExecutor() }

    val biometricPrompt = remember(activity) {
        activity?.let {
            BiometricPrompt(it, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.unlock(isDecoy = false)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }
            })
        }
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Identity Verification")
            .setSubtitle("Authenticate to enter the Ghost Vault")
            .setNegativeButtonText("Cancel")
            .build()
    }

    // Auto-unlock based on initial session (Identity Shift)
    val sessionIsDecoy by com.metashield.app.util.VaultSession.isDecoy.collectAsStateWithLifecycle()
    LaunchedEffect(state.isLocked, sessionIsDecoy) {
        if (state.isLocked) {
            // If we came from a decoy PIN in MainActivity, auto-unlock into decoy
            if (sessionIsDecoy) {
                viewModel.unlock(isDecoy = true)
            } else {
                // If we're entering normally, ask for biometrics (Real Vault)
                biometricPrompt?.authenticate(promptInfo)
            }
        }
    }

    CyberScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (state.isDecoyMode) Icons.Outlined.VisibilityOff else Icons.Outlined.Security, 
                            null, 
                            Modifier.size(20.dp), 
                            tint = if (state.isDecoyMode) ObsidianPink else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (state.isDecoyMode) "Ghost Vault (DECOY)" else "Secure Vault", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            color = if (state.isDecoyMode) ObsidianPink else TextPrimary
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = {
                    if (!state.isLocked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.isDecoyMode) {
                                Text("DECEPTION_ACTIVE", style = MaterialTheme.typography.labelSmall, color = ObsidianPink.copy(0.7f), fontFamily = FontFamily.Monospace)
                                Spacer(Modifier.width(8.dp))
                            }
                            IconButton(onClick = { viewModel.lock() }) {
                                Icon(Icons.Filled.Lock, "Lock Vault", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLocked) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.Fingerprint, 
                        null, 
                        Modifier.size(80.dp), 
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Vault is Locked", 
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Authentication required to view hidden files", 
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(32.dp))
                    CyberButton(
                        onClick = { biometricPrompt?.authenticate(promptInfo) },
                        label = "Authenticate",
                        icon = Icons.Filled.Fingerprint,
                        gradient = listOf(CyberBlue, CyberBlue)
                    )
                }
            } else {
                if (state.entries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(
                                if (state.isDecoyMode) Icons.Outlined.FolderOff else Icons.Outlined.Security, 
                                null, 
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), 
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (state.isDecoyMode) "No Decoy Files" else "The Vault is Empty", 
                                style = MaterialTheme.typography.titleMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (state.isDecoyMode) "This partition is empty correctly." else "Move protected files here to keep them invisible to other apps.", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.entries, key = { it.id }) { entry ->
                            VaultEntryCard(
                                entry = entry,
                                onDelete = { viewModel.deleteEntry(entry) },
                                onExport = { viewModel.exportToGallery(entry) },
                                onShare = { viewModel.shareFile(entry) }
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
private fun VaultEntryCard(
    entry: VaultEntity,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        shape = CyberCardShape
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CyberCardShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (entry.fileType) {
                        "PHOTO" -> Icons.Outlined.Image
                        "VIDEO" -> Icons.Outlined.VideoFile
                        else    -> Icons.Outlined.InsertDriveFile
                    },
                    null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    entry.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${(entry.originalSize / 1024)} KB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        dateFormat.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
            
            Spacer(Modifier.width(4.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { onShare() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Share, "Share", tint = CyberBlue, modifier = Modifier.size(18.dp))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, SafeGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { onExport() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Download, "Download", tint = SafeGreen, modifier = Modifier.size(18.dp))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, DangerRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Delete, "Delete", tint = DangerRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
