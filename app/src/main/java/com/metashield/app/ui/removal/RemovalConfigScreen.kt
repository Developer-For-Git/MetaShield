package com.metashield.app.ui.removal

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metashield.app.data.model.RemovalOptions
import com.metashield.app.ui.components.*
import com.metashield.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemovalConfigScreen(
    fileUri: Uri,
    onNavigateUp: () -> Unit,
    onProcessed: (String, String) -> Unit,
    viewModel: RemovalConfigViewModel = hiltViewModel()
) {
    LaunchedEffect(fileUri) { viewModel.setFile(fileUri) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigateToPreview) {
        state.navigateToPreview?.let { (inEnc, outEnc) ->
            viewModel.clearNavigation()
            onProcessed(inEnc, outEnc)
        }
    }

    CyberScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state.fileType) {
                            com.metashield.app.data.model.FileType.PHOTO -> "Photo Cleaner"
                            com.metashield.app.data.model.FileType.VIDEO -> "Video Cleaner"
                            com.metashield.app.data.model.FileType.AUDIO -> "Audio Cleaner"
                            else -> "File Cleaner"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextSecondary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            CyberActionBottomBar {
                CyberButton(
                    onClick = { viewModel.processFile() },
                    label = if (state.isProcessing) "Processing…" else "Remove Metadata Now",
                    icon = if (state.isProcessing) null else Icons.Filled.Shield,
                    gradient = if (state.isProcessing) listOf(TextHint, TextHint) else GradientDanger,
                    enabled = !state.isProcessing,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Protection Score ──────────────────────────────────────────────
            item {
                ProtectionScoreCard(score = state.potentialScore, isProcessing = state.isProcessing)
            }

            // ── Zero-Knowledge Profiles ───────────────────────────────────────
            item {
                SectionHeader("Zero-Knowledge Profiles", modifier = Modifier.padding(vertical = 4.dp))
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileChip("WhatsApp", Icons.AutoMirrored.Outlined.Chat, listOf(SafeGreen, ObsidianCyan)) { viewModel.applyPreset(RemovalOptions.PROFILE_WHATSAPP) }
                    ProfileChip("Discord", Icons.Outlined.Forum, GradientViolet) { viewModel.applyPreset(RemovalOptions.PROFILE_DISCORD) }
                    ProfileChip("Stealth", Icons.Outlined.VisibilityOff, listOf(DangerRed, ObsidianPink)) { viewModel.applyPreset(RemovalOptions.PROFILE_STEALTH) }
                    ProfileChip("Strip All", Icons.Outlined.DeleteSweep, GradientDanger) { viewModel.applyPreset(RemovalOptions.STRIP_ALL) }
                }
            }

            // ── Removal Categories ────────────────────────────────────────────
            item { SectionHeader("Data Categories", modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) }

            item {
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        RemovalToggle("Location & GPS", "GPS coordinates, altitude, direction", Icons.Outlined.LocationOff, DangerRed, true,
                            state.options.removeAll || state.options.removeLocation) { viewModel.updateOptions(state.options.copy(removeLocation = it, removeAll = false)) }
                        OptionDivider()
                        RemovalToggle("Camera Info", "Make, model, lens, aperture, ISO", Icons.Outlined.CameraAlt, ObsidianIndigo, false,
                            state.options.removeAll || state.options.removeCamera) { viewModel.updateOptions(state.options.copy(removeCamera = it, removeAll = false)) }
                        OptionDivider()
                        RemovalToggle("Timestamps", "Date taken, digitized, modified", Icons.Outlined.Schedule, ObsidianCyan, false,
                            state.options.removeAll || state.options.removeTimestamps) { viewModel.updateOptions(state.options.copy(removeTimestamps = it, removeAll = false)) }
                        OptionDivider()
                        RemovalToggle("Device Identity", "Software, serial number, unique ID", Icons.Outlined.PhoneAndroid, DangerRed, true,
                            state.options.removeAll || state.options.removeDevice) { viewModel.updateOptions(state.options.copy(removeDevice = it, removeAll = false)) }
                        OptionDivider()
                        RemovalToggle("Copyright", "Artist, copyright notice", Icons.Outlined.Copyright, TextSecondary, false,
                            state.options.removeAll || state.options.removeCopyright) { viewModel.updateOptions(state.options.copy(removeCopyright = it, removeAll = false)) }
                    }
                }
            }

            // ── Options ───────────────────────────────────────────────────────
            item { SectionHeader("Options", modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) }
            item {
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ToggleRow("Preserve Essentials", "Keep codec, resolution, duration",
                            state.options.preserveEssentials) { viewModel.updateOptions(state.options.copy(preserveEssentials = it)) }
                        OptionDivider()
                        ToggleRow("Anonymize Timestamps", "Replace dates with 2000-01-01",
                            state.options.anonymizeTimestamps) { viewModel.updateOptions(state.options.copy(anonymizeTimestamps = it)) }
                    }
                }
            }

            // ── Intelligence Tools ────────────────────────────────────────────
            item { SectionHeader("Intelligence Tools", modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) }
            item {
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ToggleRow("Location Decoy", "Replace GPS with fake coordinates",
                            state.options.spoofLocation) { viewModel.updateOptions(state.options.copy(spoofLocation = it)) }
                        OptionDivider()
                        ToggleRow("Device Masking", "Hide camera model from trackers",
                            state.options.spoofDevice) { viewModel.updateOptions(state.options.copy(spoofDevice = it)) }
                        if (state.fileType == com.metashield.app.data.model.FileType.AUDIO) {
                            OptionDivider()
                            ToggleRow("Sonic Stealth", "Strip ultrasonic watermarks (~18kHz+)",
                                state.options.removeWatermarks) { viewModel.updateOptions(state.options.copy(removeWatermarks = it)) }
                        }
                        OptionDivider()
                        ToggleRow("Deep Hash Anonymizer", "Mutate file hash to break tracker fingerprints",
                            state.options.mutateHash) { viewModel.updateOptions(state.options.copy(mutateHash = it)) }
                        if (state.fileType == com.metashield.app.data.model.FileType.PHOTO) {
                            OptionDivider()
                            ToggleRow("Temporal Drift", "Shift timestamps by random offset while preserving order",
                                state.options.useTemporalDrift) { viewModel.updateOptions(state.options.copy(useTemporalDrift = it, driftOffsetMinutes = if (it) ((-720..720).random()) else 0)) }
                        }
                    }
                }
            }

            if (state.error != null) {
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth(), borderColor = DangerRed.copy(alpha = 0.4f)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Warning, null, tint = DangerRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(state.error!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Protection Score ──────────────────────────────────────────────────────────
@Composable
private fun ProtectionScoreCard(score: Int, isProcessing: Boolean) {
    val color = when {
        score > 80 -> SafeGreen
        score > 40 -> PrivacyAmber
        else       -> DangerRed
    }
    CyberCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PROTECTION LEVEL", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$score", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = color)
                Text("/100", style = MaterialTheme.typography.titleMedium, color = TextHint)
            }
            Spacer(Modifier.height(12.dp))
            if (isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50.dp)).height(6.dp), color = ObsidianIndigo, trackColor = SpaceRaised)
            } else {
                LinearProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50.dp)).height(6.dp),
                    color = color, trackColor = SpaceRaised
                )
            }
        }
    }
}

// ── Profile Chip ──────────────────────────────────────────────────────────────
@Composable
private fun ProfileChip(label: String, icon: ImageVector, gradient: List<Color>, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(ChipShape)
            .background(gradient.first().copy(alpha = 0.12f))
            .border(1.dp, Brush.linearGradient(gradient), ChipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = gradient.first(), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = gradient.first(), fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Removal Toggle (checkbox-style) ──────────────────────────────────────────
@Composable
private fun RemovalToggle(
    title: String, description: String,
    icon: ImageVector, iconColor: Color, sensitive: Boolean,
    checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                if (sensitive) {
                    Spacer(Modifier.width(6.dp))
                    StatusBadge("Sensitive", DangerRed)
                }
            }
            Text(description, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Checkbox(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = ObsidianIndigo, uncheckedColor = TextHint)
        )
    }
}

// ── Toggle Row (switch-style) ─────────────────────────────────────────────────
@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ObsidianIndigo, uncheckedThumbColor = TextHint, uncheckedTrackColor = SpaceRaised)
        )
    }
}

@Composable
private fun OptionDivider() {
    HorizontalDivider(color = GlassBorder.copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
}
