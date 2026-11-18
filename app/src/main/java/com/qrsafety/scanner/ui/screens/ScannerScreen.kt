package com.qrsafety.scanner.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.qrsafety.scanner.analyzer.AnalysisResult
import com.qrsafety.scanner.analyzer.UrlSafetyAnalyzer
import com.qrsafety.scanner.camera.CameraPreview
import com.qrsafety.scanner.camera.QrCodeAnalyzer
import com.qrsafety.scanner.data.ScanHistoryRepository
import com.qrsafety.scanner.data.ScanRecord
import com.qrsafety.scanner.ui.components.ScannerOverlay
import com.qrsafety.scanner.ui.theme.DarkBackground
import com.qrsafety.scanner.ui.theme.PrimaryBlue
import com.qrsafety.scanner.ui.theme.SurfaceDark
import com.qrsafety.scanner.ui.theme.TextMuted
import com.qrsafety.scanner.ui.theme.TextPrimary
import com.qrsafety.scanner.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ScannerScreen(
    repository: ScanHistoryRepository,
    onNavigateToHistory: () -> Unit,
    onNavigateToTestSamples: () -> Unit,
    preselectedTestUrl: String? = null,
    onClearPreselectedTestUrl: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission is needed to scan QR codes", Toast.LENGTH_LONG).show()
        }
    }

    var isTorchEnabled by remember { mutableStateOf(false) }
    var currentLensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var currentAnalysis by remember { mutableStateOf<AnalysisResult?>(null) }

    val qrAnalyzer = remember {
        QrCodeAnalyzer { rawString ->
            val result = UrlSafetyAnalyzer.analyze(rawString)
            currentAnalysis = result

            // Persist to scan history automatically
            scope.launch {
                val summary = result.warnings.joinToString("; ") { it.title }
                repository.addRecord(
                    ScanRecord(
                        url = result.parsedUrl,
                        host = result.host,
                        threatLevel = result.threatLevel,
                        warningSummary = summary
                    )
                )
            }
        }
    }

    // Handle preselected test URL if passed from test sandbox
    LaunchedEffect(preselectedTestUrl) {
        if (!preselectedTestUrl.isNullOrBlank()) {
            qrAnalyzer.pauseScanning()
            val result = UrlSafetyAnalyzer.analyze(preselectedTestUrl)
            currentAnalysis = result
            scope.launch {
                val summary = result.warnings.joinToString("; ") { it.title }
                repository.addRecord(
                    ScanRecord(
                        url = result.parsedUrl,
                        host = result.host,
                        threatLevel = result.threatLevel,
                        warningSummary = summary
                    )
                )
            }
            onClearPreselectedTestUrl()
        }
    }

    // Gallery Image Picker fallback
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    val scanner = BarcodeScanning.getClient()
                    scanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            val detected = barcodes.firstOrNull()?.rawValue
                            if (!detected.isNullOrBlank()) {
                                qrAnalyzer.pauseScanning()
                                val result = UrlSafetyAnalyzer.analyze(detected)
                                currentAnalysis = result
                                scope.launch {
                                    val summary = result.warnings.joinToString("; ") { it.title }
                                    repository.addRecord(
                                        ScanRecord(
                                            url = result.parsedUrl,
                                            host = result.host,
                                            threatLevel = result.threatLevel,
                                            warningSummary = summary
                                        )
                                    )
                                }
                            } else {
                                Toast.makeText(context, "No QR code found in selected image", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to analyze selected image", Toast.LENGTH_SHORT).show()
                        }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading image file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        if (hasCameraPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                analyzer = qrAnalyzer,
                torchEnabled = isTorchEnabled,
                lensFacing = currentLensFacing
            )
            ScannerOverlay(modifier = Modifier.fillMaxSize())
        } else {
            // Camera permission fallback UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "QR Safety Scanner requires camera access to scan and analyze QR codes.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Grant Permission", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC0F172A))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "QR Safety Scanner",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Torch toggle (Must-Have #1)
                if (hasCameraPermission) {
                    IconButton(
                        onClick = { isTorchEnabled = !isTorchEnabled },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isTorchEnabled) PrimaryBlue else Color(0xCC0F172A))
                    ) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch",
                            tint = if (isTorchEnabled) DarkBackground else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Camera Switch (Front/Back) (Must-Have #1)
                    IconButton(
                        onClick = {
                            currentLensFacing = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
                                isTorchEnabled = false
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC0F172A))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Pick from Gallery
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC0F172A))
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Scan Image",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Test Playground button
                IconButton(
                    onClick = onNavigateToTestSamples,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC0F172A))
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = "Test Sandbox",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // History button (Bonus requirement)
                IconButton(
                    onClick = onNavigateToHistory,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC0F172A))
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Safety Analysis Verdict Dialog / Sheet (Must-Have #2, #3, #4)
        currentAnalysis?.let { analysis ->
            SafetyDialog(
                analysis = analysis,
                onOpenAnyway = { url ->
                    currentAnalysis = null
                    qrAnalyzer.resumeScanning()
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(browserIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open browser for this link", Toast.LENGTH_SHORT).show()
                    }
                },
                onDontOpen = {
                    currentAnalysis = null
                    qrAnalyzer.resumeScanning()
                }
            )
        }
    }
}
