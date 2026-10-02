package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.data.model.ChatMessage
import com.example.data.model.Member
import com.example.ui.MainViewModel
import com.example.ui.components.MemberAvatar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HousemateChatScreen(
    viewModel: MainViewModel,
    messages: List<ChatMessage>,
    members: List<Member>,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var selectedSender by remember { mutableStateOf(members.firstOrNull { it.isMe } ?: members.firstOrNull()) }
    var selectedType by remember { mutableStateOf("TEXT") } // "TEXT", "SHOPPING_REQ", "EXPENSE_ALERT", "FOOD_ALERT"
    var showClearDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickTemplates = listOf(
        "🛒 کی امروز خرید می‌ره؟" to "SHOPPING_REQ",
        "🍽️ غذا حاضر است، بفرمایید سر میز!" to "FOOD_ALERT",
        "💰 لطفا دُنگ‌ها رو بررسی و تسویه کنید" to "EXPENSE_ALERT",
        "⚡ یک قلم خرید فوری داریم، لطفا چک کنید" to "SHOPPING_REQ",
        "⚠️ خوراکی‌های یخچال در حال انقضاست" to "FOOD_ALERT",
        "👍 ممنون، ثبت شد!" to "TEXT"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "گفتگوی مشترک هم‌سفره",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (members.isNotEmpty()) members.joinToString("، ") { it.name } else "اعضای خانه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "پاک کردن پیام‌ها", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Sender Switcher Bar (Useful for shared household phones/sessions)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ارسال از طرف:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(members) { m ->
                            FilterChip(
                                selected = selectedSender?.id == m.id,
                                onClick = { selectedSender = m },
                                label = {
                                    Text(
                                        text = if (m.isMe) "${m.name} (من)" else m.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedSender?.id == m.id) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Messages List
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Forum,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "هنوز پیامی ارسال نشده است!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "از قالب‌های سریع زیر یا کادر پایین، اولین پیام را بفرستید.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isFromSelectedMe = (selectedSender != null && msg.senderMemberId == selectedSender?.id) || msg.isFromMe
                        val senderMember = members.firstOrNull { it.id == msg.senderMemberId }

                        ChatMessageBubble(
                            message = msg,
                            sender = senderMember,
                            isFromMe = isFromSelectedMe,
                            onDelete = { viewModel.deleteChatMessage(msg) }
                        )
                    }
                }
            }

            // Quick Action Template Chips
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(quickTemplates) { (text, type) ->
                            SuggestionChip(
                                onClick = {
                                    val sender = selectedSender ?: return@SuggestionChip
                                    viewModel.sendChatMessage(text, sender, type)
                                },
                                label = { Text(text, fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Message Type selector (Text, Shopping, Expense, Food)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val types = listOf(
                            Triple("TEXT", "پیام عادی", Icons.Default.Chat),
                            Triple("SHOPPING_REQ", "خرید 🛒", Icons.Default.ShoppingCart),
                            Triple("EXPENSE_ALERT", "دُنگ 💰", Icons.Default.ReceiptLong),
                            Triple("FOOD_ALERT", "غذا 🍽️", Icons.Default.Kitchen)
                        )
                        types.forEach { (typeKey, label, icon) ->
                            FilterChip(
                                selected = selectedType == typeKey,
                                onClick = { selectedType = typeKey },
                                label = { Text(label, fontSize = 10.sp) },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }

                    // Bottom Input Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    when (selectedType) {
                                        "SHOPPING_REQ" -> "درخواست خرید به هم‌خانه‌ها..."
                                        "EXPENSE_ALERT" -> "یادآوری هزینه یا دُنگ..."
                                        "FOOD_ALERT" -> "اطلاع‌رسانی وضعیت غذا و انبار..."
                                        else -> "پیام به هم‌سفره‌ها..."
                                    },
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledIconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val sender = selectedSender ?: return@FilledIconButton
                                    viewModel.sendChatMessage(inputText, sender, selectedType)
                                    inputText = ""
                                    selectedType = "TEXT"
                                }
                            },
                            enabled = inputText.isNotBlank(),
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "ارسال پیام",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirm Clear Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("پاک کردن گفتگوی هم‌سفره") },
            text = { Text("آیا مطمئن هستید که می‌خواهید تمام پیام‌های چت خانگی را پاک کنید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChatMessages()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بله، پاک کن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    sender: Member?,
    isFromMe: Boolean,
    onDelete: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestampMillis) { timeFormat.format(Date(message.timestampMillis)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isFromMe && sender != null) {
            MemberAvatar(member = sender, size = 32)
            Spacer(modifier = Modifier.width(6.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromMe) 4.dp else 16.dp,
                bottomEnd = if (isFromMe) 16.dp else 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isFromMe -> MaterialTheme.colorScheme.primaryContainer
                    message.messageType == "SHOPPING_REQ" -> Color(0xFFE0F2FE)
                    message.messageType == "EXPENSE_ALERT" -> Color(0xFFDCFCE7)
                    message.messageType == "FOOD_ALERT" -> Color(0xFFFEF3C7)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header (Sender name & message type badge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFromMe) "شما (${message.senderMemberName})" else message.senderMemberName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Type Badge
                    when (message.messageType) {
                        "SHOPPING_REQ" -> Surface(
                            color = Color(0xFF0284C7).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "🛒 خرید",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        "EXPENSE_ALERT" -> Surface(
                            color = Color(0xFF16A34A).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "💰 دُنگ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        "FOOD_ALERT" -> Surface(
                            color = Color(0xFFD97706).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "🍽️ غذا",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message Text
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Footer with timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        if (isFromMe && sender != null) {
            Spacer(modifier = Modifier.width(6.dp))
            MemberAvatar(member = sender, size = 32)
        }
    }
}
