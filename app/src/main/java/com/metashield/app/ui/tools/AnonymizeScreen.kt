package com.metashield.app.ui.tools

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.metashield.app.data.processor.IdentityMasker
import com.metashield.app.ui.components.CyberButton
import com.metashield.app.ui.components.CyberScaffold
import com.metashield.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnonymizeScreen(
    fileUri: Uri,
    onNavigateBack: () -> Unit,
    onProcessed: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val masker = remember { IdentityMasker(context) }
    
    var isProcessing by remember { mutableStateOf(false) }
    var resultFile by remember { mutableStateOf<java.io.File?>(null) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "offset"
    )

    CyberScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identity Masking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Immersive Preview Console
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SpaceCard)
                    .border(1.dp, GlassBorder.copy(0.1f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = resultFile ?: fileUri,
                    contentDescription = "Identity Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                // Tactical Scanning Line
                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.Center)
                            .offset(y = (200 * scanOffset).dp)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, ObsidianCyan, Color.Transparent)))
                    )
                }
                
                // Protocol Overlays
                Column(
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (isProcessing) PrivacyAmber else SafeGreen))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isProcessing) "SCANNING_ID..." else "SOURCE_ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                    Text(
                        "PROTOCOL: ID_MASK_v1.0",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary.copy(0.7f)
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Interaction Console
            if (resultFile == null) {
                CyberButton(
                    onClick = {
                        scope.launch {
                            isProcessing = true
                            masker.anonymize(fileUri).fold(
                                onSuccess = { resultFile = it },
                                onFailure = { android.widget.Toast.makeText(context, it.message, android.widget.Toast.LENGTH_SHORT).show() }
                            )
                            isProcessing = false
                        }
                    },
                    label = "INITIALIZE_IDENTITY_MASK",
                    icon = Icons.Outlined.Face,
                    gradient = GradientPrimary,
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = { resultFile = null },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder.copy(0.2f))
                    ) {
                        Text("RESET", color = TextPrimary)
                    }
                    CyberButton(
                        onClick = { onProcessed(Uri.fromFile(resultFile!!)) },
                        label = "COMMIT_MASK",
                        icon = Icons.Outlined.Security,
                        gradient = listOf(ObsidianCyan, ObsidianViolet),
                        modifier = Modifier.weight(1.5f).height(56.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text(
                "AI Identity Masking operates locally. No biometric data leaves this device.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
