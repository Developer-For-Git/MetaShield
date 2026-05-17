package com.metashield.app.ui.scanner

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metashield.app.ui.components.*
import com.metashield.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScannerScreen(
    onNavigateUp: () -> Unit,
    viewModel: PrivacyScannerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_IMAGES
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) viewModel.startScan()
    }

    CyberScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Health Scan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextSecondary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (!state.isScanning && state.scannedCount > 0) {
                CyberActionBottomBar {
                    CyberButton(
                        onClick = { launcher.launch(permission) },
                        label = "Initiate Deep Re-Scan",
                        icon = Icons.Outlined.Refresh,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Primary Scan Tool / Report Hub ──────────────────────────────
            item {
                if (state.isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(GlassWhite.copy(alpha = 0.05f))
                            .border(1.dp, GlassBorder.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RadarScanner(
                                isScanning = true,
                                scannedCount = state.scannedCount,
                                totalFound = state.totalPhotosFound
                            )
                            Spacer(Modifier.height(24.dp))
                            LiveActivityLog(
                                fileName = state.currentFileName,
                                gpsDetected = state.photosWithGps,
                                deviceDetected = state.photosWithDevice
                            )
                        }
                    }
                } else if (state.scannedCount > 0) {
                    PrivacyReportHub(
                        score = state.privacyScore,
                        scannedCount = state.scannedCount
                    )
                } else {
                    // Ready State (Pre-scan)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        RadarScanner(isScanning = false, scannedCount = 0, totalFound = 0)
                    }
                }
            }

            if (!state.isScanning && state.scannedCount == 0) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Analyze Privacy Integrity", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Conduct a multi-layer analysis of binary metadata to uncover latent privacy vulnerabilities across your media.",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center)
                    }
                }
                item {
                    CyberButton(
                        onClick = { launcher.launch(permission) },
                        label = "Initiate Security Audit",
                        icon = Icons.Outlined.Security,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }
            }

            if (!state.isScanning && state.scannedCount > 0) {
                // ── Risk Matrix (2x2) ───────────────────────────────────────
                item {
                    SectionHeader("Privacy Threat Matrix", modifier = Modifier.padding(bottom = 8.dp))
                }
                
                val gpsRisk = if (state.scannedCount > 0) (state.photosWithGps * 100 / state.scannedCount) else 0
                val deviceRisk = if (state.scannedCount > 0) (state.photosWithDevice * 100 / state.scannedCount) else 0
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            RiskTile("GPS Leak", gpsRisk, Icons.Outlined.LocationOff, listOf(SafeGreen, ObsidianCyan), modifier = Modifier.weight(1f))
                            RiskTile("Device ID", deviceRisk, Icons.Outlined.PhoneAndroid, GradientPrimary, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            RiskTile("Time Exposure", (gpsRisk * 0.7f).toInt(), Icons.Outlined.Schedule, GradientViolet, modifier = Modifier.weight(1f))
                            RiskTile("Software Sig", (deviceRisk * 0.8f).toInt(), Icons.Outlined.CameraAlt, listOf(ObsidianCyan, SafeGreen), modifier = Modifier.weight(1f))
                        }
                    }
                }
                
                // ── Hardening Tips ──────────────────────────────────────────
                item {
                    SectionHeader("Hardening Recommendations", modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                }
                
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            HardeningStep(
                                icon = Icons.Outlined.GpsFixed,
                                title = "Strip GPS Latency",
                                description = "Disable camera location access in System Settings to prevent future coordinate leakage."
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = GlassWhite.copy(0.05f))
                            HardeningStep(
                                icon = Icons.Outlined.CleaningServices,
                                title = "Execute Batch Purge",
                                description = "Use the 'Clean Phone' tool to strip metadata from ${state.scannedCount} identified files."
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun PrivacyReportHub(score: Int, scannedCount: Int) {
    val scoreColor = when {
        score >= 80 -> SafeGreen
        score >= 50 -> PrivacyAmber
        else -> DangerRed
    }
    val statusText = when {
        score >= 80 -> "OPTIMIZED"
        score >= 50 -> "VULNERABLE"
        else -> "CRITICAL"
    }

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = GlassWhite.copy(alpha = 0.03f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Privacy Audit Report", style = MaterialTheme.typography.labelSmall, color = TextHint, letterSpacing = 2.sp)
            Spacer(Modifier.height(20.dp))
            
            Box(contentAlignment = Alignment.Center) {
                // Glowing background pulse
                Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(scoreColor.copy(0.08f)).blur(40.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = scoreColor
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                        letterSpacing = 1.sp
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = GlassWhite.copy(0.08f))
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                ReportStat(label = "Analyzed", value = scannedCount.toString())
                Box(Modifier.width(1.dp).height(24.dp).background(GlassWhite.copy(0.1f)))
                ReportStat(label = "Secured", value = (scannedCount * (score/100f)).toInt().toString())
                Box(Modifier.width(1.dp).height(24.dp).background(GlassWhite.copy(0.1f)))
                ReportStat(label = "Integrity", value = "$score%")
            }
        }
    }
}

@Composable
private fun ReportStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextHint)
    }
}

