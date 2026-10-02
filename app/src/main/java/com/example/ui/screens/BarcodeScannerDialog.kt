package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.barcode.BarcodeCatalog
import com.example.data.barcode.BarcodeProductInfo

@Composable
fun BarcodeScannerDialog(
    onDismissRequest: () -> Unit,
    onBarcodeScanned: (String, BarcodeProductInfo) -> Unit,
    scanViewModel: BarcodeScanViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by scanViewModel.uiState.collectAsStateWithLifecycle()

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
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var manualBarcodeInput by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("FRIDGE") }
    var quantityAdjustment by remember { mutableStateOf(1.0) }

    LaunchedEffect(uiState.productInfo) {
        uiState.productInfo?.let { product ->
            selectedLocation = product.defaultLocation
            quantityAdjustment = product.defaultQuantity
        }
    }

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
                        text = "اسکن بارکد و ورود سریع به انبار",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { scanViewModel.toggleTorch() }) {
                        Icon(
                            if (uiState.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "چراغ قوه",
                            tint = if (uiState.isTorchOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Camera Scanner Area or Permission / Detected Product Result
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        CameraXBarcodeView(scanViewModel = scanViewModel)

                        // Reticle Overlay
                        Box(
                            modifier = Modifier
                                .size(250.dp, 160.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (uiState.scannedBarcode != null) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        )

                        Text(
                            text = if (uiState.scannedBarcode != null) "بارکد شناسایی شد!" else "بارکد محصول را در کادر قرار دهید",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
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
                                modifier = Modifier.size(54.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "دسترسی دوربین برای اسکن بارکد لازم است",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                                Text("اعطای مجوز دوربین")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Add Card if a product was scanned
                AnimatedVisibility(visible = uiState.productInfo != null) {
                    uiState.productInfo?.let { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "دسته: ${product.category} | ماندگاری پیش‌فرض: ${product.shelfLifeDays} روز",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }

                                    // Location selector
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("FRIDGE" to "یخچال", "FREEZER" to "فریزر", "PANTRY" to "انبار").forEach { (loc, label) ->
                                            FilterChip(
                                                selected = selectedLocation == loc,
                                                onClick = { selectedLocation = loc },
                                                label = { Text(label, fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Quantity Selector
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("تعداد:", style = MaterialTheme.typography.bodySmall)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        FilledTonalIconButton(
                                            onClick = { if (quantityAdjustment > 0.5) quantityAdjustment -= 1.0 },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        Text(
                                            text = " $quantityAdjustment ${product.defaultUnit} ",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        FilledTonalIconButton(
                                            onClick = { quantityAdjustment += 1.0 },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    // Action Buttons Row
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                // Trigger detailed manual adjustment dialog
                                                scanViewModel.onBarcodeIdentified(uiState.scannedBarcode ?: product.barcode)
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تنظیم جزئیات و تاریخ انقضا", fontSize = 11.sp)
                                        }

                                        // 1-Click Quick Add Button
                                        Button(
                                            onClick = {
                                                scanViewModel.quickAddToInventory(
                                                    quantity = quantityAdjustment,
                                                    location = selectedLocation,
                                                    onSuccess = { addedFood ->
                                                        onBarcodeScanned(addedFood.barcode, product)
                                                        onDismissRequest()
                                                    }
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("افزودن فوری")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick test items for emulator/testing without physical barcodes
                Text(
                    text = "آزمایش سریع اقلام (بدون دوربین / شبیه‌ساز):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(BarcodeCatalog.KNOWN_PRODUCTS.values.toList().take(6)) { item ->
                        SuggestionChip(
                            onClick = { scanViewModel.onBarcodeIdentified(item.barcode) },
                            label = { Text(item.name, fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                                scanViewModel.onBarcodeIdentified(manualBarcodeInput.trim())
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("جستجو")
                    }
                }
            }
        }
    }

    // Detailed Pop-Up Dialog to manually adjust quantity, expiry date, etc. before finalizing
    if (uiState.showAdjustmentDialog && uiState.productInfo != null) {
        val currentProduct = uiState.productInfo!!
        ScannedBarcodeAdjustmentDialog(
            productInfo = currentProduct,
            scannedBarcode = uiState.scannedBarcode ?: currentProduct.barcode,
            onDismiss = {
                scanViewModel.dismissAdjustmentDialog()
            },
            onFinalize = { adjustedData ->
                scanViewModel.finalizeScannedItemToInventory(
                    customName = adjustedData.name,
                    customCategory = adjustedData.category,
                    quantity = adjustedData.quantity,
                    unit = adjustedData.unit,
                    location = adjustedData.location,
                    expiryDateMillis = adjustedData.expiryDateMillis,
                    price = adjustedData.price,
                    buyerId = adjustedData.purchasedByMemberId,
                    onSuccess = { savedItem ->
                        onBarcodeScanned(savedItem.barcode, currentProduct)
                        onDismissRequest()
                    }
                )
            }
        )
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraXBarcodeView(
    scanViewModel: BarcodeScanViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

                imageAnalysis.setAnalyzer(scanViewModel.cameraExecutor) { imageProxy ->
                    scanViewModel.processImageProxy(imageProxy)
                }

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    scanViewModel.bindCamera(camera)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
