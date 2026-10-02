package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class FoodShareRepository(
    private val memberDao: MemberDao,
    private val foodItemDao: FoodItemDao,
    private val purchaseDao: PurchaseDao,
    private val consumptionDao: ConsumptionDao,
    private val shoppingListDao: ShoppingListDao,
    private val settlementDao: SettlementDao,
    private val chatDao: ChatDao
) {
    // Members
    val allMembers: Flow<List<Member>> = memberDao.getAllMembers()
    suspend fun insertMember(member: Member) = memberDao.insertMember(member)
    suspend fun updateMember(member: Member) = memberDao.updateMember(member)
    suspend fun deleteMember(member: Member) = memberDao.deleteMember(member)
    suspend fun getMemberById(id: Long) = memberDao.getMemberById(id)

    // Food Items
    val allFoodItems: Flow<List<FoodItem>> = foodItemDao.getAllFoodItems()
    suspend fun insertFoodItem(item: FoodItem) = foodItemDao.insertFoodItem(item)
    suspend fun updateFoodItem(item: FoodItem) = foodItemDao.updateFoodItem(item)
    suspend fun deleteFoodItem(item: FoodItem) = foodItemDao.deleteFoodItem(item)
    suspend fun updateFoodQuantity(id: Long, quantity: Double) = foodItemDao.updateQuantity(id, quantity)
    suspend fun getFoodItemByBarcode(barcode: String) = foodItemDao.getFoodItemByBarcode(barcode)
    suspend fun getFoodItemById(id: Long) = foodItemDao.getFoodItemById(id)

    // Purchases
    val allPurchases: Flow<List<PurchaseRecord>> = purchaseDao.getAllPurchases()
    suspend fun insertPurchase(purchase: PurchaseRecord) = purchaseDao.insertPurchase(purchase)
    suspend fun deletePurchase(purchase: PurchaseRecord) = purchaseDao.deletePurchase(purchase)

    // Consumption
    val allConsumptionLogs: Flow<List<ConsumptionLog>> = consumptionDao.getAllConsumptionLogs()
    suspend fun insertConsumptionLog(log: ConsumptionLog) = consumptionDao.insertConsumptionLog(log)
    suspend fun deleteConsumptionLog(log: ConsumptionLog) = consumptionDao.deleteConsumptionLog(log)

    // Shopping List
    val allShoppingItems: Flow<List<ShoppingListItem>> = shoppingListDao.getAllShoppingItems()
    suspend fun insertShoppingItem(item: ShoppingListItem) = shoppingListDao.insertShoppingItem(item)
    suspend fun updateShoppingItem(item: ShoppingListItem) = shoppingListDao.updateShoppingItem(item)
    suspend fun deleteShoppingItem(item: ShoppingListItem) = shoppingListDao.deleteShoppingItem(item)
    suspend fun updateShoppingPurchased(id: Long, isPurchased: Boolean) =
        shoppingListDao.updatePurchasedState(id, isPurchased)
    suspend fun updateShoppingUrgent(id: Long, isUrgent: Boolean) =
        shoppingListDao.updateUrgentState(id, isUrgent)
    suspend fun assignShoppingMember(id: Long, memberId: Long?, memberName: String?) =
        shoppingListDao.assignMember(id, memberId, memberName)
    suspend fun clearCompletedShoppingItems() = shoppingListDao.clearCompletedItems()

    // Settlements
    val allSettlements: Flow<List<SettlementRecord>> = settlementDao.getAllSettlements()
    suspend fun insertSettlement(settlement: SettlementRecord) = settlementDao.insertSettlement(settlement)
    suspend fun deleteSettlement(settlement: SettlementRecord) = settlementDao.deleteSettlement(settlement)

    // Housemate Chat
    val allChatMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()
    suspend fun insertChatMessage(message: ChatMessage) = chatDao.insertMessage(message)
    suspend fun deleteChatMessage(message: ChatMessage) = chatDao.deleteMessage(message)
    suspend fun clearChatMessages() = chatDao.clearAllMessages()
}
