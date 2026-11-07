package id.storia.receipt.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.storia.receipt.data.AnalysisResult
import id.storia.receipt.data.LocalReceiptParser
import id.storia.receipt.data.UnknownReceipt
import id.storia.receipt.ui.components.DigitalReceiptView
import id.storia.receipt.utils.ReceiptImageUtils

import androidx.compose.material.icons.filled.TouchApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    imageUri: Uri,
    onNavigateBack: () -> Unit,
    onOpenInteractiveOcr: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var showRawText by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        isLoading = true
        val parser = LocalReceiptParser(context)
        analysisResult = parser.parseReceipt(imageUri)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR Analysis Result") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Scanning receipt with ML Kit...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                val result = analysisResult
                if (result != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DigitalReceiptView(
                            receipt = result.receipt,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Processing Info",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Category: ${result.receipt.category.name}")
                                result.processingTimeMs?.let { time ->
                                    Text("Execution Time: ${time}ms")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onOpenInteractiveOcr,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.TouchApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit / Tap to Select Fields on Image")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showRawText = !showRawText },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (showRawText) "Hide Raw OCR Text" else "View Raw OCR Text")
                        }

                        if (showRawText) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    text = (result.receipt as? UnknownReceipt)?.rawText
                                        ?: "No raw text recorded.",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    Text("Failed to analyze image.")
                }
            }
        }
    }
}
