package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkshopTransaction
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.SoftwarePurple
import com.example.ui.theme.GeneralBlue
import com.example.ui.theme.AccessoryOrange
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionListItem(
    transaction: WorkshopTransaction,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    showDeleteOption: Boolean = false,
    onToggleDelivery: (() -> Unit)? = null
) {
    val categoryDetails = getCategoryDetails(transaction.category)
    val formattedDate = remember(transaction.date) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        sdf.format(Date(transaction.date))
    }

    val isExpense = transaction.category == "EXPENSE" || transaction.wallet == "مصروف شخصي"
    val itemColor = if (isExpense) {
        when {
            transaction.title.contains("🚌") || transaction.title.contains("مواصلات") -> Color(0xFF00BCD4)
            transaction.title.contains("🏠") || transaction.title.contains("إيجار") -> Color(0xFF3F51B5)
            transaction.title.contains("🌐") || transaction.title.contains("إنترنت") || transaction.title.contains("ويفي") || transaction.title.contains("Wifi") -> Color(0xFF2196F3)
            transaction.title.contains("🤲") || transaction.title.contains("صدقة") -> Color(0xFF4CAF50)
            transaction.title.contains("🧾") || transaction.title.contains("فواتير") -> Color(0xFFFF9800)
            transaction.title.contains("🍔") || transaction.title.contains("غذاء") -> Color(0xFFE91E63)
            transaction.title.contains("☕") || transaction.title.contains("قهوة") -> Color(0xFF795548)
            transaction.title.contains("🛒") || transaction.title.contains("تسوق") -> Color(0xFF9C27B0)
            transaction.title.contains("🏥") || transaction.title.contains("صحة") -> Color(0xFFF44336)
            transaction.title.contains("📦") || transaction.title.contains("أخرى") || transaction.title.contains("علبة") -> Color(0xFF607D8B)
            else -> Color(0xFF7B1FA2)
        }
    } else {
        categoryDetails.color
    }

    val isLiquidTheme = com.example.ui.theme.LocalIsLiquidTheme.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val cardBgColor = remember(itemColor, isLiquidTheme) {
        if (isLiquidTheme) {
            itemColor.copy(alpha = 0.12f)
        } else {
            itemColor.copy(alpha = 0.18f) // Slightly more intense background
        }
    }
    
    val cardBorder = if (isLiquidTheme) {
        BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDark) 0.22f else 0.65f),
                    itemColor.copy(alpha = 0.45f),
                    Color.White.copy(alpha = if (isDark) 0.05f else 0.15f)
                )
            )
        )
    } else {
        BorderStroke(1.5.dp, itemColor.copy(alpha = 0.4f)) // Thicker, more visible border
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isLiquidTheme) {
                if (isDark) Color(0x351E1E2E) else Color(0x75FFFFFF)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLiquidTheme) 0.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBgColor)
        ) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = categoryDetails.icon,
                    contentDescription = null,
                    tint = itemColor.copy(alpha = 0.05f),
                    modifier = Modifier
                        .offset(x = 12.dp)
                        .size(56.dp)
                        .graphicsLayer { rotationZ = -15f }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(itemColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (transaction.deviceModel.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(itemColor) 
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = transaction.deviceModel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 120.dp)
                                    )
                                }
                            }
                            
                            // Delivery status badge for non-expense/inventory items
                            if (transaction.category != "EXPENSE") {
                                val badgeColor = if (transaction.isDelivered) ProfitGreen else AccessoryOrange
                                val badgeText = if (transaction.isDelivered) {
                                    if (transaction.category == "INVENTORY" || transaction.category == "REFURB") "تم التسليم 💸"
                                    else if (transaction.category == "DEBT") "تم التسديد ✅"
                                    else "تم التسليم"
                                } else {
                                    if (transaction.category == "INVENTORY") "في المخزن 📦" 
                                    else if (transaction.category == "REFURB") "استثمار متاح 📈" 
                                    else if (transaction.category == "DEBT") "قيد السداد ⏳"
                                    else "في الورشة"
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeColor.copy(alpha = 0.15f))
                                        .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .clickable(enabled = onToggleDelivery != null) { onToggleDelivery?.invoke() }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor)
                                        )
                                        Text(
                                            text = badgeText + (if (onToggleDelivery != null) " 🔄" else ""),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = badgeColor
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = formattedDate,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(itemColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                categoryDetails.icon,
                                contentDescription = categoryDetails.nameAr,
                                tint = itemColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = transaction.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (transaction.category == "DEBT" && transaction.isDelivered) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (transaction.category == "DEBT" && transaction.isDelivered) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (transaction.customerName.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "الزبون: ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = transaction.customerName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "التصنيف: ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = categoryDetails.nameAr,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = itemColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (transaction.notes.isNotBlank()) {
                                Text(
                                    text = transaction.notes,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    lineHeight = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (transaction.creditAmount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (transaction.creditRemaining > 0) ExpenseRed.copy(alpha = 0.1f)
                                            else ProfitGreen.copy(alpha = 0.08f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (transaction.creditRemaining > 0) {
                                            "ديْن متبقي: ${formatCurrencyNoSymbol(transaction.creditRemaining)}"
                                        } else {
                                            "مستوفى بالكامل ✅"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (transaction.creditRemaining > 0) ExpenseRed else ProfitGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "+${formatCurrencyNoSymbol(transaction.sellingPrice)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (transaction.category == "DEBT" && transaction.isDelivered) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (transaction.category == "DEBT" && transaction.isDelivered) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )

                            if (transaction.costPrice > 0) {
                                Text(
                                    text = "كلفة: -${formatCurrencyNoSymbol(transaction.costPrice)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (transaction.category == "DEBT" && transaction.isDelivered) ExpenseRed.copy(alpha = 0.5f) else ExpenseRed,
                                    style = MaterialTheme.typography.labelSmall,
                                    textDecoration = if (transaction.category == "DEBT" && transaction.isDelivered) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                )
                            }

                            if (transaction.profit > 0) {
                                Text(
                                    text = if (transaction.category == "DEBT") "باقي: +${formatCurrency(transaction.profit)}" else "ربح: +${formatCurrency(transaction.profit)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (transaction.category == "DEBT" && transaction.isDelivered) ProfitGreen.copy(alpha = 0.5f) else ProfitGreen,
                                    textDecoration = if (transaction.category == "DEBT" && transaction.isDelivered) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                )
                            } else if (transaction.profit < 0) {
                                Text(
                                    text = if (transaction.category == "DEBT") "باقي: ${formatCurrency(transaction.profit)}" else "خسارة: ${formatCurrency(transaction.profit)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (transaction.category == "DEBT" && transaction.isDelivered) ExpenseRed.copy(alpha = 0.5f) else ExpenseRed,
                                    textDecoration = if (transaction.category == "DEBT" && transaction.isDelivered) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CategoryItem(
    val id: String,
    val nameAr: String,
    val icon: ImageVector,
    val color: Color
)

fun getCategoryDetails(categoryId: String): CategoryItem {
    return when (categoryId) {
        "SCREEN" -> CategoryItem("SCREEN", "صيانة شاشات", Icons.Default.Smartphone, ProfitGreen)
        "PARTS" -> CategoryItem("PARTS", "تغيير قطع غيار", Icons.Default.MiscellaneousServices, GeneralBlue)
        "ACCESSORY" -> CategoryItem("ACCESSORY", "بيع اكسسوارات", Icons.Default.Headphones, AccessoryOrange)
        "SERVICE" -> CategoryItem("SERVICE", "فلاش وتخطي FRP", Icons.Default.DeveloperMode, SoftwarePurple)
        "EXPENSE" -> CategoryItem("EXPENSE", "مصروف جديد", Icons.AutoMirrored.Filled.ReceiptLong, Color(0xFFE53935))
        "REFURB" -> CategoryItem("REFURB", "استثمار وتدوير ♻️", Icons.Default.Autorenew, Color(0xFF8BC34A))
        "INVENTORY" -> CategoryItem("INVENTORY", "شراء مخزون", Icons.Default.Inventory, Color(0xFF8D6E63))
        "DEBT" -> CategoryItem("DEBT", "دين تسليف/اقتراض", Icons.Default.MoneyOff, ExpenseRed)
        "OTHER" -> CategoryItem("OTHER", "صيانة وأخرى", Icons.Default.HomeRepairService, Color.Gray)
        else -> CategoryItem("OTHER", "أخرى عامة", Icons.AutoMirrored.Filled.Help, Color.Gray)
    }
}

fun formatCurrency(amount: Double): String {
    return try {
        val symbols = java.text.DecimalFormatSymbols(Locale.US)
        java.text.DecimalFormat("#,##0", symbols).format(amount) + " د.ج"
    } catch (e: Exception) {
        "${amount.toLong()} د.ج"
    }
}

fun formatCurrencyNoSymbol(amount: Double): String {
    return try {
        val symbols = java.text.DecimalFormatSymbols(Locale.US)
        java.text.DecimalFormat("#,##0", symbols).format(amount) + " د.ج"
    } catch (e: Exception) {
        "${amount.toLong()} د.ج"
    }
}
