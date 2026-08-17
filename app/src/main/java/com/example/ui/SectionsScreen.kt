package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WorkshopTransaction
import com.example.ui.theme.Translator
import com.example.ui.viewmodel.WorkshopViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionsScreen(
    viewModel: WorkshopViewModel,
    onAddTransactionForCategory: (String) -> Unit,
    onTransactionClicked: (WorkshopTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val transactions by viewModel.transactionsFlow.collectAsStateWithLifecycle()
    val refurbishedDevices by viewModel.refurbishedDevicesFlow.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpensesFlow.collectAsStateWithLifecycle()
    val personalDebts by viewModel.debtsFlow.collectAsStateWithLifecycle()
    val goodsInit by viewModel.walletGoodsInit.collectAsStateWithLifecycle()
    val pocketInit by viewModel.walletPocketInit.collectAsStateWithLifecycle()
    val bankInit by viewModel.walletBankInit.collectAsStateWithLifecycle()
    val personalInit by viewModel.walletPersonalInit.collectAsStateWithLifecycle()
    val goodsName by viewModel.walletGoodsName.collectAsStateWithLifecycle()
    
    // UI state to track which section is currently viewed in detail (Bottom Sheet)
    var selectedCategoryForDetail by remember { mutableStateOf<CategorySectionInfo?>(null) }
    var showGlobalStatsDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val categories = listOf(
        CategorySectionInfo("SCREEN", "تصليح الشاشات 📱", "Réparation d'Écrans", "Screen Repairs", Icons.Default.Smartphone, Color(0xFF60A5FA)),
        CategorySectionInfo("PARTS", "قطع الغيار ⚙️", "Pièces de Rechange", "Spare Parts", Icons.Default.MiscellaneousServices, Color(0xFFFB923C)),
        CategorySectionInfo("ACCESSORY", "الأكسسوارات 🎧", "Vente d'Accessoires", "Accessories & Sales", Icons.Default.Headphones, Color(0xFFF472B6)),
        CategorySectionInfo("SERVICE", "البرمجة والسوفتر 💻", "Direct Flash & Soft", "Software & Flashing", Icons.Default.DeveloperMode, Color(0xFFA78BFA)),
        CategorySectionInfo("EXPENSE", "مصروف 💸", "Dépenses / Charges", "Expenses & Costs", Icons.Default.ReceiptLong, Color(0xFFF87171)),
        CategorySectionInfo("REFURB", "استثمار وتدوير ♻️", "Investissement & Recyclage", "Refurbished Devices", Icons.Default.Autorenew, Color(0xFF34D399)),
        CategorySectionInfo("INVENTORY", "مخزون المحل 📦", "Stock du Magasin", "Shop Inventory", Icons.Default.Inventory, Color(0xFFD97706)),
        CategorySectionInfo("OTHER", "صيانات عامة 🛠️", "Matériels & Divers", "Other Hardware", Icons.Default.HomeRepairService, Color(0xFF6366F1))
    )

    // Calculate sum of revenue across all categories for percentage weights
    val totalInitialBalanceAll = pocketInit + bankInit + goodsInit + personalInit
    val totalRevenueAll = transactions.sumOf { it.sellingPrice } + totalInitialBalanceAll
    val totalCostAll = transactions.sumOf { it.costPrice }
    val totalProfitAll = totalRevenueAll - totalCostAll
    val totalCreditAll = transactions.sumOf { it.creditAmount - it.creditPaid }

    val goodsChange = transactions.filter { it.category == "ACCESSORY" && it.affectBalance }.sumOf { it.cashFlow }
    val goodsBalance = goodsInit + goodsChange

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBackgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onSurface

    val isLiquidTheme = com.example.ui.theme.LocalIsLiquidTheme.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isLiquidTheme) Color.Transparent else MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
        // Welcoming Section Dynamic Header
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Debt Alerts Feature
                val currentTime = System.currentTimeMillis()
                val activeAlerts = transactions.filter { it.creditRemaining > 0 && it.dueDate != null && it.dueDate <= currentTime }
                val activePersonalAlerts = personalDebts.filter { !it.isPaid && it.dueDate != null && it.dueDate <= currentTime }
                
                if (activeAlerts.isNotEmpty() || activePersonalAlerts.isNotEmpty()) {
                    val isLiquidTheme = com.example.ui.theme.LocalIsLiquidTheme.current
                    val isDarkTheme = isSystemInDarkTheme()
                    
                    activeAlerts.forEach { alertTrx ->
                        val alertBgColor = if (isLiquidTheme) {
                            if (isDarkTheme) Color(0x30E65100) else Color(0x35FFE0B2)
                        } else {
                            Color(0xFFFFF3E0)
                        }
                        val alertBorder = if (isLiquidTheme) {
                            BorderStroke(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isDarkTheme) 0.2f else 0.62f),
                                        Color(0xFFFF9800).copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                )
                            )
                        } else {
                            BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f))
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onTransactionClicked(alertTrx) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = alertBgColor
                            ),
                            border = alertBorder
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF9800).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Alert",
                                            tint = Color(0xFFE65100),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (lang == "ar") "موعد استحقاق دين زبون ⚠️" else "Customer Debt Due Alert",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100)
                                        )
                                        val customerName = alertTrx.customerName.ifEmpty { if (lang == "ar") "زبون غير معروف" else "Unknown Customer" }
                                        Text(
                                            text = "$customerName: ${formatWithLoc(alertTrx.creditRemaining, lang)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFE65100).copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.updateTransaction(alertTrx.copy(dueDate = null)) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFFE65100))
                                }
                              }
                            }
                        }

                    activePersonalAlerts.forEach { alertDebt ->
                        val alertBgColor = if (isLiquidTheme) {
                            if (alertDebt.isOwedToMe) {
                                if (isDarkTheme) Color(0x302E7D32) else Color(0x35C8E6C9)
                            } else {
                                if (isDarkTheme) Color(0x30C62828) else Color(0x35FFCDD2)
                            }
                        } else {
                            if (alertDebt.isOwedToMe) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        }
                        val alertBorder = if (isLiquidTheme) {
                            BorderStroke(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isDarkTheme) 0.2f else 0.62f),
                                        (if (alertDebt.isOwedToMe) Color(0xFF4CAF50) else Color(0xFFF44336)).copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                )
                            )
                        } else {
                            BorderStroke(1.dp, if (alertDebt.isOwedToMe) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFFF44336).copy(alpha = 0.5f))
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = alertBgColor
                            ),
                            border = alertBorder
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(if (alertDebt.isOwedToMe) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFF44336).copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Alert",
                                            tint = if (alertDebt.isOwedToMe) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (lang == "ar") {
                                                if (alertDebt.isOwedToMe) "موعد استحقاق دين (لك) ⚠️" else "موعد سداد دين (عليك) ⚠️"
                                            } else "Personal Debt Due Alert",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (alertDebt.isOwedToMe) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                        Text(
                                            text = "${alertDebt.name}: ${formatWithLoc(alertDebt.amount, lang)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = (if (alertDebt.isOwedToMe) Color(0xFF2E7D32) else Color(0xFFC62828)).copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.updateDebt(alertDebt.copy(dueDate = null)) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = if (alertDebt.isOwedToMe) Color(0xFF2E7D32) else Color(0xFFC62828))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Grid of Sections
        val totalItems = categories.size + 1

        items(totalItems) { index ->
            if (index < categories.size) {
                val cat = categories[index]
                val count: Int
                val sumCost: Double
                val sumRevenue: Double
                val sumProfit: Double
                if (cat.id == "REFURB") {
                    count = refurbishedDevices.size
                    
                    // 1. Devices and Expenses
                    val sumCostDevices = refurbishedDevices.sumOf { it.purchasePrice }
                    val sumCostExpenses = allExpenses.sumOf { it.cost }
                    val sumRevenueDevices = refurbishedDevices.filter { it.salePrice != null }.sumOf { it.salePrice ?: 0.0 }
                    
                    // 2. Manual transactions of category REFURB
                    val manualRefurbTrxs = transactions.filter { trx ->
                        trx.category == "REFURB" && 
                        !trx.title.startsWith("شراء هاتف للاستثمار") && 
                        !trx.title.contains("تجهيز هاتف استثمار") && 
                        !trx.title.startsWith("بيع هاتف استثمار")
                    }
                    val sumCostManual = manualRefurbTrxs.sumOf { it.costPrice }
                    val sumRevenueManual = manualRefurbTrxs.sumOf { it.sellingPrice }
                    
                    sumCost = sumCostDevices + sumCostExpenses + sumCostManual
                    sumRevenue = sumRevenueDevices + sumRevenueManual
                    sumProfit = sumRevenue - sumCost
                } else {
                    val catTransactions = transactions.filter { it.category == cat.id }
                    count = catTransactions.size
                    sumCost = catTransactions.sumOf { it.costPrice }
                    sumRevenue = catTransactions.sumOf { it.sellingPrice }
                    sumProfit = catTransactions.sumOf { it.cashFlow }
                }
                
                Box(
                    modifier = Modifier
                        .height(140.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = if (isLiquidTheme) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.20f),
                                        Color.White.copy(alpha = 0.20f)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(cardBackgroundColor, cardBackgroundColor.copy(alpha = 0.1f))
                                )
                            }
                        )
                        .then(
                            if (isLiquidTheme) Modifier else {
                                Modifier.drawBehind {
                                    drawRoundRect(
                                        brush = Brush.radialGradient(
                                            colors = listOf(cat.color.copy(alpha = if (isDarkTheme) 0.5f else 0.3f), Color.Transparent),
                                            center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.25f),
                                            radius = size.width * 0.7f
                                        )
                                    )
                                }
                            }
                        )
                        .border(
                            width = if (isLiquidTheme) 1.2.dp else 2.dp,
                            brush = if (isLiquidTheme) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.76f),
                                        Color.White.copy(alpha = 0.15f)
                                    )
                                )
                            } else {
                                androidx.compose.ui.graphics.SolidColor(cat.color.copy(alpha = if (isDarkTheme) 0.6f else 0.8f))
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { selectedCategoryForDetail = cat }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Badge (Count and Plus) - Left in RTL
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cat.color.copy(alpha = 0.15f))
                                .clickable {
                                    if (cat.id == "REFURB") {
                                        selectedCategoryForDetail = cat
                                    } else {
                                        onAddTransactionForCategory(cat.id)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = cat.color,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$count ع",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = cat.color
                            )
                        }
                        
                        val catName = when(lang) {
                            "fr" -> cat.nameFr
                            "en" -> cat.nameEn
                            else -> cat.nameAr.replace(Regex(" [^\\w\\sأ-ي].*"), "") // remove emoji for space
                        }

                        // Category Icon and Name - Sharp and Right in RTL
                        Column(
                            modifier = Modifier.align(Alignment.TopStart),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isLiquidTheme) cat.color.copy(alpha = 0.22f)
                                        else (if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.05f))
                                    )
                                    .then(
                                        if (isLiquidTheme) {
                                            Modifier.border(
                                                width = 1.dp,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.70f),
                                                        Color.White.copy(alpha = 0.20f)
                                                    )
                                                ),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.id,
                                    tint = cat.color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = catName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        // Price and Daily Increase at Bottom Left
                        Column(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            horizontalAlignment = Alignment.End
                        ) {
                            val displaySum = if (cat.id == "EXPENSE") {
                                sumCost
                            } else {
                                sumProfit
                            }

                            val isToday: (Long) -> Boolean = { timestamp ->
                                val cal1 = java.util.Calendar.getInstance()
                                val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
                                cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
                            }

                            val todaySum = if (cat.id == "REFURB") {
                                val devicesBoughtToday = refurbishedDevices.filter { isToday(it.createdAt) }
                                val expensesToday = allExpenses.filter { isToday(it.date) }
                                val devicesSoldToday = refurbishedDevices.filter { it.saleDate != null && isToday(it.saleDate) }

                                val totalSpentToday = devicesBoughtToday.sumOf { it.purchasePrice } + expensesToday.sumOf { it.cost }
                                val totalRevenueToday = devicesSoldToday.sumOf { it.salePrice ?: 0.0 }

                                val manualRefurbToday = transactions.filter { trx -> 
                                    trx.category == "REFURB" && 
                                    isToday(trx.date) && 
                                    !trx.title.startsWith("شراء هاتف للاستثمار") && 
                                    !trx.title.contains("تجهيز") && 
                                    !trx.title.startsWith("بيع هاتف استثمار")
                                }
                                val manualSpentToday = manualRefurbToday.sumOf { it.costPrice }
                                val manualRevenueToday = manualRefurbToday.sumOf { it.sellingPrice }

                                (totalRevenueToday + manualRevenueToday) - (totalSpentToday + manualSpentToday)
                            } else {
                                val catTransactions = transactions.filter { it.category == cat.id }
                                if (cat.id == "EXPENSE") {
                                    catTransactions.filter { isToday(it.date) }.sumOf { it.costPrice }
                                } else if (cat.id == "INVENTORY") {
                                    val inventoryToday = catTransactions.filter { isToday(it.date) }
                                    val spentToday = inventoryToday.sumOf { it.costPrice }
                                    val soldToday = inventoryToday.sumOf { it.sellingPrice }
                                    soldToday - spentToday
                                } else {
                                    catTransactions.filter { isToday(it.date) }.sumOf { it.cashFlow }
                                }
                            }

                            val representsExpense = (cat.id == "EXPENSE")

                            val icon = if (representsExpense) {
                                Icons.Default.ArrowDownward
                            } else {
                                if (todaySum < 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
                            }

                            val displayColor = if (todaySum == 0.0) {
                                textColor.copy(alpha = 0.3f)
                            } else if (representsExpense) {
                                Color(0xFFEF4444) // Cash Outflow
                            } else {
                                if (todaySum < 0) Color(0xFFEF4444) else Color(0xFF34D399)
                            }

                            // 1. Accumulative Total (displaySum) displayed LARGE
                            val totalColor = if (isLiquidTheme) {
                                cat.color
                            } else if (representsExpense) {
                                textColor
                            } else {
                                if (displaySum < 0) Color(0xFFEF4444) else if (displaySum > 0) Color(0xFF34D399) else textColor
                            }

                            Text(
                                text = androidx.compose.ui.text.buildAnnotatedString {
                                    val prefix = if (!representsExpense && displaySum > 0) "+" else ""
                                    append(prefix + formatKDCurrency(displaySum))
                                },
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = totalColor,
                                style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr)
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // 2. Daily Increase (todaySum) displayed SMALL
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = displayColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = formatKDCurrency(todaySum),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (todaySum == 0.0) textColor.copy(alpha = 0.5f) else displayColor,
                                    style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr)
                                )
                            }
                        }
                    }
                }
                    } else if (index == categories.size) {
                        // Global Overview Square
                        val primaryColor = MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .height(140.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    brush = if (isLiquidTheme) {
                                        Brush.linearGradient(
                                            colors = if (isDarkTheme) {
                                                listOf(Color(0x351E1E2E), Color(0x101E1E2E))
                                            } else {
                                                listOf(Color(0x80FFFFFF), Color(0x20FFFFFF))
                                            }
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = listOf(cardBackgroundColor, cardBackgroundColor.copy(alpha = 0.1f))
                                        )
                                    }
                                )
                                .drawBehind {
                                    drawRoundRect(
                                        brush = Brush.radialGradient(
                                            colors = listOf(primaryColor.copy(alpha = if (isDarkTheme) 0.5f else 0.3f), Color.Transparent),
                                            center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.25f),
                                            radius = size.width * 0.7f
                                        )
                                    )
                                }
                                .border(
                                    width = if (isLiquidTheme) 1.2.dp else 2.dp,
                                    brush = if (isLiquidTheme) {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = if (isDarkTheme) 0.22f else 0.65f),
                                                primaryColor.copy(alpha = 0.40f),
                                                Color.White.copy(alpha = if (isDarkTheme) 0.05f else 0.15f)
                                            )
                                        )
                                    } else {
                                        androidx.compose.ui.graphics.SolidColor(primaryColor.copy(alpha = if (isDarkTheme) 0.6f else 0.8f))
                                    },
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .clickable { showGlobalStatsDialog = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                // Badge - Left in RTL
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(primaryColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = primaryColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${transactions.size + refurbishedDevices.size} ع",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    )
                                }
                                
                                val globalName = when(lang) {
                                    "fr" -> "Aperçu"
                                    "en" -> "Overview"
                                    else -> "الاحصائيات"
                                }

                                // Icon and Name - Sharp and Right in RTL
                                Column(
                                    modifier = Modifier.align(Alignment.TopStart),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.05f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Leaderboard,
                                            contentDescription = "Total Revenue",
                                            tint = primaryColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = globalName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                // Price at Bottom Left
                                Column(
                                    modifier = Modifier.align(Alignment.BottomEnd),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = formatKDCurrency(totalProfitAll) + (if(totalProfitAll > 0) "+" else ""),
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textColor,
                                        style = androidx.compose.ui.text.TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr)
                                    )
                                }
                            }
                        }
                    }
                }
        }

        // Modal Bottom Sheet displaying repair details inside the chosen Category Section
        val currentDetailCat = selectedCategoryForDetail
        if (currentDetailCat != null) {
            key(currentDetailCat.id) {
                val cat = currentDetailCat
                val catTransactions = transactions.filter { it.category == cat.id }
                val titleText = when (lang) {
                    "fr" -> cat.nameFr
                    "en" -> cat.nameEn
                    else -> cat.nameAr
                }
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                ModalBottomSheet(
                    onDismissRequest = { selectedCategoryForDetail = null },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrimColor = Color.Black.copy(alpha = 0.45f)
                ) {
                    if (cat.id == "REFURB") {
                        RefurbishedDevicesSection(viewModel)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .navigationBarsPadding() // respectful of system notch navigation
                        ) {
                            // Bottom Sheet Header
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
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(cat.color.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = cat.icon,
                                            contentDescription = null,
                                            tint = cat.color,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = titleText,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (lang == "ar") "سجل العمليات الأخير بهذا القسم" else "Recent section operations",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                // Plus shortcut in Sheet
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            sheetState.hide()
                                            selectedCategoryForDetail = null
                                            onAddTransactionForCategory(cat.id)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = cat.color),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Text(
                                            text = if (lang == "ar") "إضافة عملية" else "Ajouter",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(16.dp))

                            // List the actual operations
                            if (catTransactions.isEmpty()) {
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
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Text(
                                            text = if (lang == "ar") "لا توجد معاملات مسجلة في هذا التخصص."
                                            else if (lang == "fr") "Aucune transaction enregistrée."
                                            else "No operations recorded under this section.",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 350.dp) // responsive containment
                                ) {
                                    items(catTransactions.reversed(), key = { it.id }) { trx ->
                                        val personalName = viewModel.walletPersonalName.value
                                        val isExpense = trx.category == "EXPENSE" || trx.wallet == "مصروف شخصي" || trx.wallet == "مصروفي شخصي" || trx.wallet == "مصروفي الشخصي" || trx.wallet == personalName
                                        val itemColor = if (isExpense) {
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
                                                trx.title.contains("📦") || trx.title.contains("أخرى") || trx.title.contains("علبة") -> Color(0xFF607D8B)
                                                else -> Color(0xFF7B1FA2)
                                            }
                                        } else {
                                            cat.color
                                        }

                                        val cardBgColor = itemColor.copy(alpha = 0.15f)
                                        val cardBorderColor = itemColor.copy(alpha = 0.30f)

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    coroutineScope.launch {
                                                        sheetState.hide()
                                                        selectedCategoryForDetail = null
                                                        onTransactionClicked(trx)
                                                    }
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.5.dp, cardBorderColor),
                                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(if (isLiquidTheme) Color.Transparent else MaterialTheme.colorScheme.surface)
                                                    .background(cardBgColor)
                                            ) {
                                                // Watermark Icon replacing letters
                                                Icon(
                                                    imageVector = cat.icon,
                                                    contentDescription = null,
                                                    tint = itemColor.copy(alpha = 0.08f),
                                                    modifier = Modifier
                                                        .align(Alignment.CenterEnd)
                                                        .offset(x = 10.dp)
                                                        .size(64.dp)
                                                        .graphicsLayer { rotationZ = -15f }
                                                )

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        // Neon vertical bar
                                                        Box(
                                                            modifier = Modifier
                                                                .width(4.dp)
                                                                .height(32.dp)
                                                                .clip(RoundedCornerShape(2.dp))
                                                                .background(itemColor)
                                                        )

                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .clip(CircleShape)
                                                                .background(itemColor.copy(alpha = 0.15f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = cat.icon,
                                                                contentDescription = null,
                                                                tint = itemColor,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }

                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = trx.title,
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                if (trx.deviceModel.isNotEmpty()) {
                                                                    Text(
                                                                        text = trx.deviceModel,
                                                                        fontSize = 10.sp,
                                                                        color = MaterialTheme.colorScheme.primary,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                                if (trx.customerName.isNotEmpty()) {
                                                                    Text(
                                                                        text = "• " + trx.customerName,
                                                                        fontSize = 10.sp,
                                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                                    )
                                                                }
                                                            }
                                                            
                                                            if (trx.notes.isNotBlank()) {
                                                                Text(
                                                                    text = trx.notes,
                                                                    fontSize = 10.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Column(horizontalAlignment = Alignment.End) {
                                                        val (primaryAmount, primaryColor, prefix) = when (trx.category) {
                                                            "EXPENSE" -> {
                                                                Triple(trx.costPrice, Color(0xFFEF4444), "-")
                                                            }
                                                            "DEBT" -> {
                                                                val isOwedToMe = trx.costPrice >= trx.sellingPrice
                                                                if (trx.isDelivered) {
                                                                    Triple(0.0, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), "+")
                                                                } else {
                                                                    if (isOwedToMe) {
                                                                        val unpaid = trx.costPrice - trx.sellingPrice
                                                                        Triple(unpaid, Color(0xFFEF4444), "-")
                                                                    } else {
                                                                        val unpaid = trx.sellingPrice - trx.costPrice
                                                                        Triple(unpaid, Color(0xFF10B981), "+")
                                                                    }
                                                                }
                                                            }
                                                            else -> {
                                                                if (trx.isDelivered || trx.isPrepaid) {
                                                                    Triple(trx.profit, Color(0xFF10B981), "+")
                                                                } else {
                                                                    Triple(trx.costPrice, Color(0xFFEF4444), "-")
                                                                }
                                                            }
                                                        }
                                                        Text(
                                                            text = prefix + formatWithLoc(primaryAmount, lang),
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = primaryColor
                                                        )
                                                        
                                                        if (!isExpense && trx.costPrice > 0) {
                                                            Text(
                                                                text = "الشراء: -${formatWithLoc(trx.costPrice, lang)}",
                                                                textDecoration = if (trx.isDelivered || trx.isPrepaid) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                                                fontSize = 10.sp,
                                                                color = Color(0xFFEF4444),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }

                                                        val isStockTransfer = trx.category == "INVENTORY" || (trx.category == "REFURB" && !trx.isDelivered)
                                                        if (trx.category != "EXPENSE" && trx.category != "DEBT" && (trx.isDelivered || trx.isPrepaid)) {
                                                            Text(
                                                                text = "${if (lang == "ar") "البيع" else "Vente"}: ${formatWithLoc(trx.sellingPrice, lang)}",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                            )
                                                        } else if (!isExpense && !isStockTransfer) {
                                                            val netProfit = trx.profit
                                                            val profitLbl = if (trx.category == "DEBT") {
                                                                if (lang == "ar") "الباقي" else "Solde"
                                                            } else {
                                                                if (netProfit >= 0) (if (lang == "ar") "الربح" else "Profit") else (if (lang == "ar") "الخسارة" else "Perte")
                                                            }
                                                            val profitColor = if (netProfit >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                                            val profitPrefix = if (netProfit >= 0) "+" else ""
                                                            Text(
                                                                text = "$profitLbl: $profitPrefix${formatWithLoc(netProfit, lang)}",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = profitColor
                                                            )
                                                        }

                                                        Text(
                                                            text = formatDate(trx.date),
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }

        if (showGlobalStatsDialog) {
            GlobalStatsDialog(
                viewModel = viewModel,
                onDismiss = { showGlobalStatsDialog = false }
            )
        }
    }
}

// Data model holding category basic info
data class CategorySectionInfo(
    val id: String,
    val nameAr: String,
    val nameFr: String,
    val nameEn: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@Composable
fun GlobalStatsDialog(
    viewModel: WorkshopViewModel,
    onDismiss: () -> Unit
) {
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val stats by viewModel.statsFlow.collectAsStateWithLifecycle()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lang == "ar") "إغلاق" else "Fermer")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Leaderboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(if (lang == "ar") "تفاصيل الإحصائيات المالية" else "Statistiques Financières", fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Main Metric Cards
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatsMetricRow(
                        label = if (lang == "ar") "المجموع المحصل" else "Total Encaissé",
                        amount = stats.totalRevenue,
                        color = Color(0xFF4CAF50),
                        icon = Icons.Default.AddChart,
                        lang = lang
                    )
                    StatsMetricRow(
                        label = if (lang == "ar") "تكاليف قطع الغيار" else "Coût des Pièces",
                        amount = stats.partsCost,
                        color = Color(0xFF0288D1),
                        icon = Icons.Default.Build,
                        lang = lang
                    )
                    StatsMetricRow(
                        label = if (lang == "ar") "مصاريف المحل والتشغيل" else "Dépenses Atelier",
                        amount = stats.shopExpenses,
                        color = Color(0xFFFF9800),
                        icon = Icons.Default.Payments,
                        lang = lang
                    )
                    StatsMetricRow(
                        label = if (lang == "ar") "صافي ربح الورشة" else "Bénéfice Atelier",
                        amount = stats.workshopNetProfit,
                        color = Color(0xFF2E7D32),
                        icon = Icons.Default.TrendingUp,
                        isBold = true,
                        lang = lang
                    )
                    StatsMetricRow(
                        label = if (lang == "ar") "المصاريف الشخصية" else "Dépenses Personnelles",
                        amount = stats.personalExpenses,
                        color = Color(0xFFEC407A),
                        icon = Icons.Default.Person,
                        lang = lang
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    StatsMetricRow(
                        label = if (lang == "ar") "الربح المتبقي للادخار" else "Bénéfice Restant",
                        amount = stats.totalProfit,
                        color = MaterialTheme.colorScheme.primary,
                        icon = Icons.Default.AccountBalanceWallet,
                        isBold = true,
                        lang = lang
                    )
                }

                // Simple Bar Chart
                Text(
                    text = if (lang == "ar") "مخطط بياني للمقارنة" else "Graphique de Comparaison",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                FinancialBarChart(
                    revenue = stats.totalRevenue,
                    partsCost = stats.partsCost,
                    expenses = stats.expensesCost,
                    profit = stats.totalProfit,
                    lang = lang,
                    personalExpenses = stats.personalExpenses,
                    shopExpenses = stats.shopExpenses,
                    workshopDebts = stats.workshopDebts,
                    personalDebtsOwedToMe = stats.personalDebtsOwedToMe,
                    personalDebtsOwedByMe = stats.personalDebtsOwedByMe
                )
                
                Spacer(modifier = Modifier.height(10.dp))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun StatsMetricRow(
    label: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isBold: Boolean = false,
    lang: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isBold) FontWeight.Black else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = formatWithLoc(amount, lang),
                fontSize = if (isBold) 16.sp else 14.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun FinancialBarChart(
    revenue: Double,
    partsCost: Double,
    expenses: Double,
    profit: Double,
    lang: String,
    personalExpenses: Double? = null,
    shopExpenses: Double? = null,
    workshopDebts: Double = 0.0,
    personalDebtsOwedToMe: Double = 0.0,
    personalDebtsOwedByMe: Double = 0.0
) {
    val maxVal = maxOf(
        revenue, 
        partsCost, 
        expenses, 
        profit, 
        workshopDebts, 
        personalDebtsOwedToMe, 
        personalDebtsOwedByMe
    ).coerceAtLeast(1.0)
    
    val items = mutableListOf<ChartItem>()
    items.add(ChartItem(if (lang == "ar") "المداخيل" else "Revenus", revenue, Color(0xFF4CAF50)))
    items.add(ChartItem(if (lang == "ar") "قطع الغيار" else "Pièces", partsCost, Color(0xFF0288D1)))
    
    if (workshopDebts > 0.0) {
        items.add(ChartItem(if (lang == "ar") "ديون الورشة" else "Dettes Atelier", workshopDebts, Color(0xFFE53935)))
    }
    if (personalDebtsOwedToMe > 0.0) {
        items.add(ChartItem(if (lang == "ar") "ديون شخصية لنا" else "Dettes à nous", personalDebtsOwedToMe, Color(0xFF00796B)))
    }
    if (personalDebtsOwedByMe > 0.0) {
        items.add(ChartItem(if (lang == "ar") "ديون شخصية علينا" else "Dettes sur nous", personalDebtsOwedByMe, Color(0xFF8D6E63)))
    }

    if (personalExpenses != null && shopExpenses != null) {
        if (shopExpenses > 0.0) {
            items.add(ChartItem(if (lang == "ar") "مصاريف المحل" else "Dépenses Atelier", shopExpenses, Color(0xFFFF9800)))
        }
        if (personalExpenses > 0.0) {
            items.add(ChartItem(if (lang == "ar") "المصاريف الشخصية" else "Dépenses Personnelles", personalExpenses, Color(0xFFEC407A)))
        }
    } else {
        if (expenses > 0.0) {
            items.add(ChartItem(if (lang == "ar") "المصاريف" else "Dépenses", expenses, Color(0xFFE53935)))
        }
    }
    
    items.add(ChartItem(if (lang == "ar") "صافي الربح" else "Profit", profit, MaterialTheme.colorScheme.primary))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatWithLoc(item.value, lang), fontSize = 10.sp, color = item.color, fontWeight = FontWeight.Black)
                }
                
                val progress = (item.value / maxVal).coerceIn(0.0, 1.0).toFloat()
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(item.color)
                    )
                }
            }
        }
    }
}

data class ChartItem(val label: String, val value: Double, val color: Color)

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun formatWithLoc(amount: Double, lang: String): String {
    return try {
        val symbols = java.text.DecimalFormatSymbols(Locale.ENGLISH).apply {
            groupingSeparator = ' '
        }
        val formatted = java.text.DecimalFormat("#,###", symbols).format(amount)
        "\u200E$formatted DA\u200E"
    } catch (e: Exception) {
        val formatted = String.format("%,d", amount.toLong()).replace(',', ' ')
        "\u200E$formatted DA\u200E"
    }
}

private fun formatKDCurrency(amount: Double): String {
    return formatWithLoc(amount, "ar")
}
