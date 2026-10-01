package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.barcode.BarcodeProductInfo
import com.example.data.model.Member
import com.example.data.model.ShoppingListItem
import com.example.data.model.SmartShoppingSuggestion
import com.example.ui.MainViewModel

@Composable
fun ShoppingListScreen(
    viewModel: MainViewModel,
    shoppingItems: List<ShoppingListItem>,
    members: List<Member>,
    smartSuggestions: List<SmartShoppingSuggestion> = emptyList(),
    onOpenBarcodeScanner: () -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToAssign by remember { mutableStateOf<ShoppingListItem?>(null) }
    var showConvertExpenseDialog by remember { mutableStateOf(false) }
    var onlyShowUrgent by remember { mutableStateOf(false) }

    val pendingItems = remember(shoppingItems, onlyShowUrgent) {
        val list = shoppingItems.filter { !it.isPurchased }
        if (onlyShowUrgent) list.filter { it.isUrgent } else list
    }
    val purchasedItems = remember(shoppingItems) { shoppingItems.filter { it.isPurchased } }
    val urgentCount = remember(shoppingItems) { shoppingItems.count { !it.isPurchased && it.isUrgent } }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null) },
                text = { Text("افزودن به لیست") },
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

            // Action Toolbar for Collaborative Features
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share via WhatsApp / SMS
                FilledTonalButton(
                    onClick = { viewModel.shareShoppingList(context) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ارسال لیست", fontSize = 12.sp)
                }

                // Convert Checked to Purchase & Inventory
                if (purchasedItems.isNotEmpty()) {
                    Button(
                        onClick = { showConvertExpenseDialog = true },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ثبت خرج و انبار (${purchasedItems.size})", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips: All vs Urgently Needed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !onlyShowUrgent,
                    onClick = { onlyShowUrgent = false },
                    label = { Text("همه اقلام (${shoppingItems.count { !it.isPurchased }})") },
                    leadingIcon = {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                FilterChip(
                    selected = onlyShowUrgent,
                    onClick = { onlyShowUrgent = !onlyShowUrgent },
                    label = { Text("⚡ فقط نیازهای فوری ($urgentCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFE4E6),
                        selectedLabelColor = Color(0xFFBE123C)
                    ),
                    border = BorderStroke(1.dp, if (onlyShowUrgent) Color(0xFFBE123C) else MaterialTheme.colorScheme.outline)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Section: Smart Suggestions based on consumption patterns
                if (smartSuggestions.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "پیشنهاد هوشمند خرید بر اساس الگوی مصرف خانوار",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(smartSuggestions) { suggestion ->
                                        SmartSuggestionCard(
                                            suggestion = suggestion,
                                            onAddNormal = {
                                                viewModel.addSuggestedToShopping(suggestion, isUrgent = false)
                                            },
                                            onAddUrgent = {
                                                viewModel.addSuggestedToShopping(suggestion, isUrgent = true)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: Pending Shopping Items
                if (pendingItems.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (onlyShowUrgent) "اقلام فوری مورد نیاز (${pendingItems.size})" else "اقلام مورد نیاز (${pendingItems.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (onlyShowUrgent) Color(0xFFBE123C) else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "برای تغییر به فوری روی ⚡ بزنید",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    items(pendingItems, key = { it.id }) { item ->
                        ShoppingListItemRow(
                            item = item,
                            onTogglePurchased = { viewModel.toggleShoppingPurchased(item) },
                            onToggleUrgent = { viewModel.toggleShoppingUrgent(item) },
                            onAssignClick = { itemToAssign = item },
                            onDelete = { viewModel.deleteShoppingItem(item) }
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    if (onlyShowUrgent) Icons.Default.Bolt else Icons.Default.ShoppingCartCheckout,
                                    contentDescription = null,
                                    modifier = Modifier.size(54.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (onlyShowUrgent) "هیچ قلم فوری در لیست وجود ندارد!" else "لیست خرید مشترک خالی است!",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "از دکمه زیر یا پیشنهادات هوشمند بالا اقلام مورد نیاز را اضافه کنید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // Section: Purchased Items
                if (purchasedItems.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "خریده شده (${purchasedItems.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            TextButton(onClick = { viewModel.clearCompletedShopping() }) {
                                Text("پاک کردن خریده‌شده‌ها", fontSize = 11.sp)
                            }
                        }
                    }
                    items(purchasedItems, key = { it.id }) { item ->
                        ShoppingListItemRow(
                            item = item,
                            onTogglePurchased = { viewModel.toggleShoppingPurchased(item) },
                            onToggleUrgent = { viewModel.toggleShoppingUrgent(item) },
                            onAssignClick = { itemToAssign = item },
                            onDelete = { viewModel.deleteShoppingItem(item) }
                        )
                    }
                }
            }
        }
    }

    // Add Shopping Item Dialog with Duplicate Warning & Urgent Toggle
    if (showAddDialog) {
        AddShoppingItemDialog(
            members = members,
            onDismiss = { showAddDialog = false },
            onSave = { name, qty, unit, cat, buyer, assigned, urgent, barcode, estPrice ->
                // Check duplicate check
                val dupMsg = viewModel.checkForDuplicates(name, barcode)
                if (dupMsg != null) {
                    viewModel.showMessage(dupMsg)
                }
                viewModel.addShoppingItem(
                    name = name,
                    quantity = qty,
                    unit = unit,
                    category = cat,
                    addedBy = buyer,
                    assignedTo = assigned,
                    isUrgent = urgent,
                    barcode = barcode,
                    estimatedPrice = estPrice
                )
                showAddDialog = false
            },
            onScanBarcode = onOpenBarcodeScanner
        )
    }

    // Assign Member Dialog
    itemToAssign?.let { item ->
        AssignMemberDialog(
            item = item,
            members = members,
            onDismiss = { itemToAssign = null },
            onAssign = { member ->
                viewModel.assignShoppingItem(item, member)
                itemToAssign = null
            }
        )
    }

    // Convert Checked to Purchase Dialog
    if (showConvertExpenseDialog) {
        ConvertShoppingToExpenseDialog(
            items = purchasedItems,
            members = members,
            onDismiss = { showConvertExpenseDialog = false },
            onConfirm = { buyer, beneficiaries, amount ->
                viewModel.convertPurchasedItemsToExpense(buyer, beneficiaries, amount)
                showConvertExpenseDialog = false
            }
        )
    }
}

@Composable
fun SmartSuggestionCard(
    suggestion: SmartShoppingSuggestion,
    onAddNormal: () -> Unit,
    onAddUrgent: () -> Unit
) {
    Card(
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = suggestion.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (suggestion.isUrgentRecommendation) {
                    Surface(
                        color = Color(0xFFFFE4E6),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "⚡ فوری",
                            color = Color(0xFFBE123C),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = suggestion.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                fontSize = 10.sp,
                maxLines = 2,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onAddNormal,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ افزودن", fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = onAddUrgent,
                    modifier = Modifier.weight(1.1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFFFE4E6),
                        contentColor = Color(0xFFBE123C)
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("⚡ خرید فوری", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ShoppingListItemRow(
    item: ShoppingListItem,
    onTogglePurchased: () -> Unit,
    onToggleUrgent: () -> Unit,
    onAssignClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isUrgent = item.isUrgent && !item.isPurchased

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                item.isPurchased -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isUrgent -> Color(0xFFFFF1F2)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isUrgent) BorderStroke(1.5.dp, Color(0xFFFDA4AF)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isPurchased) 0.dp else 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isPurchased,
                onCheckedChange = { onTogglePurchased() }
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (item.isPurchased) FontWeight.Normal else FontWeight.SemiBold,
                        textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None,
                        color = when {
                            item.isPurchased -> MaterialTheme.colorScheme.outline
                            isUrgent -> Color(0xFF9F1239)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Urgent Toggle Pill (Interactive 1-tap toggle!)
                    if (!item.isPurchased) {
                        Surface(
                            color = if (item.isUrgent) Color(0xFFBE123C) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onToggleUrgent() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = "تغییر اولویت فوری",
                                    tint = if (item.isUrgent) Color.White else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (item.isUrgent) "نیاز فوری" else "عادی",
                                    color = if (item.isUrgent) Color.White else MaterialTheme.colorScheme.outline,
                                    fontSize = 10.sp,
                                    fontWeight = if (item.isUrgent) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${item.quantity} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Assignment pill
                    Surface(
                        color = if (item.assignedToMemberName != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onAssignClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (item.assignedToMemberName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = item.assignedToMemberName?.let { "مسئول: $it" } ?: "تعیین مسئول",
                                fontSize = 10.sp,
                                color = if (item.assignedToMemberName != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShoppingItemDialog(
    members: List<Member>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        qty: Double,
        unit: String,
        cat: String,
        buyer: Member,
        assigned: Member?,
        urgent: Boolean,
        barcode: String,
        estPrice: Long
    ) -> Unit,
    onScanBarcode: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1.0") }
    var unit by remember { mutableStateOf("عدد") }
    var category by remember { mutableStateOf("خواروبار") }
    var isUrgent by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf(members.firstOrNull()) }
    var assignedMember by remember { mutableStateOf<Member?>(null) }
    var estPriceText by remember { mutableStateOf("") }

    val categories = listOf("لبنیات", "میوه و سبزیجات", "پروتئین", "نان و غلات", "خواروبار", "تنقلات", "نوشیدنی")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن به لیست خرید مشترک") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("نام قلم کالا *") },
                        singleLine = true
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("مقدار") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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
                    Text("دسته‌بندی:", style = MaterialTheme.typography.labelSmall)
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
                    Text("مسئول خرید (برای پیشگیری از خرید تکراری):", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = assignedMember == null,
                                onClick = { assignedMember = null },
                                label = { Text("تعیین نشده (هرکس)", fontSize = 11.sp) }
                            )
                        }
                        items(members) { m ->
                            FilterChip(
                                selected = assignedMember?.id == m.id,
                                onClick = { assignedMember = m },
                                label = { Text(m.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Urgently Needed Toggle Switch
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUrgent) Color(0xFFFFE4E6) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
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
                                    Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = if (isUrgent) Color(0xFFBE123C) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "علامت‌گذاری به عنوان «نیاز فوری»",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isUrgent) Color(0xFFBE123C) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "اولویت بالا برای خرید سریع امروز",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUrgent) Color(0xFF9F1239) else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Switch(
                                checked = isUrgent,
                                onCheckedChange = { isUrgent = it }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val buyer = selectedMember ?: return@Button
                    val qty = quantityText.toDoubleOrNull() ?: 1.0
                    val est = estPriceText.toLongOrNull() ?: 0L
                    onSave(
                        name.trim(),
                        qty,
                        unit.trim(),
                        category,
                        buyer,
                        assignedMember,
                        isUrgent,
                        barcode.trim(),
                        est
                    )
                }
            ) {
                Text("افزودن")
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
fun AssignMemberDialog(
    item: ShoppingListItem,
    members: List<Member>,
    onDismiss: () -> Unit,
    onAssign: (Member?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعیین مسئول خرید «${item.name}»") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("چه کسی این مورد را خریداری می‌کند تا بقیه دوباره نخرند؟")
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { onAssign(null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("آزاد برای همه (هیچ‌کس انتخاب نشده)")
                }
                members.forEach { m ->
                    Button(
                        onClick = { onAssign(m) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(m.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertShoppingToExpenseDialog(
    items: List<ShoppingListItem>,
    members: List<Member>,
    onDismiss: () -> Unit,
    onConfirm: (buyer: Member, beneficiaries: List<Member>, amount: Long) -> Unit
) {
    var selectedBuyer by remember { mutableStateOf(members.firstOrNull()) }
    var selectedBeneficiaries by remember { mutableStateOf(members.toSet()) }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت خرید اقلام و ورود به انبار") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${items.size} قلم خریداری شده به انبار/یخچال اضافه و هزینه‌اش تقسیم خواهد شد:",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = items.joinToString("، ") { "${it.name} (${it.quantity} ${it.unit})" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مبلغ کل پرداخت شده (تومان) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("چه کسی پرداخت کرده؟", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(members) { m ->
                        FilterChip(
                            selected = selectedBuyer?.id == m.id,
                            onClick = { selectedBuyer = m },
                            label = { Text(m.name) }
                        )
                    }
                }

                Text("تقسیم بین چه کسانی؟", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(members) { m ->
                        FilterChip(
                            selected = selectedBeneficiaries.contains(m),
                            onClick = {
                                selectedBeneficiaries = if (selectedBeneficiaries.contains(m)) {
                                    if (selectedBeneficiaries.size > 1) selectedBeneficiaries - m else selectedBeneficiaries
                                } else {
                                    selectedBeneficiaries + m
                                }
                            },
                            label = { Text(m.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    val buyer = selectedBuyer ?: return@Button
                    if (amount <= 0L) return@Button
                    onConfirm(buyer, selectedBeneficiaries.toList(), amount)
                }
            ) {
                Text("تأیید و ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
