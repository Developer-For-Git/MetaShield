package com.metashield.app.ui.viewer

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metashield.app.data.model.MetadataCategory
import com.metashield.app.data.model.MetadataField
import com.metashield.app.data.model.SensitivityLevel
import com.metashield.app.ui.components.*
import com.metashield.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataViewerScreen(
    fileUri: Uri,
    onNavigateUp: () -> Unit,
    onNavigateToRemoval: () -> Unit,
    onNavigateToEditor: () -> Unit,
    viewModel: MetadataViewerViewModel = hiltViewModel()
) {
    LaunchedEffect(fileUri) { viewModel.loadMetadata(fileUri) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CyberScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.fileName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = TextSecondary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEditor) {
                        Icon(Icons.Outlined.Edit, "Edit", tint = ObsidianCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (state.fields.isNotEmpty()) {
                PrivacyControlDock(
                    onStrip = onNavigateToRemoval,
                    onEdit = onNavigateToEditor
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    // Loading state
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            val infiniteTransition = rememberInfiniteTransition(label = "load")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f, targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    androidx.compose.animation.core.tween(1200, easing = androidx.compose.animation.core.LinearEasing)
                                ),
                                label = "rot"
                            )
                            Box(
                                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp))
                                    .background(Brush.linearGradient(GradientPrimary)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.FindInPage, null, tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                            Text("Reading metadata…", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }

                state.error != null -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(DangerRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.ErrorOutline, null, tint = DangerRed, modifier = Modifier.size(36.dp)) }
                            Text("Unable to read metadata", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text(state.error!!, color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }
                }

                else -> {
                    MetadataContent(
                        fields = state.fields,
                        privacyScore = state.privacyScore,
                        searchQuery = state.searchQuery,
                        onSearchChange = viewModel::updateSearch,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// ── Metadata Content ──────────────────────────────────────────────────────────
@Composable
private fun MetadataContent(
    fields: List<MetadataField>,
    privacyScore: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val grouped = fields.groupBy { it.category }
    val sensitiveCount = fields.count { it.isSensitive }
    val scoreColor = when {
        privacyScore > 80 -> SafeGreen
        privacyScore > 40 -> PrivacyAmber
        else              -> DangerRed
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // ── Privacy Integrity Gauge ──────────────────────────────────────────
        item {
            IntegrityGaugeHub(privacyScore = privacyScore, sensitiveCount = sensitiveCount)
        }

        // ── Stat badges (Holographic) ────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge("${fields.size} Tags Detected", ObsidianIndigo)
                if (sensitiveCount > 0) StatusBadge("$sensitiveCount High Risk", DangerRed)
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Filter metadata identifiers...", color = TextHint, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Filled.Close, null, tint = TextSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ObsidianIndigo,
                    unfocusedBorderColor = GlassBorder.copy(alpha = 0.1f),
                    focusedContainerColor = GlassWhite.copy(alpha = 0.04f),
                    unfocusedContainerColor = GlassWhite.copy(alpha = 0.02f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                textStyle = MaterialTheme.typography.bodySmall
            )
        }

        // ── Category sections ─────────────────────────────────────────────────
        grouped.forEach { (category, catFields) ->
            item(key = "cat_${category.name}") {
                CategorySection(category = category, fields = catFields)
            }
        }

        // ── Empty search ──────────────────────────────────────────────────────
        if (fields.isEmpty() && searchQuery.isNotEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.SearchOff, null, tint = TextHint, modifier = Modifier.size(40.dp))
                        Text("IDENTIFIER_NOT_FOUND", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun IntegrityGaugeHub(privacyScore: Int, sensitiveCount: Int) {
    val scoreColor = when {
        privacyScore > 80 -> SafeGreen
        privacyScore > 40 -> PrivacyAmber
        else              -> DangerRed
    }

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.Transparent
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("INTEGRITY_INDEX_v1.0", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = scoreColor.copy(0.7f), letterSpacing = 1.sp)
            
            Spacer(Modifier.height(24.dp))

            // Circular Radar Gauge
            Box(contentAlignment = Alignment.Center) {
                val infiniteTransition = rememberInfiniteTransition(label = "radar")
                val shadowRotation by infiniteTransition.animateFloat(
                    initialValue = 0f, targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
                    label = "rot"
                )

                // Background Ring
                Canvas(modifier = Modifier.size(160.dp)) {
                    drawCircle(color = Color.White.copy(0.04f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                    
                    // Score Arc
                    drawArc(
                        color = scoreColor.copy(0.12f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    drawArc(
                        color = scoreColor,
                        startAngle = -90f,
                        sweepAngle = (privacyScore / 100f) * 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }

                // Radar Sweep Animation
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(1.2f)
                        .rotate(shadowRotation)
                        .drawBehind {
                            drawArc(
                                brush = Brush.sweepGradient(0f to scoreColor.copy(0.3f), 0.1f to Color.Transparent),
                                startAngle = 0f,
                                sweepAngle = 90f,
                                useCenter = true
                            )
                        }
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$privacyScore", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color.White)
                    Text("SECURE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = scoreColor)
                }
            }

            if (sensitiveCount > 0) {
                Spacer(Modifier.height(32.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DangerRed.copy(alpha = 0.08f))
                        .border(1.dp, DangerRed.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.ReportGmailerrorred, null, tint = DangerRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("INTEGRITY_BREACH_DETECTED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = DangerRed, letterSpacing = 1.sp)
                        Text("$sensitiveCount fields compromise your identity", style = MaterialTheme.typography.labelSmall, color = DangerRed.copy(0.7f))
                    }
                }
            }
        }
    }
}

// ── Category Section ──────────────────────────────────────────────────────────
@Composable
private fun CategorySection(category: MetadataCategory, fields: List<MetadataField>) {
    var expanded by remember { mutableStateOf(true) }

    val hasSensitive = fields.any { it.isSensitive }
    val categoryColor = when (category.name) {
        "LOCATION" -> DangerRed
        "DEVICE"   -> ObsidianPink
        "CAMERA"   -> ObsidianCyan
        "TIME"     -> ObsidianViolet
        else       -> TextSecondary
    }

    CyberCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (hasSensitive) DangerRed.copy(alpha = 0.3f) else GlassBorder.copy(alpha = 0.1f)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon with Halo
                Box(contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(48.dp).background(Brush.radialGradient(listOf(categoryColor.copy(0.12f), Color.Transparent))))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(categoryColor))
                }
                
                Spacer(Modifier.width(16.dp))
                
                Text(
                    category.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                
                if (hasSensitive) {
                    StatusBadge("RISK", DangerRed)
                    Spacer(Modifier.width(8.dp))
                }
                StatusBadge("${fields.size}", ObsidianIndigo)
                Spacer(Modifier.width(12.dp))
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = TextHint, modifier = Modifier.size(16.dp))
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    fields.forEachIndexed { index, field ->
                        if (index > 0) HorizontalDivider(color = GlassWhite.copy(0.04f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
                        FieldRow(field)
                    }
                }
            }
        }
    }
}

// ── Field Row (System Console Format) ─────────────────────────────────────────
@Composable
private fun FieldRow(field: MetadataField) {
    val sensitivityColor = when {
        !field.isSensitive -> null
        field.sensitivityLevel == SensitivityLevel.HIGH   -> DangerRed
        field.sensitivityLevel == SensitivityLevel.MEDIUM -> PrivacyAmber
        else -> SafeGreen
    }

    Row(
        modifier = Modifier.fillMaxWidth()
            .then(if (sensitivityColor != null) Modifier.background(sensitivityColor.copy(alpha = 0.04f)) else Modifier)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // High Contrast Identifier
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    field.tag.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = sensitivityColor?.copy(0.7f) ?: TextSecondary,
                    letterSpacing = 1.sp
                )
                if (sensitivityColor != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(sensitivityColor))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                field.value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = sensitivityColor ?: TextPrimary,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
        }
        
        if (sensitivityColor != null) {
            val label = when (field.sensitivityLevel) {
                SensitivityLevel.HIGH   -> "HIGH_RISK"
                SensitivityLevel.MEDIUM -> "SENSITIVE"
                SensitivityLevel.LOW    -> "PROTECT"
            }
            StatusBadge(label, sensitivityColor)
        }
    }
}

@Composable
private fun PrivacyControlDock(
    onStrip: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(SpaceVoid.copy(0.1f), SpaceVoid)))
            .drawBehind {
                // Neon Path Border
                val strokeWidth = 1.5.dp.toPx()
                drawLine(
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, ObsidianCyan.copy(0.4f), ObsidianPink.copy(0.4f), Color.Transparent)),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = strokeWidth
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(top = 28.dp, start = 20.dp, end = 20.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Strip Action Segment (Primary)
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(DangerRed, ObsidianPink)))
                    .clickable(onClick = onStrip),
                contentAlignment = Alignment.Center
            ) {
                // Surface Shimmer
                val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
                val shimmerX by infiniteTransition.animateFloat(
                    initialValue = -100f, targetValue = 500f,
                    animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
                    label = "x"
                )
                Box(modifier = Modifier.fillMaxSize().drawBehind {
                    drawRect(
                        brush = Brush.linearGradient(
                            0f to Color.Transparent, 
                            0.5f to Color.White.copy(0.12f), 
                            1f to Color.Transparent,
                            start = androidx.compose.ui.geometry.Offset(shimmerX, 0f),
                            end = androidx.compose.ui.geometry.Offset(shimmerX + 100f, size.height)
                        )
                    )
                })

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Shield, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "STRIP_IDENTITY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Edit Action Segment (Secondary)
            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(GlassWhite.copy(0.04f))
                    .border(1.dp, GlassWhite.copy(0.08f), RoundedCornerShape(18.dp))
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                // Identity Halo
                Box(modifier = Modifier.size(40.dp).background(Brush.radialGradient(listOf(ObsidianCyan.copy(0.12f), Color.Transparent))))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Edit, null, tint = ObsidianCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "EDIT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
