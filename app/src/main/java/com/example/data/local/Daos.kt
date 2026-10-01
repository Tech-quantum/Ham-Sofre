package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY id ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Long): Member?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Update
    suspend fun updateMember(member: Member)

    @Delete
    suspend fun deleteMember(member: Member)

    @Query("SELECT COUNT(*) FROM members")
    suspend fun getCount(): Int
}

@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items ORDER BY expiryDateMillis ASC")
    fun getAllFoodItems(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE location = :location ORDER BY expiryDateMillis ASC")
    fun getFoodItemsByLocation(location: String): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getFoodItemByBarcode(barcode: String): FoodItem?

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getFoodItemById(id: Long): FoodItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(item: FoodItem): Long

    @Update
    suspend fun updateFoodItem(item: FoodItem)

    @Delete
    suspend fun deleteFoodItem(item: FoodItem)

    @Query("UPDATE food_items SET quantity = :newQuantity WHERE id = :id")
    suspend fun updateQuantity(id: Long, newQuantity: Double)

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun getCount(): Int
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchase_records ORDER BY dateMillis DESC")
    fun getAllPurchases(): Flow<List<PurchaseRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseRecord): Long

    @Delete
    suspend fun deletePurchase(purchase: PurchaseRecord)

    @Query("SELECT * FROM purchase_records WHERE id = :id")
    suspend fun getPurchaseById(id: Long): PurchaseRecord?
}

@Dao
interface ConsumptionDao {
    @Query("SELECT * FROM consumption_logs ORDER BY timestampMillis DESC")
    fun getAllConsumptionLogs(): Flow<List<ConsumptionLog>>

    @Query("SELECT * FROM consumption_logs WHERE foodItemId = :foodItemId ORDER BY timestampMillis DESC")
    fun getLogsByFoodItem(foodItemId: Long): Flow<List<ConsumptionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumptionLog(log: ConsumptionLog): Long

    @Delete
    suspend fun deleteConsumptionLog(log: ConsumptionLog)
}

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_list_items ORDER BY isPurchased ASC, isUrgent DESC, id DESC")
    fun getAllShoppingItems(): Flow<List<ShoppingListItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingListItem): Long

    @Update
    suspend fun updateShoppingItem(item: ShoppingListItem)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingListItem)

    @Query("UPDATE shopping_list_items SET isPurchased = :isPurchased WHERE id = :id")
    suspend fun updatePurchasedState(id: Long, isPurchased: Boolean)

    @Query("UPDATE shopping_list_items SET isUrgent = :isUrgent WHERE id = :id")
    suspend fun updateUrgentState(id: Long, isUrgent: Boolean)

    @Query("UPDATE shopping_list_items SET assignedToMemberId = :memberId, assignedToMemberName = :memberName WHERE id = :id")
    suspend fun assignMember(id: Long, memberId: Long?, memberName: String?)

    @Query("DELETE FROM shopping_list_items WHERE isPurchased = 1")
    suspend fun clearCompletedItems()
}

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlement_records ORDER BY timestampMillis DESC")
    fun getAllSettlements(): Flow<List<SettlementRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementRecord): Long

    @Delete
    suspend fun deleteSettlement(settlement: SettlementRecord)
}
