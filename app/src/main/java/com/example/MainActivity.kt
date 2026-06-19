package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import android.content.Intent
import com.example.data.model.PersonalDebt
import com.example.data.model.InstallmentPayment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WorkshopTransaction
import com.example.data.model.RefurbishedDevice
import com.example.data.model.MaintenanceExpense
import com.example.data.api.GeminiManager
import kotlinx.coroutines.launch
import com.example.ui.theme.*
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.viewmodel.DateFilter
import com.example.ui.viewmodel.WorkshopStats
import com.example.ui.viewmodel.WorkshopViewModel
import com.example.ui.viewmodel.WorkshopViewModelFactory
import java.text.SimpleDateFormat
import java.util.*
import com.example.ui.components.*
import com.example.ui.components.TransactionListItem

import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.SettingsScreen
import com.example.ui.SectionsScreen
import com.example.ui.RefurbishedDevicesSection
import com.example.ui.IntroDashboardScreen
import com.example.ui.MainDashboardContainer

class MainActivity : ComponentActivity() {
    private val viewModel: WorkshopViewModel by viewModels {
        val app = application as WorkshopApplication
        WorkshopViewModelFactory(app.repository, app.settingsManager)
    }

    private val exportCsvLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    val transactions = viewModel.transactionsFlow.value
                    val debts = viewModel.debtsFlow.value
                    val csvString = com.example.data.repository.BackupHandler.createCsvBackup(transactions, debts)
                    outputStream.write(csvString.toByteArray())
                    Toast.makeText(this, "تم تصدير نسخة الاحتياط بنجاح!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "خطأ أثناء حفظ الملف: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val importCsvLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                contentResolver.openInputStream(it)?.use { inputStream ->
                    val csvData = inputStream.bufferedReader().use { br -> br.readText() }
                    val pair = com.example.data.repository.BackupHandler.parseCsvBackup(csvData)
                    if (pair.first.isNotEmpty() || pair.second.isNotEmpty()) {
                        viewModel.importBackup(pair.first, pair.second)
                        Toast.makeText(
                            this,
                            "تم استيراد ${pair.first.size} عملية و ${pair.second.size} دين بنجاح!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(this, "فشل الاستيراد: صيغة الملف غير مدعومة أو فارغة.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "خطأ أثناء القراءة: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeKey by viewModel.appTheme.collectAsStateWithLifecycle()
            val darkModeVal by viewModel.darkMode.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

            MyApplicationTheme(themeKey = themeKey, darkModeVal = darkModeVal) {
                val layoutDirection = if (appLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    WorkshopApp(
                        viewModel = viewModel,
                        onExportBackup = { exportCsvLauncher.launch("warshati_backup.csv") },
                        onImportBackup = { importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/plain", "*/*")) }
                    )
                }
            }
        }
    }
}

// App Screens / Tabs
enum class AppTab(val icon: ImageVector) {
    HOME(Icons.Default.Dashboard),
    TRANSACTIONS(Icons.AutoMirrored.Filled.List),
    SECTIONS(Icons.Default.AddCircle),
    DEBTS(Icons.Default.CreditCard),
    SETTINGS(Icons.Default.Settings)
}

@Composable
fun WorkshopApp(
    viewModel: WorkshopViewModel,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeKey by viewModel.appTheme.collectAsStateWithLifecycle()
    val darkModeVal by viewModel.darkMode.collectAsStateWithLifecycle()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (darkModeVal) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }
    val isLiquidTheme = themeKey == "LIQUID_GLASS"

    var currentTab by remember { mutableStateOf(AppTab.HOME) }
    var initialCategoryForAdd by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddDebtDialogGlobal by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<WorkshopTransaction?>(null) }
    var showGoogleAssistantDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val transactions by viewModel.transactionsFlow.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val personalDebts by viewModel.debtsFlow.collectAsStateWithLifecycle()
    val installments by viewModel.installmentsFlow.collectAsStateWithLifecycle()
    val stats by viewModel.statsFlow.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val dateFilter by viewModel.dateFilter.collectAsStateWithLifecycle()
    val deliveryFilter by viewModel.deliveryFilter.collectAsStateWithLifecycle()

    // Dialog state
    if (showAddDialog) {
        AddEditTransactionDialog(
            viewModel = viewModel,
            transaction = null,
            initialCategory = initialCategoryForAdd,
            onDismiss = { 
                showAddDialog = false
                initialCategoryForAdd = null
            },
            onSave = { title, category, cost, sale, model, name, notes, creditAmount, creditPaid, wallet, dueDate, tDate, isDelivered, affectBalance ->
                viewModel.addTransaction(title, category, cost, sale, model, name, notes, creditAmount, creditPaid, wallet, dueDate, tDate, isDelivered, affectBalance)
                showAddDialog = false
                initialCategoryForAdd = null
                Toast.makeText(context, "تمت إضافة العملية بنجاح!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (transactionToEdit != null) {
        AddEditTransactionDialog(
            viewModel = viewModel,
            transaction = transactionToEdit,
            onDismiss = { transactionToEdit = null },
            onDelete = {
                viewModel.deleteTransaction(transactionToEdit!!)
                transactionToEdit = null
                Toast.makeText(context, "تم حذف العملية بنجاح!", Toast.LENGTH_SHORT).show()
            },
            onSave = { title, category, cost, sale, model, name, notes, creditAmount, creditPaid, wallet, dueDate, tDate, isDelivered, affectBalance ->
                viewModel.deleteTransaction(transactionToEdit!!)
                viewModel.addTransaction(title, category, cost, sale, model, name, notes, creditAmount, creditPaid, wallet, dueDate, tDate, isDelivered, affectBalance)
                transactionToEdit = null
                Toast.makeText(context, "تم حفظ التعديلات!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showGoogleAssistantDialog) {
        GoogleAssistantDialog(
            stats = stats,
            transactionsList = transactions,
            personalDebts = personalDebts,
            onDismiss = { showGoogleAssistantDialog = false }
        )
    }

    if (showAddDebtDialogGlobal) {
        AddPersonalDebtDialog(
            onDismiss = { showAddDebtDialogGlobal = false },
            onSave = { name, amount, isOwedToMe, wallet, notes, dueDate ->
                viewModel.addPersonalDebt(name, amount, isOwedToMe, wallet, notes, dueDate)
                showAddDebtDialogGlobal = false
                Toast.makeText(context, "تمت إضافة الدين بنجاح!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                NavigationBar(
                    containerColor = if (isLiquidTheme) Color.Transparent else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (isLiquidTheme) 0.dp else 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isLiquidTheme) {
                                Modifier
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isDark) Color(0x3D11111A) else Color(0x66FFFFFF)
                                    )
                                    .border(
                                        1.dp,
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.22f),
                                                Color.White.copy(alpha = 0.05f)
                                            )
                                        ),
                                        RoundedCornerShape(20.dp)
                                    )
                            } else Modifier
                        )
                ) {
                    AppTab.values().forEach { tab ->
                        val tabNameStr = Translator.translate(tab.name.lowercase(), appLanguage)
                        if (tab == AppTab.SECTIONS) {
                            NavigationBarItem(
                                selected = false,
                                onClick = { currentTab = AppTab.SECTIONS },
                                alwaysShowLabel = false,
                                icon = { 
                                    Box(modifier = Modifier.size(24.dp))
                                },
                                label = null,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color.Transparent
                                )
                            )
                        } else {
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                alwaysShowLabel = false,
                                icon = { 
                                    Icon(
                                        tab.icon, 
                                        contentDescription = tabNameStr,
                                        modifier = Modifier.size(24.dp)
                                    ) 
                                },
                                label = {
                                    Text(
                                        text = tabNameStr,
                                        fontWeight = if (currentTab == tab) FontWeight.Black else FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { currentTab = AppTab.SECTIONS },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .padding(bottom = 28.dp)
                        .size(56.dp)
                        .testTag("sections_floating_tab_btn")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "الاقسام",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab != AppTab.SETTINGS && currentTab != AppTab.HOME && currentTab != AppTab.TRANSACTIONS && currentTab != AppTab.SECTIONS) {
                if (currentTab == AppTab.DEBTS) {
                   // Floating Action Button for Adding Debt - Far Left
                   FloatingActionButton(
                        onClick = { showAddDebtDialogGlobal = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .testTag("add_debt_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "اضافة دين")
                    }
                } else {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(22.dp),
                        elevation = FloatingActionButtonDefaults.elevation(8.dp),
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .testTag("add_transaction_fab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "➕", modifier = Modifier.size(24.dp))
                            Text(
                                text = Translator.translate("add_transaction", appLanguage).replace(" ➕", ""),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        },
        floatingActionButtonPosition = if (currentTab == AppTab.DEBTS) FabPosition.Start else FabPosition.End
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isLiquidTheme) Modifier else {
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                                )
                            )
                        )
                    }
                )
                .padding(paddingValues)
        ) {
            if (isLiquidTheme) {
                LiquidGlassBlobBackground(isDark = isDark)
            }
            when (currentTab) {
                AppTab.HOME -> {
                    MainDashboardContainer(
                        viewModel = viewModel,
                        stats = stats,
                        transactions = transactions,
                        dateFilter = dateFilter,
                        onDateFilterChanged = { viewModel.setDateFilter(it) },
                        onPopulateSampleData = {
                            populateSampleTransactions(viewModel)
                            Toast.makeText(context, "تم تحميل عينة من بيانات الصيانة!", Toast.LENGTH_SHORT).show()
                        },
                        onClearAll = {
                            viewModel.clearAllTransactions()
                            Toast.makeText(context, "تم تفريغ السجل بالكامل", Toast.LENGTH_SHORT).show()
                        },
                        onOpenGoogleAssistant = { showGoogleAssistantDialog = true },
                        onNavigateToSections = { currentTab = AppTab.SECTIONS },
                        onNavigateToTransactions = { currentTab = AppTab.TRANSACTIONS },
                        onTransactionClicked = { 
                            if (it.category != "DEBT") transactionToEdit = it 
                            else Toast.makeText(context, "يتم تعديل الديون من قسم الديون", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                AppTab.SECTIONS -> {
                    com.example.ui.SectionsScreen(
                        viewModel = viewModel,
                        onAddTransactionForCategory = { catId ->
                            initialCategoryForAdd = catId
                            showAddDialog = true
                        },
                        onTransactionClicked = { 
                            if (it.category != "DEBT") transactionToEdit = it 
                            else Toast.makeText(context, "يتم تعديل الديون من قسم الديون", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                AppTab.TRANSACTIONS -> {
                    TransactionsListScreen(
                        filteredTransactions = filteredTransactions,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        dateFilter = dateFilter,
                        deliveryFilter = deliveryFilter,
                        onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                        onCategorySelected = { viewModel.setSelectedCategory(it) },
                        onDateFilterChanged = { viewModel.setDateFilter(it) },
                        onDeliveryFilterChanged = { viewModel.setDeliveryFilter(it) },
                        onDeleteTransaction = {
                            if (it.category != "DEBT") {
                                viewModel.deleteTransaction(it)
                                Toast.makeText(context, "تم حذف العملية بنجاح", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "يتم حذف الديون من قسم الديون", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onTransactionClicked = { 
                            if (it.category != "DEBT") transactionToEdit = it 
                            else Toast.makeText(context, "يتم تعديل الديون من قسم الديون", Toast.LENGTH_SHORT).show()
                        },
                        onToggleDelivery = { 
                            if (it.category != "DEBT") viewModel.toggleTransactionDelivery(it) 
                        }
                    )
                }
                AppTab.DEBTS -> {
                    DebtsScreen(
                        viewModel = viewModel,
                        personalDebts = personalDebts,
                        transactions = transactions,
                        onAddDebtRequested = { showAddDebtDialogGlobal = true }
                    )
                }
                AppTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onExportBackup = onExportBackup,
                        onImportBackup = onImportBackup
                    )
                }
            }
        }
    }
}

// ======================== STATISTICS MAIN SCREEN WITH TABS ========================

@Composable
fun StatisticsMainScreen(
    viewModel: WorkshopViewModel,
    stats: WorkshopStats,
    transactions: List<WorkshopTransaction>,
    dateFilter: DateFilter,
    onDateFilterChanged: (DateFilter) -> Unit,
    onPopulateSampleData: () -> Unit,
    onClearAll: () -> Unit,
    onCardClicked: (WorkshopTransaction) -> Unit,
    onOpenGoogleAssistant: () -> Unit,
    personalDebts: List<PersonalDebt>,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    appLanguage: String
) {
    var subTab by remember { mutableStateOf(0) }
    
    val headings = listOf(
        if (appLanguage == "ar") "المداخيل والأرباح 📊" else if (appLanguage == "fr") "Finances 📊" else "Finances 📊",
        if (appLanguage == "ar") "إعدادات الرقمنة ⚙️" else if (appLanguage == "fr") "Paramètres ⚙️" else "App Settings ⚙️"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            headings.forEachIndexed { index, text ->
                Tab(
                    selected = subTab == index,
                    onClick = { subTab = index },
                    text = {
                        Text(
                            text = text,
                            fontWeight = if (subTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (subTab) {
                0 -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        stats = stats,
                        transactionsList = transactions,
                        dateFilter = dateFilter,
                        onDateFilterChanged = onDateFilterChanged,
                        onPopulateSampleData = onPopulateSampleData,
                        onClearAll = onClearAll,
                        onCardClicked = onCardClicked,
                        onOpenGoogleAssistant = onOpenGoogleAssistant
                    )
                }
                1 -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onExportBackup = onExportBackup,
                        onImportBackup = onImportBackup
                    )
                }
            }
        }
    }
}

// ======================== DASHBOARD SCREEN ========================

@Composable
fun DashboardScreen(
    viewModel: WorkshopViewModel,
    stats: WorkshopStats,
    transactionsList: List<WorkshopTransaction>,
    dateFilter: DateFilter,
    onDateFilterChanged: (DateFilter) -> Unit,
    onPopulateSampleData: () -> Unit,
    onClearAll: () -> Unit,
    onCardClicked: (WorkshopTransaction) -> Unit,
    onOpenGoogleAssistant: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ورشتي الذكية",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "أرباح الصيانة والمبيعات الفورية 📱",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                // Quick Theme indicator or Reset icon
                IconButton(
                    onClick = onClearAll,
                    modifier = Modifier.clip(CircleShape).background(Color.Red.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = "فرمتة البيانات",
                        tint = Color.Red
                    )
                }
            }
        }

        // Period filter selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DateFilter.values().forEach { filter ->
                        val isSelected = dateFilter == filter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .clickable { onDateFilterChanged(filter) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter.displayNameAr,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Ledger Overview (Primary Balance Card as seen in Masroofati)
        item {
            val netProfitSign = if (stats.totalProfit >= 0) "+" else ""
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                                )
                            )
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "صافي الأرباح للورشة (${dateFilter.displayNameAr})",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$netProfitSign${formatCurrency(stats.totalProfit)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (stats.totalProfit >= 0) ProfitGreen else ExpenseRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "إجمالي العمليات: ${stats.transactionCount} عملية صيانة وبيع",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Income
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(ProfitGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "المجـموع المحصل", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                            Text(
                                text = formatCurrency(stats.totalRevenue),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Parts Cost
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(GeneralBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = GeneralBlue, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "تكاليف القطع", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                            Text(
                                text = formatCurrency(stats.partsCost),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Expenses
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(ExpenseRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "ديون ومصاريف", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                            Text(
                                text = formatCurrency(stats.expensesCost),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 0.5.dp)
                    
                    // Simple Bar Chart
                    SimpleFinancialBarChart(
                        revenue = stats.totalRevenue,
                        partsCost = stats.partsCost,
                        expenses = stats.expensesCost,
                        profit = stats.totalProfit,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }

        // Wallets Section
        item {
            WalletsSection(viewModel, transactionsList, onTransactionClicked = onCardClicked)
        }

        // Customer Credit Stats Overview Card
        val customerCreditTotal = transactionsList.sumOf { it.creditAmount }
        val customerCreditPaid = transactionsList.sumOf { it.creditPaid }
        val customerCreditRemaining = transactionsList.sumOf { it.creditRemaining }

        if (customerCreditTotal > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ديون وكريدي الزبائن 👥", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Badge(containerColor = if (customerCreditRemaining > 0) ExpenseRed.copy(alpha = 0.15f) else ProfitGreen.copy(alpha = 0.15f)) {
                                Text(
                                    text = if (customerCreditRemaining > 0) "غير مستوفى" else "مستوفى بالكامل",
                                    color = if (customerCreditRemaining > 0) ExpenseRed else ProfitGreen,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("إجمالي الكريدي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text(formatCurrency(customerCreditTotal), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("المسترجع منه", fontSize = 11.sp, color = ProfitGreen)
                                Text(formatCurrency(customerCreditPaid), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("الكريدي الباقي", fontSize = 11.sp, color = ExpenseRed)
                                Text(formatCurrency(customerCreditRemaining), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        }
                    }
                }
            }
        }

        // Help tip warning: No pre-registered stock needed
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💡", fontSize = 18.sp)
                    }
                    Text(
                        text = "لا تحتاج لتسجيل مخزن مسبق! بمجرد إتمام تصليح (شاشة، فلاش، غيار...)، سجل التكلفة وسعر البيع واحسب ربحك مباشرة.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Google AI Assistant Magical Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenGoogleAssistant() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.5.dp,
                    Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF4285F4), // Google Blue
                            Color(0xFFEA4335), // Google Red
                            Color(0xFFFBBC05), // Google Yellow
                            Color(0xFF34A853), // Google Green
                            Color(0xFF4285F4)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF4285F4).copy(alpha = 0.2f),
                                        Color(0xFF34A853).copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨🤖", fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مساعد جوجل الذكي للورشة ✨",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "تشخيص ذكي للأعطال بالذكاء الاصطناعي واستشارات مالية فورية لأرباح الورشة والديون بالدارجة الجزائريّة!",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "فتح المساعد",
                        tint = Color(0xFFFBBC05),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Section Title: Latest Transactions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر العمليات المسجلة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (transactionsList.isEmpty()) {
                    TextButton(onClick = onPopulateSampleData) {
                        Text("تحميل عينة توضيحية 📥", fontSize = 12.sp)
                    }
                }
            }
        }

        // List representation
        if (transactionsList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = "السجل فارغ",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لا توجد عمليات مسجلة حالياً.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "اضغط على زر (تسجيل عملية) للتجربة الفورية للورشة!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            // Display first 5 latest transactions on dashboard
            val displayedList = transactionsList.take(5)
            items(displayedList) { transaction ->
                TransactionListItem(
                    transaction = transaction,
                    onClick = { onCardClicked(transaction) },
                    onDelete = {}, // Handled in Transactions tab
                    onToggleDelivery = { viewModel.toggleTransactionDelivery(transaction) }
                )
            }
        }
    }
}

// ======================== DASHBOARD HELPERS ========================

@Composable
fun SimpleFinancialBarChart(
    revenue: Double,
    partsCost: Double,
    expenses: Double,
    profit: Double,
    modifier: Modifier = Modifier
) {
    val totalCosts = partsCost + expenses
    val maxVal = maxOf(revenue, totalCosts, profit).coerceAtLeast(1.0)
    
    val items = listOf(
        Triple("المداخيل", revenue, ProfitGreen),
        Triple("قطع الغيار", partsCost, GeneralBlue),
        Triple("المصاريف", expenses, ExpenseRed),
        Triple("صافي الربح", profit, MaterialTheme.colorScheme.primary)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { (label, value, color) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatCurrency(value),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = color
                    )
                }
                
                val progress = (value / maxVal).coerceIn(0.0, 1.0).toFloat()
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }
        }
    }
}

// ======================== TRANSACTIONS SCREEN ========================

@Composable
fun TransactionsListScreen(
    filteredTransactions: List<WorkshopTransaction>,
    searchQuery: String,
    selectedCategory: String?,
    dateFilter: DateFilter,
    deliveryFilter: com.example.ui.viewmodel.DeliveryFilter,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onDateFilterChanged: (DateFilter) -> Unit,
    onDeliveryFilterChanged: (com.example.ui.viewmodel.DeliveryFilter) -> Unit,
    onDeleteTransaction: (WorkshopTransaction) -> Unit,
    onTransactionClicked: (WorkshopTransaction) -> Unit,
    onToggleDelivery: ((WorkshopTransaction) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("بحث عن قطعة، هاتف أو زبون...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "جلاء")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .testTag("search_field"),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Categories Row
        val categories = listOf(
            CategoryItem("ALL", "الكل", Icons.Default.QrCode, MaterialTheme.colorScheme.primary),
            CategoryItem("SCREEN", "الشاشات", Icons.Default.AspectRatio, ProfitGreen),
            CategoryItem("PARTS", "قطع الغيار", Icons.Default.Build, GeneralBlue),
            CategoryItem("ACCESSORY", "اكسسوارات", Icons.Default.ShoppingBag, AccessoryOrange),
            CategoryItem("SERVICE", "فلاش و FRP", Icons.Default.Memory, SoftwarePurple),
            CategoryItem("EXPENSE", "مصروف", Icons.Default.Payments, Color(0xFFE53935)),
            CategoryItem("REFURB", "استثمار وتدوير", Icons.Default.Sync, Color(0xFF8BC34A)),
            CategoryItem("INVENTORY", "مخزون المحل", Icons.Default.Inventory, Color(0xFF795548)),
            CategoryItem("OTHER", "عام وأخرى", Icons.Default.PhoneAndroid, Color.Gray)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = (selectedCategory ?: "ALL") == cat.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(cat.id) },
                    label = { Text(cat.nameAr, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = cat.nameAr,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) Color.White else cat.color
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = cat.color,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Delivery Status Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "حالة التسليم:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
            
            com.example.ui.viewmodel.DeliveryFilter.values().forEach { filterVal ->
                val isSelected = deliveryFilter == filterVal
                val chipColor = when (filterVal) {
                    com.example.ui.viewmodel.DeliveryFilter.ALL -> MaterialTheme.colorScheme.primary
                    com.example.ui.viewmodel.DeliveryFilter.DELIVERED -> ProfitGreen
                    com.example.ui.viewmodel.DeliveryFilter.NOT_DELIVERED -> AccessoryOrange
                }
                
                FilterChip(
                    selected = isSelected,
                    onClick = { onDeliveryFilterChanged(filterVal) },
                    label = { 
                        Text(
                            text = filterVal.displayNameAr, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Live count
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "العمليات المكتشفة: ${filteredTransactions.size}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            // Date filter tag display
            Text(
                text = "الفترة: ${dateFilter.displayNameAr}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Results LazyColumn
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد نتائج مطابقة للبحث",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "جرب اختصار اسم الهاتف أو تصنيف آخر.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                items(filteredTransactions) { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        onClick = { onTransactionClicked(transaction) },
                        onDelete = { onDeleteTransaction(transaction) },
                        showDeleteOption = false,
                        onToggleDelivery = { onToggleDelivery?.invoke(transaction) }
                    )
                }
            }
        }
    }
}

// ======================== ADD / EDIT DIALOG ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    viewModel: WorkshopViewModel,
    transaction: WorkshopTransaction? = null,
    initialCategory: String? = null,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (
        title: String,
        category: String,
        costPrice: Double,
        sellingPrice: Double,
        deviceModel: String,
        customerName: String,
        notes: String,
        creditAmount: Double,
        creditPaid: Double,
        wallet: String,
        dueDate: Long?,
        transactionDate: Long,
        isDelivered: Boolean,
        affectBalance: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(transaction?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(transaction?.category ?: initialCategory ?: "SCREEN") }
    var costPriceStr by remember { mutableStateOf(transaction?.costPrice?.let { if (it == 0.0) "" else if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var sellingPriceStr by remember { mutableStateOf(transaction?.sellingPrice?.let { if (it == 0.0) "" else if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var deviceModel by remember { mutableStateOf(transaction?.deviceModel ?: "") }
    var customerName by remember { mutableStateOf(transaction?.customerName ?: "") }
    var notes by remember { mutableStateOf(transaction?.notes ?: "") }
    var isDelivered by remember { mutableStateOf(transaction?.isDelivered ?: true) }
    var affectBalance by remember { mutableStateOf(transaction?.affectBalance ?: true) }
    var creditPaidStr by remember { mutableStateOf(transaction?.creditPaid?.let { if (it == 0.0) "" else if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    
    // Auto-update isDelivered based on selectedCategory for new items
    var hasManuallyChosenedStatus by remember { mutableStateOf(false) }
    LaunchedEffect(selectedCategory) {
        if (transaction == null && !hasManuallyChosenedStatus) {
            if (selectedCategory == "INVENTORY" || selectedCategory == "REFURB") {
                isDelivered = false
            } else if (selectedCategory == "EXPENSE") {
                isDelivered = true
            }
        }
    }
    val pocketName by viewModel.walletPocketName.collectAsStateWithLifecycle()
    val bankName by viewModel.walletBankName.collectAsStateWithLifecycle()
    val goodsName by viewModel.walletGoodsName.collectAsStateWithLifecycle()
    val personalName by viewModel.walletPersonalName.collectAsStateWithLifecycle()

    var isCreditSale by remember(transaction) {
        mutableStateOf(transaction?.let { it.creditAmount > 0.0 } ?: false)
    }

    val initialDownPayment = remember(transaction) {
        transaction?.let {
            val dp = it.sellingPrice - it.creditAmount
            if (dp <= 0.0) "" else if (dp % 1.0 == 0.0) dp.toLong().toString() else dp.toString()
        } ?: ""
    }
    var downPaymentStr by remember(initialDownPayment) { mutableStateOf(initialDownPayment) }

    var dueDate by remember { mutableStateOf(transaction?.dueDate) }
    var formattedDueDate by remember(dueDate) { 
        mutableStateOf(
            dueDate?.let {
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = it
                java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(cal.time)
            } ?: ""
        )
    }
    val context = androidx.compose.ui.platform.LocalContext.current

    var transactionDate by remember { mutableStateOf(transaction?.date ?: System.currentTimeMillis()) }
    var formattedTransactionDate by remember(transactionDate) { 
        mutableStateOf(
            java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(transactionDate))
        )
    }

    var selectedWallet by remember(transaction, personalName, pocketName, goodsName) { 
        mutableStateOf(
            transaction?.wallet ?: if (initialCategory == "EXPENSE") personalName else if (initialCategory == "ACCESSORY") goodsName else pocketName
        ) 
    }

    val personalPresets = listOf(
        Triple("مواصلات", "🚌", Color(0xFF00BCD4)),
        Triple("إيجار", "🏠", Color(0xFF3F51B5)),
        Triple("إنترنت", "🌐", Color(0xFF2196F3)),
        Triple("صدقة الأسرة", "🤲", Color(0xFF4CAF50)),
        Triple("فواتير", "🧾", Color(0xFFFF9800)),
        Triple("غذاء", "🍔", Color(0xFFE91E63)),
        Triple("قهوة", "☕", Color(0xFF795548)),
        Triple("تسوق", "🛒", Color(0xFF9C27B0)),
        Triple("صحة وعلاج", "🏥", Color(0xFFF44336)),
        Triple("العناية الشخصية", "✨", Color(0xFFCDDC39)),
        Triple("كوسميتيك", "🧴", Color(0xFFF06292)),
        Triple("حلاقة", "✂️", Color(0xFF009688)),
        Triple("أخرى", "📦", Color(0xFF607D8B))
    )
    var selectedPresetName by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Live balance calculation for each wallet in dialog
    val pocketInit by viewModel.walletPocketInit.collectAsStateWithLifecycle()
    val bankInit by viewModel.walletBankInit.collectAsStateWithLifecycle()
    val goodsInit by viewModel.walletGoodsInit.collectAsStateWithLifecycle()
    val personalInit by viewModel.walletPersonalInit.collectAsStateWithLifecycle()

    val transactionsList by viewModel.transactionsFlow.collectAsStateWithLifecycle()

    val pocketChange = transactionsList.filter { 
        (it.wallet == pocketName || it.wallet == "مصروف الشهر" || it.wallet == "الصندوق (Pocket)" || it.wallet == "محفظة المحل" || it.wallet.isBlank()) 
        && it.category != "ACCESSORY" 
        && it.affectBalance 
    }.sumOf { it.profit }
    val pocketBalance = pocketInit + pocketChange

    val bankChange = transactionsList.filter { 
        (it.wallet == bankName || it.wallet == "حساب بنكي") 
        && it.category != "ACCESSORY" 
        && it.affectBalance 
    }.sumOf { it.profit }
    val bankBalance = bankInit + bankChange

    val goodsChange = transactionsList.filter { 
        it.category == "ACCESSORY" 
        && it.affectBalance 
    }.sumOf { it.profit }
    val goodsBalance = goodsInit + goodsChange

    val personalChange = transactionsList.filter { 
        (it.wallet == personalName || it.wallet == "مصروف شخصي" || it.wallet == "مصروفي شخصي" || it.wallet == "مصروفي الشخصي") 
        && it.category != "ACCESSORY" 
        && it.affectBalance 
    }.sumOf { it.profit }
    val personalBalance = personalInit + personalChange

    val selectedBalance = when {
        selectedWallet == pocketName || selectedWallet == "مصروف الشهر" || selectedWallet == "الصندوق (Pocket)" || selectedWallet.isEmpty() -> pocketBalance
        selectedWallet == bankName || selectedWallet == "حساب بنكي" -> bankBalance
        selectedWallet == goodsName || selectedWallet == "سلعة" -> goodsBalance
        else -> personalBalance
    }

    val isEdit = transaction != null

    // Real-time calculations for expected profit representation
    val costDouble = costPriceStr.toDoubleOrNull() ?: 0.0
    val sellingDouble = sellingPriceStr.toDoubleOrNull() ?: 0.0
    val expectedProfit = sellingDouble - costDouble

    val downPaymentDouble = downPaymentStr.toDoubleOrNull() ?: 0.0
    val calculatedCreditAmount = if (isCreditSale) {
        if (sellingDouble > downPaymentDouble) sellingDouble - downPaymentDouble else 0.0
    } else {
        0.0
    }
    val creditPaidDouble = creditPaidStr.toDoubleOrNull() ?: 0.0
    val creditRemainingDouble = if (calculatedCreditAmount > creditPaidDouble) calculatedCreditAmount - creditPaidDouble else 0.0

    val categories = listOf(
        CategoryItem("SCREEN", "صيانة شاشة", Icons.Default.AspectRatio, ProfitGreen),
        CategoryItem("PARTS", "قطع غيار داخلية", Icons.Default.Build, GeneralBlue),
        CategoryItem("ACCESSORY", "بيع اكسسوار", Icons.Default.ShoppingBag, AccessoryOrange),
        CategoryItem("SERVICE", "سوفتوير وفلاش", Icons.Default.Memory, SoftwarePurple),
        CategoryItem("EXPENSE", "مصروف / نفقة", Icons.Default.Payments, Color(0xFFE53935)),
        CategoryItem("REFURB", "استثمار وتدوير", Icons.Default.Sync, Color(0xFF8BC34A)),
        CategoryItem("INVENTORY", "شراء مخزون (شاشة، الخ)", Icons.Default.Inventory, Color(0xFF795548)),
        CategoryItem("OTHER", "صيانة وأخرى", Icons.Default.PhoneAndroid, Color.Gray)
    )

    val performSave = {
        val finalCost = costPriceStr.toDoubleOrNull()
        val finalSelling = sellingPriceStr.toDoubleOrNull()
        
        val isExpenseType = selectedCategory == "EXPENSE"
        if (title.isBlank() && selectedCategory != "INVENTORY") {
            validationError = "يرجى كتابة اسم العملية أو القطعة بوضوح ⚠️"
        } else if (isExpenseType && (finalCost == null || finalCost <= 0)) {
            validationError = "يرجى إدخال المبلغ بشكل صحيح ⚠️"
        } else if (selectedCategory == "INVENTORY" && finalCost == null) {
            validationError = "يرجى إدخال سعر الشراء للمخزون ⚠️"
        } else if (!isExpenseType && finalSelling == null) {
            validationError = "يرجى إدخال سعر البيع للتأكد من حساباتك ⚠️"
        } else if (isCreditSale && (downPaymentStr.toDoubleOrNull() ?: 0.0) > (finalSelling ?: 0.0)) {
            validationError = "المبلغ المقدم لا يمكن أن يكون أكبر من سعر البيع ⚠️"
        } else {
            validationError = null // Clear any errors

            var finalTitle = title
            if (finalTitle.isBlank()) {
                finalTitle = when (selectedCategory) {
                    "SCREEN" -> "شاشة صيانة"
                    "PARTS" -> "صيانة غيار هاتف"
                    "ACCESSORY" -> "بيع ملحقات"
                    "SERVICE" -> "فلاش / FRP"
                    "EXPENSE" -> "مصروف / نفقة"
                    "INVENTORY" -> "شراء مخزون"
                    else -> "صيانة عامة"
                }
            }
            val finalCategory = selectedCategory
            val costValue = finalCost ?: 0.0
            val sellingValue = if (isExpenseType) 0.0 else (finalSelling ?: 0.0)
            val finalModel = if (isExpenseType) "" else deviceModel
            val finalCustomer = if (isExpenseType) "" else customerName
            val finalCreditAmount = if (isExpenseType) 0.0 else calculatedCreditAmount
            val finalCreditPaid = if (isExpenseType) 0.0 else creditPaidDouble

            onSave(
                finalTitle,
                finalCategory,
                costValue,
                sellingValue,
                finalModel,
                finalCustomer,
                notes,
                finalCreditAmount,
                finalCreditPaid,
                selectedWallet,
                dueDate,
                transactionDate,
                isDelivered,
                affectBalance
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(top = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "اغلاق")
                    }
                    Text(
                        text = if (isEdit) "تعديل العملية" else "تسجيل صيانة/مبيعات جديدة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = performSave,
                        modifier = Modifier
                            .background(ProfitGreen.copy(alpha = 0.15f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "اتمام وتسجيل العملية",
                            tint = ProfitGreen
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.wrapContentHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category Selection Grid
                    item {
                        Text(
                            text = "اختر التصنيف المباشر للعملية:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = selectedCategory == cat.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) cat.color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) cat.color.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { 
                                            selectedCategory = cat.id 
                                            if (cat.id == "EXPENSE") {
                                                selectedWallet = personalName
                                            } else if (cat.id == "ACCESSORY") {
                                                selectedWallet = goodsName
                                            } else {
                                                if (selectedWallet == personalName || selectedWallet == "مصروف شخصي") {
                                                    selectedWallet = pocketName
                                                }
                                            }
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            cat.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else cat.color,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = cat.nameAr.replace(" ", "\n"),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Personal Presets list if selectedCategory == "EXPENSE"
                    if (selectedCategory == "EXPENSE") {
                        item {
                            Text(
                                text = "اختر نوع المصروف تلقائياً للتحميل السريع ⚡:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            personalPresets.chunked(2).forEach { rowPresets ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowPresets.forEach { (name, emoji, color) ->
                                        val isSelected = selectedPresetName == name
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(20.dp))
                                                .clickable {
                                                    selectedPresetName = name
                                                    title = "$emoji $name"
                                                    selectedCategory = "EXPENSE"
                                                },
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                            ),
                                            color = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp).copy(alpha = 0.5f),
                                            tonalElevation = if (isSelected) 0.dp else 2.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(color.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = emoji, fontSize = 16.sp)
                                                }
                                                Text(
                                                    text = name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                                )
                                            }
                                        }
                                    }
                                    if (rowPresets.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Title
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { 
                                title = it
                                selectedPresetName = null
                            },
                            label = { Text(if (selectedCategory == "EXPENSE") "نوع أو تفاصيل المصروف" else if (selectedCategory == "INVENTORY") "اسم القطعة أو المنتوج" else "اسم العملية أو القطعة (مثلاً: أفيشار Oppo A3s)") },
                            modifier = Modifier.fillMaxWidth().testTag("title_field"),
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    }

                    // Transaction Date/Time Selection
                    item {
                        OutlinedButton(
                            onClick = {
                                val cal = java.util.Calendar.getInstance()
                                cal.timeInMillis = transactionDate
                                android.app.DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        cal.set(java.util.Calendar.YEAR, y)
                                        cal.set(java.util.Calendar.MONTH, m)
                                        cal.set(java.util.Calendar.DAY_OF_MONTH, d)
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, h, min ->
                                                cal.set(java.util.Calendar.HOUR_OF_DAY, h)
                                                cal.set(java.util.Calendar.MINUTE, min)
                                                transactionDate = cal.timeInMillis
                                            },
                                            cal.get(java.util.Calendar.HOUR_OF_DAY),
                                            cal.get(java.util.Calendar.MINUTE),
                                            true
                                        ).show()
                                    },
                                    cal.get(java.util.Calendar.YEAR),
                                    cal.get(java.util.Calendar.MONTH),
                                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("تاريخ ووقت العملية", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(formattedTransactionDate, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (selectedCategory != "EXPENSE") {
                        // Model and client name
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = deviceModel,
                                    onValueChange = { deviceModel = it },
                                    label = { Text("موديل الهاتف") },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )
                                OutlinedTextField(
                                    value = customerName,
                                    onValueChange = { customerName = it },
                                    label = { Text("اسم الزبون") },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )
                            }
                        }
                    }

                    // Pricing
                    item {
                        if (selectedCategory == "EXPENSE") {
                            // Single spending amount field
                            OutlinedTextField(
                                value = costPriceStr,
                                onValueChange = { costPriceStr = it },
                                label = { Text("مبلغ المصروف المخصوم (د.ج) 💸") },
                                placeholder = { Text("0") },
                                modifier = Modifier.fillMaxWidth().testTag("cost_field"),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                maxLines = 1,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF7B1FA2),
                                    focusedLabelColor = Color(0xFF7B1FA2)
                                )
                            )
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Cost Price
                                OutlinedTextField(
                                    value = costPriceStr,
                                    onValueChange = { costPriceStr = it },
                                    label = { Text("سعر الشراء / التكلفة (د.ج)") },
                                    placeholder = { Text("0") },
                                    modifier = Modifier.weight(1f).testTag("cost_field"),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    maxLines = 1,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CrimsonCost,
                                        focusedLabelColor = CrimsonCost
                                    )
                                )

                                // Selling / Service Price
                                OutlinedTextField(
                                    value = sellingPriceStr,
                                    onValueChange = { sellingPriceStr = it },
                                    label = { Text("سعر البيع / المقبوض (د.ج)") },
                                    placeholder = { Text("0") },
                                    modifier = Modifier.weight(1f).testTag("selling_field"),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    maxLines = 1,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        focusedLabelColor = EmeraldPrimary
                                    )
                                )
                            }
                        }
                    }

                    if (selectedCategory != "EXPENSE") {
                        // Real-time interactive profit card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (expectedProfit >= 0) ProfitGreen.copy(alpha = 0.08f) else ExpenseRed.copy(alpha = 0.08f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (expectedProfit >= 0) "الربح الفوري المتوقع للعملية:" else "الخسارة المتوقعة:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = formatCurrency(expectedProfit),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (expectedProfit >= 0) ProfitGreen else ExpenseRed
                                    )
                                }
                            }
                        }

                        // Customer Credit / Debt section
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "بيع بالكريدي (دين للزبون)؟",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Switch(
                                            checked = isCreditSale,
                                            onCheckedChange = { isCreditSale = it }
                                        )
                                    }

                                    if (isCreditSale) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                        // Explanatory note matching user's exact query
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = "💡 مثال توضيحي: إذا شريت شاشة بـ 200 د.ج وبعتها بـ 400 د.ج وأعطاك العميل 150 د.ج، اكتب 150 في خانة المبلغ المقدم، وسيقوم التطبيق تلقائياً بحساب الكريدي المتبقي للزبون وهو 250 د.ج.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                lineHeight = 13.sp,
                                                textAlign = TextAlign.Start
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = downPaymentStr,
                                                onValueChange = { downPaymentStr = it },
                                                label = { Text("المبلغ المقدم (دفع الآن)") },
                                                placeholder = { Text("0") },
                                                modifier = Modifier.weight(1f).testTag("dialog_down_payment"),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Number,
                                                    imeAction = ImeAction.Next
                                                ),
                                                maxLines = 1,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = ProfitGreen,
                                                    focusedLabelColor = ProfitGreen
                                                )
                                            )

                                            OutlinedTextField(
                                                value = creditPaidStr,
                                                onValueChange = { creditPaidStr = it },
                                                label = { Text("ما استُرجع وسُدد لاحقاً") },
                                                placeholder = { Text("0") },
                                                modifier = Modifier.weight(1f).testTag("dialog_credit_paid"),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Number,
                                                    imeAction = ImeAction.Next
                                                ),
                                                maxLines = 1,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = GeneralBlue,
                                                    focusedLabelColor = GeneralBlue
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        OutlinedButton(
                                            onClick = {
                                                val cal = java.util.Calendar.getInstance()
                                                if (dueDate != null) {
                                                    cal.timeInMillis = dueDate!!
                                                }
                                                android.app.DatePickerDialog(
                                                    context,
                                                    { _, y, m, d ->
                                                        cal.set(java.util.Calendar.YEAR, y)
                                                        cal.set(java.util.Calendar.MONTH, m)
                                                        cal.set(java.util.Calendar.DAY_OF_MONTH, d)
                                                        android.app.TimePickerDialog(
                                                            context,
                                                            { _, h, min ->
                                                                cal.set(java.util.Calendar.HOUR_OF_DAY, h)
                                                                cal.set(java.util.Calendar.MINUTE, min)
                                                                dueDate = cal.timeInMillis
                                                                formattedDueDate = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(cal.time)
                                                            },
                                                            cal.get(java.util.Calendar.HOUR_OF_DAY),
                                                            cal.get(java.util.Calendar.MINUTE),
                                                            true
                                                        ).show()
                                                    },
                                                    cal.get(java.util.Calendar.YEAR),
                                                    cal.get(java.util.Calendar.MONTH),
                                                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                                                ).show()
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                                Text(
                                                    if (dueDate == null) "تحديد وقت وتاريخ التنبيه والإشعار..." else "تنبيه في: $formattedDueDate",
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        if (dueDate != null) {
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                                TextButton(onClick = { dueDate = null; formattedDueDate = "" }) {
                                                    Text("إلغاء التنبيه", color = Color.Red.copy(alpha = 0.8f))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Detailed breakdown calculations
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("سعر البيع الإجمالي:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                Text(formatCurrency(sellingDouble), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("ناقص المبلغ المقدم (المدفوع حالياً):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                Text("- ${formatCurrency(downPaymentDouble)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("إجمالي الكريدي (الباقي المسجل على الزبون):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                Text(formatCurrency(calculatedCreditAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccessoryOrange)
                                            }
                                            if (creditPaidDouble > 0) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("ناقص المبالغ المسددة لاحقاً:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                    Text("- ${formatCurrency(creditPaidDouble)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GeneralBlue)
                                                }
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("الكريدي المتبقي على الزبون حالياً:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = formatCurrency(creditRemainingDouble),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (creditRemainingDouble > 0) ExpenseRed else ProfitGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Wallet selection block
                    item {
                        var isWalletDropdownExpanded by remember { mutableStateOf(false) }
                        val (selectedIcon, selectedColor) = when {
                            selectedWallet == pocketName || selectedWallet == "مصروف الشهر" || selectedWallet == "الصندوق (Pocket)" || selectedWallet.isEmpty() -> 
                                Pair(Icons.Default.AccountBalanceWallet, Color(0xFF2E7D32))
                            selectedWallet == bankName || selectedWallet == "حساب بنكي" -> 
                                Pair(Icons.Default.CreditCard, Color(0xFF1565C0))
                            selectedWallet == goodsName || selectedWallet == "سلعة" -> 
                                Pair(Icons.Default.ShoppingBag, Color(0xFFD84315))
                            else -> Pair(Icons.Default.Person, Color(0xFF7B1FA2))
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "المحفظة المرتبطة بالعملية 💳",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { isWalletDropdownExpanded = true },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left/Start side: Dropdown arrow
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "اختر المحفظة",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        
                                        // Right/End side: Selected Wallet Details (RTL representation)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Text Column: Wallet Name & Balance
                                            Column(
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text(
                                                    text = selectedWallet,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = formatCurrency(selectedBalance),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = selectedColor
                                                )
                                            }
                                            
                                            // Circle Badge with Icon
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(selectedColor.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = selectedIcon,
                                                    contentDescription = null,
                                                    tint = selectedColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Dropdown Menu matching width of card
                                DropdownMenu(
                                    expanded = isWalletDropdownExpanded,
                                    onDismissRequest = { isWalletDropdownExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    val walletOptions = listOf(
                                        Triple(pocketName, Icons.Default.AccountBalanceWallet, Pair(Color(0xFF2E7D32), pocketBalance)),
                                        Triple(bankName, Icons.Default.CreditCard, Pair(Color(0xFF1565C0), bankBalance)),
                                        Triple(goodsName, Icons.Default.ShoppingBag, Pair(if (goodsBalance < 0.0) Color(0xFFC62828) else Color(0xFF2E7D32), goodsBalance)),
                                        Triple(personalName, Icons.Default.Person, Pair(Color(0xFF7B1FA2), personalBalance))
                                    )
                                    
                                    walletOptions.forEach { (name, icon, details) ->
                                        val (color, balance) = details
                                        DropdownMenuItem(
                                            onClick = {
                                                selectedWallet = name
                                                isWalletDropdownExpanded = false
                                                if (name == personalName || name == "مصروف شخصي") {
                                                    selectedCategory = "EXPENSE"
                                                } else {
                                                    if (selectedCategory == "EXPENSE") {
                                                        selectedCategory = "OTHER"
                                                    }
                                                    // INVENTORY can stay INVENTORY even if wallet changes
                                                }
                                            },
                                            text = {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Start/Left: Balance
                                                    Text(
                                                        text = formatCurrency(balance),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = color
                                                    )
                                                    
                                                    // End/Right: Name and Icon
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Text(
                                                            text = name,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape)
                                                                .background(color.copy(alpha = 0.1f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = icon,
                                                                contentDescription = null,
                                                                tint = color,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = if (!affectBalance) selectedColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "عدم التأثير على الرصيد 💸",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (!affectBalance) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    Switch(
                                        checked = !affectBalance,
                                        onCheckedChange = { isChecked -> affectBalance = !isChecked }
                                    )
                                }
                            }
                        }
                    }

                    // Delivery Status (حالة التسليم)
                    if (selectedCategory != "EXPENSE") {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = if (selectedCategory == "INVENTORY" || selectedCategory == "REFURB") "حالة البيع والتسليم 📦" else "حالة التسليم 📦",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Delivered chip
                                        FilterChip(
                                            selected = isDelivered,
                                            onClick = { 
                                                isDelivered = true 
                                                hasManuallyChosenedStatus = true
                                            },
                                            leadingIcon = {
                                                if (isDelivered) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            },
                                            label = { Text(if (selectedCategory == "INVENTORY" || selectedCategory == "REFURB") "تم البيع والتسليم ✅" else "تم التسليم (ليس في الورشه) ✅", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                            modifier = Modifier.weight(1f),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ProfitGreen,
                                                selectedLabelColor = Color.White,
                                                selectedLeadingIconColor = Color.White
                                            )
                                        )
                                        
                                        // Not Delivered chip
                                        FilterChip(
                                            selected = !isDelivered,
                                            onClick = { 
                                                isDelivered = false 
                                                hasManuallyChosenedStatus = true
                                            },
                                            leadingIcon = {
                                                if (!isDelivered) {
                                                    Icon(
                                                        imageVector = if (selectedCategory == "INVENTORY" || selectedCategory == "REFURB") Icons.Default.Inventory else Icons.Default.Build,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            },
                                            label = { Text(if (selectedCategory == "INVENTORY" || selectedCategory == "REFURB") "في المخزن / لم تباع 📦" else "لم يتم التسليم (في الورشه) 🛠️", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                            modifier = Modifier.weight(1f),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AccessoryOrange,
                                                selectedLabelColor = Color.White,
                                                selectedLeadingIconColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Notes
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text(if (selectedCategory == "EXPENSE") "ملاحظات وتفاصيل المصروف ✏️" else if (selectedCategory == "INVENTORY") "ملاحظات حول القطعة (مثلاً: من أين تم شراؤها)" else "ملاحظات (مثل: عيب الكارت مار، الزبون يخلص السبت...)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }

                    // Actions Button
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (validationError != null) {
                            Text(
                                text = validationError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }


                    }

                    if (isEdit && onDelete != null) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val deleteInteractionSource = remember { MutableInteractionSource() }
                            val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
                            val deleteScale by animateFloatAsState(targetValue = if (isDeletePressed) 0.95f else 1f, label = "DeleteBtnScale")
                            
                            var showConfirmDelete by remember { mutableStateOf(false) }
                            
                            if (showConfirmDelete) {
                                AlertDialog(
                                    onDismissRequest = { showConfirmDelete = false },
                                    title = { Text("تأكيد الحذف ⚠️") },
                                    text = { Text("هل أنت متأكد من رغبتك في حذف هذه العملية بشكل نهائي؟ لا يمكن التراجع عن هذا الإجراء.") },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                showConfirmDelete = false
                                                onDelete()
                                            }
                                        ) {
                                            Text("نعم، احذف 🗑️", color = Color.Red, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showConfirmDelete = false }) {
                                            Text("إلغاء")
                                        }
                                    }
                                )
                            }
                            
                            OutlinedButton(
                                onClick = { showConfirmDelete = true },
                                interactionSource = deleteInteractionSource,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .scale(deleteScale)
                                    .testTag("delete_transaction_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Red
                                ),
                                border = BorderStroke(1.2.dp, Color.Red.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف العملية",
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.Red
                                    )
                                    Text(
                                        text = "حذف هذه العملية نهائياً 🗑️",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Prepopulate the Database with real illustrative examples of a mobile shop
private fun populateSampleTransactions(viewModel: WorkshopViewModel) {
    viewModel.addTransaction(
        title = "أفيشار Redmi 9A أصلي",
        category = "SCREEN",
        costPrice = 1800.0,
        sellingPrice = 3000.0,
        deviceModel = "Redmi 9a",
        customerName = "عماد",
        notes = "تركيب ممتاز مع حماية مجانية"
    )
    viewModel.addTransaction(
        title = "تخطي حماية حساب Google (FRP)",
        category = "SERVICE",
        costPrice = 0.0, // purely software service - zero cost!
        sellingPrice = 1200.0,
        deviceModel = "Oppo A1k",
        customerName = "يونس",
        notes = "خدمة برمجية سريعة"
    )
    viewModel.addTransaction(
        title = "تغيير شاحن كونيكتور Type-C",
        category = "PARTS",
        costPrice = 150.0,
        sellingPrice = 800.0,
        deviceModel = "Samsung A32",
        customerName = "مراد",
        notes = "تلحيم أصلي"
    )
    viewModel.addTransaction(
        title = "بيع كابل سريع شاحن Infinix 33W",
        category = "ACCESSORY",
        costPrice = 350.0,
        sellingPrice = 900.0,
        deviceModel = "",
        customerName = "زبون عابر",
        notes = "اكسسوارات ممتازة"
    )
    viewModel.addTransaction(
        title = "فلاش هاتف معلق على اللوكو",
        category = "SERVICE",
        costPrice = 0.0,
        sellingPrice = 1500.0,
        deviceModel = "Condor Griffe T9",
        customerName = "أمين",
        notes = "فلاش سوفتوير كامل"
    )
    viewModel.addTransaction(
        title = "بطارية هاتف آيفون X نسبة 100%",
        category = "PARTS",
        costPrice = 1200.0,
        sellingPrice = 2800.0,
        deviceModel = "iPhone X",
        customerName = "سارة",
        notes = "ضمان شهر كامل"
    )
}

// ======================== DEBTS & CREDITS SCREEN ========================

data class DebtorCustomer(
    val name: String,
    val totalCreditAmount: Double,
    val totalPaid: Double,
    val totalRemaining: Double,
    val transactions: List<WorkshopTransaction>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    viewModel: WorkshopViewModel,
    personalDebts: List<PersonalDebt>,
    transactions: List<WorkshopTransaction>,
    onAddDebtRequested: () -> Unit
) {
    val installments by viewModel.installmentsFlow.collectAsStateWithLifecycle()
    var selectedTransactionToCollect by remember { mutableStateOf<WorkshopTransaction?>(null) }
    var selectedPersonalDebtForInstallments by remember { mutableStateOf<PersonalDebt?>(null) }
    var showPersonalDebtsPaidFilter by remember { mutableStateOf(true) } // toggle to view paid personal debts
    var showWorkshopDebtsExplanation by remember { mutableStateOf(false) }
    var selectedListTab by remember { mutableStateOf(0) } // 0: Customer Credits Log, 1: Debtors Directory, 2: Personal Debts Log
    var debtorSearchQuery by remember { mutableStateOf("") }
    val expandedDebtors = remember { mutableStateMapOf<String, Boolean>() }
    
    // Customer Credits Stats
    val customerCredits = transactions.filter { it.creditAmount > 0 }
    val totalCustomerCredit = customerCredits.sumOf { it.creditAmount }
    val totalCustomerPaid = customerCredits.sumOf { it.creditPaid }
    val totalCustomerRemaining = customerCredits.sumOf { it.creditRemaining }

    // Personal Debts Stats
    val unpaidPersonalDebts = personalDebts.filter { !it.isPaid }
    val totalOwedToMe = unpaidPersonalDebts.filter { it.isOwedToMe }.sumOf { it.amount }
    val totalOwedByMe = unpaidPersonalDebts.filter { !it.isOwedToMe }.sumOf { it.amount }

    val totalPersonalOwedToMeAllTime = personalDebts.filter { it.isOwedToMe }.sumOf { it.amount }
    val totalPersonalOwedByMeAllTime = personalDebts.filter { !it.isOwedToMe }.sumOf { it.amount }

    val totalPersonalOwedToMeReceived = personalDebts.filter { it.isOwedToMe }.sumOf { debt ->
        if (debt.isPaid) {
            debt.amount
        } else {
            installments.filter { it.refId == debt.id && it.refType == "PERSONAL_DEBT" }.sumOf { it.amountPaid }
        }
    }

    val totalPersonalOwedByMePaid = personalDebts.filter { !it.isOwedToMe }.sumOf { debt ->
        if (debt.isPaid) {
            debt.amount
        } else {
            installments.filter { it.refId == debt.id && it.refType == "PERSONAL_DEBT" }.sumOf { it.amountPaid }
        }
    }

    val context = LocalContext.current

    // Dialog: Collect Customer Credit Payment
    if (selectedTransactionToCollect != null) {
        CollectCustomerCreditDialog(
            transaction = selectedTransactionToCollect!!,
            installments = installments,
            onDismiss = { selectedTransactionToCollect = null },
            onSave = { addPayment, paymentNote ->
                val newPaid = selectedTransactionToCollect!!.creditPaid + addPayment
                // Update transaction in-place to preserve original ID
                viewModel.updateTransaction(selectedTransactionToCollect!!.copy(creditPaid = newPaid))
                
                // Add installment payment record
                viewModel.addInstallment(
                    refId = selectedTransactionToCollect!!.id,
                    refType = "TRANSACTION",
                    amountPaid = addPayment,
                    notes = paymentNote.ifBlank { "تسديد جزء من الكريدي" }
                )
                selectedTransactionToCollect = null
                Toast.makeText(context, "تم تسجيل الدفعة وتحديث الكريدي!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog: Manage Personal Debt Installments
    if (selectedPersonalDebtForInstallments != null) {
        PersonalDebtInstallmentsDialog(
            debt = selectedPersonalDebtForInstallments!!,
            installments = installments,
            onDismiss = { selectedPersonalDebtForInstallments = null },
            onTogglePaid = {
                viewModel.togglePersonalDebtPaid(selectedPersonalDebtForInstallments!!)
                selectedPersonalDebtForInstallments = null
            },
            onSaveNewPayment = { amountPaid, paymentNote ->
                // Add installment record
                viewModel.addInstallment(
                    refId = selectedPersonalDebtForInstallments!!.id,
                    refType = "PERSONAL_DEBT",
                    amountPaid = amountPaid,
                    notes = paymentNote.ifBlank { "تسديد جزء من الدين" }
                )
                
                // Auto-set paid true if total reaches/exceeds original debt amount
                val debtId = selectedPersonalDebtForInstallments!!.id
                val existingPaid = installments.filter { it.refId == debtId && it.refType == "PERSONAL_DEBT" }.sumOf { it.amountPaid }
                val totalPaidNow = existingPaid + amountPaid
                if (totalPaidNow >= selectedPersonalDebtForInstallments!!.amount) {
                    viewModel.updateDebt(selectedPersonalDebtForInstallments!!.copy(isPaid = true))
                    Toast.makeText(context, "تم تسجيل الدفعة وسداد كامل الدين تلقائياً! 🎉", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "تم تسجيل قسط الدين بنجاح! 💸", Toast.LENGTH_SHORT).show()
                }
                selectedPersonalDebtForInstallments = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Screen Header
        item {
            Column {
                Text(
                    text = "إدارة الديون والكريدي 💳",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "تتبع كريدي الزبائن وديونك الشخصية (دائن ومدين) في مكان واحد.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Custom Tab Selection
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val debtTabs = listOf(
                    Triple(0, "ديون الورشة 💼", "شراء وصيانة بالكريدي من الزبائن"),
                    Triple(1, "دليل المدينين 📢", "تجميع حسب اسم العميل وتذكيره"),
                    Triple(2, "الديون الشخصية 👤", "قروض وسلفيات مالية شخصية")
                )
                debtTabs.forEach { (tabIdx, label, desc) ->
                    val isSelected = selectedListTab == tabIdx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { selectedListTab = tabIdx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Section 1: Customer Credit Stats Cards
        if (selectedListTab == 0 || selectedListTab == 1) {
            item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showWorkshopDebtsExplanation = !showWorkshopDebtsExplanation }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ديون الورشة (كريدي الزبائن) 💼",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Text(
                                text = if (showWorkshopDebtsExplanation) "إغلاق الشرح ❌" else "شرح التفاصيل ❓",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = if (showWorkshopDebtsExplanation) Icons.Default.Close else Icons.Default.HelpOutline,
                                contentDescription = "Show explanation",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showWorkshopDebtsExplanation,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "💡 دليل تفاصيل ديون الورشة والكريدي:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // 1. مجموع ديون الورشة
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                Column {
                                    Text(
                                        text = "مجموع ديون الورشة:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "يمثل كامل المستحقات والكريدي الإجمالي الذي سجلته على الزبائن مقابل مبيعات الأجهزة، أو عمليات الصيانة والقطع.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. الديون المستلمة
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "•", fontWeight = FontWeight.Bold, color = ProfitGreen, fontSize = 14.sp)
                                Column {
                                    Text(
                                        text = "الديون المستلمة للورشة ✅:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfitGreen
                                    )
                                    Text(
                                        text = "هي الأموال والدفعات التي قام الزبائن بتسديدها بالفعل (كاش أو بالتقسيط) وتم صبها في رصيد الورشة.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. الديون المتبقية (اللي راهي برا)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "•", fontWeight = FontWeight.Bold, color = ExpenseRed, fontSize = 14.sp)
                                Column {
                                    Text(
                                        text = "الديون المتبقية حالياً (اللي راهي برا مازال ما خلصونيش) ⏳:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                    Text(
                                        text = "هي باقي الكريدي المعلق الذي ما زال في ذمة الزبائن خارج الورشة وتنتظر استلامه منهم.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مجموع الكريدي 💰",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCurrency(totalCustomerCredit),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مبالغ مستلمة ✅",
                                fontSize = 11.sp,
                                color = ProfitGreen,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCurrency(totalCustomerPaid),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen,
                                maxLines = 1
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                text = "متبقي برا ⏳",
                                fontSize = 11.sp,
                                color = ExpenseRed,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCurrency(totalCustomerRemaining),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
        }

        // Section 2: Personal Debts Summary Cards
        if (selectedListTab == 2) {
            item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الديون الشخصية وقروض السَّلَف (خارج الورشة) 👤",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = { onAddDebtRequested() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Text("إضافة دين/سلفة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Column A: الديون الشخصية لنا (ليّنا)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ProfitGreen.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(ProfitGreen, CircleShape)
                            )
                            Text(
                                text = "ديون شخصية لنا (أموال نقرضها للآخرين):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("مجموع الديون الشخصية", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(formatCurrency(totalPersonalOwedToMeAllTime), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("المبالغ المستلمة ✅", fontSize = 10.sp, color = ProfitGreen, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(formatCurrency(totalPersonalOwedToMeReceived), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("المتبقي المطلوب ⏳", fontSize = 10.sp, color = ExpenseRed, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(formatCurrency(totalOwedToMe), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Column B: الديون الشخصية علينا (عليّنا)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ExpenseRed.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(ExpenseRed, CircleShape)
                            )
                            Text(
                                text = "ديون شخصية علينا (أموال نقترضها من الآخرين):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("مجموع الديون الشخصية", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(formatCurrency(totalPersonalOwedByMeAllTime), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("المبالغ المدفوعة ✅", fontSize = 10.sp, color = ProfitGreen, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(formatCurrency(totalPersonalOwedByMePaid), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("المتبقي للسداد ⏳", fontSize = 10.sp, color = ExpenseRed, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(formatCurrency(totalOwedByMe), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        }
                    }
                }
            }
        }
        }

        if (selectedListTab == 0) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        text = "ديون الورشة (المبيعات بالكريدي للزبائن):",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "هذا القسم يمثل السجلات الناتجة عن مبيعات السلع، صيانة الأجهزة، والقطع التي لم يسدد الزبائن ثمنها بالكامل (كريدي معلق).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // List of Customer Credits (paid and unpaid)
            val sortedCredits = customerCredits.sortedBy { it.creditRemaining <= 0 }
            if (sortedCredits.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "لا توجد مبيعات ولا كريدي نشط غير مدفوع على الزبائن! 🎉",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(sortedCredits) { tx ->
                    val isPaid = tx.creditRemaining <= 0
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isPaid) 0.dp else 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.0f)) {
                                    Text(
                                        text = tx.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        style = if (isPaid) TextStyle(textDecoration = TextDecoration.LineThrough) else TextStyle.Default,
                                        color = if (isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "الزبون: ${tx.customerName.ifEmpty { "غير محدد" }} • ${tx.deviceModel.ifEmpty { "بدون موديل" }}",
                                        fontSize = 11.sp,
                                        style = if (isPaid) TextStyle(textDecoration = TextDecoration.LineThrough) else TextStyle.Default,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                if (!isPaid) {
                                    Button(
                                        onClick = { selectedTransactionToCollect = tx },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("دفع قسط 💵", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Gray.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text("مكتمل ✅", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isPaid) 0.02f else 0.05f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "الكريدي: ${formatCurrencyNoSymbol(tx.creditAmount)}",
                                        fontSize = 11.sp,
                                        color = if (isPaid) Color.Gray.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "مقبوض: ${formatCurrencyNoSymbol(tx.creditPaid)}",
                                        fontSize = 11.sp,
                                        color = if (isPaid) Color.Gray.copy(alpha = 0.6f) else ProfitGreen
                                    )
                                }
                                Text(
                                    text = "الباقي: ${formatCurrency(tx.creditRemaining)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPaid) Color.Gray.copy(alpha = 0.5f) else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        } else if (selectedListTab == 1) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "دليل العملاء المدينين والتنبيهات 📢",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "تجميع كافة الديون المعلقة على كل عميل مع إمكانية إرسال رسالة تذكير مخصصة لها.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Search text field
                    OutlinedTextField(
                        value = debtorSearchQuery,
                        onValueChange = { debtorSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("بحث باسم العميل المدين...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (debtorSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { debtorSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح وبحث جديد", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            val debtorCustomers = customerCredits
                .filter { it.customerName.isNotBlank() }
                .groupBy { it.customerName.trim().lowercase() }
                .map { (_, txs) ->
                    val rawName = txs.firstOrNull()?.customerName?.trim() ?: ""
                    DebtorCustomer(
                        name = rawName,
                        totalCreditAmount = txs.sumOf { it.creditAmount },
                        totalPaid = txs.sumOf { it.creditPaid },
                        totalRemaining = txs.sumOf { it.creditRemaining },
                        transactions = txs.sortedBy { it.creditRemaining <= 0 }
                    )
                }
                .sortedWith(compareBy({ it.totalRemaining <= 0 }, { -it.totalRemaining }))

            val filteredDebtors = if (debtorSearchQuery.isBlank()) {
                debtorCustomers
            } else {
                debtorCustomers.filter { it.name.contains(debtorSearchQuery.trim(), ignoreCase = true) }
            }

            if (filteredDebtors.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (debtorSearchQuery.isNotBlank()) "لم يتم العثور على أي عملاء يطابقون البحث! 🔍"
                                       else "لا يوجد عملاء لديهم ديون معلقة حالياً! 🎉",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredDebtors) { debtor ->
                    val isExpanded = expandedDebtors[debtor.name] ?: false
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedDebtors[debtor.name] = !isExpanded },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Row 1: Name and Expand Icon, total amount
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = debtor.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = debtor.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "لديه ${debtor.transactions.size} معلقات بالدين",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatCurrency(debtor.totalRemaining),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = ExpenseRed
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = if (isExpanded) "إخفاء التفاصيل" else "عرض التفاصيل",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Stats breakdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("المجموع الأصلي", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(formatCurrency(debtor.totalCreditAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column {
                                    Text("المسدد سابقا", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(formatCurrency(debtor.totalPaid), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("المتبقي بذمته", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(formatCurrency(debtor.totalRemaining), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                                }
                            }

                            // Expanded Transactions Details list
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "تفصيل المعلقات للعميل:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        debtor.transactions.forEachIndexed { index, tx ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${index + 1}. ${tx.title}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        style = if (tx.creditRemaining <= 0) TextStyle(textDecoration = TextDecoration.LineThrough) else TextStyle.Default,
                                                        color = if (tx.creditRemaining <= 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (tx.deviceModel.isNotBlank()) {
                                                        Text(
                                                            text = "جهاز: ${tx.deviceModel}",
                                                            fontSize = 9.sp,
                                                            style = if (tx.creditRemaining <= 0) TextStyle(textDecoration = TextDecoration.LineThrough) else TextStyle.Default,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                        )
                                                    }
                                                }
                                                if (tx.creditRemaining <= 0) {
                                                    Text(
                                                        text = "مكتمل ✅",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ProfitGreen
                                                    )
                                                } else {
                                                    Text(
                                                        text = formatCurrency(tx.creditRemaining),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ExpenseRed
                                                    )
                                                }
                                            }
                                            if (index < debtor.transactions.size - 1) {
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons: Send reminder via WhatsApp or generalize sharing
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val txDetailsText = debtor.transactions.joinToString("، ") { "${it.title} (الباقي: ${formatCurrencyNoSymbol(it.creditRemaining)} د.ج)" }
                                val rawMessage = """
                                    السلام عليكم ورحمة الله يا سيدي المحترم ${debtor.name}،
                                    
                                    نود تذكيركم بلطف بمستحقات الكريدي المتبقية بذمتكم لورشة التصليح وقدرها الإجمالي: ${formatCurrency(debtor.totalRemaining)}.
                                    تفصيل المعلقات:
                                    $txDetailsText
                                    
                                    نشكركم جزيل الشكر على ثقتكم وتفهمكم، ونحن دائماً في خدمتكم! ✨
                                """.trimIndent().trim()

                                // WhatsApp Quick Reminder Button
                                Button(
                                    onClick = {
                                        val waIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            setPackage("com.whatsapp")
                                            putExtra(Intent.EXTRA_TEXT, rawMessage)
                                        }
                                        try {
                                            context.startActivity(waIntent)
                                        } catch (e: Exception) {
                                            // Fallback helper to general share
                                            val generalShare = Intent.createChooser(
                                                Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, rawMessage)
                                                    type = "text/plain"
                                                },
                                                "إرسال تذكير عبر:"
                                            )
                                            context.startActivity(generalShare)
                                            Toast.makeText(context, "تطبيق واتساب ليس مثبتاً حالياً، تم تحويل التذكير للمشاركة العامة.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("تذكير واتساب 💬", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                // General Share Link Reminder Button
                                OutlinedButton(
                                    onClick = {
                                        val chooseIntent = Intent.createChooser(
                                            Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, rawMessage)
                                                type = "text/plain"
                                            },
                                            "إرسال التنبيه عبر:"
                                        )
                                        context.startActivity(chooseIntent)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text("مشاركة تذكير عام 🔗", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedListTab == 2) {
            // Personal Debts Section Title
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الديون الشخصية وقروض السلفيات: 👤",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        // Filter to show/hide paid debts
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("عرض المسدد", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Switch(
                                checked = showPersonalDebtsPaidFilter,
                                onCheckedChange = { showPersonalDebtsPaidFilter = it },
                                modifier = Modifier.scale(0.7f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "هذا القسم خاص بالمعاملات المالية الشخصية (خارج مبيعات الورشة المباشرة) مثل إقراض صديق مالاً أو استلاف مال من شخص لتسديد التزامات معينة.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            val displayedDebts = personalDebts.filter { showPersonalDebtsPaidFilter || !it.isPaid }

            if (displayedDebts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "لا توجد ديون شخصية لعرضها! 👍",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(displayedDebts) { debt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPersonalDebtForInstallments = debt },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (debt.isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (debt.isPaid) Color.Transparent
                            else if (debt.isOwedToMe) ProfitGreen.copy(alpha = 0.2f)
                            else ExpenseRed.copy(alpha = 0.2f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Colored Type Indicator
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (debt.isPaid) Color.Gray.copy(alpha = 0.15f)
                                        else if (debt.isOwedToMe) ProfitGreen.copy(alpha = 0.15f)
                                        else ExpenseRed.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (debt.isPaid) Icons.Default.Check
                                    else if (debt.isOwedToMe) Icons.Default.TrendingUp
                                    else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (debt.isPaid) Color.Gray
                                    else if (debt.isOwedToMe) ProfitGreen
                                    else ExpenseRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = debt.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        style = if (debt.isPaid) TextStyle(textDecoration = TextDecoration.LineThrough) else TextStyle.Default,
                                        color = if (debt.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (debt.isPaid) Color.Gray.copy(alpha = 0.1f)
                                                else if (debt.isOwedToMe) ProfitGreen.copy(alpha = 0.1f)
                                                else ExpenseRed.copy(alpha = 0.1f)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (debt.isPaid) "تم التسديد"
                                            else if (debt.isOwedToMe) "ليا نسال"
                                            else "عليا يسالو",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (debt.isPaid) Color.Gray
                                            else if (debt.isOwedToMe) ProfitGreen
                                            else ExpenseRed
                                        )
                                    }
                                }
                                if (debt.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = debt.notes,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = formatCurrency(debt.amount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (debt.isPaid) Color.Gray
                                    else if (debt.isOwedToMe) ProfitGreen
                                    else ExpenseRed
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Mark Paid button
                                    IconButton(
                                        onClick = { viewModel.togglePersonalDebtPaid(debt) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (debt.isPaid) Icons.Default.Undo else Icons.Default.CheckCircle,
                                            contentDescription = "تم التسديد",
                                            tint = if (debt.isPaid) Color.Gray else ProfitGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    // Delete button
                                    IconButton(
                                        onClick = { viewModel.deletePersonalDebt(debt) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف الدين",
                                            tint = Color.Red.copy(alpha = 0.5f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}
}

// Dialog: Add Personal Debt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonalDebtDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double, isOwedToMe: Boolean, wallet: String, notes: String, dueDate: Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var isOwedToMe by remember { mutableStateOf(false) } // Default: عليا يسالوني
    var notes by remember { mutableStateOf("") }
    
    val walletOptions = listOf("محفظة المحل", "سلعة", "حساب بنكي", "مصروف شخصي")
    var selectedWallet by remember { mutableStateOf(walletOptions[0]) }
    var isWalletDropdownExpanded by remember { mutableStateOf(false) }

    var dueDate by remember { mutableStateOf<Long?>(null) }
    var formattedDueDate by remember(dueDate) { 
        mutableStateOf(
            dueDate?.let {
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = it
                java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(cal.time)
            } ?: ""
        )
    }
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة دين شخصي جديد 📝",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الشخص أو الجهة المعنية") },
                    placeholder = { Text("مثال: المورد عمار، أحمد المصلّح...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ (د.ج)") },
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    maxLines = 1
                )

                Text(text = "نوع الدين وسداده:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                // Selection row for OwedToMe vs OwedByMe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f).clickable { isOwedToMe = false },
                        border = BorderStroke(1.5.dp, if (!isOwedToMe) ExpenseRed else Color.Transparent),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isOwedToMe) ExpenseRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Box(modifier = Modifier.padding(10.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("عليا (يسالوني الناس)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (!isOwedToMe) ExpenseRed else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f).clickable { isOwedToMe = true },
                        border = BorderStroke(1.5.dp, if (isOwedToMe) ProfitGreen else Color.Transparent),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOwedToMe) ProfitGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Box(modifier = Modifier.padding(10.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("ليَّا (أنا نسالهم)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isOwedToMe) ProfitGreen else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                
                // Wallet Selector
                ExposedDropdownMenuBox(
                    expanded = isWalletDropdownExpanded,
                    onExpandedChange = { isWalletDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedWallet,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المحفظة المرتبطة بالدين") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isWalletDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = isWalletDropdownExpanded,
                        onDismissRequest = { isWalletDropdownExpanded = false }
                    ) {
                        walletOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedWallet = option
                                    isWalletDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    placeholder = { Text("مثال: ثمن شاشات بالكريدي...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(2.dp))

                OutlinedButton(
                    onClick = {
                        val cal = java.util.Calendar.getInstance()
                        if (dueDate != null) {
                            cal.timeInMillis = dueDate!!
                        }
                        android.app.DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                cal.set(java.util.Calendar.YEAR, y)
                                cal.set(java.util.Calendar.MONTH, m)
                                cal.set(java.util.Calendar.DAY_OF_MONTH, d)
                                android.app.TimePickerDialog(
                                    context,
                                    { _, h, min ->
                                        cal.set(java.util.Calendar.HOUR_OF_DAY, h)
                                        cal.set(java.util.Calendar.MINUTE, min)
                                        dueDate = cal.timeInMillis
                                    },
                                    cal.get(java.util.Calendar.HOUR_OF_DAY),
                                    cal.get(java.util.Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH),
                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            if (dueDate == null) "تحديد وقت وتاريخ التنبيه والإشعار..." else "تنبيه في: $formattedDueDate",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    }
                }
                if (dueDate != null) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = { dueDate = null }) {
                            Text("إلغاء التنبيه", color = Color.Red.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amount > 0) {
                        onSave(name, amount, isOwedToMe, selectedWallet, notes, dueDate)
                    }
                },
                enabled = name.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("إضافة الدين", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

// Dialog: Collect Customer Credit Payment
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectCustomerCreditDialog(
    transaction: WorkshopTransaction,
    installments: List<InstallmentPayment>,
    onDismiss: () -> Unit,
    onSave: (addPayment: Double, paymentNote: String) -> Unit
) {
    var paymentStr by remember { mutableStateOf("") }
    var paymentNoteStr by remember { mutableStateOf("") }
    val currentPaid = transaction.creditPaid
    val outstanding = transaction.creditRemaining

    val transactionInstallments = remember(installments, transaction.id) {
        installments.filter { it.refId == transaction.id && it.refType == "TRANSACTION" }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تسجيل قسط / دفعة كريدي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Customer info card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "الزبون: ${transaction.customerName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "العملية: ${transaction.title}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    if (transaction.deviceModel.isNotBlank()) {
                        Text(text = "الجهاز: ${transaction.deviceModel}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                // Balance summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("الكريدي الأصلي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(formatCurrency(transaction.creditAmount), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("المقبوض سابقاً", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(formatCurrency(currentPaid), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("الباقي المطلوب", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(formatCurrency(outstanding), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = ExpenseRed)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Installments List / Record Log
                Text(
                    text = "📋 سجل الأقساط والدفعات المدفوعة:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                if (transactionInstallments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "لا توجد أقساط مسجلة مسبقاً لهذه العملية",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        transactionInstallments.forEachIndexed { index, inst ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = inst.notes.ifBlank { "دفعة #${index + 1}" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val formattedTime = try {
                                        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(java.util.Date(inst.date))
                                    } catch (e: Exception) {
                                        ""
                                    }
                                    Text(
                                        text = formattedTime,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                Text(
                                    text = "+ " + formatCurrency(inst.amountPaid),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // New Payment input section
                Text(
                    text = "➕ إضافة قسط جديد الآن:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = paymentStr,
                        onValueChange = { paymentStr = it },
                        label = { Text("المبلغ المدفوع (د.ج)") },
                        placeholder = { Text("مثال: 500") },
                        modifier = Modifier.weight(1.1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        maxLines = 1,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = paymentNoteStr,
                        onValueChange = { paymentNoteStr = it },
                        label = { Text("ملاحظة الدفع") },
                        placeholder = { Text("اختياري (مثال: قسط ثاني)") },
                        modifier = Modifier.weight(1.4f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                        maxLines = 1,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Preset button or validation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { paymentStr = if (outstanding % 1.0 == 0.0) outstanding.toLong().toString() else outstanding.toString() }
                    ) {
                        Text("دفع كامل الباقي (${formatCurrencyNoSymbol(outstanding)})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("إلغاء")
                        }
                        Button(
                            onClick = {
                                val amt = paymentStr.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    onSave(amt, paymentNoteStr)
                                }
                            },
                            enabled = (paymentStr.toDoubleOrNull() ?: 0.0) > 0,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تثبيت الدفعة", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// Dialog: Manage Personal Debt Installments
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDebtInstallmentsDialog(
    debt: PersonalDebt,
    installments: List<InstallmentPayment>,
    onDismiss: () -> Unit,
    onSaveNewPayment: (amount: Double, note: String) -> Unit,
    onTogglePaid: () -> Unit
) {
    var paymentStr by remember { mutableStateOf("") }
    var paymentNoteStr by remember { mutableStateOf("") }

    val debtInstallments = remember(installments, debt.id) {
        installments.filter { it.refId == debt.id && it.refType == "PERSONAL_DEBT" }
    }

    val totalPaidSoFar = remember(debtInstallments) {
        debtInstallments.sumOf { it.amountPaid }
    }
    val outstanding = remember(debt.amount, totalPaidSoFar) {
        val rem = debt.amount - totalPaidSoFar
        if (rem < 0) 0.0 else rem
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تفاصيل وسجل دفع قسط الدين",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Debt Person / Details Info Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (debt.isOwedToMe) ProfitGreen.copy(alpha = 0.07f)
                            else ExpenseRed.copy(alpha = 0.07f)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "صاحب الدين: ${debt.name}", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (debt.isOwedToMe) "نوع الدين: دين لي على هذا الشخص (ديون ليا)" else "نوع الدين: دين عليّ لهذا الشخص (ديون عليا)", 
                        fontSize = 11.sp, 
                        color = if (debt.isOwedToMe) ProfitGreen else ExpenseRed,
                        fontWeight = FontWeight.Medium
                    )
                    if (debt.notes.isNotBlank()) {
                        Text(text = "ملاحظات: ${debt.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }

                // Financial balance indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("قيمة الدين الإجمالية", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(formatCurrency(debt.amount), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("المدفوع حتى الآن", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(formatCurrency(totalPaidSoFar), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("المتبقي غير المسدد", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(formatCurrency(outstanding), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (outstanding > 0) ExpenseRed else ProfitGreen)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Installments payment history
                Text(
                    text = "📋 سجل الأقساط والدفعات المستلمة/المدفوعة للعملية:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                if (debtInstallments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "لا توجد عمليات دفع مسجلة مسبقاً لهذا الدين",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        debtInstallments.forEachIndexed { index, inst ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = inst.notes.ifBlank { "دفعة #${index + 1}" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val formattedTime = try {
                                        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(java.util.Date(inst.date))
                                    } catch (e: Exception) {
                                        ""
                                    }
                                    Text(
                                        text = formattedTime,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                Text(
                                    text = "+ " + formatCurrency(inst.amountPaid),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // New Payment section
                Text(
                    text = "➕ إضافة دفعة جديدة الآن (باقي: ${formatCurrencyNoSymbol(outstanding)}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = paymentStr,
                        onValueChange = { paymentStr = it },
                        label = { Text("مبلغ القسط") },
                        placeholder = { Text("0") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        maxLines = 1,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = paymentNoteStr,
                        onValueChange = { paymentNoteStr = it },
                        label = { Text("الملاحظات / المستلم") },
                        placeholder = { Text("مثال: دفعة مارس") },
                        modifier = Modifier.weight(1.3f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                        maxLines = 1,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Action controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = { paymentStr = if (outstanding % 1.0 == 0.0) outstanding.toLong().toString() else outstanding.toString() }
                        ) {
                            Text("الباقي بالكامل", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = onTogglePaid
                        ) {
                            Text(if (debt.isPaid) "تحديد كغير مسدد" else "تحديد كَمُسدد بالكامل", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (debt.isPaid) ExpenseRed else ProfitGreen)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("إغلاق")
                        }
                        Button(
                            onClick = {
                                val amt = paymentStr.toDoubleOrNull() ?: 0.0
                                if (amt > 0) {
                                    onSaveNewPayment(amt, paymentNoteStr)
                                }
                            },
                            enabled = (paymentStr.toDoubleOrNull() ?: 0.0) > 0,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تنزيل قسط", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ======================== GOOGLE SMART ASSISTANT DIALOG ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAssistantDialog(
    stats: WorkshopStats,
    transactionsList: List<WorkshopTransaction>,
    personalDebts: List<PersonalDebt>,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Financial Advice, 1: Smart Diagnosis

    // Key status helper
    val isKeyMissing = GeminiManager.isApiKeyMissing()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Dialog Header with sweep gradient accent strip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF4285F4),
                                    Color(0xFFEA4335),
                                    Color(0xFFFBBC05),
                                    Color(0xFF34A853)
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✨", fontSize = 22.sp)
                        Text(
                            text = "مساعد جـوجـل الذكي",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                if (isKeyMissing) {
                    // API KEY MISSING INFORMATION PANEL
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔑", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "مفتاح Google Gemini API غير مفعّل!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "لتشغيل ميزات الذكاء الاصطناعي وجلب اللمسة السحرية، يرجى إدخال مفتاح الـ API الخاص بك بأمان في لوحة الأسرار (Secrets Panel) في واجهة AI Studio تحت اسم:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "GEMINI_API_KEY",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "📌 بعد إضافة المفتاح وتحديث السحاب، سيتكامل التطبيق مباشرة مع خوادم جوجل الذكية.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // TAB SELECTOR WITH GOOGLE TOUCH
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .padding(4.dp)
                    ) {
                        // Tab 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "تدقيق مالي واستشارة 📈",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        // Tab 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "مكتشف أعطال الصيانة 🛠️",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // VIEW BASED ON TAB
                    Box(modifier = Modifier.weight(1f)) {
                        if (selectedTab == 0) {
                            FinancialAdvisorTab(
                                stats = stats,
                                transactionsList = transactionsList,
                                personalDebts = personalDebts
                            )
                        } else {
                            SmartDiagnosticsTab()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAdvisorTab(
    stats: WorkshopStats,
    transactionsList: List<WorkshopTransaction>,
    personalDebts: List<PersonalDebt>
) {
    var responseText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Calculated metrics inside
    val activeCustomerCredits = transactionsList.sumOf { it.creditRemaining }
    val totalOwedToMe = personalDebts.filter { !it.isPaid && it.isOwedToMe }.sumOf { it.amount }
    val totalOwedByMe = personalDebts.filter { !it.isPaid && !it.isOwedToMe }.sumOf { it.amount }

    // Grouping transactions to find top repaired Category
    val topCategory = transactionsList
        .groupBy { it.category }
        .maxByOrNull { it.value.size }?.key ?: "عامة"

    val displayTopCategoryAr = when (topCategory) {
        "SCREEN" -> "تصليح الشاشات 📱"
        "PARTS" -> "تبديل قطع الغيار ⚙️"
        "ACCESSORY" -> "بيع الإكسسوارات 🎧"
        "SERVICE" -> "الخدمات الفورية (فلاش، برمجة...) 💻"
        "REPAIR" -> "تصليحات الميكروصولدينغ الدقيقة 🛠️"
        "EXPENSE" -> "مصروف 💸"
        else -> topCategory
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📊", fontSize = 20.sp)
                        Text("المعطيات المالية الحالية للورشة:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• عدد الصيانة والمبيعات:", fontSize = 12.sp)
                        Text("${stats.transactionCount} عملية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• صافي أرباح الورشة الحالية:", fontSize = 12.sp)
                        Text(formatCurrency(stats.totalProfit), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• كريدي زبائن غير مستوفى (نسالهم):", fontSize = 12.sp)
                        Text(formatCurrency(activeCustomerCredits), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• ديون الورشة لآخرين (يسالوني):", fontSize = 12.sp)
                        Text(formatCurrency(totalOwedByMe), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• ديون الورشة على آخرين (نسالهم):", fontSize = 12.sp)
                        Text(formatCurrency(totalOwedToMe), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• النشاط الأكثر تكراراً:", fontSize = 12.sp)
                        Text(displayTopCategoryAr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    isLoading = true
                    responseText = ""
                    scope.launch {
                        val prompt = """
                            أنا صاحب ورشة صيانة هواتف جزائرية، إليك الأرقام الحالية في الورشة:
                            - عدد العمليات الإجمالية: ${stats.transactionCount} عملية.
                            - إجمالي الإيرادات: ${stats.totalRevenue} دج.
                            - إجمالي التكاليف والمصاريف: ${stats.totalCost} دج.
                            - صافي الأرباح المسجل: ${stats.totalProfit} دج.
                            - الكريدي الذي نساله للزبائن ولم يسددوه بعد: $activeCustomerCredits دج.
                            - الديون الشخصية التي يسالوها ليا بعض الأشخاص/الموردين: $totalOwedByMe دج.
                            - الديون الشخصية التي نسالها أنا للناس: $totalOwedToMe دج.
                            - الفئة الأكثر نشاطاً في العمليات هي: $topCategory ($displayTopCategoryAr).
                            
                            من فضلك قم بإعداد تدقيق مالي ذكي، بالدرجة الجزائرية الدارجة وبأسلوب احترافي تشجيعي ومختصر جداً، يتضمن:
                            1. تقييم سريع (صحة التدفق النقدي للورشة).
                            2. توجيه عاجل لاسترداد أموالي أو التعامل مع الكريدي القائم.
                            3. نصيحة تسعيرية ذكية للفئة الأكثر مبيعاً ($displayTopCategoryAr) لرفع الأرباح.
                        """.trimIndent()

                        val instruction = """
                            You are "Google Smart Assistant for Warshati (ورشتي)" - an expert Algerian phone repair shop financial auditor.
                            Your language MUST be a elegant blend of clear Algerian Arabic (Darja / الدارجة الجزائرية) and professional Arabic.
                            Do not write long text blocks. Use neat short lists, clear sections, and relevant emojis. Keep it under 200 words.
                        """.trimIndent()

                        responseText = GeminiManager.askGemini(prompt, instruction)
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري استشارة جوجل الذكي... ✨", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        Text("توليد التدقيق والتقرير المالي الذكي 🤖✨", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (responseText.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🤖 AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("تقرير التدقيق والاستشارة الماليّة للورشة:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = responseText,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("تم توليده بواسطة Google Gemini لتوجيهات استشارية عامة وعملية.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        } else if (!isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally, 
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎯", fontSize = 32.sp)
                        Text(
                            "اضغط على الزر في الأعلى بلمسة سحرية ليقوم الذكاء الاصطناعي من جوجل بتحليل أرباحك وعمليات الورشة، وصياغة تدقيق مالي ذكي لمساعدتك في اتخاذ قرارات تسعيرية ممتازة!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartDiagnosticsTab() {
    var brandAndModel by remember { mutableStateOf("") }
    var symptomText by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val popularModels = listOf("iPhone 11", "Samsung S20", "Redmi Note 10", "Poco X3")
    val popularSymptoms = listOf("لا يشحن ⚡", "شاشة سوداء 📱", "سقوط بالماء 💧", "يتجمد عند الشعار 🔄")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "موديل الهاتف والماركة:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = brandAndModel,
                    onValueChange = { brandAndModel = it },
                    placeholder = { Text("مثال: Galaxy S21 أو iPhone 12...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    singleLine = true
                )

                // Quick model select chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(popularModels) { model ->
                        val isSel = brandAndModel == model
                        FilterChip(
                            selected = isSel,
                            onClick = { brandAndModel = model },
                            label = { Text(model, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "وصف المشكلة / أعراض العطل ومظاهره:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = symptomText,
                    onValueChange = { symptomText = it },
                    placeholder = { Text("مثال: الهاتف لا يشحن تماما وتيار السحب 0 أمبير...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // Quick symptom select chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(popularSymptoms) { symptom ->
                        val isSel = symptomText == symptom
                        FilterChip(
                            selected = isSel,
                            onClick = { symptomText = symptom },
                            label = { Text(symptom, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    isLoading = true
                    responseText = ""
                    scope.launch {
                        val prompt = """
                            أريد تشخيصاً وهندسة خطوات لتصليح هاتف بالورشة:
                            - الموديل والماركة: $brandAndModel
                            - الأعراض والمشكلة الحالية: $symptomText
                            
                            من فضلك قم بتوفير roadmap تصليح احترافية للورش الشريكة في الجزائر تتضمن:
                            1. الفحص الأولي وسحب التيار بالملتيمتر والتيستر (troubleshooting steps).
                            2. قطع الهاردوير أو المكونات المشتبه فيها على المذربورد.
                            3. مستوى الصعوبة المتوقع على مقياس 1-10 والوقت الوسطي المستغرق.
                            4. اقتراح كلفة تصليح مناسبة بالدينار الجزائري (DA) لعمل هذا التصليح بالجزائر.
                        """.trimIndent()

                        val instruction = """
                            You are "Google Smart Diagnostics Advisor (مرشد ورشتي الذكي للتصليح)" - an expert hardware engineer and master phone micro-soldering educator for workshops in Algeria.
                            Your language MUST be a clear, simple blend of Arabic and Algerian terms, with standard hardware terms in French/English as commonly used in Algerian workshops (e.g., "flex connecteur", "PMIC", "connecteur de charge", "court-circuit", "multimètre", "alimentation DC").
                            Keep response brief, energetic, nicely structured into 4 clean sections with distinct bullet points, and under 250 words.
                        """.trimIndent()

                        responseText = GeminiManager.askGemini(prompt, instruction)
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && brandAndModel.isNotBlank() && symptomText.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري استخلاص دليل الصيانة الذكي... 🔍", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                        Text("تحليل وتوليد دليل التصليح الذكي 🛠️✨", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (responseText.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🛠️ Diagnosed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("دليل تشخيص وإصلاح هاتف $brandAndModel:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = responseText,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("دليل تشخيص هاردوير استرشادي مقدم من Google Gemini لتسهيل الصيانة.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        } else if (!isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally, 
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔬", fontSize = 32.sp)
                        Text(
                            "املأ موديل الهاتف ووصف العطل في الأعلى، ثم اضغط على زر التحليل ليقوم الذكاء الاصطناعي ببناء خطة فحص ممنهجة وتقديم قطع الهاردوير البديلة المرجح تلفها!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditWalletInitialBalanceDialog(
    walletName: String,
    currentInitial: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var textValue by remember { mutableStateOf(currentInitial.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تعديل الرصيد الأساسي: $walletName",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "سيقوم التطبيق بإضافة هذا الرصيد كأساس لحساب الرصيد الإجمالي لهذه المحفظة تزامناً مع العمليات المسجلة.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("الرصيد الأساسي (د.ج)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = textValue.toDoubleOrNull() ?: 0.0
                onSave(value)
            }) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun EditWalletDialog(
    walletKey: String, // "pocket", "bank", "goods", "personal"
    currentName: String,
    currentInitial: Double,
    currentInclude: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Double, Boolean) -> Unit
) {
    var nameValue by remember { mutableStateOf(currentName) }
    var textValue by remember { mutableStateOf(if (currentInitial % 1.0 == 0.0) currentInitial.toLong().toString() else currentInitial.toString()) }
    var includeValue by remember { mutableStateOf(currentInclude) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تعديل محفظة: $currentName",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "سيقوم التطبيق بتعديل اسم المحفظة والرصيد الأساسي ليتناسب مع رغباتك وحساباتك.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                OutlinedTextField(
                    value = nameValue,
                    onValueChange = { nameValue = it },
                    label = { Text("اسم المحفظة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("الرصيد الأساسي (د.ج)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { includeValue = !includeValue }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeValue,
                        onCheckedChange = { includeValue = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تضمين في الرصيد الإجمالي")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = textValue.toDoubleOrNull() ?: 0.0
                onSave(nameValue, value, includeValue)
            }) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun AddWalletBalanceDialog(
    walletName: String,
    onDismiss: () -> Unit,
    onAdd: (Double) -> Unit
) {
    var textValue by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة رصيد إلى: $walletName",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "أدخل المبلغ المراد إضافته مباشرة إلى الرصيد الأساسي لهذه المحفظة:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("المبلغ المضاف (د.ج)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = textValue.toDoubleOrNull() ?: 0.0
                onAdd(value)
            }) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun WithdrawWalletBalanceDialog(
    walletName: String,
    onDismiss: () -> Unit,
    onWithdraw: (Double) -> Unit
) {
    var textValue by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "سحب رصيد من: $walletName",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "أدخل المبلغ المراد سحبه مباشرة من الرصيد الأساسي لهذه المحفظة:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("المبلغ المسحوب (د.ج)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = textValue.toDoubleOrNull() ?: 0.0
                onWithdraw(value)
            }) {
                Text("سحب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun TransferWalletBalanceDialog(
    sourceWalletName: String,
    walletOptions: List<String>, // other wallet names
    onDismiss: () -> Unit,
    onTransfer: (String, Double) -> Unit
) {
    var textValue by remember { mutableStateOf("") }
    var selectedDestWallet by remember { mutableStateOf(walletOptions.firstOrNull() ?: "") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تحويل رصيد من: $sourceWalletName",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "سيتم خصم المبلغ من المحفظة الحالية وإضافته مباشرة إلى المحفظة المستهدفة.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("المبلغ المراد تحويله (د.ج)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Column {
                    Text(
                        text = "تحويل إلى محفظة:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedDestWallet, color = MaterialTheme.colorScheme.onSurface)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            walletOptions.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedDestWallet = name
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = textValue.toDoubleOrNull() ?: 0.0
                if (value > 0 && selectedDestWallet.isNotEmpty()) {
                    onTransfer(selectedDestWallet, value)
                }
            }) {
                Text("تحويل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun WalletActionBottomSheet(
    walletName: String,
    walletBalance: Double,
    walletIcon: ImageVector,
    walletColor: Color,
    onDismiss: () -> Unit,
    onWithdraw: () -> Unit,
    onDeposit: () -> Unit,
    onTransfer: () -> Unit,
    onEdit: () -> Unit,
    onViewTransactions: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // prevent dismissing when clicking content
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 24.dp)
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                )

                // Wallet Info Row at the top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "الرصيد المتاح",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatCurrency(walletBalance),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = walletColor
                        )
                        if (walletName == "سلعة" || walletName.contains("سلعة")) {
                            val goodsProfit = if (walletBalance < 0.0) 0.0 else walletBalance
                            val phaseText = if (walletBalance < 0.0) "استرجاع رأس المال 📉" else "الأرباح الحقيقية 📈"
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "الفوائد: ${formatCurrency(goodsProfit)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (walletBalance < 0.0) Color(0xFFC62828) else Color(0xFF2E7D32)
                            )
                            Text(
                                text = "الوضعية الماليّة: $phaseText",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = walletName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(walletColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = walletIcon,
                                contentDescription = null,
                                tint = walletColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(12.dp))

                // Actions matching the screenshot
                val actions = listOf(
                    Triple("سحب رصيد", Icons.Default.VerticalAlignBottom, onWithdraw),
                    Triple("إضافة رصيد", Icons.Default.VerticalAlignTop, onDeposit),
                    Triple("تحويل رصيد بين المحافظ", Icons.Default.CompareArrows, onTransfer),
                    Triple("عرض المعاملات", Icons.AutoMirrored.Filled.List, onViewTransactions),
                )

                actions.forEach { (title, icon, action) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = {
                                action()
                            })
                            .padding(vertical = 14.dp, horizontal = 24.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                // Edit (تعديل) at the bottom (Red/Accent styled with Edit icon as per the screenshot!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = {
                            onEdit()
                        })
                        .padding(vertical = 14.dp, horizontal = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تعديل",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WalletTransactionsDialog(
    walletName: String,
    pocketName: String,
    bankName: String,
    goodsName: String,
    personalName: String,
    transactions: List<WorkshopTransaction>,
    onDismiss: () -> Unit,
    onTransactionClicked: (WorkshopTransaction) -> Unit,
    onToggleDelivery: ((WorkshopTransaction) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter transactions belonging to the current wallet
    val walletTransactions = remember(transactions, walletName, pocketName, bankName, goodsName, personalName) {
        transactions.filter {
            when (walletName) {
                pocketName, "مصروف الشهر" -> it.wallet == pocketName || it.wallet == "مصروف الشهر" || it.wallet.isBlank()
                bankName, "حساب بنكي" -> it.wallet == bankName || it.wallet == "حساب بنكي"
                goodsName, "سلعة" -> it.wallet == goodsName || it.wallet == "سلعة"
                else -> it.wallet == personalName || it.wallet == "مصروف شخصي" || it.wallet == "مصروفي شخصي" || it.wallet == "مصروفي الشخصي"
            }
        }
    }
    
    // Search filter
    val displayedTransactions = remember(walletTransactions, searchQuery) {
        if (searchQuery.isBlank()) {
            walletTransactions
        } else {
            walletTransactions.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.deviceModel.contains(searchQuery, ignoreCase = true) ||
                it.customerName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val totalFlow = remember(walletTransactions) {
        walletTransactions.sumOf { it.profit }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                    Text(
                        text = "معاملات محفظة: $walletName",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Wallet Quick Stats inside Dialog
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "عدد العمليات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(text = "${walletTransactions.size} عملية", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "صافي الحركة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(
                            text = formatCurrency(totalFlow),
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = if (totalFlow >= 0) ProfitGreen else Color(0xFFE53935)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في معاملات هذه المحفظة...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable List of Transactions
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (displayedTransactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔍", fontSize = 32.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "لا توجد أي معاملات مسجلة",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        items(displayedTransactions) { trx ->
                            TransactionListItem(
                                transaction = trx,
                                onClick = {
                                    onTransactionClicked(trx)
                                    onDismiss() // Dismiss so they see edit mode
                                },
                                onDelete = {},
                                showDeleteOption = false,
                                onToggleDelivery = { onToggleDelivery?.invoke(trx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WalletsSection(
    viewModel: WorkshopViewModel,
    transactionsList: List<WorkshopTransaction>,
    onTransactionClicked: (WorkshopTransaction) -> Unit
) {
    val pocketInit by viewModel.walletPocketInit.collectAsStateWithLifecycle()
    val bankInit by viewModel.walletBankInit.collectAsStateWithLifecycle()
    val goodsInit by viewModel.walletGoodsInit.collectAsStateWithLifecycle()
    val personalInit by viewModel.walletPersonalInit.collectAsStateWithLifecycle()

    val pocketName by viewModel.walletPocketName.collectAsStateWithLifecycle()
    val bankName by viewModel.walletBankName.collectAsStateWithLifecycle()
    val goodsName by viewModel.walletGoodsName.collectAsStateWithLifecycle()
    val personalName by viewModel.walletPersonalName.collectAsStateWithLifecycle()

    val pocketInclude by viewModel.walletPocketInclude.collectAsStateWithLifecycle()
    val bankInclude by viewModel.walletBankInclude.collectAsStateWithLifecycle()
    val goodsInclude by viewModel.walletGoodsInclude.collectAsStateWithLifecycle()
    val personalInclude by viewModel.walletPersonalInclude.collectAsStateWithLifecycle()

    var selectedWalletForActions by remember { mutableStateOf<String?>(null) }
    var showEditDialogFor by remember { mutableStateOf<String?>(null) }
    var showDepositDialogFor by remember { mutableStateOf<String?>(null) }
    var showWithdrawDialogFor by remember { mutableStateOf<String?>(null) }
    var showTransferDialogFor by remember { mutableStateOf<String?>(null) }
    var showTransactionsForWallet by remember { mutableStateOf<String?>(null) }

    val personalDebts by viewModel.debtsFlow.collectAsStateWithLifecycle()
    val installments by viewModel.installmentsFlow.collectAsStateWithLifecycle()

    val unpaidOwedToMe = personalDebts.filter { it.isOwedToMe && !it.isPaid }.sumOf { debt -> debt.amount - installments.filter { it.refId == debt.id && it.refType == "PERSONAL_DEBT" }.sumOf { it.amountPaid } }
    val unpaidOwedByMe = personalDebts.filter { !it.isOwedToMe && !it.isPaid }.sumOf { debt -> debt.amount - installments.filter { it.refId == debt.id && it.refType == "PERSONAL_DEBT" }.sumOf { it.amountPaid } }

    val pocketChange = transactionsList.filter { 
        (it.wallet == pocketName || it.wallet == "مصروف الشهر" || it.wallet == "الصندوق (Pocket)" || it.wallet == "محفظة المحل" || it.wallet.isBlank()) 
        && it.category != "ACCESSORY" 
        && it.affectBalance 
    }.sumOf { it.profit }
    val pocketBalance = pocketInit + pocketChange

    val bankChange = transactionsList.filter { 
        (it.wallet == bankName || it.wallet == "حساب بنكي") 
        && it.category != "ACCESSORY" 
        && it.affectBalance 
    }.sumOf { it.profit }
    val bankBalance = bankInit + bankChange

    val goodsChange = transactionsList.filter { 
        it.category == "ACCESSORY" 
        && it.affectBalance 
    }.sumOf { it.profit }
    val goodsBalance = goodsInit + goodsChange

    val personalChange = transactionsList.filter { 
        (it.wallet == personalName || it.wallet == "مصروف شخصي" || it.wallet == "مصروفي شخصي" || it.wallet == "مصروفي الشخصي") 
        && it.category != "ACCESSORY" 
        && it.affectBalance 
    }.sumOf { it.profit }
    val personalBalance = personalInit + personalChange

    val totalBalance = (if (pocketInclude) pocketBalance else 0.0) +
                       (if (bankInclude) bankBalance else 0.0) +
                       (if (goodsInclude) goodsBalance else 0.0) +
                       (if (personalInclude) personalBalance else 0.0)

    // Toggle hide/show balances decoration state
    var isBalancesVisible by remember { mutableStateOf(true) }
    val isLiquidTheme = com.example.ui.theme.LocalIsLiquidTheme.current
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLiquidTheme) {
                if (isDark) Color(0x3312121A) else Color(0x33FFFFFF)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLiquidTheme) 0.dp else 2.dp),
        border = if (isLiquidTheme) {
            BorderStroke(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.22f else 0.65f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "المحفظات والمدخرات 💳",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Show/Hide Toggle action
                IconButton(
                    onClick = { isBalancesVisible = !isBalancesVisible },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isBalancesVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "عرض/إخفاء أرصدة المحفظة",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Wallets list
            val walletsData = listOf(
                Triple(pocketName, pocketBalance, pocketInit),
                Triple(bankName, bankBalance, bankInit),
                Triple(goodsName, goodsBalance, goodsInit),
                Triple(personalName, personalBalance, personalInit)
            )

            walletsData.forEach { (name, balance, initValue) ->
                // Customize colors/icons
                val (icon, brush) = when (name) {
                    pocketName, "مصروف الشهر" -> Pair(
                        Icons.Default.AccountBalanceWallet,
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2E7D32),
                                Color(0xFF1B5E20)
                            )
                        )
                    )
                    bankName, "حساب بنكي" -> Pair(
                        Icons.Default.CreditCard,
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1565C0),
                                Color(0xFF0D47A1)
                            )
                        )
                    )
                    goodsName, "سلعة" -> Pair(
                        Icons.Default.ShoppingBag,
                        Brush.linearGradient(
                            colors = if (balance < 0.0) {
                                listOf(
                                    Color(0xFFC62828), // Deep Red
                                    Color(0xFFB71C1C)
                                )
                            } else {
                                listOf(
                                    Color(0xFF2E7D32), // Deep Green
                                    Color(0xFF1B5E20)
                                )
                            }
                        )
                    )
                    else -> Pair(
                        Icons.Default.Person,
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF7B1FA2),
                                Color(0xFF4A148C)
                            )
                        )
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedWalletForActions = name },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(brush)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = "الرصيد المتاح",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    if (name == goodsName || name == "سلعة") {
                                        val goodsProfit = if (balance < 0.0) 0.0 else balance
                                        val phaseText = if (balance < 0.0) "استرجاع رأس المال 📉" else "الأرباح الحقيقية 📈"
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "الفوائد: ${if (isBalancesVisible) formatCurrency(goodsProfit) else "•••••• دج"} ($phaseText)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.95f)
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isBalancesVisible) formatCurrency(balance) else "•••••• دج",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "خيارات المحفظة",
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Actions Sheet (Custom Bottom Sheet Dialog like screenshot)
    selectedWalletForActions?.let { name ->
        val (icon, color) = when (name) {
            pocketName, "مصروف الشهر" -> Pair(Icons.Default.AccountBalanceWallet, Color(0xFF2E7D32))
            bankName, "حساب بنكي" -> Pair(Icons.Default.CreditCard, Color(0xFF1565C0))
            goodsName, "سلعة" -> Pair(
                Icons.Default.ShoppingBag,
                if (goodsBalance < 0.0) Color(0xFFC62828) else Color(0xFF2E7D32)
            )
            else -> Pair(Icons.Default.Person, Color(0xFF7B1FA2))
        }
        val balance = when (name) {
            pocketName, "مصروف الشهر" -> pocketBalance
            bankName, "حساب بنكي" -> bankBalance
            goodsName, "سلعة" -> goodsBalance
            else -> personalBalance
        }

        WalletActionBottomSheet(
            walletName = name,
            walletBalance = balance,
            walletIcon = icon,
            walletColor = color,
            onDismiss = { selectedWalletForActions = null },
            onWithdraw = {
                showWithdrawDialogFor = name
                selectedWalletForActions = null
            },
            onDeposit = {
                showDepositDialogFor = name
                selectedWalletForActions = null
            },
            onTransfer = {
                showTransferDialogFor = name
                selectedWalletForActions = null
            },
            onEdit = {
                showEditDialogFor = name
                selectedWalletForActions = null
            },
            onViewTransactions = {
                showTransactionsForWallet = name
                selectedWalletForActions = null
            }
        )
    }

    // 2. Deposit Dialog (إضافة الرصيد)
    showDepositDialogFor?.let { name ->
        AddWalletBalanceDialog(
            walletName = name,
            onDismiss = { showDepositDialogFor = null },
            onAdd = { value ->
                if (value > 0) {
                    when (name) {
                        pocketName, "مصروف الشهر" -> viewModel.setWalletPocketInit(pocketInit + value)
                        bankName, "حساب بنكي" -> viewModel.setWalletBankInit(bankInit + value)
                        goodsName, "سلعة" -> viewModel.setWalletGoodsInit(goodsInit + value)
                        else -> viewModel.setWalletPersonalInit(personalInit + value)
                    }
                }
                showDepositDialogFor = null
            }
        )
    }

    // 3. Withdraw Dialog (سحب الرصيد)
    showWithdrawDialogFor?.let { name ->
        WithdrawWalletBalanceDialog(
            walletName = name,
            onDismiss = { showWithdrawDialogFor = null },
            onWithdraw = { value ->
                if (value > 0) {
                    when (name) {
                        pocketName, "مصروف الشهر" -> viewModel.setWalletPocketInit(pocketInit - value)
                        bankName, "حساب بنكي" -> viewModel.setWalletBankInit(bankInit - value)
                        goodsName, "سلعة" -> viewModel.setWalletGoodsInit(goodsInit - value)
                        else -> viewModel.setWalletPersonalInit(personalInit - value)
                    }
                }
                showWithdrawDialogFor = null
            }
        )
    }

    // 4. Transfer Dialog (تحويل بين المحافظ)
    showTransferDialogFor?.let { name ->
        val otherWallets = listOf(pocketName, bankName, goodsName, personalName).filter { it != name }
        TransferWalletBalanceDialog(
            sourceWalletName = name,
            walletOptions = otherWallets,
            onDismiss = { showTransferDialogFor = null },
            onTransfer = { targetName, value ->
                if (value > 0) {
                    // deduct from source
                    when (name) {
                        pocketName, "مصروف الشهر" -> viewModel.setWalletPocketInit(pocketInit - value)
                        bankName, "حساب بنكي" -> viewModel.setWalletBankInit(bankInit - value)
                        goodsName, "سلعة" -> viewModel.setWalletGoodsInit(goodsInit - value)
                        else -> viewModel.setWalletPersonalInit(personalInit - value)
                    }
                    // add to target
                    when (targetName) {
                        pocketName -> viewModel.setWalletPocketInit(pocketInit + value)
                        bankName -> viewModel.setWalletBankInit(bankInit + value)
                        goodsName -> viewModel.setWalletGoodsInit(goodsInit + value)
                        personalName -> viewModel.setWalletPersonalInit(personalInit + value)
                    }
                }
                showTransferDialogFor = null
            }
        )
    }

    // 5. Rename & Edit Dialog (تعديل الاسم والأساسي)
    showEditDialogFor?.let { name ->
        if (name == personalName || name == "مصروف شخصي") {
            PersonalExpenseManagerDialog(
                viewModel = viewModel,
                personalBalance = personalBalance,
                personalInit = personalInit,
                onDismiss = { showEditDialogFor = null }
            )
        } else {
            val currentInitValue = when (name) {
                pocketName, "مصروف الشهر" -> pocketInit
                bankName, "حساب بنكي" -> bankInit
                goodsName, "سلعة" -> goodsInit
                else -> personalInit
            }
            val walletKey = when (name) {
                pocketName, "مصروف الشهر" -> "pocket"
                bankName, "حساب بنكي" -> "bank"
                goodsName, "سلعة" -> "goods"
                else -> "personal"
            }
            val currentIncludeValue = when (name) {
                pocketName, "مصروف الشهر" -> pocketInclude
                bankName, "حساب بنكي" -> bankInclude
                goodsName, "سلعة" -> goodsInclude
                else -> personalInclude
            }

            EditWalletDialog(
                walletKey = walletKey,
                currentName = name,
                currentInitial = currentInitValue,
                currentInclude = currentIncludeValue,
                onDismiss = { showEditDialogFor = null },
                onSave = { newName, newInitValue, newInclude ->
                    // Always save the include setting when 'save' is pressed, even if name is blank, or at least update it
                    when (walletKey) {
                        "pocket" -> viewModel.setWalletPocketInclude(newInclude)
                        "bank" -> viewModel.setWalletBankInclude(newInclude)
                        "goods" -> viewModel.setWalletGoodsInclude(newInclude)
                        "personal" -> viewModel.setWalletPersonalInclude(newInclude)
                    }

                    if (newName.isNotBlank()) {
                        // update initial balance
                        when (walletKey) {
                            "pocket" -> {
                                viewModel.setWalletPocketInit(newInitValue)
                                viewModel.setWalletPocketName(newName)
                            }
                            "bank" -> {
                                viewModel.setWalletBankInit(newInitValue)
                                viewModel.setWalletBankName(newName)
                            }
                            "goods" -> {
                                viewModel.setWalletGoodsInit(newInitValue)
                                viewModel.setWalletGoodsName(newName)
                            }
                            "personal" -> {
                                viewModel.setWalletPersonalInit(newInitValue)
                                viewModel.setWalletPersonalName(newName)
                            }
                        }
                        // migrate transaction names
                        viewModel.renameWalletInTransactions(name, newName)
                    }
                    showEditDialogFor = null
                }
            )
        }
    }

    // 6. View Transactions Dialog
    showTransactionsForWallet?.let { name ->
        WalletTransactionsDialog(
            walletName = name,
            pocketName = pocketName,
            bankName = bankName,
            goodsName = goodsName,
            personalName = personalName,
            transactions = transactionsList,
            onDismiss = { showTransactionsForWallet = null },
            onTransactionClicked = onTransactionClicked,
            onToggleDelivery = { viewModel.toggleTransactionDelivery(it) }
        )
    }
}

@Composable
fun PersonalExpenseManagerDialog(
    viewModel: WorkshopViewModel,
    personalBalance: Double,
    personalInit: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<String?>(null) }
    var showEditBaseDialog by remember { mutableStateOf(false) }

    val transactionsList by viewModel.transactionsFlow.collectAsStateWithLifecycle()
    val personalTransactions = transactionsList.filter { 
        it.wallet == "مصروف شخصي" || it.wallet == "مصروفي شخصي" || it.wallet == "مصروفي الشخصي" 
    }.sortedByDescending { it.date }

    if (showEditBaseDialog) {
        EditWalletInitialBalanceDialog(
            walletName = "مصروف شخصي",
            currentInitial = personalInit,
            onDismiss = { showEditBaseDialog = false },
            onSave = { newValue ->
                viewModel.setWalletPersonalInit(newValue)
                showEditBaseDialog = false
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(top = 10.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Modern Header with Custom Colors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF7B1FA2).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = Color(0xFF7B1FA2),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "محفظة المصروف الشخصي 💳",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "تتبع وادر نفقاتك اليومية بكل دقة وسهولة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "اغلاق", 
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Balance Gradient Box (Modern glassmorphic / futuristic design)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF7B1FA2),
                                                Color(0xFFE91E63)
                                            )
                                        )
                                    )
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "الرصيد الشخصي المتبقي",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.8f),
                                            letterSpacing = 0.3.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatCurrency(personalBalance),
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                    
                                    // Elegant modify base balance button
                                    Button(
                                        onClick = { showEditBaseDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.22f),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        elevation = null
                                    ) {
                                        Icon(
                                            Icons.Default.Edit, 
                                            contentDescription = null, 
                                            modifier = Modifier.size(12.dp), 
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "الأساسي: ${personalInit.toInt()} د.ج", 
                                            fontSize = 10.sp, 
                                            fontWeight = FontWeight.Bold, 
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section: Quick Select Presets
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "تسجيل سريع بالرموز التعبييرية ⚡",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            val presets = listOf(
                                Triple("مواصلات", "🚌", Color(0xFF00BCD4)),
                                Triple("إيجار", "🏠", Color(0xFF3F51B5)),
                                Triple("إنترنت", "🌐", Color(0xFF2196F3)),
                                Triple("صدقة الأسرة", "🤲", Color(0xFF4CAF50)),
                                Triple("فواتير", "🧾", Color(0xFFFF9800)),
                                Triple("غذاء", "🍔", Color(0xFFE91E63)),
                                Triple("قهوة", "☕", Color(0xFF795548)),
                                Triple("تسوق", "🛒", Color(0xFF9C27B0)),
                                Triple("صحة وعلاج", "🏥", Color(0xFFF44336)),
                                Triple("أخرى", "📦", Color(0xFF607D8B))
                            )

                            presets.chunked(2).forEach { rowPresets ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPresets.forEach { (name, emoji, color) ->
                                        val isSelected = selectedPreset == name
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
                                                    selectedPreset = name
                                                    title = "$emoji $name"
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(
                                                width = 1.5.dp,
                                                color = if (isSelected) color else Color.Transparent
                                            ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) {
                                                    color.copy(alpha = 0.12f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                                }
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) color.copy(alpha = 0.2f) else color.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = emoji, fontSize = 14.sp)
                                                }
                                                Text(
                                                    text = name,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                    }
                                    if (rowPresets.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Section: Input Form (Enhanced text fields and styling)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "تفاصيل المصروف اليدوي:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            OutlinedTextField(
                                value = title,
                                onValueChange = { 
                                    title = it
                                    selectedPreset = null
                                },
                                label = { Text("نوع أو اسم المصروف الشخصي") },
                                placeholder = { Text("مثال: قهوة الصباح ☕، تذكرة باص 🚌") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF7B1FA2),
                                    focusedLabelColor = Color(0xFF7B1FA2)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = amountStr,
                                    onValueChange = { amountStr = it },
                                    label = { Text("المبلغ المخصوم (د.ج)") },
                                    placeholder = { Text("0") },
                                    modifier = Modifier.weight(1.1f),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFE91E63),
                                        focusedLabelColor = Color(0xFFE91E63)
                                    )
                                )

                                OutlinedTextField(
                                    value = details,
                                    onValueChange = { details = it },
                                    label = { Text("ملاحظات إضافية") },
                                    placeholder = { Text("اختياري") },
                                    modifier = Modifier.weight(1.3f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF7B1FA2),
                                        focusedLabelColor = Color(0xFF7B1FA2)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Glowing action gradient save button
                            Button(
                                onClick = {
                                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                                    if (amount <= 0.0) {
                                        Toast.makeText(context, "الرجاء إدخال مبلغ صحيح للمصروف", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (title.isBlank()) {
                                        Toast.makeText(context, "الرجاء تحديد نوع المصروف أو كتابته", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    viewModel.addTransaction(
                                        title = title,
                                        category = "EXPENSE",
                                        costPrice = amount,
                                        sellingPrice = 0.0,
                                        deviceModel = "",
                                        customerName = "",
                                        notes = details,
                                        creditAmount = 0.0,
                                        creditPaid = 0.0,
                                        wallet = "مصروف شخصي"
                                    )

                                    title = ""
                                    amountStr = ""
                                    details = ""
                                    selectedPreset = null
                                    Toast.makeText(context, "تم تسجيل المصروف الشخصي بنجاح! 💸", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF7B1FA2),
                                                    Color(0xFF4A148C)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "تسجيل هذا المصروف الآن 💸", 
                                            fontWeight = FontWeight.Bold, 
                                            color = Color.White, 
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section: Logs Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "سجل المصروفات الأخيرة 📜",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (personalTransactions.isNotEmpty()) {
                                Text(
                                    text = "إجمالي العمليات: ${personalTransactions.size}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // List of personal expenses
                    if (personalTransactions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "لا توجد مصروفات شخصية مسجلة بعد.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    )
                                }
                            }
                        }
                    } else {
                        items(personalTransactions) { trx ->
                            // Visual Icon Circle based on Transaction title matches
                            val iconColor = remember(trx.title) {
                                when {
                                    trx.title.contains("🚌") || trx.title.contains("مواصلات") -> Color(0xFF00BCD4)
                                    trx.title.contains("🏠") || trx.title.contains("إيجار") -> Color(0xFF3F51B5)
                                    trx.title.contains("🌐") || trx.title.contains("إنترنت") -> Color(0xFF2196F3)
                                    trx.title.contains("🤲") || trx.title.contains("صدقة") -> Color(0xFF4CAF50)
                                    trx.title.contains("🧾") || trx.title.contains("فواتير") -> Color(0xFFFF9800)
                                    trx.title.contains("🍔") || trx.title.contains("غذاء") -> Color(0xFFE91E63)
                                    trx.title.contains("☕") || trx.title.contains("قهوة") -> Color(0xFF795548)
                                    trx.title.contains("🛒") || trx.title.contains("تسوق") -> Color(0xFF9C27B0)
                                    trx.title.contains("🏥") || trx.title.contains("صحة") -> Color(0xFFF44336)
                                    else -> Color(0xFF607D8B)
                                }
                            }
                            val cardBgColor = iconColor.copy(alpha = 0.16f)
                            val cardBorderColor = iconColor.copy(alpha = 0.45f)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBgColor
                                ),
                                border = BorderStroke(1.5.dp, cardBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(iconColor.copy(alpha = 0.18f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Extract emoji or use general wallet icon
                                            val emojiChar = trx.title.trim().take(2).filter { it.isSurrogate() || it.code > 127 || it.toString() == "☕" }
                                            if (emojiChar.isNotEmpty()) {
                                                Text(text = emojiChar, fontSize = 26.sp)
                                            } else {
                                                Icon(Icons.Default.Payments, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = trx.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (trx.notes.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = trx.notes,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(trx.date)),
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically, 
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "- " + formatCurrency(trx.costPrice),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFD32F2F)
                                        )
                                        
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteTransaction(trx)
                                                Toast.makeText(context, "تم حذف المصروف!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "حذف المصروف",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidGlassBlobBackground(modifier: Modifier = Modifier, isDark: Boolean = true) {
    val animX1 = remember { Animatable(0.2f) }
    val animY1 = remember { Animatable(0.1f) }
    val animX2 = remember { Animatable(0.8f) }
    val animY2 = remember { Animatable(0.8f) }
    val animX3 = remember { Animatable(0.5f) }
    val animY3 = remember { Animatable(0.4f) }

    val pulse1 = remember { Animatable(0.75f) }
    val pulse2 = remember { Animatable(0.70f) }
    val pulse3 = remember { Animatable(0.65f) }

    LaunchedEffect(Unit) {
        launch {
            while (true) {
                animX1.animateTo(0.8f, animationSpec = tween(18000, easing = FastOutSlowInEasing))
                animX1.animateTo(0.2f, animationSpec = tween(18000, easing = FastOutSlowInEasing))
            }
        }
        launch {
            while (true) {
                animY1.animateTo(0.7f, animationSpec = tween(22000, easing = FastOutSlowInEasing))
                animY1.animateTo(0.1f, animationSpec = tween(22000, easing = FastOutSlowInEasing))
            }
        }
        launch {
            while (true) {
                animX2.animateTo(0.1f, animationSpec = tween(25000, easing = FastOutSlowInEasing))
                animX2.animateTo(0.8f, animationSpec = tween(25000, easing = FastOutSlowInEasing))
            }
        }
        launch {
            while (true) {
                animY2.animateTo(0.2f, animationSpec = tween(20000, easing = FastOutSlowInEasing))
                animY2.animateTo(0.8f, animationSpec = tween(20000, easing = FastOutSlowInEasing))
            }
        }
        launch {
            while (true) {
                animX3.animateTo(0.9f, animationSpec = tween(28000, easing = FastOutSlowInEasing))
                animX3.animateTo(0.3f, animationSpec = tween(28000, easing = FastOutSlowInEasing))
            }
        }
        launch {
            while (true) {
                animY3.animateTo(0.1f, animationSpec = tween(24000, easing = FastOutSlowInEasing))
                animY3.animateTo(0.9f, animationSpec = tween(24000, easing = FastOutSlowInEasing))
            }
        }
        // Organic biological fluid pulse animations
        launch {
            while (true) {
                pulse1.animateTo(0.95f, animationSpec = tween(12000, easing = LinearOutSlowInEasing))
                pulse1.animateTo(0.75f, animationSpec = tween(12000, easing = LinearOutSlowInEasing))
            }
        }
        launch {
            while (true) {
                pulse2.animateTo(0.88f, animationSpec = tween(14000, easing = LinearOutSlowInEasing))
                pulse2.animateTo(0.68f, animationSpec = tween(14000, easing = LinearOutSlowInEasing))
            }
        }
        launch {
            while (true) {
                pulse3.animateTo(0.82f, animationSpec = tween(16000, easing = LinearOutSlowInEasing))
                pulse3.animateTo(0.58f, animationSpec = tween(16000, easing = LinearOutSlowInEasing))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF141727), Color(0xFF060811))
                    } else {
                        listOf(Color(0xFFEFF5FB), Color(0xFFD3E2F2))
                    },
                    radius = 2200f
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (isDark) {
                // Blob 1: Electric Sky Blue
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF007AFF).copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(width * animX1.value, height * animY1.value),
                        radius = width * pulse1.value
                    ),
                    center = Offset(width * animX1.value, height * animY1.value),
                    radius = width * pulse1.value
                )
                // Blob 2: Vibrant Violet
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFD355F5).copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(width * animX2.value, height * animY2.value),
                        radius = width * pulse2.value
                    ),
                    center = Offset(width * animX2.value, height * animY2.value),
                    radius = width * pulse2.value
                )
                // Blob 3: Premium Crimson Rose
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF2D55).copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(width * animX3.value, height * animY3.value),
                        radius = width * pulse3.value
                    ),
                    center = Offset(width * animX3.value, height * animY3.value),
                    radius = width * pulse3.value
                )
            } else {
                // Blob 1: Vibrant Aqua Blue
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF007AFF).copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(width * animX1.value, height * animY1.value),
                        radius = width * pulse1.value
                    ),
                    center = Offset(width * animX1.value, height * animY1.value),
                    radius = width * pulse1.value
                )
                // Blob 2: Soft Purple Pearl
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFD355F5).copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(width * animX2.value, height * animY2.value),
                        radius = width * pulse2.value
                    ),
                    center = Offset(width * animX2.value, height * animY2.value),
                    radius = width * pulse2.value
                )
                // Blob 3: Warm Liquid Coral Pink
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF9500).copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(width * animX3.value, height * animY3.value),
                        radius = width * pulse3.value
                    ),
                    center = Offset(width * animX3.value, height * animY3.value),
                    radius = width * pulse3.value
                )
            }
        }
    }
}
