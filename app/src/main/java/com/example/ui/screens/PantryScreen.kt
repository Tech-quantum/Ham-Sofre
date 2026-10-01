package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.barcode.BarcodeProductInfo
import com.example.data.model.FoodItem
import com.example.data.model.Member
import com.example.ui.MainViewModel
import com.example.ui.components.ExpiryBadge
import com.example.ui.components.LocationBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(
    viewModel: MainViewModel,
    foodItems: List<FoodItem>,
    members: List<Member>,
    onOpenBarcodeScanner: () -> Unit,
    onOpenAiRecipes: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, EXPIRING, FRIDGE, FREEZER, PANTRY
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToConsume by remember { mutableStateOf<FoodItem?>(null) }
    var itemToEdit by remember { mutableStateOf<FoodItem?>(null) }

    val now = remember { System.currentTimeMillis() }

    val filteredItems = remember(foodItems, searchQuery, selectedFilter) {
        foodItems.filter { item ->
            val matchesSearch = item.name.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "EXPIRING" -> item.daysUntilExpiry(now) <= 3
                "FRIDGE" -> item.location == "FRIDGE"
                "FREEZER" -> item.location == "FREEZER"
                "PANTRY" -> item.location == "PANTRY"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val expiringCount = remember(foodItems) {
        foodItems.count { it.daysUntilExpiry(now) in 0..3 }
    }
    val expiredCount = remember(foodItems) {
        foodItems.count { it.isExpired(now) }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("افزودن خوراکی") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Expiry Waste Alert Banner (Requirement: notification / prioritize consumption)
            if (expiringCount > 0 || expiredCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (expiredCount > 0) MaterialTheme.colorScheme.errorContainer else Color(0xFFFEF3C7)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (expiredCount > 0) MaterialTheme.colorScheme.error else Color(0xFFB45309),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (expiredCount > 0) "$expiredCount قلم منقضی و $expiringCount قلم در آستانه انقضا!"
                                else "$expiringCount قلم خوراکی نزدیک به تاریخ انقضا!",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (expiredCount > 0) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF78350F)
                            )
                            Text(
                                text = "برای جلوگیری از اسراف، این اقلام را در اولویت مصرف قرار دهید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (expiredCount > 0) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else Color(0xFF92400E)
                            )
                        }
                        IconButton(
                            onClick = {
                                val count = viewModel.triggerExpiryNotificationCheck()
                                viewModel.showMessage("هشدار انقضا برای $count قلم خوراکی به گوشی ارسال شد. 🔔")
                            }
                        ) {
                            Icon(
                                Icons.Default.Campaign,
                                contentDescription = "ارسال هشدار به گوشی",
                                tint = if (expiredCount > 0) MaterialTheme.colorScheme.error else Color(0xFFB45309)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 10.dp)
                            .clickable { onOpenAiRecipes() },
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "چی بپزیم؟ پیشنهاد دستور پخت هوشمند با جمینای",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Search and Barcode Scan Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("جستجو در یخچال و انبار...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = onOpenBarcodeScanner,
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "اسکن بارکد",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("همه (${foodItems.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "EXPIRING",
                        onClick = { selectedFilter = "EXPIRING" },
                        label = { Text("نزدیک انقضا ($expiringCount)") },
                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "FRIDGE",
                        onClick = { selectedFilter = "FRIDGE" },
                        label = { Text("یخچال") },
                        leadingIcon = { Icon(Icons.Default.Kitchen, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "FREEZER",
                        onClick = { selectedFilter = "FREEZER" },
                        label = { Text("فریزر") },
                        leadingIcon = { Icon(Icons.Default.AcUnit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "PANTRY",
                        onClick = { selectedFilter = "PANTRY" },
                        label = { Text("کابینت و انبار") },
                        leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Food Items List
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SoupKitchen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "موردی با این نام پیدا نشد" else "انبار و یخچال خالی است!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "با زدن دکمه اسکن یا دکمه افزودن، خوراکی‌ها را وارد کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        FoodItemCard(
                            item = item,
                            onConsumeClick = { itemToConsume = item },
                            onDeleteClick = { viewModel.deleteFoodItem(item) },
                            onEditClick = { itemToEdit = item }
                        )
                    }
                }
            }
        }
    }

    // Add Food Item Dialog
    if (showAddDialog) {
        AddEditFoodItemDialog(
            item = null,
            members = members,
            onDismiss = { showAddDialog = false },
            onSave = { newItem ->
                // Check duplicate duplicate warning
                val duplicateWarning = viewModel.checkForDuplicates(newItem.name, newItem.barcode)
                if (duplicateWarning != null) {
                    viewModel.showMessage(duplicateWarning)
                }
                viewModel.addOrUpdateFoodItem(newItem)
                showAddDialog = false
            },
            onScanBarcode = onOpenBarcodeScanner
        )
    }

    // Edit Dialog
    itemToEdit?.let { editItem ->
        AddEditFoodItemDialog(
            item = editItem,
            members = members,
            onDismiss = { itemToEdit = null },
            onSave = { updated ->
                viewModel.addOrUpdateFoodItem(updated)
                itemToEdit = null
            },
            onScanBarcode = onOpenBarcodeScanner
        )
    }

    // Consume Dialog
    itemToConsume?.let { consumeTarget ->
        ConsumeFoodDialog(
            item = consumeTarget,
            members = members,
            onDismiss = { itemToConsume = null },
            onConfirm = { quantity, member, note ->
                viewModel.consumeFood(consumeTarget, quantity, member, note)
                itemToConsume = null
            }
        )
    }
}

@Composable
fun FoodItemCard(
    item: FoodItem,
    onConsumeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LocationBadge(item.location)
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                ExpiryBadge(item)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stock progress and details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "موجودی: ${if (item.quantity % 1.0 == 0.0) item.quantity.toInt() else item.quantity} ${item.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = onConsumeClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ثبت مصرف", fontSize = 12.sp)
                    }

                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFoodItemDialog(
    item: FoodItem?,
    members: List<Member>,
    onDismiss: () -> Unit,
    onSave: (FoodItem) -> Unit,
    onScanBarcode: () -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var barcode by remember { mutableStateOf(item?.barcode ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "لبنیات") }
    var quantityText by remember { mutableStateOf(item?.quantity?.toString() ?: "1.0") }
    var unit by remember { mutableStateOf(item?.unit ?: "عدد") }
    var location by remember { mutableStateOf(item?.location ?: "FRIDGE") }
    var shelfLifeDaysText by remember {
        mutableStateOf(
            if (item != null) item.daysUntilExpiry().toString() else "7"
        )
    }
    var priceText by remember { mutableStateOf(if (item != null && item.price > 0) item.price.toString() else "") }
    var selectedBuyerId by remember { mutableStateOf(item?.purchasedByMemberId ?: members.firstOrNull()?.id ?: 0L) }

    val categories = listOf("لبنیات", "میوه و سبزیجات", "پروتئین", "نان و غلات", "خواروبار", "تنقلات", "نوشیدنی")
    val units = listOf("عدد", "کیلوگرم", "گرم", "بسته", "لیتر", "قوطی")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (item == null) "افزودن خوراکی جدید" else "ویرایش خوراکی")
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Barcode scan button inside dialog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("بارکد محصول") },
                            placeholder = { Text("اختیاری") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledIconButton(onClick = onScanBarcode) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "اسکن")
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("نام خوراکی *") },
                        singleLine = true
                    )
                }

                item {
                    Text("دسته‌بندی:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Text("محل نگهداری:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = location == "FRIDGE",
                            onClick = { location = "FRIDGE" },
                            label = { Text("یخچال") },
                            leadingIcon = { Icon(Icons.Default.Kitchen, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = location == "FREEZER",
                            onClick = { location = "FREEZER" },
                            label = { Text("فریزر") },
                            leadingIcon = { Icon(Icons.Default.AcUnit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = location == "PANTRY",
                            onClick = { location = "PANTRY" },
                            label = { Text("کابینت") },
                            leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("تعداد / مقدار") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("واحد") },
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = shelfLifeDaysText,
                        onValueChange = { shelfLifeDaysText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("مدت ماندگاری تا انقضا (روز)") },
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("قیمت خرید (تومان - اختیاری)") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val qty = quantityText.toDoubleOrNull() ?: 1.0
                    val shelfDays = shelfLifeDaysText.toLongOrNull() ?: 7L
                    val price = priceText.toLongOrNull() ?: 0L
                    val now = System.currentTimeMillis()

                    val newItem = item?.copy(
                        name = name.trim(),
                        barcode = barcode.trim(),
                        category = category,
                        quantity = qty,
                        unit = unit.trim(),
                        location = location,
                        expiryDateMillis = now + (shelfDays * 24 * 60 * 60 * 1000),
                        price = price,
                        purchasedByMemberId = selectedBuyerId
                    ) ?: FoodItem(
                        name = name.trim(),
                        barcode = barcode.trim(),
                        category = category,
                        quantity = qty,
                        unit = unit.trim(),
                        location = location,
                        purchaseDateMillis = now,
                        expiryDateMillis = now + (shelfDays * 24 * 60 * 60 * 1000),
                        price = price,
                        purchasedByMemberId = selectedBuyerId
                    )
                    onSave(newItem)
                }
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
fun ConsumeFoodDialog(
    item: FoodItem,
    members: List<Member>,
    onDismiss: () -> Unit,
    onConfirm: (Double, Member, String) -> Unit
) {
    var quantityText by remember {
        mutableStateOf(
            if (item.quantity >= 1.0) "1.0" else item.quantity.toString()
        )
    }
    var selectedMember by remember { mutableStateOf(members.firstOrNull { it.isMe } ?: members.firstOrNull()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت مصرف «${item.name}»") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "کل موجودی: ${item.quantity} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("مقدار مصرف شده (${item.unit})") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("چه کسی مصرف کرد؟", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(members) { m ->
                        FilterChip(
                            selected = selectedMember?.id == m.id,
                            onClick = { selectedMember = m },
                            label = { Text(m.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("یادداشت (اختیاری مثل پخت غذا)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toDoubleOrNull() ?: 1.0
                    val member = selectedMember ?: return@Button
                    onConfirm(qty, member, note)
                }
            ) {
                Text("تأیید مصرف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
