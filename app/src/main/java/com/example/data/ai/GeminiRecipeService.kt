package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.FoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRecipeService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getRecipeSuggestions(availableItems: List<FoodItem>): List<Recipe> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val now = System.currentTimeMillis()

        // Prioritize items expiring soon
        val expiringItems = availableItems.filter { it.daysUntilExpiry(now) <= 4 }
        val otherItems = availableItems.filter { it.daysUntilExpiry(now) > 4 }

        // If no real API key is configured or offline, return smart local recipe suggestions
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateSmartFallbackRecipes(availableItems, expiringItems)
        }

        try {
            val ingredientsPrompt = buildString {
                append("اقلام موجود در یخچال و انبار خانه ما به این شرح است:\n")
                if (expiringItems.isNotEmpty()) {
                    append("⚠️ اقلام نزدیک به انقضا (اینها حتما باید سریعتر مصرف شوند تا اسراف نشوند):\n")
                    expiringItems.forEach {
                        append("- ${it.name} (${it.quantity} ${it.unit} - ${it.daysUntilExpiry(now)} روز تا انقضا)\n")
                    }
                }
                append("\nسایر اقلام موجود در انبار:\n")
                otherItems.take(10).forEach {
                    append("- ${it.name} (${it.quantity} ${it.unit})\n")
                }
            }

            val systemPrompt = """
                شما یک سرآشپز هوشمند و متبحر ایرانی و بین‌المللی هستید.
                هدف شما: پیشنهاد ۳ دستور غذای بسیار خوشمزه، سریع و کاربردی با اولویت دادن به مصرف موادی که تاریخ انقضای آنها نزدیک است، تا از هدررفت و اسراف غذا جلوگیری شود.
                پاسخ را دقیقا به صورت یک آرایه JSON با ساختار زیر برگردانید (بدون مارک‌داون یا توضیحات اضافه):
                [
                  {
                    "title": "نام غذا به فارسی",
                    "cookingTimeMinutes": 25,
                    "difficulty": "آسان",
                    "cuisine": "ایرانی / ملل",
                    "expiringIngredientsUsed": ["مورد ۱ از اقلام در حال انقضا", "مورد ۲"],
                    "otherIngredientsUsed": ["مورد موجود دیگر"],
                    "missingIngredients": ["مواد احتمالی اختیاری"],
                    "instructions": [
                      "مرحله اول",
                      "مرحله دوم",
                      "مرحله سوم"
                    ],
                    "tips": "نکته سرآشپز برای جلوگیری از خراب شدن مواد"
                  }
                ]
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply { put("text", ingredientsPrompt) })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.7)
                }
                put("generationConfig", generationConfig)

                val systemInstruction = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    }
                    put("parts", partsArray)
                }
                put("systemInstruction", systemInstruction)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                return@withContext generateSmartFallbackRecipes(availableItems, expiringItems)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text")

            if (!rawText.isNullOrBlank()) {
                parseRecipesFromJson(rawText)
            } else {
                generateSmartFallbackRecipes(availableItems, expiringItems)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            generateSmartFallbackRecipes(availableItems, expiringItems)
        }
    }

    private fun parseRecipesFromJson(jsonText: String): List<Recipe> {
        val list = mutableListOf<Recipe>()
        try {
            val jsonArray = JSONArray(jsonText.trim())
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val instructions = mutableListOf<String>()
                val instructionsArray = obj.optJSONArray("instructions")
                if (instructionsArray != null) {
                    for (j in 0 until instructionsArray.length()) {
                        instructions.add(instructionsArray.getString(j))
                    }
                }

                val expiringUsed = mutableListOf<String>()
                val expArray = obj.optJSONArray("expiringIngredientsUsed")
                if (expArray != null) {
                    for (j in 0 until expArray.length()) {
                        expiringUsed.add(expArray.getString(j))
                    }
                }

                val otherUsed = mutableListOf<String>()
                val otherArray = obj.optJSONArray("otherIngredientsUsed")
                if (otherArray != null) {
                    for (j in 0 until otherArray.length()) {
                        otherUsed.add(otherArray.getString(j))
                    }
                }

                val missing = mutableListOf<String>()
                val missArray = obj.optJSONArray("missingIngredients")
                if (missArray != null) {
                    for (j in 0 until missArray.length()) {
                        missing.add(missArray.getString(j))
                    }
                }

                list.add(
                    Recipe(
                        title = obj.optString("title", "غذای پیشنهادی"),
                        cookingTimeMinutes = obj.optInt("cookingTimeMinutes", 20),
                        difficulty = obj.optString("difficulty", "آسان"),
                        cuisine = obj.optString("cuisine", "ایرانی"),
                        expiringIngredientsUsed = expiringUsed,
                        otherIngredientsUsed = otherUsed,
                        missingIngredients = missing,
                        instructions = instructions,
                        tips = obj.optString("tips", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun generateSmartFallbackRecipes(
        availableItems: List<FoodItem>,
        expiringItems: List<FoodItem>
    ): List<Recipe> {
        val names = availableItems.map { it.name.lowercase() }
        val hasEgg = names.any { it.contains("تخم") }
        val hasMilk = names.any { it.contains("شیر") }
        val hasCheese = names.any { it.contains("پنیر") }
        val hasChicken = names.any { it.contains("مرغ") }
        val hasPasta = names.any { it.contains("ماکارونی") || it.contains("پاستا") }
        val hasYogurt = names.any { it.contains("ماست") }

        val recipes = mutableListOf<Recipe>()

        if (hasChicken && hasPasta) {
            recipes.add(
                Recipe(
                    title = "پاستا چیکن آلفردو خانگی",
                    cookingTimeMinutes = 25,
                    difficulty = "آسان",
                    cuisine = "ایتالیایی",
                    expiringIngredientsUsed = expiringItems.filter { it.name.contains("شیر") || it.name.contains("پنیر") }.map { it.name },
                    otherIngredientsUsed = listOf("فیله مرغ", "ماکارونی / پاستا", "شیر یا خامه"),
                    missingIngredients = listOf("کمی سیر", "جعفری"),
                    instructions = listOf(
                        "ماکارونی را در آب جوش با کمی نمک بجوشانید و آبکش کنید.",
                        "فیله‌های مرغ را نواری برش زده و با کره و ادویه تفت دهید.",
                        "شیر و پنیر موجود در یخچال را به تابه اضافه کنید تا سس غلیظ شود.",
                        "پاستا را با سس مرغ مخلوط کرده و داغ سرو نمایید."
                    ),
                    tips = "استفاده از شیر و پنیر رو به انقضا، بافت بسیار خامه‌ای و لذیذی به این پاستا می‌دهد!"
                )
            )
        }

        if (hasEgg && hasCheese) {
            recipes.add(
                Recipe(
                    title = "املت اسفناج و پنیر فتا",
                    cookingTimeMinutes = 15,
                    difficulty = "خیلی آسان",
                    cuisine = "صبحانه / شام سریع",
                    expiringIngredientsUsed = expiringItems.filter { it.name.contains("تخم") || it.name.contains("پنیر") }.map { it.name },
                    otherIngredientsUsed = listOf("تخم‌مرغ", "پنیر صبحانه فتا"),
                    missingIngredients = listOf("گوجه یا سبزیجات تازه"),
                    instructions = listOf(
                        "تخم‌مرغ‌ها را با نمک و فلفل سیاه هم بزنید.",
                        "کره یا روغن را در تابه داغ کرده و مایه تخم‌مرغ را بریزید.",
                        "وقتی نیم‌بند شد، پنیر فتا را روی آن خرد کرده و تا بزنید."
                    ),
                    tips = "پنیر صبحانه وقتی حرارت ملایم می‌بیند طعم کرمی عالی پیدا می‌کند."
                )
            )
        }

        if (hasEgg && hasMilk) {
            recipes.add(
                Recipe(
                    title = "فرنچ تست طلایی با شیر",
                    cookingTimeMinutes = 12,
                    difficulty = "آسان",
                    cuisine = "صبحانه / میان‌وعده",
                    expiringIngredientsUsed = expiringItems.filter { it.name.contains("شیر") }.map { it.name },
                    otherIngredientsUsed = listOf("شیر کم‌چرب", "تخم‌مرغ", "نان تست"),
                    missingIngredients = listOf("کمی دارچین و عسل"),
                    instructions = listOf(
                        "شیر، تخم‌مرغ و دارچین را در یک ظرف گود مخلوط کنید.",
                        "نان‌های تست را چند ثانیه در مایه قرار دهید تا مایع را جذب کنند.",
                        "در تابه کره‌ای هر طرف را ۲ دقیقه سرخ کنید تا طلایی و برشته شوند."
                    ),
                    tips = "بهترین راه برای مصرف آخرین استکان شیر یخچال قبل از خراب شدن!"
                )
            )
        }

        if (hasYogurt) {
            recipes.add(
                Recipe(
                    title = "بورانی یا سس ماست و فیله گریل",
                    cookingTimeMinutes = 18,
                    difficulty = "ساده",
                    cuisine = "ایرانی سبک",
                    expiringIngredientsUsed = expiringItems.filter { it.name.contains("ماست") }.map { it.name },
                    otherIngredientsUsed = listOf("ماست سون", "مرغ یا سبزیجات"),
                    instructions = listOf(
                        "ماست را با کمی نعناع خشک و نمک مزه‌دار کنید.",
                        "مرغ یا سبزیجات گریل‌شده را کنار سس ماست خنک نوش جان کنید."
                    ),
                    tips = "ماست‌های غلیظ برای دیپ و طعم‌دار کردن گوشت عالی هستند."
                )
            )
        }

        if (recipes.isEmpty()) {
            recipes.add(
                Recipe(
                    title = "خوراک سریع سرآشپز با اقلام موجود",
                    cookingTimeMinutes = 20,
                    difficulty = "آسان",
                    cuisine = "خانگی",
                    expiringIngredientsUsed = expiringItems.map { it.name },
                    otherIngredientsUsed = availableItems.take(3).map { it.name },
                    instructions = listOf(
                        "اقلام در آستانه انقضا را تمیز کرده و خرد نمایید.",
                        "در تابه با کمی روغن زیتون یا کره و زردچوبه تفت دهید.",
                        "با نان گرم یا برنج به صورت خوراک روزانه میل فرمایید."
                    ),
                    tips = "تفت دادن ملایم باعث حفظ مواد مغذی و مصرف بهینه مواد می‌شود."
                )
            )
        }

        return recipes
    }
}
