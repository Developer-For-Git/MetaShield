package com.metashield.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metashield.app.ui.components.*
import com.metashield.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportScreen(
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    var summary by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    CyberScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Report a Bug", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.Filled.ArrowBack, "Back", tint = CyberGreen) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Section 1: Tell us about the bug
            BugSectionTitle("1. Tell us about the bug")
            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it },
                placeholder = { Text("Short summary of the issue...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberBlue,
                    unfocusedBorderColor = CyberBlue.copy(alpha = 0.4f),
                    cursorColor = CyberBlue,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Section 2: Upload proof
            BugSectionTitle("2. Please upload video or screenshot proof in email")
            CyberCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                borderColor = CyberOutline.copy(alpha = 0.2f),
                containerColor = CyberSurface.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "To help us fix the bug faster, please attach any relevant screenshots or videos directly in your email app before sending.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
                )
            }

            // Section 3: Describe your bug
            BugSectionTitle("3. Describe your bug")
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Step by step details on how to reproduce...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberBlue,
                    unfocusedBorderColor = CyberBlue.copy(alpha = 0.4f),
                    cursorColor = CyberBlue,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(Modifier.weight(1f))

            // Send Button
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("modstoredeveloperteam@outlook.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "MetaShield Bug Report: $summary")
                        putExtra(Intent.EXTRA_TEXT, "--- BUG REPORT ---\n\nSummary: $summary\n\nDetails:\n$description\n\n(Please remember to attach screenshots/videos below)")
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Send Email..."))
                    } catch (_: Exception) {}
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(CyberGlitch)), // Using CyberGlitch (Magenta/Blue)
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Send, null, tint = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "SEND BUG REPORT",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BugSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = CyberBlue,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
