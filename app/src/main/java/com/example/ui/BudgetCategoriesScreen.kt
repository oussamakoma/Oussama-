package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.WorkshopViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.TransactionListItem
import com.example.AddEditTransactionDialog
import com.example.ui.theme.GlassmorphicCard
import com.example.ui.theme.LocalIsLiquidTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.nativeCanvas

data class BudgetCategory(
    val name: String,
    val spent: Double,
    val total: Double,
    val percentageChange: String,
    val lastMonthSpent: Double
)

@Composable
fun BudgetCategoriesScreen(viewModel: WorkshopViewModel) {
    val categories by viewModel.budgetCategories.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<BudgetCategory?>(null) }
    var categoryToEditLimit by remember { mutableStateOf<BudgetCategory?>(null) }
    var showChartDialog by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "الميزانيات الشهرية",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { showChartDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = "Show Chart",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Prominent Statistical Summary Card
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                cornerRadius = 24.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "ملخص المصاريف",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val totalSpentThisMonth = categories.sumOf { it.spent }
                        Text(
                            text = "إجمالي مصاريف الشهر: ${"%,.0f".format(totalSpentThisMonth)} د.ج",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Button(
                        onClick = { showChartDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text("الرسوم البيانية", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(categories, key = { it.name }) { category ->
                    BudgetCategoryItem(
                        category = category,
                        onEditLimit = { categoryToEditLimit = category },
                        onClick = { selectedCategory = category }
                    )
                }
            }
        }

        if (selectedCategory != null) {
            TransactionListDetailScreen(
                category = selectedCategory!!,
                viewModel = viewModel,
                onDismiss = { selectedCategory = null }
            )
        }

        if (categoryToEditLimit != null) {
            EditBudgetLimitDialog(
                category = categoryToEditLimit!!,
                onDismiss = { categoryToEditLimit = null },
                onSave = { newLimit ->
                    viewModel.updateBudgetLimit(categoryToEditLimit!!.name, newLimit)
                    categoryToEditLimit = null
                }
            )
        }

        if (showChartDialog) {
            BudgetPieChartDialog(
                viewModel = viewModel,
                onDismiss = { showChartDialog = false }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetPieChartDialog(
    viewModel: WorkshopViewModel,
    onDismiss: () -> Unit
) {
    var startDate by remember { 
        mutableStateOf(Calendar.getInstance().apply { 
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis) 
    }
    var endDate by remember { 
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis) 
    }
    
    val categories by viewModel.getCategoriesForRange(startDate, endDate).collectAsStateWithLifecycle(emptyList())
    val totalSpent = categories.sumOf { it.spent }
    val lastMonthTotalSpent = categories.sumOf { it.lastMonthSpent }
    val expenseCategories = categories.filter { it.spent > 0 }.sortedByDescending { it.spent }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    val chartColors = listOf(
        Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853),
        Color(0xFF9C27B0), Color(0xFFFF6D00), Color(0xFF00ACC1), Color(0xFF795548),
        Color(0xFF607D8B), Color(0xFFE91E63), Color(0xFF2196F3), Color(0xFF4CAF50)
    )

    val diffPercent = if (lastMonthTotalSpent > 0) {
        ((totalSpent - lastMonthTotalSpent) / lastMonthTotalSpent) * 100
    } else if (totalSpent > 0) {
        100.0
    } else {
        0.0
    }

    val percentageChangeText = if (diffPercent > 0) {
        "+${"%.1f".format(diffPercent)}%"
    } else if (diffPercent < 0) {
        "${"%.1f".format(diffPercent)}%"
    } else {
        "0.0%"
    }
    
    val trendColor = if (diffPercent > 0) MaterialTheme.colorScheme.error 
                     else if (diffPercent < 0) com.example.ui.theme.ProfitGreen 
                     else MaterialTheme.colorScheme.onSurfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("إغلاق")
            }
        },
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("تحليل المصاريف", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                // Date Filter UI
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            modifier = Modifier.clickable { showStartDatePicker = true },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("من", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateFormatter.format(startDate), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        
                        Column(
                            modifier = Modifier.clickable { showEndDatePicker = true },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("إلى", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateFormatter.format(endDate), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        text = {
            if (totalSpent <= 0) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("لا توجد مصاريف مسجلة لهذه الفترة")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Custom Donut Chart with Labels
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(240.dp)) {
                            val canvasSize = size
                            val radius = canvasSize.minDimension / 2.8f
                            val innerRadius = radius * 0.75f
                            val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                            
                            var startAngle = -90f
                            
                            expenseCategories.forEachIndexed { index, category ->
                                val sweepAngle = (category.spent / totalSpent * 360f).toFloat()
                                val color = chartColors[index % chartColors.size]
                                
                                // Draw Arc (Donut Segment)
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = radius - innerRadius)
                                )
                                
                                // Draw Label lines and percentages
                                val middleAngle = startAngle + sweepAngle / 2f
                                val angleRad = Math.toRadians(middleAngle.toDouble())
                                
                                val lineStart = Offset(
                                    (center.x + Math.cos(angleRad) * radius).toFloat(),
                                    (center.y + Math.sin(angleRad) * radius).toFloat()
                                )
                                
                                val lineEnd = Offset(
                                    (center.x + Math.cos(angleRad) * (radius + 20f)).toFloat(),
                                    (center.y + Math.sin(angleRad) * (radius + 20f)).toFloat()
                                )
                                
                                val horizontalLineEnd = Offset(
                                    if (lineEnd.x > center.x) lineEnd.x + 30f else lineEnd.x - 30f,
                                    lineEnd.y
                                )
                                
                                // Only draw labels for segments > 1% to avoid clutter
                                if (sweepAngle > 3.6f) {
                                    drawLine(
                                        color = Color.Gray.copy(alpha = 0.5f),
                                        start = lineStart,
                                        end = lineEnd,
                                        strokeWidth = 1f
                                    )
                                    drawLine(
                                        color = Color.Gray.copy(alpha = 0.5f),
                                        start = lineEnd,
                                        end = horizontalLineEnd,
                                        strokeWidth = 1f
                                    )
                                    
                                    val percentageText = "%.1f %%".format(category.spent / totalSpent * 100)
                                    val textAnchor = if (lineEnd.x > center.x) horizontalLineEnd.x + 5f else horizontalLineEnd.x - 45f
                                    
                                    drawContext.canvas.nativeCanvas.drawText(
                                        percentageText,
                                        textAnchor,
                                        horizontalLineEnd.y + 5f,
                                        android.graphics.Paint().apply {
                                            setColor(android.graphics.Color.GRAY)
                                            textSize = 24f
                                            isAntiAlias = true
                                        }
                                    )
                                }
                                
                                startAngle += sweepAngle
                            }
                        }
                        
                        // Center Text - Updated to Red Bold Total and comparison
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%,.0f".format(totalSpent),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Red // Set to Red Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = percentageChangeText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = trendColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(سابقاً: %,.0f)".format(lastMonthTotalSpent),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
// Legend (Refined)
                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            expenseCategories.forEachIndexed { index, category ->
                                val percentage = (category.spent / totalSpent * 100)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(chartColors[index % chartColors.size])
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = category.name,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${"%,.0f".format(category.spent)} د.ج",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${ "%.1f".format(percentage) }%",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(28.dp)
    )

    if (showStartDatePicker) {
        CalendarDatePickerDialog(
            initialDate = startDate,
            onDateSelected = { 
                startDate = it
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        CalendarDatePickerDialog(
            initialDate = endDate,
            onDateSelected = { 
                endDate = it
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

/**
 * A proper Material 3 calendar-based date picker dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDatePickerDialog(
    initialDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                }
            ) {
                Text("تأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun EditBudgetLimitDialog(
    category: BudgetCategory,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var limitStr by rememberSaveable { mutableStateOf(category.total.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحديد ميزانية ${category.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("أدخل الحد الأقصى للميزانية الشهرية لهذا الصنف:", fontSize = 14.sp)
                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("الميزانية (د.ج)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitStr.toDoubleOrNull() ?: category.total
                    onSave(limit)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ")
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
fun TransactionListDetailScreen(category: BudgetCategory, viewModel: WorkshopViewModel, onDismiss: () -> Unit) {
    val transactions by viewModel.getTransactionsForBudgetCategory(category.name).collectAsStateWithLifecycle(emptyList())
    var transactionToEdit by remember { mutableStateOf<com.example.data.model.WorkshopTransaction?>(null) }

    val isLiquidTheme = LocalIsLiquidTheme.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isLiquidTheme) Color.Transparent else MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(text = "تفاصيل ${category.name}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionListItem(
                        transaction = transaction,
                        onClick = { transactionToEdit = transaction },
                        onDelete = {},
                        onToggleDelivery = { viewModel.toggleTransactionDelivery(transaction) }
                    )
                }
            }
        }
        if (transactionToEdit != null) {
            AddEditTransactionDialog(
                viewModel = viewModel,
                transaction = transactionToEdit,
                onDismiss = { transactionToEdit = null },
                onDelete = {
                    viewModel.deleteTransaction(transactionToEdit!!)
                    transactionToEdit = null
                },
                onSave = { title, category, cost, sale, model, name, notes, creditAmount, creditPaid, wallet, dueDate, tDate, isDelivered, affectBalance, isPrepaid ->
                    viewModel.deleteTransaction(transactionToEdit!!)
                    viewModel.addTransaction(title, category, cost, sale, model, name, notes, creditAmount, creditPaid, wallet, dueDate, tDate, isDelivered, affectBalance, isPrepaid)
                    transactionToEdit = null
                }
            )
        }
    }
}

fun getIconForCategory(name: String): ImageVector {
    return when (name) {
        "الغذاء" -> Icons.Default.Fastfood
        "الأسرة" -> Icons.Default.Home
        "مواصلات" -> Icons.Default.DirectionsCar
        "إنترنت" -> Icons.Default.Wifi
        "فواتير" -> Icons.Default.Receipt
        "قهوة" -> Icons.Default.Coffee
        "صحة وعلاج" -> Icons.Default.MedicalServices
        "إيجار" -> Icons.Default.Apartment
        "تسوق" -> Icons.Default.ShoppingBag
        "أخرى" -> Icons.Default.Inventory
        else -> Icons.Default.ShoppingCart
    }
}

@Composable
fun BudgetCategoryItem(category: BudgetCategory, onEditLimit: () -> Unit, onClick: () -> Unit) {
    val progress = (category.spent / category.total).coerceIn(0.0, 2.0).toFloat()
    val isExceeded = category.spent > category.total
    
    val isLiquidTheme = LocalIsLiquidTheme.current
    val isDark = isSystemInDarkTheme()
    
    val cardBg = if (isLiquidTheme) {
        if (isExceeded) {
            Color(0x35E53935) // Light red glass for exceeded budgets
        } else {
            if (isDark) Color(0x351E1E2E) else Color(0x75FFFFFF)
        }
    } else {
        if (isExceeded) MaterialTheme.colorScheme.errorContainer 
        else MaterialTheme.colorScheme.surfaceVariant
    }

    val cardBorder = if (isLiquidTheme) {
        BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                colors = if (isExceeded) {
                    listOf(Color(0xFFE53935), Color(0xFFFFCDD2).copy(alpha = 0.40f), Color(0xFFE53935).copy(alpha = 0.20f))
                } else {
                    if (isDark) {
                        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f))
                    } else {
                        listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.12f))
                    }
                }
            )
        )
    } else {
        BorderStroke(
            width = 1.dp,
            color = if (isExceeded) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isLiquidTheme) {
                    Modifier.border(cardBorder, RoundedCornerShape(16.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLiquidTheme) 0.dp else 2.dp),
        border = if (isLiquidTheme) null else cardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForCategory(category.name),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = category.name, 
                            fontWeight = FontWeight.ExtraBold, 
                            fontSize = 18.sp, 
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (isExceeded) {
                                Text(
                                    text = "تجاوزت الميزانية!",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Display percentage change from last month
                            val isIncrease = category.percentageChange.startsWith("+")
                            val isDecrease = category.percentageChange.startsWith("-")
                            val trendColor = if (isIncrease) MaterialTheme.colorScheme.error 
                                             else if (isDecrease) com.example.ui.theme.ProfitGreen 
                                             else MaterialTheme.colorScheme.onSurfaceVariant
                            
                            Surface(
                                color = trendColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${category.percentageChange} (${"%,.0f".format(category.lastMonthSpent)} د.ج) مقارنة بالشهر الماضي",
                                    color = trendColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                
                IconButton(onClick = onEditLimit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Budget",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = progress.coerceAtMost(1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "المصروف الحالي", 
                        fontSize = 11.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${"%,.0f".format(category.spent)} د.ج", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Black,
                        color = if (isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "الميزانية الكلية", 
                        fontSize = 11.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${"%,.0f".format(category.total)} د.ج", 
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
