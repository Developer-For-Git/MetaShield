package com.metashield.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.metashield.app.data.repository.SettingsRepository
import com.metashield.app.ui.components.CyberButton
import com.metashield.app.ui.components.CyberScaffold
import com.metashield.app.ui.navigation.NavGraph
import com.metashield.app.ui.theme.*
import com.metashield.app.util.BiometricHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@OptIn(ExperimentalFoundationApi::class)
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var sharedUris: List<Uri> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Initialize PDFBox for document metadata processing
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)

        enableEdgeToEdge()

        // Handle incoming shared files from Android share sheet
        sharedUris = extractSharedUris(intent)

        setContent {
            val appLockEnabled by settingsRepository.appLockEnabled.collectAsState(initial = null)
            val themeMode by settingsRepository.themeMode.collectAsState(initial = "SYSTEM")
            val isUnlocked = remember { mutableStateOf(false) }

            // Decide whether to show lock screen. 
            // null state means we're still loading from DataStore, so we wait.
            val shouldLock = appLockEnabled == true && !isUnlocked.value

            val isDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MetaShieldTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (shouldLock) {
                        LockScreen(
                            onAuthenticate = { isDecoy ->
                                com.metashield.app.util.VaultSession.startSession(isDecoy)
                                isUnlocked.value = true
                            },
                            onBiometricPrompt = {
                                val helper = BiometricHelper(this@MainActivity)
                                helper.showBiometricPrompt(
                                    title = "MetaShield Locked",
                                    subtitle = "Authenticate to access your workspace",
                                    onSuccess = { 
                                        com.metashield.app.util.VaultSession.startSession(false)
                                        isUnlocked.value = true 
                                    },
                                    onError = { _, err ->
                                        android.widget.Toast.makeText(this@MainActivity, err, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        )
                    } else if (appLockEnabled != null) {
                        NavGraph(initialSharedUris = sharedUris)
                    }
                }
            }
        }
    }

    @Composable
    private fun LockScreen(onAuthenticate: (Boolean) -> Unit, onBiometricPrompt: () -> Unit) {
        var showPinEntry by remember { mutableStateOf(false) }
        var pinValue by remember { mutableStateOf("") }
        
        val infiniteTransition = rememberInfiniteTransition(label = "auth")
        val radarRotation by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
            label = "rot"
        )
        val auraAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f, targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing)),
            label = "aura"
        )
        val auraScale by infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.6f,
            animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing)),
            label = "scale"
        )

        LaunchedEffect(Unit) {
            onBiometricPrompt()
        }

        CyberScaffold {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!showPinEntry) {
                    // Identity Radar Core
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.combinedClickable(
                            onClick = onBiometricPrompt,
                            onLongClick = { showPinEntry = true }
                        )
                    ) {
                        // Pulsating Identity Aura
                        Box(modifier = Modifier.scale(auraScale).size(120.dp).clip(CircleShape).background(ObsidianIndigo.copy(alpha = auraAlpha)))
                        
                        // Biometric Radar Disk
                        Canvas(modifier = Modifier.size(160.dp)) {
                            drawRadarTerminal(this, radarRotation, ObsidianIndigo)
                        }

                        // Biometric Icon
                        Icon(
                            Icons.Outlined.Fingerprint,
                            null,
                            Modifier.size(64.dp),
                            tint = ObsidianIndigo
                        )
                    }

                    Spacer(Modifier.height(48.dp))

                    // Holographic Status Console
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "ENCRYPTED ACCESS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "PROTOCOL_LOCK_v4.2 // PENDING_IDENTITY",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = ObsidianIndigo.copy(0.7f),
                            letterSpacing = 1.sp
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Text(
                            "Identity verification required to continue",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(64.dp))

                    // Liquid Authorization Console
                    CyberButton(
                        onClick = onBiometricPrompt,
                        label = "GRANT_IDENTITY_PROTOCOL",
                        icon = Icons.Filled.Fingerprint,
                        gradient = GradientPrimary,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )
                } else {
                    // Tactical PIN Overlay (Identity Shift)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "IDENTITY_SHIFT_PROTOCOL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = ObsidianCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Manual Override Code Required",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        
                        Spacer(Modifier.height(48.dp))
                        
                        // Digital PIN Readout
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            repeat(4) { i ->
                                val active = i < pinValue.length
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) ObsidianCyan.copy(0.15f) else SpaceCard)
                                        .border(1.dp, if (active) ObsidianCyan else GlassBorder.copy(0.1f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (active) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ObsidianCyan))
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(48.dp))
                        
                        // Tactile Numeric Grid
                        val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "OK")
                        numbers.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                                row.forEach { num ->
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(SpaceCard)
                                            .border(1.dp, GlassBorder.copy(0.05f), CircleShape)
                                            .clickable {
                                                when (num) {
                                                    "C" -> if (pinValue.isNotEmpty()) pinValue = pinValue.dropLast(1)
                                                    "OK" -> {
                                                        if (pinValue == "0000") onAuthenticate(true)
                                                        else if (pinValue == "1234") onAuthenticate(false)
                                                        else pinValue = "" // Reset on fail
                                                    }
                                                    else -> if (pinValue.length < 4) pinValue += num
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(num, style = MaterialTheme.typography.titleMedium, color = if (num == "OK") ObsidianCyan else TextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        
                        TextButton(onClick = { showPinEntry = false; pinValue = "" }) {
                            Text("CANCEL_SHIFT", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    private fun drawRadarTerminal(scope: DrawScope, rotation: Float, color: Color) {
        with(scope) {
            // Background Ring
            drawCircle(color = color.copy(0.06f), style = Stroke(width = 1.dp.toPx()))
            
            // Sweep Gradient
            rotate(rotation) {
                drawArc(
                    brush = Brush.sweepGradient(0f to color.copy(0.3f), 0.15f to Color.Transparent),
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = true
                )
            }
            
            // Outer Ring
            drawCircle(color = color.copy(0.12f), style = Stroke(width = 2.dp.toPx()))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Update shared URIs when new intent arrives (e.g., new share while app is open)
        sharedUris = extractSharedUris(intent)
        setIntent(intent)
    }

    private fun extractSharedUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                listOfNotNull(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
            }
            else -> emptyList()
        }
    }
}
