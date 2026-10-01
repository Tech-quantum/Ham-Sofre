package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Member::class,
        FoodItem::class,
        PurchaseRecord::class,
        ConsumptionLog::class,
        ShoppingListItem::class,
        SettlementRecord::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun consumptionDao(): ConsumptionDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun settlementDao(): SettlementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hamsofreh_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val memberDao = database.memberDao()
            val foodItemDao = database.foodItemDao()
            val purchaseDao = database.purchaseDao()
            val shoppingListDao = database.shoppingListDao()

            if (memberDao.getCount() > 0) return

            // 1. Initial Members
            val aliId = memberDao.insertMember(
                Member(name = "علی", colorHex = "#1B6A45", avatarEmoji = "👨‍🍳", isMe = true)
            )
            val saraId = memberDao.insertMember(
                Member(name = "سارا", colorHex = "#B45309", avatarEmoji = "👩‍🌾")
            )
            val rezaId = memberDao.insertMember(
                Member(name = "رضا", colorHex = "#BE123C", avatarEmoji = "🧑‍💻")
            )

            val now = System.currentTimeMillis()
            val oneDay = 24L * 60 * 60 * 1000

            // 2. Initial Food Items (some fresh, some expiring soon, some in pantry)
            foodItemDao.insertFoodItem(
                FoodItem(
                    name = "شیر کم‌چرب کاله",
                    barcode = "6260123456789",
                    category = "لبنیات",
                    quantity = 1.0,
                    unit = "لیتر",
                    location = "FRIDGE",
                    purchaseDateMillis = now - (3 * oneDay),
                    expiryDateMillis = now + (1 * oneDay), // Expiring in 1 day!
                    price = 45000,
                    purchasedByMemberId = aliId,
                    notes = "برای صبحانه مشترک"
                )
            )
            foodItemDao.insertFoodItem(
                FoodItem(
                    name = "تخم‌مرغ محلی",
                    barcode = "6260987654321",
                    category = "پروتئین",
                    quantity = 15.0,
                    unit = "عدد",
                    location = "FRIDGE",
                    purchaseDateMillis = now - (2 * oneDay),
                    expiryDateMillis = now + (6 * oneDay),
                    price = 85000,
                    purchasedByMemberId = saraId
                )
            )
            foodItemDao.insertFoodItem(
                FoodItem(
                    name = "پنیر صبحانه فتا",
                    barcode = "6260111222333",
                    category = "لبنیات",
                    quantity = 400.0,
                    unit = "گرم",
                    location = "FRIDGE",
                    purchaseDateMillis = now - (5 * oneDay),
                    expiryDateMillis = now + (2 * oneDay), // Expiring soon
                    price = 62000,
                    purchasedByMemberId = saraId
                )
            )
            foodItemDao.insertFoodItem(
                FoodItem(
                    name = "ماکارونی مانا ۵۰۰ گرمی",
                    barcode = "6260444555666",
                    category = "نان و غلات",
                    quantity = 2.0,
                    unit = "بسته",
                    location = "PANTRY",
                    purchaseDateMillis = now - (10 * oneDay),
                    expiryDateMillis = now + (180 * oneDay),
                    price = 48000,
                    purchasedByMemberId = rezaId
                )
            )
            foodItemDao.insertFoodItem(
                FoodItem(
                    name = "فیله سینه مرغ",
                    barcode = "6260777888999",
                    category = "پروتئین",
                    quantity = 1.5,
                    unit = "کیلوگرم",
                    location = "FREEZER",
                    purchaseDateMillis = now - (4 * oneDay),
                    expiryDateMillis = now + (45 * oneDay),
                    price = 280000,
                    purchasedByMemberId = aliId
                )
            )
            foodItemDao.insertFoodItem(
                FoodItem(
                    name = "ماست یونانی سون",
                    barcode = "6260555666777",
                    category = "لبنیات",
                    quantity = 900.0,
                    unit = "گرم",
                    location = "FRIDGE",
                    purchaseDateMillis = now - (1 * oneDay),
                    expiryDateMillis = now + (9 * oneDay),
                    price = 78000,
                    purchasedByMemberId = aliId
                )
            )

            // 3. Initial Purchases
            purchaseDao.insertPurchase(
                PurchaseRecord(
                    title = "خرید سوپرمارکت و میوه",
                    amount = 240000,
                    paidByMemberId = aliId,
                    paidByMemberName = "علی",
                    dateMillis = now - (2 * oneDay),
                    beneficiaryMemberIds = "$aliId,$saraId,$rezaId",
                    itemsSummary = "شیر، ماست، موز و نان سنگک",
                    notes = "تقسیم مساوی بین ۳ نفر"
                )
            )
            purchaseDao.insertPurchase(
                PurchaseRecord(
                    title = "خرید پروتئین و تخم‌مرغ",
                    amount = 365000,
                    paidByMemberId = saraId,
                    paidByMemberName = "سارا",
                    dateMillis = now - (4 * oneDay),
                    beneficiaryMemberIds = "$aliId,$saraId,$rezaId",
                    itemsSummary = "تخم‌مرغ و پنیر فتا",
                    notes = "خرید هفتگی"
                )
            )

            // 4. Initial Collaborative Shopping List
            shoppingListDao.insertShoppingItem(
                ShoppingListItem(
                    name = "نان تست سبوس‌دار",
                    barcode = "6260333444555",
                    quantity = 2.0,
                    unit = "بسته",
                    category = "نان و غلات",
                    addedByMemberId = aliId,
                    addedByMemberName = "علی",
                    assignedToMemberId = saraId,
                    assignedToMemberName = "سارا",
                    isPurchased = false,
                    estimatedPrice = 55000,
                    isUrgent = true
                )
            )
            shoppingListDao.insertShoppingItem(
                ShoppingListItem(
                    name = "رب گوجه فرنگی روژین",
                    barcode = "6260888999000",
                    quantity = 1.0,
                    unit = "قوطی",
                    category = "خواروبار",
                    addedByMemberId = rezaId,
                    addedByMemberName = "رضا",
                    assignedToMemberId = null,
                    assignedToMemberName = null,
                    isPurchased = false,
                    estimatedPrice = 72000,
                    isUrgent = false
                )
            )
            shoppingListDao.insertShoppingItem(
                ShoppingListItem(
                    name = "چای سیاه ایرانی",
                    barcode = "6260222333444",
                    quantity = 1.0,
                    unit = "بسته",
                    category = "نوشیدنی",
                    addedByMemberId = saraId,
                    addedByMemberName = "سارا",
                    isPurchased = true,
                    estimatedPrice = 135000
                )
            )
        }
    }
}