@Composable
private fun RiskTile(label: String, riskPercent: Int, icon: ImageVector, gradient: List<Color>, modifier: Modifier = Modifier) {
    val color = when {
        riskPercent < 15 -> SafeGreen
        riskPercent < 50 -> PrivacyAmber
        else -> DangerRed
    }
    
    Card(
        modifier = modifier
            .border(1.dp, GlassBorder.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GlassWhite.copy(0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconBox(icon = icon, gradient = gradient, size = 36.dp, iconSize = 18.dp, shape = RoundedCornerShape(10.dp))
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            }
            Spacer(Modifier.height(16.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$riskPercent%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = TextPrimary)
                Spacer(Modifier.width(4.dp))
                Text("RISK", style = MaterialTheme.typography.labelSmall, color = TextHint, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun HardeningStep(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = ObsidianCyan, modifier = Modifier.size(20.dp).padding(top = 2.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(description, style = MaterialTheme.typography.labelSmall, color = TextSecondary, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun RadarScanner(isScanning: Boolean, scannedCount: Int, totalFound: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "sweep"
    )

    Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Radar Background Circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = GlassWhite.copy(alpha = 0.05f), radius = size.width / 2.2f)
            drawCircle(color = GlassWhite.copy(alpha = 0.05f), radius = size.width / 3.5f)
            drawCircle(color = GlassWhite.copy(alpha = 0.05f), radius = size.width / 8f)
            
            // Crosshairs
            drawLine(GlassWhite.copy(0.1f), Offset(0f, size.height/2), Offset(size.width, size.height/2), 1f)
            drawLine(GlassWhite.copy(0.1f), Offset(size.width/2, 0f), Offset(size.width/2, size.height), 1f)
        }

        // Animated Sweep
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(sweepAngle)
                    .drawWithCache {
                        onDrawBehind {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    0f to Color.Transparent,
                                    0.5f to ObsidianCyan.copy(alpha = 0.4f),
                                    1f to Color.Transparent
                                ),
                                startAngle = -90f,
                                sweepAngle = 90f,
                                useCenter = true,
                                style = androidx.compose.ui.graphics.drawscope.Fill
                            )
                            // Leading line
                            drawLine(
                                color = ObsidianCyan,
                                start = Offset(size.width / 2, size.height / 2),
                                end = Offset(size.width / 2, 0f),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
            )
        }

        // Center Score / Status
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(SpaceRaised, SpaceVoid)))
                .border(BorderStroke(1.dp, GlassBorder.copy(alpha = 0.2f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isScanning) {
                    val progress = if (totalFound > 0) (scannedCount.toFloat() / totalFound) else 0f
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = ObsidianCyan)
                    Text("SCANNING", style = MaterialTheme.typography.labelSmall, color = TextHint, letterSpacing = 2.sp)
                } else {
                    Icon(Icons.Filled.Shield, null, tint = ObsidianCyan, modifier = Modifier.size(42.dp))
                    Text("READY", style = MaterialTheme.typography.labelSmall, color = TextHint, letterSpacing = 2.sp)
                }
            }
        }
    }
}

@Composable
private fun LiveActivityLog(fileName: String?, gpsDetected: Int, deviceDetected: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Text
        Text(
            text = if (fileName != null) "Analyzing: $fileName" else "Initializing deep link scanner...",
            style = MaterialTheme.typography.labelSmall,
            color = ObsidianCyan,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Detection Stats
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(0.2f)).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Detections", (gpsDetected + deviceDetected).toString(), PrivacyAmber)
            Box(Modifier.width(1.dp).height(24.dp).background(GlassWhite.copy(0.1f)))
            StatItem("GPS Latent", gpsDetected.toString(), DangerRed)
            Box(Modifier.width(1.dp).height(24.dp).background(GlassWhite.copy(0.1f)))
            StatItem("Digital ID", deviceDetected.toString(), SafeGreen)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextHint, fontSize = 9.sp)
    }
}


