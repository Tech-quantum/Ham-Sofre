package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.barcode.BarcodeProductInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AdjustedInventoryData(
    val name: String,
    val barcode: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val location: String,
    val expiryDateMillis: Long,
    val price: Long,
    val purchasedByMemberId: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedBarcodeAdjustmentDialog(
    productInfo: BarcodeProductInfo,
    scannedBarcode: String,
    onDismiss: () -> Unit,
    onFinalize: (AdjustedInventoryData) -> Unit
) {
    val now = remember { System.currentTimeMillis() }

    var itemName by remember(productInfo) { mutableStateOf(productInfo.name) }
    var selectedCategory by remember(productInfo) { mutableStateOf(productInfo.category) }
    var selectedLocation by remember(productInfo) { mutableStateOf(productInfo.defaultLocation) }
    var selectedUnit by remember(productInfo) { mutableStateOf(productInfo.defaultUnit) }

    var quantityText by remember(productInfo) {
        val qty = productInfo.defaultQuantity
        val str = if (qty == qty.toLong().toDouble()) qty.toLong().toString() else qty.toString()
        mutableStateOf(str)
    }

    var shelfLifeDaysText by remember(productInfo) {
        mutableStateOf(productInfo.shelfLifeDays.toString())
    }

    var priceText by remember(productInfo) {
        mutableStateOf(if (productInfo.estimatedPrice > 0) productInfo.estimatedPrice.toString() else "")
    }

    val currentShelfDays = shelfLifeDaysText.toIntOrNull() ?: productInfo.shelfLifeDays
    val computedExpiryMillis = remember(now, currentShelfDays) {
        now + (currentShelfDays.toLong() * 24L * 60L * 60L * 1000L)
    }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val formattedExpiryDate = remember(computedExpiryMillis) {
        dateFormat.format(Date(computedExpiryMillis))
    }

    val categories = listOf("لبنیات", "میوه و سبزیجات", "پروتئین", "نان و غلات", "خواروبار", "تنقلات", "نوشیدنی")
    val units = listOf("عدد", "کیلوگرم", "گرم", "بسته", "لیتر", "قوطی", "شانه")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تنظیم و نهایی‌سازی ورود کالا",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "بارکد با موفقیت اسکن شد",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Barcode Info Strip
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Tag,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "بارکد: $scannedBarcode",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            SuggestionChip(
                                onClick = {},
                                label = { Text("کشف‌شده از کاتالوگ", fontSize = 10.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    // Product Name Field
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("نام خوراکی *") },
                        leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    // 1. QUANTITY ADJUSTMENT SECTION (تنظیم دستی مقدار و تعداد)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Balance,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تنظیم مقدار و تعداد موجودی",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "واحد: $selectedUnit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Stepper and Direct Input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        val cur = quantityText.toDoubleOrNull() ?: 1.0
                                        if (cur > 1.0) {
                                            val next = cur - 1.0
                                            quantityText = if (next == next.toLong().toDouble()) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
                                        } else if (cur > 0.1) {
                                            val next = (cur - 0.5).coerceAtLeast(0.1)
                                            quantityText = String.format(Locale.US, "%.1f", next)
                                        }
                                    },
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "کاهش")
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                OutlinedTextField(
                                    value = quantityText,
                                    onValueChange = { quantityText = it },
                                    modifier = Modifier.width(120.dp),
                                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                FilledTonalIconButton(
                                    onClick = {
                                        val cur = quantityText.toDoubleOrNull() ?: 1.0
                                        val next = cur + 1.0
                                        quantityText = if (next == next.toLong().toDouble()) next.toLong().toString() else String.format(Locale.US, "%.1f", next)
                                    },
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "افزایش")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Unit selection chips
                            Text("انتخاب واحد سنجش:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(units) { u ->
                                    FilterChip(
                                        selected = selectedUnit == u,
                                        onClick = { selectedUnit = u },
                                        label = { Text(u, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // 2. EXPIRY DATE ADJUSTMENT SECTION (تنظیم دستی تاریخ انقضا)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تنظیم تاریخ انقضا و ماندگاری",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }

                                // Freshness Status Indicator Badge
                                val statusBadgeText = when {
                                    currentShelfDays < 0 -> "🔴 منقضی‌شده"
                                    currentShelfDays <= 3 -> "⚠️ نزدیک به انقضا"
                                    else -> "🟢 تازه و دارای مهلت"
                                }
                                Text(
                                    text = statusBadgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentShelfDays <= 3) MaterialTheme.colorScheme.error else Color(0xFF15803D)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Days input and date preview
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = shelfLifeDaysText,
                                    onValueChange = { shelfLifeDaysText = it },
                                    label = { Text("روز مانده تا انقضا") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "موعد انقضا تقویمی:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = formattedExpiryDate,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick Preset Days Chips
                            Text("انتخاب سریع مهلت نگهداری:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val presets = listOf(
                                    3 to "+۳ روز (فوری)",
                                    7 to "+۷ روز (۱ هفته)",
                                    14 to "+۱۴ روز (۲ هفته)",
                                    30 to "+۳۰ روز (۱ ماه)",
                                    90 to "+۹۰ روز (۳ ماه)",
                                    365 to "+۱ سال"
                                )
                                items(presets) { (days, label) ->
                                    FilterChip(
                                        selected = currentShelfDays == days,
                                        onClick = { shelfLifeDaysText = days.toString() },
                                        label = { Text(label, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // 3. STORAGE LOCATION & CATEGORY
                    Text("محل نگهداری در منزل:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("FRIDGE", "یخچال", Icons.Default.Kitchen),
                            Triple("FREEZER", "فریزر", Icons.Default.AcUnit),
                            Triple("PANTRY", "انبار و کابینت", Icons.Default.Inventory2)
                        ).forEach { (loc, label, icon) ->
                            FilterChip(
                                selected = selectedLocation == loc,
                                onClick = { selectedLocation = loc },
                                label = { Text(label, fontSize = 11.sp) },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Category row
                    Text("دسته‌بندی کالا:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Optional Price field
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("قیمت خرید (تومان) - اختیاری") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("انصراف")
                    }

                    Button(
                        onClick = {
                            val finalQuantity = quantityText.toDoubleOrNull() ?: 1.0
                            val finalPrice = priceText.toLongOrNull() ?: 0L
                            val finalExpiry = computedExpiryMillis

                            onFinalize(
                                AdjustedInventoryData(
                                    name = itemName.trim().ifEmpty { productInfo.name },
                                    barcode = scannedBarcode,
                                    category = selectedCategory,
                                    quantity = finalQuantity,
                                    unit = selectedUnit,
                                    location = selectedLocation,
                                    expiryDateMillis = finalExpiry,
                                    price = finalPrice,
                                    purchasedByMemberId = 0L
                                )
                            )
                        },
                        modifier = Modifier.weight(1.8f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ثبت نهایی در انبار", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
