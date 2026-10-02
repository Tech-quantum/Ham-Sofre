package com.example.ui.screens

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HamSofrehApplication
import com.example.data.barcode.BarcodeCatalog
import com.example.data.barcode.BarcodeProductInfo
import com.example.data.model.FoodItem
import com.example.data.repository.FoodShareRepository
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

data class BarcodeScanUiState(
    val isScanningActive: Boolean = true,
    val isTorchOn: Boolean = false,
    val scannedBarcode: String? = null,
    val productInfo: BarcodeProductInfo? = null,
    val showAdjustmentDialog: Boolean = false,
    val isQuickAdded: Boolean = false,
    val statusMessage: String? = null,
    val recentScans: List<BarcodeProductInfo> = emptyList()
)

class BarcodeScanViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HamSofrehApplication
    private val repository = FoodShareRepository(
        app.database.memberDao(),
        app.database.foodItemDao(),
        app.database.purchaseDao(),
        app.database.consumptionDao(),
        app.database.shoppingListDao(),
        app.database.settlementDao(),
        app.database.chatDao()
    )

    private val _uiState = MutableStateFlow(BarcodeScanUiState())
    val uiState: StateFlow<BarcodeScanUiState> = _uiState.asStateFlow()

    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()

    private var activeCamera: Camera? = null

    fun bindCamera(camera: Camera) {
        activeCamera = camera
        camera.cameraControl.enableTorch(_uiState.value.isTorchOn)
    }

    fun toggleTorch() {
        val nextState = !_uiState.value.isTorchOn
        activeCamera?.cameraControl?.enableTorch(nextState)
        _uiState.value = _uiState.value.copy(isTorchOn = nextState)
    }

    /**
     * CameraX ImageAnalysis Analyzer implementation using ML Kit
     */
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && _uiState.value.isScanningActive && _uiState.value.scannedBarcode == null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrBlank() && _uiState.value.scannedBarcode == null) {
                            onBarcodeIdentified(rawValue)
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

    fun onBarcodeIdentified(barcode: String) {
        val product = BarcodeCatalog.lookup(barcode)
        triggerVibration()

        val updatedRecent = (listOf(product) + _uiState.value.recentScans).distinctBy { it.barcode }.take(5)
        _uiState.value = _uiState.value.copy(
            scannedBarcode = barcode,
            productInfo = product,
            showAdjustmentDialog = true,
            isScanningActive = false,
            statusMessage = "محصول «${product.name}» شناسایی شد.",
            recentScans = updatedRecent
        )
    }

    /**
     * Finalize scanned item into Room Food Inventory with custom quantity, expiry date, location, etc.
     */
    fun finalizeScannedItemToInventory(
        customName: String? = null,
        customCategory: String? = null,
        quantity: Double,
        unit: String? = null,
        location: String,
        expiryDateMillis: Long,
        price: Long = 0L,
        buyerId: Long = 0L,
        onSuccess: (FoodItem) -> Unit = {}
    ) {
        val product = _uiState.value.productInfo ?: return
        val barcode = _uiState.value.scannedBarcode ?: product.barcode

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val foodItem = FoodItem(
                name = customName?.takeIf { it.isNotBlank() } ?: product.name,
                barcode = barcode,
                category = customCategory?.takeIf { it.isNotBlank() } ?: product.category,
                quantity = quantity,
                unit = unit ?: product.defaultUnit,
                location = location,
                purchaseDateMillis = now,
                expiryDateMillis = expiryDateMillis,
                price = if (price > 0) price else product.estimatedPrice,
                purchasedByMemberId = buyerId
            )

            val insertedId = repository.insertFoodItem(foodItem)
            val completeItem = foodItem.copy(id = insertedId)

            _uiState.value = _uiState.value.copy(
                showAdjustmentDialog = false,
                isQuickAdded = true,
                statusMessage = "«${foodItem.name}» با موفقیت به انبار اضافه شد! ✔"
            )
            onSuccess(completeItem)
        }
    }

    /**
     * Quick Add directly to Room Food Inventory
     */
    fun quickAddToInventory(
        quantity: Double? = null,
        location: String? = null,
        shelfLifeDays: Int? = null,
        onSuccess: (FoodItem) -> Unit = {}
    ) {
        val product = _uiState.value.productInfo ?: return
        val barcode = _uiState.value.scannedBarcode ?: product.barcode

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val days = shelfLifeDays ?: product.shelfLifeDays
            val finalQuantity = quantity ?: product.defaultQuantity
            val finalLocation = location ?: product.defaultLocation

            val foodItem = FoodItem(
                name = product.name,
                barcode = barcode,
                category = product.category,
                quantity = finalQuantity,
                unit = product.defaultUnit,
                location = finalLocation,
                purchaseDateMillis = now,
                expiryDateMillis = now + (days.toLong() * 24 * 60 * 60 * 1000),
                price = product.estimatedPrice
            )

            val insertedId = repository.insertFoodItem(foodItem)
            val completeItem = foodItem.copy(id = insertedId)

            _uiState.value = _uiState.value.copy(
                showAdjustmentDialog = false,
                isQuickAdded = true,
                statusMessage = "«${foodItem.name}» سریعاً به انبار اضافه شد! ✔"
            )
            onSuccess(completeItem)
        }
    }

    fun dismissAdjustmentDialog() {
        _uiState.value = _uiState.value.copy(
            showAdjustmentDialog = false,
            isScanningActive = true
        )
    }

    fun resumeScanning() {
        _uiState.value = _uiState.value.copy(
            isScanningActive = true,
            scannedBarcode = null,
            productInfo = null,
            showAdjustmentDialog = false,
            isQuickAdded = false,
            statusMessage = null
        )
    }

    private fun triggerVibration() {
        try {
            val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(60)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }
}
