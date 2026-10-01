package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HamSofrehApplication
import com.example.data.barcode.BarcodeCatalog
import com.example.data.barcode.BarcodeProductInfo
import com.example.data.model.*
import com.example.data.repository.FoodShareRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DebtTransfer(
    val fromMemberId: Long,
    val fromMemberName: String,
    val toMemberId: Long,
    val toMemberName: String,
    val amount: Long
)

data class MemberBalanceSummary(
    val member: Member,
    val totalPaid: Long,
    val totalShare: Long,
    val netBalance: Long // positive = creditor (طلبکار), negative = debtor (بدهکار)
)

data class UiMessage(
    val message: String,
    val isError: Boolean = false,
    val id: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HamSofrehApplication
    private val repository = FoodShareRepository(
        app.database.memberDao(),
        app.database.foodItemDao(),
        app.database.purchaseDao(),
        app.database.consumptionDao(),
        app.database.shoppingListDao(),
        app.database.settlementDao()
    )
    private val notificationManager = app.notificationManager
    private val recipeService = com.example.data.ai.GeminiRecipeService()

    // UI state flows
    val members: StateFlow<List<Member>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foodItems: StateFlow<List<FoodItem>> = repository.allFoodItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<PurchaseRecord>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val consumptionLogs: StateFlow<List<ConsumptionLog>> = repository.allConsumptionLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shoppingItems: StateFlow<List<ShoppingListItem>> = repository.allShoppingItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settlements: StateFlow<List<SettlementRecord>> = repository.allSettlements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _recipes = MutableStateFlow<List<com.example.data.ai.Recipe>>(emptyList())
    val recipes: StateFlow<List<com.example.data.ai.Recipe>> = _recipes.asStateFlow()

    private val _isLoadingRecipes = MutableStateFlow(false)
    val isLoadingRecipes: StateFlow<Boolean> = _isLoadingRecipes.asStateFlow()

    private val _uiMessage = MutableStateFlow<UiMessage?>(null)
    val uiMessage: StateFlow<UiMessage?> = _uiMessage.asStateFlow()

    // Calculated Balances and Debts
    val memberBalances: StateFlow<List<MemberBalanceSummary>> = combine(
        members,
        purchases,
        settlements
    ) { memberList, purchaseList, settlementList ->
        computeMemberBalances(memberList, purchaseList, settlementList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debtTransfers: StateFlow<List<DebtTransfer>> = memberBalances.map { balances ->
        calculateDebtSettlement(balances)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expiring items count
    val expiringItemsCount: StateFlow<Int> = foodItems.map { items ->
        val now = System.currentTimeMillis()
        items.count { it.daysUntilExpiry(now) in 0..3 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val expiredItemsCount: StateFlow<Int> = foodItems.map { items ->
        val now = System.currentTimeMillis()
        items.count { it.isExpired(now) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val smartShoppingSuggestions: StateFlow<List<com.example.data.model.SmartShoppingSuggestion>> = combine(
        consumptionLogs,
        foodItems,
        shoppingItems
    ) { logs, inventory, shopping ->
        computeSmartShoppingSuggestions(logs, inventory, shopping)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun showMessage(message: String, isError: Boolean = false) {
        _uiMessage.value = UiMessage(message, isError)
    }

    // --- Pantry / Food Items Operations ---
    fun addOrUpdateFoodItem(item: FoodItem) {
        viewModelScope.launch {
            if (item.id == 0L) {
                repository.insertFoodItem(item)
                showMessage("خوراکی «${item.name}» با موفقیت به انبار افزوده شد.")
            } else {
                repository.updateFoodItem(item)
                showMessage("خوراکی «${item.name}» به‌روزرسانی شد.")
            }
        }
    }

    fun deleteFoodItem(item: FoodItem) {
        viewModelScope.launch {
            repository.deleteFoodItem(item)
            showMessage("«${item.name}» از لیست حذف شد.")
        }
    }

    fun consumeFood(item: FoodItem, consumedQuantity: Double, member: Member, note: String = "") {
        viewModelScope.launch {
            val newQuantity = (item.quantity - consumedQuantity).coerceAtLeast(0.0)
            repository.updateFoodQuantity(item.id, newQuantity)

            repository.insertConsumptionLog(
                ConsumptionLog(
                    foodItemId = item.id,
                    foodItemName = item.name,
                    consumedByMemberId = member.id,
                    consumedByMemberName = member.name,
                    quantity = consumedQuantity,
                    unit = item.unit,
                    notes = note
                )
            )

            if (newQuantity <= 0.0) {
                showMessage("«${item.name}» تمام شد و مصرف آن به نام ${member.name} ثبت گردید.")
            } else {
                showMessage("مقدار $consumedQuantity ${item.unit} از «${item.name}» توسط ${member.name} مصرف شد.")
            }
        }
    }

    // --- Barcode Scanning & Auto-fill ---
    fun lookupBarcode(barcode: String): BarcodeProductInfo {
        return BarcodeCatalog.lookup(barcode)
    }

    // Check duplicate in pantry or shopping list
    fun checkForDuplicates(name: String, barcode: String): String? {
        val trimmedName = name.trim().lowercase()
        val inPantry = foodItems.value.firstOrNull {
            it.name.trim().lowercase() == trimmedName || (barcode.isNotBlank() && it.barcode == barcode)
        }
        if (inPantry != null && inPantry.quantity > 0) {
            return "این کالا در حال حاضر در انبار موجود است (${inPantry.quantity} ${inPantry.unit} در ${getLocationTitle(inPantry.location)})."
        }

        val inShopping = shoppingItems.value.firstOrNull {
            !it.isPurchased && (it.name.trim().lowercase() == trimmedName || (barcode.isNotBlank() && it.barcode == barcode))
        }
        if (inShopping != null) {
            return "این کالا قبلاً در لیست خرید توسط ${inShopping.addedByMemberName} اضافه شده است."
        }

        return null
    }

    // --- Purchases & Expense Splitting ---
    fun recordPurchase(
        title: String,
        amount: Long,
        paidBy: Member,
        beneficiaries: List<Member>,
        addToPantry: Boolean,
        pantryCategory: String = "خواروبار",
        pantryLocation: String = "FRIDGE",
        shelfLifeDays: Int = 7,
        unit: String = "عدد",
        quantity: Double = 1.0,
        notes: String = "",
        splitMode: String = "EQUAL",
        customShares: Map<Long, Long> = emptyMap()
    ) {
        viewModelScope.launch {
            val beneficiaryIds = beneficiaries.map { it.id }.joinToString(",")
            val customSharesData = if (customShares.isNotEmpty()) {
                customShares.entries.joinToString(",") { "${it.key}:${it.value}" }
            } else ""

            val purchase = PurchaseRecord(
                title = title,
                amount = amount,
                paidByMemberId = paidBy.id,
                paidByMemberName = paidBy.name,
                beneficiaryMemberIds = beneficiaryIds,
                itemsSummary = title,
                notes = notes,
                splitMode = splitMode,
                customSharesData = customSharesData
            )
            repository.insertPurchase(purchase)

            if (addToPantry) {
                val now = System.currentTimeMillis()
                val foodItem = FoodItem(
                    name = title,
                    category = pantryCategory,
                    quantity = quantity,
                    unit = unit,
                    location = pantryLocation,
                    purchaseDateMillis = now,
                    expiryDateMillis = now + (shelfLifeDays.toLong() * 24 * 60 * 60 * 1000),
                    price = amount,
                    purchasedByMemberId = paidBy.id,
                    notes = notes
                )
                repository.insertFoodItem(foodItem)
            }

            val shareEach = if (beneficiaries.isNotEmpty()) amount / beneficiaries.size else amount
            showMessage("دُنگ خرید «$title» ثبت شد. مبلغ: ${formatNumber(amount)} تومان.")
        }
    }

    fun sharePurchaseDongSummary(context: Context, purchase: PurchaseRecord) {
        val allMem = members.value
        val shares = purchase.getMemberShares(allMem)
        val sb = StringBuilder()
        sb.append("🧾 فاکتور و تقسیم دُنگ هم‌سفره\n\n")
        sb.append("🛒 عنوان خرید: ${purchase.title}\n")
        sb.append("💰 مبلغ کل: ${formatNumber(purchase.amount)} تومان\n")
        sb.append("💳 پرداخت‌کننده: ${purchase.paidByMemberName}\n\n")
        sb.append("👥 سهم دُنگ هر یک از اعضا:\n")
        for ((mId, shareAmount) in shares) {
            val m = allMem.find { it.id == mId }
            val name = m?.name ?: "عضو $mId"
            val status = if (mId == purchase.paidByMemberId) " (پرداخت شده توسط خودش)" else " (بدهکار به ${purchase.paidByMemberName})"
            sb.append("• $name: ${formatNumber(shareAmount)} تومان$status\n")
        }
        if (purchase.notes.isNotBlank()) {
            sb.append("\n📝 یادداشت: ${purchase.notes}\n")
        }
        sb.append("\nمحاسبه شده با اپلیکیشن هم‌سفره")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        val chooser = Intent.createChooser(sendIntent, "ارسال جزئیات دُنگ به هم‌خانه‌ها")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    }

    fun deletePurchase(purchase: PurchaseRecord) {
        viewModelScope.launch {
            repository.deletePurchase(purchase)
            showMessage("سند خرید حذف شد.")
        }
    }

    // --- Shopping List Operations ---
    fun addShoppingItem(
        name: String,
        quantity: Double,
        unit: String,
        category: String,
        addedBy: Member,
        assignedTo: Member?,
        isUrgent: Boolean,
        barcode: String = "",
        estimatedPrice: Long = 0L
    ) {
        viewModelScope.launch {
            val item = ShoppingListItem(
                name = name,
                barcode = barcode,
                quantity = quantity,
                unit = unit,
                category = category,
                addedByMemberId = addedBy.id,
                addedByMemberName = addedBy.name,
                assignedToMemberId = assignedTo?.id,
                assignedToMemberName = assignedTo?.name,
                isUrgent = isUrgent,
                estimatedPrice = estimatedPrice
            )
            repository.insertShoppingItem(item)
            showMessage("«$name» به لیست خرید مشترک اضافه شد.")
        }
    }

    fun toggleShoppingPurchased(item: ShoppingListItem) {
        viewModelScope.launch {
            val newState = !item.isPurchased
            repository.updateShoppingPurchased(item.id, newState)
        }
    }

    fun assignShoppingItem(item: ShoppingListItem, member: Member?) {
        viewModelScope.launch {
            repository.assignShoppingMember(item.id, member?.id, member?.name)
            val msg = if (member != null) {
                "خرید «${item.name}» به ${member.name} محول شد."
            } else {
                "خرید «${item.name}» آزاد شد."
            }
            showMessage(msg)
        }
    }

    fun deleteShoppingItem(item: ShoppingListItem) {
        viewModelScope.launch {
            repository.deleteShoppingItem(item)
        }
    }

    fun clearCompletedShopping() {
        viewModelScope.launch {
            repository.clearCompletedShoppingItems()
            showMessage("اقلام خریداری شده از لیست پاک شدند.")
        }
    }

    fun toggleShoppingUrgent(item: ShoppingListItem) {
        viewModelScope.launch {
            val newUrgent = !item.isUrgent
            repository.updateShoppingUrgent(item.id, newUrgent)
            val msg = if (newUrgent) {
                "«${item.name}» به عنوان نیاز فوری علامت‌گذاری شد ⚡"
            } else {
                "نشان نیاز فوری از «${item.name}» برداشته شد."
            }
            showMessage(msg)
        }
    }

    fun addSuggestedToShopping(suggestion: com.example.data.model.SmartShoppingSuggestion, isUrgent: Boolean) {
        viewModelScope.launch {
            val myMember = members.value.firstOrNull { it.isMe } ?: members.value.firstOrNull()
            addShoppingItem(
                name = suggestion.name,
                quantity = suggestion.suggestedQuantity,
                unit = suggestion.unit,
                category = suggestion.category,
                addedBy = myMember ?: Member(name = "کاربر"),
                assignedTo = null,
                isUrgent = isUrgent,
                barcode = "",
                estimatedPrice = 0L
            )
            showMessage("«${suggestion.name}» بر اساس الگوی مصرف به لیست خرید افزوده شد.")
        }
    }

    // Share shopping list with housemates via external app (WhatsApp, Telegram, SMS, etc.)
    fun shareShoppingList(context: Context) {
        val items = shoppingItems.value.filter { !it.isPurchased }
        if (items.isEmpty()) {
            showMessage("لیست خرید خالی است!", isError = true)
            return
        }

        val sb = StringBuilder()
        sb.append("🛒 لیست خرید مشترک هم‌سفره:\n\n")
        items.forEachIndexed { index, item ->
            val assigned = if (!item.assignedToMemberName.isNullOrBlank()) " [مسئول: ${item.assignedToMemberName}]" else ""
            val urgent = if (item.isUrgent) " ⚡(فوری)" else ""
            sb.append("${index + 1}. ${item.name} (${item.quantity} ${item.unit})$urgent$assigned\n")
        }
        sb.append("\nثبت شده در اپلیکیشن هم‌سفره")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "اشتراک‌گذاری لیست خرید با هم‌خانه‌ها")
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    }

    // Convert checked shopping items into a purchase + add to inventory
    fun convertPurchasedItemsToExpense(
        buyer: Member,
        beneficiaries: List<Member>,
        actualTotalAmount: Long
    ) {
        viewModelScope.launch {
            val purchased = shoppingItems.value.filter { it.isPurchased }
            if (purchased.isEmpty()) {
                showMessage("هیچ موردی تیک نخورده است!", isError = true)
                return@launch
            }

            val summary = purchased.joinToString("، ") { "${it.name} (${it.quantity} ${it.unit})" }
            val beneficiaryIds = beneficiaries.map { it.id }.joinToString(",")

            // 1. Record purchase
            val purchase = PurchaseRecord(
                title = "خرید اقلام لیست: " + purchased.take(2).joinToString(" و ") { it.name },
                amount = actualTotalAmount,
                paidByMemberId = buyer.id,
                paidByMemberName = buyer.name,
                beneficiaryMemberIds = beneficiaryIds,
                itemsSummary = summary
            )
            repository.insertPurchase(purchase)

            // 2. Add each item to food inventory
            val now = System.currentTimeMillis()
            purchased.forEach { sItem ->
                val shelfLife = when (sItem.category) {
                    "لبنیات" -> 7
                    "میوه و سبزیجات" -> 5
                    "پروتئین" -> 4
                    "نان و غلات" -> 5
                    else -> 45
                }
                val location = when (sItem.category) {
                    "لبنیات", "میوه و سبزیجات", "پروتئین" -> "FRIDGE"
                    else -> "PANTRY"
                }

                repository.insertFoodItem(
                    FoodItem(
                        name = sItem.name,
                        barcode = sItem.barcode,
                        category = sItem.category,
                        quantity = sItem.quantity,
                        unit = sItem.unit,
                        location = location,
                        purchaseDateMillis = now,
                        expiryDateMillis = now + (shelfLife.toLong() * 24 * 60 * 60 * 1000),
                        price = if (purchased.isNotEmpty()) actualTotalAmount / purchased.size else 0L,
                        purchasedByMemberId = buyer.id
                    )
                )
            }

            // 3. Clear completed shopping items
            repository.clearCompletedShoppingItems()

            showMessage("خرید و ورود اقلام به انبار با موفقیت ثبت شد.")
        }
    }

    // --- Settlement Operations ---
    fun recordSettlement(from: Member, to: Member, amount: Long, notes: String = "") {
        viewModelScope.launch {
            val record = SettlementRecord(
                fromMemberId = from.id,
                fromMemberName = from.name,
                toMemberId = to.id,
                toMemberName = to.name,
                amount = amount,
                notes = notes
            )
            repository.insertSettlement(record)
            showMessage("تسویه حساب به مبلغ ${formatNumber(amount)} تومان از ${from.name} به ${to.name} ثبت شد.")
        }
    }

    fun deleteSettlement(settlement: SettlementRecord) {
        viewModelScope.launch {
            repository.deleteSettlement(settlement)
            showMessage("سند تسویه حساب حذف شد.")
        }
    }

    // --- Members Operations ---
    fun addMember(name: String, colorHex: String, emoji: String) {
        viewModelScope.launch {
            repository.insertMember(
                Member(name = name, colorHex = colorHex, avatarEmoji = emoji)
            )
            showMessage("عضو جدید «$name» اضافه شد.")
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            if (members.value.size <= 1) {
                showMessage("نمی‌توانید آخرین عضو را حذف کنید!", isError = true)
                return@launch
            }
            repository.deleteMember(member)
            showMessage("عضو «${member.name}» حذف شد.")
        }
    }

    // --- Gemini AI Recipe Suggestions ---
    fun fetchRecipeSuggestions() {
        viewModelScope.launch {
            _isLoadingRecipes.value = true
            try {
                val suggestions = recipeService.getRecipeSuggestions(foodItems.value)
                _recipes.value = suggestions
                if (suggestions.isNotEmpty()) {
                    showMessage("دستورهای پخت جدید با اقلام موجود در انبار توسط جمینای آماده شد! 👨‍🍳")
                }
            } catch (e: Exception) {
                showMessage("خطا در دریافت دستور پخت: ${e.message}", isError = true)
            } finally {
                _isLoadingRecipes.value = false
            }
        }
    }

    fun markRecipeCooked(recipe: com.example.data.ai.Recipe, cookedBy: Member) {
        viewModelScope.launch {
            val allItems = foodItems.value
            val usedNames = recipe.expiringIngredientsUsed + recipe.otherIngredientsUsed
            var consumedCount = 0

            for (ingredientName in usedNames) {
                val matchingItem = allItems.firstOrNull {
                    it.name.contains(ingredientName, ignoreCase = true) ||
                            ingredientName.contains(it.name, ignoreCase = true)
                }
                if (matchingItem != null && matchingItem.quantity > 0) {
                    val deductQty = if (matchingItem.quantity >= 1.0) 1.0 else matchingItem.quantity
                    val newQty = (matchingItem.quantity - deductQty).coerceAtLeast(0.0)
                    repository.updateFoodQuantity(matchingItem.id, newQty)

                    repository.insertConsumptionLog(
                        ConsumptionLog(
                            foodItemId = matchingItem.id,
                            foodItemName = matchingItem.name,
                            consumedByMemberId = cookedBy.id,
                            consumedByMemberName = cookedBy.name,
                            quantity = deductQty,
                            unit = matchingItem.unit,
                            notes = "پخت «${recipe.title}»"
                        )
                    )
                    consumedCount++
                }
            }

            showMessage("غذای «${recipe.title}» با موفقیت پخته شد و اقلام آن به نام ${cookedBy.name} ثبت گردید. نوش جان! 🍲")
        }
    }

    // --- Expiry Notification Trigger ---
    fun triggerExpiryNotificationCheck(): Int {
        return notificationManager.checkAndNotifyExpiringItems(foodItems.value)
    }

    // --- Calculation Helpers ---
    private fun computeMemberBalances(
        memberList: List<Member>,
        purchaseList: List<PurchaseRecord>,
        settlementList: List<SettlementRecord>
    ): List<MemberBalanceSummary> {
        val memberMap = memberList.associateBy { it.id }
        val paidMap = mutableMapOf<Long, Long>()
        val shareMap = mutableMapOf<Long, Long>()

        memberList.forEach {
            paidMap[it.id] = 0L
            shareMap[it.id] = 0L
        }

        // 1. Purchases
        for (p in purchaseList) {
            paidMap[p.paidByMemberId] = (paidMap[p.paidByMemberId] ?: 0L) + p.amount
            val shares = p.getMemberShares(memberList)
            for ((bId, shareAmount) in shares) {
                shareMap[bId] = (shareMap[bId] ?: 0L) + shareAmount
            }
        }

        // 2. Direct Settlements
        for (s in settlementList) {
            // "From" paid "To"
            paidMap[s.fromMemberId] = (paidMap[s.fromMemberId] ?: 0L) + s.amount
            shareMap[s.toMemberId] = (shareMap[s.toMemberId] ?: 0L) + s.amount
        }

        return memberList.map { member ->
            val paid = paidMap[member.id] ?: 0L
            val share = shareMap[member.id] ?: 0L
            MemberBalanceSummary(
                member = member,
                totalPaid = paid,
                totalShare = share,
                netBalance = paid - share
            )
        }
    }

    private fun calculateDebtSettlement(balances: List<MemberBalanceSummary>): List<DebtTransfer> {
        val creditors = mutableListOf<Pair<Member, Long>>()
        val debtors = mutableListOf<Pair<Member, Long>>()

        for (b in balances) {
            if (b.netBalance > 100) {
                creditors.add(b.member to b.netBalance)
            } else if (b.netBalance < -100) {
                debtors.add(b.member to -b.netBalance)
            }
        }

        val result = mutableListOf<DebtTransfer>()
        var cIdx = 0
        var dIdx = 0

        while (cIdx < creditors.size && dIdx < debtors.size) {
            val (cMember, cAmount) = creditors[cIdx]
            val (dMember, dAmount) = debtors[dIdx]

            val settleAmount = minOf(cAmount, dAmount)
            if (settleAmount > 0) {
                result.add(
                    DebtTransfer(
                        fromMemberId = dMember.id,
                        fromMemberName = dMember.name,
                        toMemberId = cMember.id,
                        toMemberName = cMember.name,
                        amount = settleAmount
                    )
                )
            }

            if (cAmount > dAmount) {
                creditors[cIdx] = cMember to (cAmount - settleAmount)
                dIdx++
            } else if (dAmount > cAmount) {
                debtors[dIdx] = dMember to (dAmount - settleAmount)
                cIdx++
            } else {
                cIdx++
                dIdx++
            }
        }

        return result
    }

    private fun getLocationTitle(loc: String): String = when (loc) {
        "FRIDGE" -> "یخچال"
        "FREEZER" -> "فریزر"
        else -> "کابینت/انبار"
    }

    fun formatNumber(number: Long): String {
        return "%,d".format(number)
    }

    private fun computeSmartShoppingSuggestions(
        logs: List<ConsumptionLog>,
        inventory: List<FoodItem>,
        shopping: List<ShoppingListItem>
    ): List<com.example.data.model.SmartShoppingSuggestion> {
        val existingShoppingNames = shopping.filter { !it.isPurchased }
            .map { it.name.trim().lowercase() }
            .toSet()

        val suggestions = mutableListOf<com.example.data.model.SmartShoppingSuggestion>()

        // 1. Group consumption logs by food item name
        val frequencyMap = logs.groupBy { it.foodItemName.trim() }

        for ((foodName, itemLogs) in frequencyMap) {
            val lowerName = foodName.lowercase()
            if (existingShoppingNames.contains(lowerName)) continue

            // Current stock in pantry
            val matchingInventory = inventory.filter {
                it.name.contains(foodName, ignoreCase = true) ||
                        foodName.contains(it.name, ignoreCase = true)
            }
            val currentStock = matchingInventory.sumOf { it.quantity }
            val unit = itemLogs.firstOrNull()?.unit ?: matchingInventory.firstOrNull()?.unit ?: "عدد"
            val category = matchingInventory.firstOrNull()?.category ?: "خواروبار"
            val count = itemLogs.size

            // If stock is empty or very low and frequency >= 1
            if (currentStock <= 1.0) {
                val isUrgent = currentStock <= 0.0 && count >= 2
                val reason = when {
                    currentStock <= 0.0 -> "مصرف مکرر ($count بار) و اتمام موجودی در یخچال/انبار"
                    else -> "مصرف بالا ($count بار) و رو به اتمام ($currentStock $unit باقی‌مانده)"
                }
                suggestions.add(
                    com.example.data.model.SmartShoppingSuggestion(
                        name = foodName,
                        suggestedQuantity = if (unit == "گرم") 500.0 else 1.0,
                        unit = unit,
                        category = category,
                        reason = reason,
                        consumptionCount = count,
                        currentStock = currentStock,
                        isUrgentRecommendation = isUrgent
                    )
                )
            }
        }

        // 2. Also check staple household essentials that might not have logs yet or are out of stock
        val staples = listOf(
            Triple("شیر کم‌چرب", "لبنیات", "بطری"),
            Triple("تخم‌مرغ محلی", "پروتئین", "عدد"),
            Triple("نان تازه", "نان و غلات", "عدد"),
            Triple("پنیر صبحانه فتا", "لبنیات", "بسته")
        )

        for ((stapleName, cat, unit) in staples) {
            val lower = stapleName.lowercase()
            val matchKey = lower.take(3)
            if (existingShoppingNames.none { it.contains(matchKey) } && suggestions.none { it.name.contains(matchKey) }) {
                val stock = inventory.filter { it.name.contains(matchKey) }.sumOf { it.quantity }
                if (stock <= 1.0) {
                    suggestions.add(
                        com.example.data.model.SmartShoppingSuggestion(
                            name = stapleName,
                            suggestedQuantity = if (stapleName.contains("تخم")) 10.0 else 1.0,
                            unit = unit,
                            category = cat,
                            reason = "کالای اساسی روزمره خانوار (موجودی فعلی کم یا صفر)",
                            consumptionCount = 1,
                            currentStock = stock,
                            isUrgentRecommendation = stock == 0.0
                        )
                    )
                }
            }
        }

        return suggestions.sortedWith(
            compareByDescending<com.example.data.model.SmartShoppingSuggestion> { it.isUrgentRecommendation }
                .thenByDescending { it.consumptionCount }
        ).take(6)
    }
}
