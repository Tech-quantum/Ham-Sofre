package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.barcode.BarcodeProductInfo
import com.example.data.model.FoodItem
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val context = LocalContext.current

                    // Notification permission check for Android 13+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val notificationPermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission(),
                            onResult = { /* Handled */ }
                        )

                        LaunchedEffect(Unit) {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    // Collect State
                    val foodItems by viewModel.foodItems.collectAsStateWithLifecycle()
                    val purchases by viewModel.purchases.collectAsStateWithLifecycle()
                    val consumptionLogs by viewModel.consumptionLogs.collectAsStateWithLifecycle()
                    val shoppingItems by viewModel.shoppingItems.collectAsStateWithLifecycle()
                    val settlements by viewModel.settlements.collectAsStateWithLifecycle()
                    val members by viewModel.members.collectAsStateWithLifecycle()
                    val balances by viewModel.memberBalances.collectAsStateWithLifecycle()
                    val debtTransfers by viewModel.debtTransfers.collectAsStateWithLifecycle()
                    val expiringCount by viewModel.expiringItemsCount.collectAsStateWithLifecycle()
                    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
                    val isLoadingRecipes by viewModel.isLoadingRecipes.collectAsStateWithLifecycle()
                    val smartSuggestions by viewModel.smartShoppingSuggestions.collectAsStateWithLifecycle()
                    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
                    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

                    val snackbarHostState = remember { SnackbarHostState() }

                    LaunchedEffect(uiMessage) {
                        uiMessage?.let {
                            snackbarHostState.showSnackbar(
                                message = it.message,
                                duration = SnackbarDuration.Short
                            )
                            viewModel.clearUiMessage()
                        }
                    }

                    var currentTab by remember { mutableStateOf(0) }
                    var showChatScreen by remember { mutableStateOf(false) }
                    var showBarcodeScanner by remember { mutableStateOf(false) }
                    var showAiRecipeDialog by remember { mutableStateOf(false) }
                    var scannedProductForAdd by remember { mutableStateOf<FoodItem?>(null) }

                    if (showChatScreen) {
                        androidx.activity.compose.BackHandler {
                            showChatScreen = false
                        }
                        HousemateChatScreen(
                            viewModel = viewModel,
                            messages = chatMessages,
                            members = members,
                            onBack = { showChatScreen = false }
                        )
                    } else {
                        Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Text(
                                            text = "هم‌سفره",
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "مدیریت غذا و دُنگ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                actions = {
                                    // Quick Expiry Alert Button
                                    if (expiringCount > 0) {
                                        IconButton(
                                            onClick = {
                                                val count = viewModel.triggerExpiryNotificationCheck()
                                                viewModel.showMessage("هشدار انقضا برای $count قلم خوراکی صادر شد.")
                                            }
                                        ) {
                                            BadgedBox(
                                                badge = {
                                                    Badge { Text(expiringCount.toString()) }
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.NotificationsActive,
                                                    contentDescription = "هشدار انقضا",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    // Gemini AI Recipe Chef Action
                                    IconButton(
                                        onClick = {
                                            showAiRecipeDialog = true
                                            if (recipes.isEmpty()) viewModel.fetchRecipeSuggestions()
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "پیشنهاد دستور پخت جمینای",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Barcode Scanner Action
                                    IconButton(onClick = { showBarcodeScanner = true }) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = "اسکن بارکد",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Housemate Collaborative Chat Action
                                    IconButton(onClick = { showChatScreen = true }) {
                                        if (chatMessages.isNotEmpty()) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                                        contentColor = MaterialTheme.colorScheme.onTertiary
                                                    ) {
                                                        Text(chatMessages.size.toString())
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Forum,
                                                    contentDescription = "چت هم‌سفره",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else {
                                            Icon(
                                                Icons.Default.Forum,
                                                contentDescription = "چت هم‌سفره",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { currentTab = 0 },
                                    icon = {
                                        Icon(
                                            if (currentTab == 0) Icons.Filled.Kitchen else Icons.Outlined.Kitchen,
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text("انبار غذا") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { currentTab = 1 },
                                    icon = {
                                        Icon(
                                            if (currentTab == 1) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text("ماژول دُنگ") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { currentTab = 2 },
                                    icon = {
                                        Icon(
                                            if (currentTab == 2) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text("تسویه") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 3,
                                    onClick = { currentTab = 3 },
                                    icon = {
                                        val pendingCount = shoppingItems.count { !it.isPurchased }
                                        if (pendingCount > 0) {
                                            BadgedBox(badge = { Badge { Text(pendingCount.toString()) } }) {
                                                Icon(
                                                    if (currentTab == 3) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                                                    contentDescription = null
                                                )
                                            }
                                        } else {
                                            Icon(
                                                if (currentTab == 3) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    label = { Text("لیست خرید") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == 4,
                                    onClick = { currentTab = 4 },
                                    icon = {
                                        Icon(
                                            if (currentTab == 4) Icons.Filled.Group else Icons.Outlined.Group,
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text("مصرف و افراد") }
                                )
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                0 -> PantryScreen(
                                    viewModel = viewModel,
                                    foodItems = foodItems,
                                    members = members,
                                    onOpenBarcodeScanner = { showBarcodeScanner = true },
                                    onOpenAiRecipes = {
                                        showAiRecipeDialog = true
                                        if (recipes.isEmpty()) viewModel.fetchRecipeSuggestions()
                                    }
                                )
                                1 -> PurchasesScreen(
                                    viewModel = viewModel,
                                    purchases = purchases,
                                    members = members,
                                    balances = balances
                                )
                                2 -> SettlementScreen(
                                    viewModel = viewModel,
                                    balances = balances,
                                    debtTransfers = debtTransfers,
                                    settlements = settlements,
                                    members = members
                                )
                                3 -> ShoppingListScreen(
                                    viewModel = viewModel,
                                    shoppingItems = shoppingItems,
                                    members = members,
                                    smartSuggestions = smartSuggestions,
                                    onOpenBarcodeScanner = { showBarcodeScanner = true }
                                )
                                4 -> ConsumptionAndMembersScreen(
                                    viewModel = viewModel,
                                    consumptionLogs = consumptionLogs,
                                    members = members,
                                    chatMessages = chatMessages
                                )
                            }
                        }
                    }
                }

                    // Barcode Scanner Dialog
                    if (showBarcodeScanner) {
                        BarcodeScannerDialog(
                            onDismissRequest = { showBarcodeScanner = false },
                            onBarcodeScanned = { barcode, productInfo ->
                                viewModel.showMessage("«${productInfo.name}» با موفقیت در انبار ثبت شد. ✔")
                                showBarcodeScanner = false
                            }
                        )
                    }

                    // If a barcode was scanned, show the pre-filled Add Food dialog
                    scannedProductForAdd?.let { prefilled ->
                        AddEditFoodItemDialog(
                            item = prefilled,
                            members = members,
                            onDismiss = { scannedProductForAdd = null },
                            onSave = { itemToSave ->
                                val duplicateMsg = viewModel.checkForDuplicates(itemToSave.name, itemToSave.barcode)
                                if (duplicateMsg != null) {
                                    viewModel.showMessage(duplicateMsg)
                                }
                                viewModel.addOrUpdateFoodItem(itemToSave)
                                scannedProductForAdd = null
                            },
                            onScanBarcode = { showBarcodeScanner = true }
                        )
                    }

                    // Gemini AI Recipe Suggestions Dialog
                    if (showAiRecipeDialog) {
                        AiRecipeDialog(
                            recipes = recipes,
                            isLoading = isLoadingRecipes,
                            members = members,
                            onDismiss = { showAiRecipeDialog = false },
                            onRefresh = { viewModel.fetchRecipeSuggestions() },
                            onMarkCooked = { recipe, cook ->
                                viewModel.markRecipeCooked(recipe, cook)
                            }
                        )
                    }
                }
            }
        }
    }
}
