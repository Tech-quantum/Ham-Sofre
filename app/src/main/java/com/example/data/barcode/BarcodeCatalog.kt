package com.example.data.barcode

data class BarcodeProductInfo(
    val barcode: String,
    val name: String,
    val category: String,
    val defaultLocation: String, // FRIDGE, FREEZER, PANTRY
    val shelfLifeDays: Int,
    val defaultUnit: String,
    val defaultQuantity: Double,
    val estimatedPrice: Long
)

object BarcodeCatalog {
    // Built-in catalog of standard food barcodes and typical grocery items
    val KNOWN_PRODUCTS: Map<String, BarcodeProductInfo> = mapOf(
        "6260123456789" to BarcodeProductInfo(
            barcode = "6260123456789",
            name = "شیر کم‌چرب کاله ۱ لیتر",
            category = "لبنیات",
            defaultLocation = "FRIDGE",
            shelfLifeDays = 6,
            defaultUnit = "لیتر",
            defaultQuantity = 1.0,
            estimatedPrice = 45000
        ),
        "6260987654321" to BarcodeProductInfo(
            barcode = "6260987654321",
            name = "تخم‌مرغ زرده طلایی (شانه ۱۵ عددی)",
            category = "پروتئین",
            defaultLocation = "FRIDGE",
            shelfLifeDays = 21,
            defaultUnit = "عدد",
            defaultQuantity = 15.0,
            estimatedPrice = 85000
        ),
        "6260111222333" to BarcodeProductInfo(
            barcode = "6260111222333",
            name = "پنیر سفید نسبتا چرب پگاه ۴۰۰ گرم",
            category = "لبنیات",
            defaultLocation = "FRIDGE",
            shelfLifeDays = 45,
            defaultUnit = "گرم",
            defaultQuantity = 400.0,
            estimatedPrice = 62000
        ),
        "6260444555666" to BarcodeProductInfo(
            barcode = "6260444555666",
            name = "ماکارونی اسپاگتی مانا ۱.۲",
            category = "نان و غلات",
            defaultLocation = "PANTRY",
            shelfLifeDays = 365,
            defaultUnit = "بسته",
            defaultQuantity = 1.0,
            estimatedPrice = 48000
        ),
        "6260777888999" to BarcodeProductInfo(
            barcode = "6260777888999",
            name = "سینه مرغ بدون پوست منجمد",
            category = "پروتئین",
            defaultLocation = "FREEZER",
            shelfLifeDays = 90,
            defaultUnit = "کیلوگرم",
            defaultQuantity = 1.0,
            estimatedPrice = 190000
        ),
        "6260555666777" to BarcodeProductInfo(
            barcode = "6260555666777",
            name = "ماست پروبیوتیک سون کاله",
            category = "لبنیات",
            defaultLocation = "FRIDGE",
            shelfLifeDays = 14,
            defaultUnit = "گرم",
            defaultQuantity = 900.0,
            estimatedPrice = 78000
        ),
        "6260888999000" to BarcodeProductInfo(
            barcode = "6260888999000",
            name = "رب گوجه‌فرنگی روژین ۸۰۰ گرم",
            category = "خواروبار",
            defaultLocation = "PANTRY",
            shelfLifeDays = 360,
            defaultUnit = "قوطی",
            defaultQuantity = 1.0,
            estimatedPrice = 72000
        ),
        "6260333444555" to BarcodeProductInfo(
            barcode = "6260333444555",
            name = "نان تست پروتئینه سه نان",
            category = "نان و غلات",
            defaultLocation = "PANTRY",
            shelfLifeDays = 5,
            defaultUnit = "بسته",
            defaultQuantity = 1.0,
            estimatedPrice = 55000
        ),
        "6260222333444" to BarcodeProductInfo(
            barcode = "6260222333444",
            name = "روغن زیتون فرابکر اویلا",
            category = "خواروبار",
            defaultLocation = "PANTRY",
            shelfLifeDays = 180,
            defaultUnit = "بطری",
            defaultQuantity = 1.0,
            estimatedPrice = 320000
        )
    )

    fun lookup(barcode: String): BarcodeProductInfo {
        val trimmed = barcode.trim()
        val found = KNOWN_PRODUCTS[trimmed]
        if (found != null) return found

        // Heuristic fallback for unknown barcodes
        val inferredCategory = when {
            trimmed.endsWith("1") -> "لبنیات"
            trimmed.endsWith("2") -> "میوه و سبزیجات"
            trimmed.endsWith("3") -> "پروتئین"
            trimmed.endsWith("4") -> "نان و غلات"
            trimmed.endsWith("5") -> "نوشیدنی"
            else -> "خواروبار"
        }
        val shelfLife = when (inferredCategory) {
            "لبنیات" -> 7
            "میوه و سبزیجات" -> 5
            "پروتئین" -> 4
            "نان و غلات" -> 6
            "نوشیدنی" -> 30
            else -> 60
        }
        val location = when (inferredCategory) {
            "لبنیات", "میوه و سبزیجات" -> "FRIDGE"
            "پروتئین" -> "FRIDGE"
            else -> "PANTRY"
        }

        return BarcodeProductInfo(
            barcode = trimmed,
            name = "کالای اسکن شده (${trimmed.takeLast(4)})",
            category = inferredCategory,
            defaultLocation = location,
            shelfLifeDays = shelfLife,
            defaultUnit = "عدد",
            defaultQuantity = 1.0,
            estimatedPrice = 50000
        )
    }
}
