package com.metashield.app.ui.update

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metashield.app.data.model.UpdateInfo
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
//  UpdateChecker — drop into NavGraph once; fully headless, auto-shows dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun UpdateChecker(viewModel: UpdateViewModel = hiltViewModel()) {
    val update by viewModel.pendingUpdate.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.checkForUpdate() }

    AnimatedVisibility(
        visible = update != null,
        enter   = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f),
        exit    = fadeOut(tween(200)) + scaleOut(tween(200))
    ) {
        update?.let { info ->
            UpdateDialog(info = info, viewModel = viewModel)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  UpdateDialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun UpdateDialog(info: UpdateInfo, viewModel: UpdateViewModel) {
    val downloadState    by viewModel.downloadState.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

    // 10-min postpone countdown (shown on the button label)
    var postponeSecondsLeft by remember { mutableStateOf(0) }
    LaunchedEffect(postponeSecondsLeft > 0) {
        if (postponeSecondsLeft > 0) {
            while (postponeSecondsLeft > 0) {
                delay(1_000L)
                postponeSecondsLeft--
            }
        }
    }

    Dialog(
        onDismissRequest = { /* block swipe-dismiss — only "Postpone" button closes */ },
        properties       = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = false,
            dismissOnClickOutside   = false
        )
    ) {
        Surface(
            modifier        = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape           = RoundedCornerShape(28.dp),
            color           = MaterialTheme.colorScheme.surface,
            tonalElevation  = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Animated badge ────────────────────────────────────────
                UpdateShieldBadge()
                Spacer(Modifier.height(20.dp))

                // ── Title ─────────────────────────────────────────────────
                Text(
                    "Update Available",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    info.title,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // ── Version pills ─────────────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    VersionPill(
                        label     = "Current",
                        value     = getLocalVersion(),
                        color     = MaterialTheme.colorScheme.secondaryContainer,
                        textColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Icon(Icons.Filled.ArrowForward, null,
                        Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    VersionPill(
                        label     = "New",
                        value     = info.version,
                        color     = MaterialTheme.colorScheme.primaryContainer,
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Released ${info.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // ── What's new ────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.NewReleases, null,
                        Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("What's New",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    info.changes.forEach { ChangelogItem(it) }
                }

                Spacer(Modifier.height(24.dp))

                // ── Download / Progress / Installing ──────────────────────
                when (downloadState) {

                    DownloadState.Idle -> {
                        Button(
                            onClick   = { viewModel.startDownload(info) },
                            modifier  = Modifier.fillMaxWidth().height(52.dp),
                            shape     = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.Download, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download v${info.version}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold)
                        }
                    }

                    DownloadState.Downloading -> {
                        DownloadProgressCard(progress = downloadProgress)
                    }

                    DownloadState.Installing -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(16.dp),
                            color    = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("Opening installer…",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }

                    DownloadState.Failed -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(16.dp),
                            color    = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.ErrorOutline, null,
                                    Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.height(8.dp))
                                Text("Download failed. Check your connection.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center)
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = { viewModel.retryDownload() }) {
                                    Icon(Icons.Filled.Refresh, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Postpone button (10 min) — only shown when idle or failed ──
                if (downloadState == DownloadState.Idle || downloadState == DownloadState.Failed) {
                    TextButton(
                        onClick = {
                            postponeSecondsLeft = 10 * 60
                            viewModel.postponeUpdate()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Schedule, null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Remind me in 10 minutes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── In-progress download card ──────────────────────────────────────────────────
@Composable
private fun DownloadProgressCard(progress: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Downloading update…",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text("$progress%",
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress       = { progress / 100f },
                modifier       = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color          = MaterialTheme.colorScheme.primary,
                trackColor     = MaterialTheme.colorScheme.primary.copy(0.2f),
                strokeCap      = StrokeCap.Round
            )
            Spacer(Modifier.height(6.dp))
            Text("Please don't close the app",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.65f))
        }
    }
}

// ── Version pill ──────────────────────────────────────────────────────────────
@Composable
private fun VersionPill(label: String, value: String, color: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(50.dp), color = color) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = textColor.copy(0.7f))
            Text("v$value", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

// ── Changelog bullet ─────────────────────────────────────────────────────────
@Composable
private fun ChangelogItem(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.padding(top = 6.dp).size(6.dp)
                    .clip(CircleShape).background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
        }
    }
}

// ── Animated shield badge ─────────────────────────────────────────────────────
@Composable
private fun UpdateShieldBadge() {
    val infinite = rememberInfiniteTransition(label = "badge")
    val pulse by infinite.animateFloat(
        1f, 1.12f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val glowAlpha by infinite.animateFloat(
        0.15f, 0.40f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow"
    )
    val primary = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
        Box(
            Modifier.size(90.dp).scale(pulse).clip(CircleShape)
                .background(Brush.radialGradient(listOf(primary.copy(glowAlpha), Color.Transparent)))
        )
        Surface(
            modifier        = Modifier.size(72.dp),
            shape           = RoundedCornerShape(20.dp),
            color           = MaterialTheme.colorScheme.primaryContainer,
            contentColor    = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.SystemUpdate, null, modifier = Modifier.size(36.dp))
            }
        }
    }
}

// ── Helper ────────────────────────────────────────────────────────────────────
@Composable
private fun getLocalVersion(): String {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
    } catch (_: Exception) { "?" }
}
