package com.example.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RefurbishedDevice
import com.example.ui.viewmodel.WorkshopViewModel
import java.text.DecimalFormat
import java.util.Locale

// Colors for Financial Interface
private val ProfitGreen = Color(0xFF00C853)
private val ProfitGreenLight = Color(0xFFE8F5E9)
private val ExpenseRed = Color(0xFFD50000)
private val SoldBlue = Color(0xFF2962FF)

private fun formatCurrency(amount: Double): String {
    val symbols = java.text.DecimalFormatSymbols(Locale.US)
    return java.text.DecimalFormat("#,##0", symbols).format(amount) + " د.ج"
}

@Composable
fun RefurbishedDevicesSection(viewModel: WorkshopViewModel) {
    val devices by viewModel.refurbishedDevicesFlow.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = "New Investment") },
                text = { Text("استثمار جديد", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Dashboard
                item {
                    val totalInvested = devices.filter { it.salePrice == null }.sumOf { device ->
                        // Approximating un-sold devices cost if we can't do blocking collect here. 
                        // In real scenario we'd track total in viewmodel. Let's just sum base prices for simple dashboard.
                        device.purchasePrice
                    }
                    val realizedProfit = devices.filter { it.salePrice != null }.sumOf { device ->
                        // This is an approximation since expenses aren't aggregated here.
                        // For exact math we will calculate per-card accurately.
                        (device.salePrice ?: 0.0) - device.purchasePrice
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "لوحة أرباح إعادة التدوير (Flipping)",
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("أجهزة متاحة", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Text("${devices.count { it.salePrice == null }}", color = MaterialTheme.colorScheme.onPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                                Divider(modifier = Modifier.height(30.dp).width(1.dp), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("تم بيعها", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Text("${devices.count { it.salePrice != null }}", color = MaterialTheme.colorScheme.onPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (devices.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("لا توجد استثمارات مسجلة.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text("ابدأ بشراء أول هاتف لإعادة تجهيزه وبيعه!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                } else {
                    items(devices) { device ->
                        DeviceFlippingCard(device, viewModel)
                    }
                }
            }
        }

        if (showAddDialog) {
            AddDeviceDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, serial, price, date, photoUriStr ->
                    val theDate = try {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).parse(date)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                    viewModel.addDevice(name, serial, price, theDate) 
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun DeviceFlippingCard(device: RefurbishedDevice, viewModel: WorkshopViewModel) {
    val expenses by viewModel.getMaintenanceExpensesFlow(device.id).collectAsState(initial = emptyList())
    val totalMaintenance = expenses.sumOf { it.cost }
    val totalInvestment = device.purchasePrice + totalMaintenance
    var showAddExpense by remember { mutableStateOf(false) }
    var showSellDialog by remember { mutableStateOf(false) }
    val isSold = device.salePrice != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Name and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = device.deviceName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("SN: ${device.serialNumber.ifBlank { "غير مسجل" }}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "وقت الشراء",
                            tint = ProfitGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                        Text(
                            text = "شراء: ${dateFormat.format(java.util.Date(device.createdAt))}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSold) SoldBlue.copy(alpha = 0.1f) else ProfitGreen.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isSold) "مُباع" else "في الورشة",
                        color = if (isSold) SoldBlue else ProfitGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Financial Summary Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FinanceMetric(label = "سعر الشراء", value = formatCurrency(device.purchasePrice))
                FinanceMetric(label = "+ تكاليف تجهيز", value = formatCurrency(totalMaintenance), color = ExpenseRed)
                FinanceMetric(label = "= التكلفة الكلية", value = formatCurrency(totalInvestment), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            // Expenses Breakdown
            if (expenses.isNotEmpty() && !isSold) {
                Text("تفاصيل التجهيز:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(4.dp))
                expenses.forEach { exp ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.width(4.dp))
                            val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                            Text("${exp.partName} - ${dateFormat.format(java.util.Date(exp.date))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatCurrency(exp.cost), fontSize = 12.sp, color = ExpenseRed.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.deleteExpense(exp) }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Expense", tint = ExpenseRed, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Results / Actions
            if (isSold) {
                val profit = device.salePrice!! - totalInvestment
                val isProfit = profit >= 0
                val margin = if (totalInvestment > 0) (profit / totalInvestment) * 100 else 0.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isProfit) ProfitGreenLight else ExpenseRed.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("سعر البيع النهائي: ${formatCurrency(device.salePrice!!)}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        if (device.saleDate != null) {
                            val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventAvailable,
                                    contentDescription = "وقت البيع",
                                    tint = SoldBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "بيع في: ${dateFormat.format(java.util.Date(device.saleDate))}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                )
                            }
                        }
                        if (!device.customerName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "اسم الزبون",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "الزبون: ${device.customerName}" + if (device.isCreditSale) " (باقي: ${formatCurrency(device.salePrice!! - device.downPayment)})" else "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (!device.saleNotes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notes,
                                    contentDescription = "ملاحظة",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "ملاحظة: ${device.saleNotes}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isProfit) ProfitGreen else ExpenseRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isProfit) "صافي الربح: ${formatCurrency(profit)}" else "نسبة الخسارة: ${formatCurrency(profit)}",
                                color = if (isProfit) ProfitGreen else ExpenseRed,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text("هامش الفائدة: ${String.format("%.1f", margin)}%", fontSize = 11.sp, color = if (isProfit) ProfitGreen else ExpenseRed)
                    }
                }
                
                // Add Restore control for sold device
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.restoreDevice(device.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("استرجاع إلى الورشة (إلغاء البيع)", fontSize = 12.sp)
                }

            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddExpense = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Handyman, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة قطعة", fontSize = 13.sp)
                    }
                    Button(
                        onClick = { showSellDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                    ) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("طرح للبيع", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Extra controls (Delete)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { viewModel.deleteDevice(device) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ExpenseRed, modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            if (showAddExpense) {
                AddExpenseDialog(
                    onDismiss = { showAddExpense = false },
                    onConfirm = { partName, cost, date ->
                        val theDate = try {
                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).parse(date)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                        viewModel.addMaintenanceExpense(device.id, partName, cost, theDate)
                        showAddExpense = false
                    }
                )
            }

            if (showSellDialog) {
                SellDeviceDialog(
                    initialCost = totalInvestment,
                    onDismiss = { showSellDialog = false },
                    onConfirm = { price, isCredit, down, cust, date, notes ->
                        viewModel.sellDevice(device.id, price, isCredit, down, cust, date, totalInvestment, notes)
                        showSellDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun FinanceMetric(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface, fontWeight: FontWeight = FontWeight.Normal) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, color = color, fontWeight = fontWeight)
    }
}

@Composable
fun AddExpenseDialog(onDismiss: () -> Unit, onConfirm: (String, Double, String) -> Unit) {
    var partName by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    
    val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()) }
    var dateStr by remember { mutableStateOf(dateFormat.format(java.util.Date())) }
    
    var validationError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("تسجيل نفقة تجهیز", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                if (validationError != null) {
                    Text(validationError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                OutlinedTextField(
                    value = partName,
                    onValueChange = { partName = it },
                    label = { Text("اسم القطعة (مثل: بطارية، شاشة)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("تكلفة الشراء (د.ج)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = dateStr, 
                    onValueChange = { dateStr = it }, 
                    label = { Text("التاريخ (YYYY-MM-DD HH:mm)") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val cost = costStr.toDoubleOrNull()
                    if (partName.isBlank() || cost == null || cost <= 0) {
                        validationError = "الرجاء إدخال اسم القطعة وتكلفة صحيحة ⚠️"
                    } else {
                        onConfirm(partName, cost, dateStr) 
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) { 
                Text("إضافة التكلفة", fontWeight = FontWeight.Bold) 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", fontWeight = FontWeight.SemiBold) }
        }
    )
}

@Composable
fun AddDeviceDialog(onDismiss: () -> Unit, onConfirm: (String, String, Double, String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    
    val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()) }
    var dateStr by remember { mutableStateOf(dateFormat.format(java.util.Date())) }
    
    var photoUri by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        photoUri = uri?.toString()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("تسجيل استثمار جديد 📦", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                if (validationError != null) {
                    Text(validationError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    IconButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (photoUri != null) ProfitGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = if (photoUri != null) ProfitGreen else MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        if (photoUri != null) "تم إرفاق صورة ✅" else "إرفاق صورة للجهاز", 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("صنف الهاتف (مثال: iPhone 11)") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                OutlinedTextField(
                    value = serial, 
                    onValueChange = { serial = it }, 
                    label = { Text("رقم IMEI / التسلسلي (اختياري)") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                OutlinedTextField(
                    value = priceStr, 
                    onValueChange = { priceStr = it }, 
                    label = { Text("الثمن / سعر الشراء (رأس المال د.ج)") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                OutlinedTextField(
                    value = dateStr, 
                    onValueChange = { dateStr = it }, 
                    label = { Text("التاريخ (YYYY-MM-DD HH:mm)") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val price = priceStr.toDoubleOrNull()
                    if (name.isBlank() || price == null || price < 0) {
                        validationError = "الرجاء إدخال صنف الهاتف وسعر صحيح ⚠️"
                    } else {
                        onConfirm(name, serial, price, dateStr, photoUri)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)
            ) { 
                Text("تسجيل في الترسانة", fontWeight = FontWeight.Bold) 
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(bottom = 4.dp)
            ) { 
                Text("تراجع", fontWeight = FontWeight.SemiBold) 
            }
        }
    )
}

@Composable
fun SellDeviceDialog(initialCost: Double, onDismiss: () -> Unit, onConfirm: (Double, Boolean, Double, String?, Long, String?) -> Unit) {
    var priceStr by remember { mutableStateOf("") }
    var isCredit by remember { mutableStateOf(false) }
    var downPaymentStr by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var saleNotes by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    
    val currentInput = priceStr.toDoubleOrNull() ?: 0.0
    val expectedProfit = currentInput - initialCost

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إغلاق وإتمام البيع 🤝", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (validationError != null) {
                    Text(validationError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text(if (isCredit) "إجمالي الثمن / سعر البيع (الكريدي)" else "المبلغ المقبوض / الثمن الفعلي (د.ج)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCredit, onCheckedChange = { isCredit = it })
                    Text("بيع بالتقسيط (كريدي)")
                }

                // Always show Customer Name and Sale Notes fields
                OutlinedTextField(
                    value = customerName, 
                    onValueChange = { customerName = it }, 
                    label = { Text(if (isCredit) "اسم الزبون (إجباري للكريدي)" else "اسم الزبون (اختياري)") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true
                )

                OutlinedTextField(
                    value = saleNotes, 
                    onValueChange = { saleNotes = it }, 
                    label = { Text("ملاحظة البيع (اختياري)") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true
                )

                if (isCredit) {
                    OutlinedTextField(
                        value = downPaymentStr, 
                        onValueChange = { downPaymentStr = it }, 
                        label = { Text("الدفعة الأولى (إن وجدت)") }, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        modifier = Modifier.fillMaxWidth(), 
                        singleLine = true
                    )
                }

                // Real-time calculation visual
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("التكلفة الكلية لتجهيزه: ${formatCurrency(initialCost)}", fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (expectedProfit >= 0) "الربح المتوقع: ${formatCurrency(expectedProfit)}" else "الخسارة المتوقعة: ${formatCurrency(expectedProfit)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (expectedProfit >= 0) ProfitGreen else ExpenseRed
                        )
                        if (isCredit) {
                            val down = downPaymentStr.toDoubleOrNull() ?: 0.0
                            val rest = currentInput - down
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("المتبقي عليه: ${formatCurrency(rest)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val sellPrice = priceStr.toDoubleOrNull()
                    val down = downPaymentStr.toDoubleOrNull() ?: 0.0
                    if (sellPrice == null || sellPrice < 0) {
                        validationError = "الرجاء إدخال مبلغ صحيح ⚠️"
                    } else if (isCredit && customerName.isBlank()) {
                        validationError = "الرجاء إدخال اسم الزبون للكريدي ⚠️"
                    } else {
                        onConfirm(
                            sellPrice, 
                            isCredit, 
                            down, 
                            customerName.ifBlank { null }, 
                            System.currentTimeMillis(), 
                            saleNotes.ifBlank { null }
                        ) 
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
            ) { Text("تأكيد البيع") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("تراجع") }
        }
    )
}
