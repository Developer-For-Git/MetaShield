package com.metashield.app.ui.stego

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.metashield.app.ui.components.*
import com.metashield.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteganographyScreen(
    onNavigateUp: () -> Unit,
    viewModel: SteganographyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()


    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectUri(it) }
    }

    CyberScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pixel Armor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Hide secret messages inside the pixels of any image.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Image Picker / Preview
            item {
                CyberCard(
                    modifier = Modifier.fillMaxWidth().height(200.dp).clickable { launcher.launch("image/*") },
                    shape = CyberCardShape
                ) {
                    if (state.selectedUri == null) {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text("Select Cover Image", fontWeight = FontWeight.Medium)
                        }
                    } else {
                        AsyncImage(
                            model = state.selectedUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CyberCardShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }

            // Tabs for Encode/Decode
            item {
                var selectedTab by remember { mutableIntStateOf(0) }
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; viewModel.clearResult() },
                        text = { Text("Conceal") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; viewModel.clearResult() },
                        text = { Text("Reveal") }
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Conceal UI
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.message,
                            onValueChange = { viewModel.updateMessage(it) },
                            label = { Text("Secret Message") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = { viewModel.updatePassword(it) },
                            label = { Text("Encryption Password (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )
                        
                        CyberButton(
                            onClick = { viewModel.encode() },
                            label = if (state.isProcessing) "Encoding..." else "Hide Message in Pixels",
                            icon = Icons.Outlined.VisibilityOff,
                            modifier = Modifier.fillMaxWidth(),
                            gradient = listOf(CyberMagenta, CyberMagenta),
                            enabled = state.selectedUri != null && state.message.isNotEmpty() && !state.isProcessing
                        )

                        if (state.resultUri != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SafeGreen.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = SafeGreen)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Success! Image protected.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CyberIconButton(
                                    onClick = { viewModel.shareImage() },
                                    icon = Icons.Outlined.Share,
                                    contentDescription = "Share",
                                    color = CyberBlue,
                                    modifier = Modifier.size(48.dp)
                                )
                                CyberButton(
                                    onClick = { viewModel.saveToDevice() },
                                    label = "Save to Gallery",
                                    icon = Icons.Outlined.FileDownload,
                                    modifier = Modifier.weight(1f),
                                    gradient = listOf(SafeGreen, SafeGreen)
                                )
                            }
                        }
                    }
                } else {
                    // Reveal UI
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = { viewModel.updatePassword(it) },
                            label = { Text("Enter Password (if encrypted)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                        )
                        
                        CyberButton(
                            onClick = { viewModel.decode() },
                            label = if (state.isProcessing) "Decoding..." else "Reveal Hidden Secret",
                            icon = Icons.Outlined.AutoFixHigh,
                            modifier = Modifier.fillMaxWidth(),
                            gradient = listOf(CyberBlue, CyberBlue),
                            enabled = state.selectedUri != null && !state.isProcessing
                        )

                        if (state.decodedMessage != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Revealed Message:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(4.dp))
                                    Text(state.decodedMessage!!, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            
            if (state.error != null) {
                item {
                    Text(state.error!!, color = DangerRed, style = MaterialTheme.typography.bodySmall)
                }
            }
            
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
