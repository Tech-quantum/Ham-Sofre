package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ai.Recipe
import com.example.data.model.Member

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRecipeDialog(
    recipes: List<Recipe>,
    isLoading: Boolean,
    members: List<Member>,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onMarkCooked: (Recipe, Member) -> Unit
) {
    var recipeToCook by remember { mutableStateOf<Recipe?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "پیشنهاد دستور پخت با جمینای (Gemini AI)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onRefresh, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "به‌روزرسانی")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Info banner explaining the AI anti-waste logic
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SoupKitchen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "جمینای با تحلیل موجودی یخچال و اولویت‌دادن به اقلام در حال انقضا، این غذاها را برای شما طراحی کرده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "سرآشپز هوشمند جمینای در حال بررسی یخچال شماست...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "طراحی دستور پخت متناسب با اقلام در آستانه انقضا",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else if (recipes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.RestaurantMenu,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "هنوز پیشنهادی دریافت نشده است",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onRefresh) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("دریافت پیشنهاد غذا از جمینای")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(recipes, key = { it.title }) { recipe ->
                            RecipeItemCard(
                                recipe = recipe,
                                onCookClick = { recipeToCook = recipe }
                            )
                        }
                    }
                }
            }
        }
    }

    // Cooked Confirmation Dialog
    recipeToCook?.let { recipe ->
        var selectedCook by remember { mutableStateOf(members.firstOrNull { it.isMe } ?: members.firstOrNull()) }

        AlertDialog(
            onDismissRequest = { recipeToCook = null },
            title = { Text("ثبت پخت «${recipe.title}»") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("با تایید، اقلام استفاده‌شده در این دستور پخت به طور خودکار از انبار کسر و در سابقه مصرف ثبت می‌گردد.")
                    Text("چه کسی این غذا را پخت / مصرف کرد؟", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(members) { m ->
                            FilterChip(
                                selected = selectedCook?.id == m.id,
                                onClick = { selectedCook = m },
                                label = { Text(m.name) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cook = selectedCook ?: return@Button
                        onMarkCooked(recipe, cook)
                        recipeToCook = null
                        onDismiss()
                    }
                ) {
                    Text("ثبت پخت و کسر از یخچال")
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToCook = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun RecipeItemCard(
    recipe: Recipe,
    onCookClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${recipe.cookingTimeMinutes} دقیقه",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "سبک: ${recipe.cuisine}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "سطح: ${recipe.difficulty}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expiring Ingredients Saved Badge
            if (recipe.expiringIngredientsUsed.isNotEmpty()) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "اقلام نزدیک انقضا که نجات می‌یابند: ${recipe.expiringIngredientsUsed.joinToString("، ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF78350F),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Other Ingredients
            if (recipe.otherIngredientsUsed.isNotEmpty()) {
                Text(
                    text = "سایر مواد موجود: ${recipe.otherIngredientsUsed.joinToString("، ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Instructions
            if (recipe.instructions.isNotEmpty()) {
                Text(
                    text = "دستور پخت گام‌به‌گام:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                recipe.instructions.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}. ",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Chef tips
            if (recipe.tips.isNotBlank()) {
                Text(
                    text = "💡 نکته سرآشپز: ${recipe.tips}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Cook Action Button
            Button(
                onClick = onCookClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("این غذا را پختم! (کسر اقلام از انبار)")
            }
        }
    }
}
