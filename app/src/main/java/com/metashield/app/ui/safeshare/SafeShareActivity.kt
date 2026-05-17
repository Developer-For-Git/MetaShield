package com.metashield.app.ui.safeshare

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.metashield.app.data.model.RemovalOptions
import com.metashield.app.data.repository.MetadataRepository
import com.metashield.app.domain.usecase.StripMetadataUseCase
import com.metashield.app.ui.components.CyberScaffold
import com.metashield.app.ui.theme.MetaShieldTheme
import com.metashield.app.ui.theme.CyberMagenta
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder
import javax.inject.Inject

@AndroidEntryPoint
class SafeShareActivity : ComponentActivity() {

    @Inject lateinit var stripMetadataUseCase: StripMetadataUseCase
    @Inject lateinit var metadataRepository: MetadataRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (uri == null) {
            finish()
            return
        }

        setContent {
            MetaShieldTheme {
                SafeShareContent(
                    onCancel = { finish() }
                )
            }
        }

        processAndShare(uri)
    }

    private fun processAndShare(uri: Uri) {
        lifecycleScope.launch {
            try {
                val fileItem = metadataRepository.getFileItem(uri)
                val outputName = "CLEAN_${fileItem.name}"
                val outputFile = File(cacheDir, outputName)
                if (outputFile.exists()) outputFile.delete()
                outputFile.createNewFile()

                val outputUri = Uri.fromFile(outputFile)
                
                stripMetadataUseCase(uri, RemovalOptions.SOCIAL_SHARE, outputUri).fold(
                    onSuccess = {
                        val shareUri = androidx.core.content.FileProvider.getUriForFile(
                            this@SafeShareActivity,
                            "${packageName}.fileprovider",
                            outputFile
                        )
                        launchSystemShare(shareUri, fileItem.mimeType ?: "*/*")
                    },
                    onFailure = {
                        // In a real app, show error toast
                        finish()
                    }
                )
            } catch (e: Exception) {
                finish()
            }
        }
    }

    private fun launchSystemShare(uri: Uri, mimeType: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Cleaned Media"))
        finish()
    }
}

@Composable
fun SafeShareContent(onCancel: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = CyberMagenta,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "SHIELDING MEDIA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = CyberMagenta
                )
                Text(
                    "Stripping sensitive metadata for safe sharing...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
