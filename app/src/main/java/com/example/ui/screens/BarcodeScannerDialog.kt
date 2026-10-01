package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.barcode.BarcodeCatalog
import com.example.data.barcode.BarcodeProductInfo
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerDialog(
    onDismissRequest: () -> Unit,
    onBarcodeScanned: (String, BarcodeProductInfo) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var manualBarcodeInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                    Text(
                        text = "اسکن بارکد خوراکی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Camera Scanner Area or Permission Placeholder
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        CameraBarcodePreview(
                            onBarcodeDetected = { scannedCode ->
                                vibrate(context)
                                val info = BarcodeCatalog.lookup(scannedCode)
                                onBarcodeScanned(scannedCode, info)
                                onDismissRequest()
                            }
                        )

                        // Targeting Reticle
                        Box(
                            modifier = Modifier
                                .size(240.dp, 160.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        )
                        Text(
                            text = "بارکد محصول را درون کادر قرار دهید",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "دسترسی دوربین برای اسکن بارکد لازم است",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("اعطای مجوز دوربین")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Simulation Barcodes for Testing (Especially useful in Emulator)
                Text(
                    text = "آزمایش سریع اقلام متداول (تست بدون دوربین):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(BarcodeCatalog.KNOWN_PRODUCTS.values.toList().take(5)) { item ->
                        SuggestionChip(
                            onClick = {
                                vibrate(context)
                                onBarcodeScanned(item.barcode, item)
                                onDismissRequest()
                            },
                            label = { Text(item.name, fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Manual Barcode Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualBarcodeInput,
                        onValueChange = { manualBarcodeInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("یا بارکد را دستی وارد کنید...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (manualBarcodeInput.isNotBlank()) {
                                vibrate(context)
                                val info = BarcodeCatalog.lookup(manualBarcodeInput)
                                onBarcodeScanned(manualBarcodeInput, info)
                                onDismissRequest()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ثبت")
                    }
                }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraBarcodePreview(
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    var isScanned by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null && !isScanned) {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val rawValue = barcode.rawValue
                                    if (!rawValue.isNullOrBlank() && !isScanned) {
                                        isScanned = true
                                        onBarcodeDetected(rawValue)
                                        break
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

private fun vibrate(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    } catch (_: Exception) {}
}
