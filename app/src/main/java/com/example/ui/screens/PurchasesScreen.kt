package com.example.ui.screens

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Member
import com.example.data.model.PurchaseRecord
import com.example.ui.MainViewModel
import com.example.ui.MemberBalanceSummary
import com.example.ui.components.DongAnalyticsView
import com.example.ui.components.MemberAvatar
import java.text.SimpleDateFormat
import java.util.*

data class ItemizedBillItem(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val name: String,
    val price: Long,
    val participantIds: Set<Long>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: MainViewModel,
    purchases: List<PurchaseRecord>,
    members: List<Member>,
    balances: List<MemberBalanceSummary> = emptyList()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: DONG History & Records, 1: Interactive DONG Calculator, 2: Analytics & Trends
    var showQuickRecordDialog by remember { mutableStateOf(false) }

    val totalSpent = remember(purchases) {
        purchases.sumOf { it.amount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Module Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("فاکتورها", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("محاسبه‌گر دُنگ", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("نمودار و روند", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedTab) {
            0 -> {
                // Purchases & DONG History
                Column(modifier = Modifier.fillMaxSize()) {
                // Total Spend & Dong Summary Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "مجموع خریدهای دُنگی این ماه",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${viewModel.formatNumber(totalSpent)} تومان",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (members.isNotEmpty()) {
                                Text(
                                    text = "میانگین سهم هر فرد: ${viewModel.formatNumber(totalSpent / members.size)} تومان",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Button(
                            onClick = { selectedTab = 1 },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("دُنگ جدید", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "لیست فاکتورها و سهم دُنگ (${purchases.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (purchases.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "هنوز خرید دُنگی ثبت نشده است!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "از برگه «محاسبه‌گر دُنگ» می‌توانید فاکتور خرید را تفکیک و ثبت کنید.",
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
                        items(purchases, key = { it.id }) { purchase ->
                            DongPurchaseCard(
                                purchase = purchase,
                                members = members,
                                onDelete = { viewModel.deletePurchase(purchase) },
                                onShare = { viewModel.sharePurchaseDongSummary(context, purchase) },
                                formatNumber = viewModel::formatNumber
                            )
                        }
                    }
                }
            }
            }
            1 -> {
                // Interactive DONG Calculator Screen
                DongCalculatorView(
                    members = members,
                    onSaveDong = { title, amount, buyer, beneficiaries, addToPantry, cat, loc, shelf, unit, qty, notes, mode, shares ->
                        viewModel.recordPurchase(
                            title = title,
                            amount = amount,
                            paidBy = buyer,
                            beneficiaries = beneficiaries,
                            addToPantry = addToPantry,
                            pantryCategory = cat,
                            pantryLocation = loc,
                            shelfLifeDays = shelf,
                            unit = unit,
                            quantity = qty,
                            notes = notes,
                            splitMode = mode,
                            customShares = shares
                        )
                        selectedTab = 0
                    },
                    formatNumber = viewModel::formatNumber
                )
            }
            else -> {
                // Visual summary and monthly spending patterns
                DongAnalyticsView(
                    purchases = purchases,
                    members = members,
                    balances = balances,
                    formatNumber = viewModel::formatNumber
                )
            }
        }
    }
}

@Composable
fun DongPurchaseCard(
    purchase: PurchaseRecord,
    members: List<Member>,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    formatNumber: (Long) -> String
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val memberShares = remember(purchase, members) {
        purchase.getMemberShares(members)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = purchase.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatNumber(purchase.amount)} تومان",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subheader: Buyer & Date & Split Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "پرداخت‌کننده: ${purchase.paidByMemberName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = dateFormat.format(Date(purchase.dateMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // DONG Breakdown Chips
            Text(
                text = "سهم دُنگ هر یک از اعضا:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(memberShares.entries.toList()) { (memberId, shareAmount) ->
                    val member = members.find { it.id == memberId }
                    val isBuyer = memberId == purchase.paidByMemberId

                    Surface(
                        color = if (isBuyer) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = member?.name ?: "عضو",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${formatNumber(shareAmount)} ت",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBuyer) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onShare,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ارسال گزارش دُنگ", fontSize = 12.sp)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "حذف فاکتور",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

// ----------------- Interactive DONG Calculator -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DongCalculatorView(
    members: List<Member>,
    onSaveDong: (
        title: String,
        amount: Long,
        buyer: Member,
        beneficiaries: List<Member>,
        addToPantry: Boolean,
        category: String,
        location: String,
        shelfDays: Int,
        unit: String,
        quantity: Double,
        notes: String,
        splitMode: String,
        customShares: Map<Long, Long>
    ) -> Unit,
    formatNumber: (Long) -> String
) {
    var title by remember { mutableStateOf("") }
    var totalBillText by remember { mutableStateOf("") }
    var selectedBuyer by remember { mutableStateOf(members.firstOrNull()) }
    var splitMode by remember { mutableStateOf("EQUAL") } // EQUAL, SHARES, EXACT, ITEMIZED

    // Equal mode state
    var selectedBeneficiaries by remember { mutableStateOf(members.toSet()) }

    // Weighted/Shares mode state (e.g. Ali: 1 share, Sara: 1 share, Reza: 2 shares)
    var memberWeights by remember {
        mutableStateOf(members.associate { it.id to 1 })
    }

    // Exact amounts mode state
    var memberExactAmounts by remember {
        mutableStateOf(members.associate { it.id to "" })
    }

    // Itemized receipt mode state
    var itemizedList by remember { mutableStateOf(listOf<ItemizedBillItem>()) }
    var newItemName by remember { mutableStateOf("") }
    var newItemPriceText by remember { mutableStateOf("") }
    var newItemParticipants by remember { mutableStateOf(members.map { it.id }.toSet()) }

    // Pantry integration
    var addToPantry by remember { mutableStateOf(true) }
    var pantryCategory by remember { mutableStateOf("خواروبار") }
    var pantryLocation by remember { mutableStateOf("FRIDGE") }

    val categories = listOf("لبنیات", "میوه و سبزیجات", "پروتئین", "نان و غلات", "خواروبار", "تنقلات", "نوشیدنی")

    // Dynamic Calculated Shares Map
    val calculatedShares = remember(
        splitMode,
        totalBillText,
        selectedBeneficiaries,
        memberWeights,
        memberExactAmounts,
        itemizedList,
        members
    ) {
        val total = totalBillText.toLongOrNull() ?: 0L
        val map = mutableMapOf<Long, Long>()

        when (splitMode) {
            "EQUAL" -> {
                val count = selectedBeneficiaries.size
                if (count > 0 && total > 0) {
                    val share = total / count
                    selectedBeneficiaries.forEach { map[it.id] = share }
                }
            }
            "SHARES" -> {
                val totalWeight = memberWeights.values.sum()
                if (totalWeight > 0 && total > 0) {
                    members.forEach { m ->
                        val w = memberWeights[m.id] ?: 1
                        map[m.id] = (total * w) / totalWeight
                    }
                }
            }
            "EXACT" -> {
                members.forEach { m ->
                    val amt = memberExactAmounts[m.id]?.toLongOrNull() ?: 0L
                    map[m.id] = amt
                }
            }
            "ITEMIZED" -> {
                // Sum per member from all items
                members.forEach { map[it.id] = 0L }
                for (item in itemizedList) {
                    if (item.participantIds.isNotEmpty()) {
                        val perPerson = item.price / item.participantIds.size
                        for (pId in item.participantIds) {
                            map[pId] = (map[pId] ?: 0L) + perPerson
                        }
                    }
                }
            }
        }
        map
    }

    val computedTotalAmount = remember(splitMode, totalBillText, itemizedList, calculatedShares) {
        if (splitMode == "ITEMIZED") {
            itemizedList.sumOf { it.price }
        } else if (splitMode == "EXACT") {
            calculatedShares.values.sum()
        } else {
            totalBillText.toLongOrNull() ?: 0L
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "محاسبه دقیق دُنگ خرید بر اساس نوع تقسیم دلخواه و ثبت خودکار در حساب‌ها",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان فاکتور خرید (مثلاً میوه‌فروشی، خریدهای هفتگی)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (splitMode != "ITEMIZED") {
            item {
                OutlinedTextField(
                    value = totalBillText,
                    onValueChange = { totalBillText = it },
                    label = { Text("مبلغ کل فاکتور (تومان)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Who paid?
        item {
            Text("پرداخت‌کننده کل فاکتور:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(members) { m ->
                    FilterChip(
                        selected = selectedBuyer?.id == m.id,
                        onClick = { selectedBuyer = m },
                        label = { Text(m.name) },
                        leadingIcon = { MemberAvatar(member = m, size = 20) }
                    )
                }
            }
        }

        // Split Mode Selector
        item {
            Text("نحوه تقسیم دُنگ (روش محاسبه):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = splitMode == "EQUAL",
                    onClick = { splitMode = "EQUAL" },
                    label = { Text("مساوی", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = splitMode == "SHARES",
                    onClick = { splitMode = "SHARES" },
                    label = { Text("ضریبی/سهمی", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = splitMode == "EXACT",
                    onClick = { splitMode = "EXACT" },
                    label = { Text("مبالغ مشخص", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = splitMode == "ITEMIZED",
                    onClick = { splitMode = "ITEMIZED" },
                    label = { Text("قلم‌به‌قلم", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Configuration according to splitMode
        when (splitMode) {
            "EQUAL" -> {
                item {
                    Text("چه کسانی در این دُنگ شریک هستند؟", style = MaterialTheme.typography.labelMedium)
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
            }

            "SHARES" -> {
                item {
                    Text("تعیین نسبت / ضریب سهم هر فرد (مثلاً ۱ یا ۲ سهم):", style = MaterialTheme.typography.labelMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        members.forEach { m ->
                            val currentWeight = memberWeights[m.id] ?: 1
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(m.name, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (currentWeight > 1) {
                                                    memberWeights = memberWeights + (m.id to (currentWeight - 1))
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "کمتر")
                                        }
                                        Text("$currentWeight سهم", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                        IconButton(
                                            onClick = {
                                                memberWeights = memberWeights + (m.id to (currentWeight + 1))
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "بیشتر")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "EXACT" -> {
                item {
                    Text("مبلغ دقیق سهم هر فرد را وارد کنید (تومان):", style = MaterialTheme.typography.labelMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        members.forEach { m ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(m.name, modifier = Modifier.width(70.dp), fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = memberExactAmounts[m.id] ?: "",
                                    onValueChange = { newVal ->
                                        memberExactAmounts = memberExactAmounts + (m.id to newVal)
                                    },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("مبلغ به تومان") },
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }

            "ITEMIZED" -> {
                item {
                    Text("اقلام فاکتور و شرکای هر قلم:", style = MaterialTheme.typography.labelMedium)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = newItemName,
                                    onValueChange = { newItemName = it },
                                    label = { Text("نام کالا") },
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedTextField(
                                    value = newItemPriceText,
                                    onValueChange = { newItemPriceText = it },
                                    label = { Text("قیمت (ت)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Text("شرکای این قلم:", style = MaterialTheme.typography.labelSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(members) { m ->
                                    FilterChip(
                                        selected = newItemParticipants.contains(m.id),
                                        onClick = {
                                            newItemParticipants = if (newItemParticipants.contains(m.id)) {
                                                if (newItemParticipants.size > 1) newItemParticipants - m.id else newItemParticipants
                                            } else {
                                                newItemParticipants + m.id
                                            }
                                        },
                                        label = { Text(m.name, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val price = newItemPriceText.toLongOrNull() ?: 0L
                                    if (newItemName.isNotBlank() && price > 0L) {
                                        itemizedList = itemizedList + ItemizedBillItem(
                                            name = newItemName.trim(),
                                            price = price,
                                            participantIds = newItemParticipants
                                        )
                                        newItemName = ""
                                        newItemPriceText = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("افزودن این قلم به فاکتور")
                            }
                        }
                    }

                    // Itemized list display
                    if (itemizedList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            itemizedList.forEachIndexed { idx, itm ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("${idx + 1}. ${itm.name} - ${formatNumber(itm.price)} ت", fontWeight = FontWeight.Bold)
                                            val partNames = itm.participantIds.mapNotNull { pId -> members.find { it.id == pId }?.name }
                                            Text("مصرف‌کنندگان: ${partNames.joinToString("، ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                        IconButton(onClick = { itemizedList = itemizedList - itm }) {
                                            Icon(Icons.Default.Close, contentDescription = "حذف قلم", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live DONG Result Preview Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📊 پیش‌نمایش زنده محاسبه دُنگ:",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "مجموع: ${formatNumber(computedTotalAmount)} تومان",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    calculatedShares.forEach { (memberId, shareAmount) ->
                        val member = members.find { it.id == memberId }
                        val isBuyer = memberId == selectedBuyer?.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(member?.name ?: "عضو", fontWeight = FontWeight.SemiBold)
                                if (isBuyer) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("(پرداخت‌کننده کل)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(
                                text = "${formatNumber(shareAmount)} تومان",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Add to pantry checkbox
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = addToPantry,
                    onCheckedChange = { addToPantry = it }
                )
                Text("افزودن این خرید به موجودی یخچال / انبار خوراکی‌ها")
            }
        }

        if (addToPantry) {
            item {
                Text("محل نگهداری و دسته‌بندی در انبار:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = pantryLocation == "FRIDGE",
                        onClick = { pantryLocation = "FRIDGE" },
                        label = { Text("یخچال") }
                    )
                    FilterChip(
                        selected = pantryLocation == "FREEZER",
                        onClick = { pantryLocation = "FREEZER" },
                        label = { Text("فریزر") }
                    )
                    FilterChip(
                        selected = pantryLocation == "PANTRY",
                        onClick = { pantryLocation = "PANTRY" },
                        label = { Text("کابینت") }
                    )
                }
            }
        }

        // Confirm & Record Button
        item {
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val buyer = selectedBuyer ?: return@Button
                    if (computedTotalAmount <= 0L) return@Button

                    val beneficiaries = calculatedShares.keys.mapNotNull { mId -> members.find { it.id == mId } }

                    onSaveDong(
                        title.trim(),
                        computedTotalAmount,
                        buyer,
                        beneficiaries,
                        addToPantry,
                        pantryCategory,
                        pantryLocation,
                        7,
                        "عدد",
                        1.0,
                        "محاسبه دُنگ با روش $splitMode",
                        splitMode,
                        calculatedShares
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.DoneAll, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ثبت فاکتور و اعمال دُنگ در حساب‌ها", fontWeight = FontWeight.Bold)
            }
        }
    }
}
