package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.data.model.WorkshopTransaction
import com.example.ui.theme.AccessoryOrange
import com.example.ui.theme.GeneralBlue
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SoftwarePurple
import com.example.ui.theme.Translator
import com.example.ui.viewmodel.WorkshopViewModel
import java.text.SimpleDateFormat
import java.util.*

data class DailyChartPoint(
    val dayLabel: String,
    val dateLabel: String,
    val income: Double,
    val expense: Double,
    val transactions: List<WorkshopTransaction>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroDashboardScreen(
    viewModel: WorkshopViewModel,
    onNavigateToSections: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onTransactionClicked: (WorkshopTransaction) -> Unit,
    modifier: Modifier = Modifier,
    isStatsPage: Boolean = false
) {
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val transactions by viewModel.transactionsFlow.collectAsStateWithLifecycle()
    val personalDebts by viewModel.debtsFlow.collectAsStateWithLifecycle()
    val installments by viewModel.installmentsFlow.collectAsStateWithLifecycle()

    var showStartingBalanceHelp by remember { mutableStateOf(false) }
    var showPaidDebtsHelp by remember { mutableStateOf(false) }
    var showReceivedDebtsHelp by remember { mutableStateOf(false) }
    val subTabMode = if (isStatsPage) "STATS" else "BUDGE"

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

    val unpaidOwedToMe = personalDebts.filter { it.isOwedToMe && !it.isPaid }.sumOf { debt -> debt.amount - installments.filter { it.refId == debt.id && it.refType == "PERSONAL_DEBT" }.sumOf { it.amountPaid } }
    val unpaidOwedByMe = personalDebts.filter { !it.isOwedToMe && !it.isPaid }.sumOf { debt -> debt.amount - installments.filter { it.refId == debt.id && it.refType == "PERSONAL_DEBT" }.sumOf { it.amountPaid } }

    val pocketChange = transactions.filter { 
        (it.wallet == pocketName || it.wallet == "مصروف الشهر" || it.wallet == "الصندوق (Pocket)" || it.wallet == "محفظة المحل" || it.wallet.isBlank()) 
        && it.category != "ACCESSORY" 
        && it.affectBalance 
    }.sumOf { it.profit }
    val pocketBalance = pocketInit + pocketChange

    val bankChange = transactions.filter { 
        (it.wallet == bankName || it.wallet == "حساب بنكي") 
        && it.category != "ACCESSORY" 
        && it.affectBalance 
    }.sumOf { it.profit }
    val bankBalance = bankInit + bankChange

    val goodsChange = transactions.filter { 
        it.category == "ACCESSORY" 
        && it.affectBalance 
    }.sumOf { it.profit }
    val goodsBalance = goodsInit + goodsChange

    val personalChange = transactions.filter { 
        (it.wallet == personalName || it.wallet == "مصروف شخصي" || it.wallet == "مصروفي شخصي" || it.wallet == "مصروفي الشخصي") 
        && it.category != "ACCESSORY" 
        && it.affectBalance 
    }.sumOf { it.profit }
    val personalBalance = personalInit + personalChange

    val totalBalance = (if (pocketInclude) pocketBalance else 0.0) +
                       (if (bankInclude) bankBalance else 0.0) +
                       (if (goodsInclude) goodsBalance else 0.0) +
                       (if (personalInclude) personalBalance else 0.0)

    // Dashboard time filter: TODAY, MONTH, ALL, CUSTOM
    var timeFilter by remember { mutableStateOf("ALL") }
    // Category group filter: ALL, MAINTENANCE, SALES
    var categoryGroupFilter by remember { mutableStateOf("ALL") }
    // Month navigation offset used when MONTH filter is selected
    var monthOffset by remember { mutableStateOf(0) }
    // Custom date picker range states
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }

    val now = System.currentTimeMillis()

    // Calculate core metrics for the stable top visual rows
    val todayTransactions = transactions.filter { isSameDay(it.date, now) }
    val todayMaintenanceProfit = todayTransactions.filter { isMaintenance(it.category) }.sumOf { it.profit }
    val todaySalesProfit = todayTransactions.filter { isSales(it.category) }.sumOf { it.profit }
    val todayExpense = todayTransactions.filter { it.category == "EXPENSE" }.sumOf { it.costPrice }
    val todayNetProfit = (todayMaintenanceProfit + todaySalesProfit) - todayExpense
    val todayOpsCount = todayTransactions.size

    val monthTransactions = transactions.filter { isSameMonth(it.date, now) }
    val monthMaintenanceProfit = monthTransactions.filter { isMaintenance(it.category) }.sumOf { it.profit }
    val monthSalesProfit = monthTransactions.filter { isSales(it.category) }.sumOf { it.profit }
    val monthExpense = monthTransactions.filter { it.category == "EXPENSE" }.sumOf { it.costPrice }
    val monthNetProfit = (monthMaintenanceProfit + monthSalesProfit) - monthExpense
    val monthOpsCount = monthTransactions.size

    // Filter operations dynamically based on visual selections
    val filteredForDashboard = remember(transactions, timeFilter, categoryGroupFilter, monthOffset, customStartDate, customEndDate) {
        transactions.filter { trx ->
            val dateMatches = when (timeFilter) {
                "TODAY" -> isSameDay(trx.date, now)
                "MONTH" -> isSameMonthOffset(trx.date, monthOffset)
                "CUSTOM" -> {
                    val start = customStartDate
                    val end = customEndDate
                    if (start != null && end != null) {
                        val startCal = Calendar.getInstance().apply {
                            timeInMillis = start
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val endCal = Calendar.getInstance().apply {
                            timeInMillis = end
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        trx.date >= startCal.timeInMillis && trx.date <= endCal.timeInMillis
                    } else true
                }
                else -> true
            }
            val catMatches = when (categoryGroupFilter) {
                "MAINTENANCE" -> isMaintenance(trx.category)
                "SALES" -> isSales(trx.category)
                "INCOME" -> trx.category != "EXPENSE"
                "EXPENSE" -> trx.category == "EXPENSE"
                "ALL" -> true // show everything (both income and expenses)
                else -> trx.category != "EXPENSE" // display core workshop operations by default (all except plain expense)
            }
            dateMatches && catMatches
        }
    }

    // Interactive Donut Chart division counters
    val timeFilteredTransactions = remember(transactions, timeFilter, monthOffset, customStartDate, customEndDate) {
        transactions.filter { trx ->
            when (timeFilter) {
                "TODAY" -> isSameDay(trx.date, now)
                "MONTH" -> isSameMonthOffset(trx.date, monthOffset)
                "CUSTOM" -> {
                    val start = customStartDate
                    val end = customEndDate
                    if (start != null && end != null) {
                        val startCal = Calendar.getInstance().apply {
                            timeInMillis = start
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val endCal = Calendar.getInstance().apply {
                            timeInMillis = end
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        trx.date >= startCal.timeInMillis && trx.date <= endCal.timeInMillis
                    } else true
                }
                else -> true
            }
        }
    }

    val incomeFiltered = timeFilteredTransactions.filter { it.category != "EXPENSE" }.sumOf { it.profit.coerceAtLeast(0.0) }
    val expensesFiltered = timeFilteredTransactions.filter { it.category == "EXPENSE" }.sumOf { it.costPrice }
    val totalProfitFiltered = incomeFiltered - expensesFiltered

    val dailyTrendPoints = remember(transactions, lang) {
        val list = mutableListOf<DailyChartPoint>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val startOfDay = dayCal.clone() as Calendar
            startOfDay.set(Calendar.HOUR_OF_DAY, 0)
            startOfDay.set(Calendar.MINUTE, 0)
            startOfDay.set(Calendar.SECOND, 0)
            startOfDay.set(Calendar.MILLISECOND, 0)
            
            val endOfDay = dayCal.clone() as Calendar
            endOfDay.set(Calendar.HOUR_OF_DAY, 23)
            endOfDay.set(Calendar.MINUTE, 59)
            endOfDay.set(Calendar.SECOND, 59)
            endOfDay.set(Calendar.MILLISECOND, 999)
            
            val dayTransactions = transactions.filter { trx ->
                trx.date >= startOfDay.timeInMillis && trx.date <= endOfDay.timeInMillis
            }
            
            val inc = dayTransactions.filter { it.category != "EXPENSE" }.sumOf { it.profit.coerceAtLeast(0.0) }
            val exp = dayTransactions.filter { it.category == "EXPENSE" }.sumOf { it.costPrice }
            
            // Format day and date labels natively from the system phone calendar/locale settings
            val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)
            val dayLabel = if (lang == "ar") {
                when (dayOfWeek) {
                    Calendar.SUNDAY -> "الأحد"
                    Calendar.MONDAY -> "الإثنين"
                    Calendar.TUESDAY -> "الثلاثاء"
                    Calendar.WEDNESDAY -> "الأربعاء"
                    Calendar.THURSDAY -> "الخميس"
                    Calendar.FRIDAY -> "الجمعة"
                    Calendar.SATURDAY -> "السبت"
                    else -> ""
                }
            } else {
                when (dayOfWeek) {
                    Calendar.SUNDAY -> "Dim"
                    Calendar.MONDAY -> "Lun"
                    Calendar.TUESDAY -> "Mar"
                    Calendar.WEDNESDAY -> "Mer"
                    Calendar.THURSDAY -> "Jeu"
                    Calendar.FRIDAY -> "Ven"
                    Calendar.SATURDAY -> "Sam"
                    else -> ""
                }
            }
            
            val dayOfMonth = dayCal.get(Calendar.DAY_OF_MONTH)
            val month = dayCal.get(Calendar.MONTH)
            val dateLabel = if (lang == "ar") {
                val monthLabel = when (month) {
                    Calendar.JANUARY -> "جانفي"
                    Calendar.FEBRUARY -> "فيفري"
                    Calendar.MARCH -> "مارس"
                    Calendar.APRIL -> "أفريل"
                    Calendar.MAY -> "ماي"
                    Calendar.JUNE -> "جوان"
                    Calendar.JULY -> "جويلية"
                    Calendar.AUGUST -> "أوت"
                    Calendar.SEPTEMBER -> "سبتمبر"
                    Calendar.OCTOBER -> "أكتوبر"
                    Calendar.NOVEMBER -> "نوفمبر"
                    Calendar.DECEMBER -> "ديسمبر"
                    else -> ""
                }
                "$dayOfMonth $monthLabel"
            } else {
                val monthLabel = when (month) {
                    Calendar.JANUARY -> "Jan"
                    Calendar.FEBRUARY -> "Fév"
                    Calendar.MARCH -> "Mar"
                    Calendar.APRIL -> "Avr"
                    Calendar.MAY -> "Mai"
                    Calendar.JUNE -> "Juin"
                    Calendar.JULY -> "Juil"
                    Calendar.AUGUST -> "Août"
                    Calendar.SEPTEMBER -> "Sep"
                    Calendar.OCTOBER -> "Oct"
                    Calendar.NOVEMBER -> "Nov"
                    Calendar.DECEMBER -> "Déc"
                    else -> ""
                }
                "$dayOfMonth $monthLabel"
            }
            
            list.add(DailyChartPoint(dayLabel, dateLabel, inc, exp, dayTransactions))
        }
        list
    }

    val dailyAverageIncome = remember(dailyTrendPoints) {
        dailyTrendPoints.map { it.income }.average()
    }
    val dailyAverageExpense = remember(dailyTrendPoints) {
        dailyTrendPoints.map { it.expense }.average()
    }

    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    // Dynamic starting and end of period bounds for initial balance calculation
    val periodStart = remember(transactions, timeFilter, monthOffset, customStartDate) {
        val calendar = Calendar.getInstance()
        when (timeFilter) {
            "TODAY" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            "MONTH" -> {
                calendar.add(Calendar.MONTH, monthOffset)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            "CUSTOM" -> {
                val start = customStartDate ?: 0L
                val cal = Calendar.getInstance().apply {
                    timeInMillis = start
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            else -> 0L
        }
    }

    val periodEnd = remember(transactions, timeFilter, monthOffset, customEndDate) {
        val calendar = Calendar.getInstance()
        when (timeFilter) {
            "TODAY" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                calendar.timeInMillis
            }
            "MONTH" -> {
                calendar.add(Calendar.MONTH, monthOffset)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                calendar.timeInMillis
            }
            "CUSTOM" -> {
                val end = customEndDate ?: Long.MAX_VALUE
                val cal = Calendar.getInstance().apply {
                    timeInMillis = end
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                cal.timeInMillis
            }
            else -> Long.MAX_VALUE
        }
    }

    val dashboardDisplayItems = remember(filteredForDashboard, categoryGroupFilter, personalDebts, installments, periodStart, periodEnd, lang) {
        if (categoryGroupFilter == "RECEIVED_DEBTS") {
            val owedToMeDebtIds = personalDebts.filter { it.isOwedToMe }.map { it.id }.toSet()
            val matchingInstallments = installments.filter { it.refType == "PERSONAL_DEBT" && it.date in periodStart..periodEnd && it.refId in owedToMeDebtIds }
                .map { inst ->
                    val debt = personalDebts.find { it.id == inst.refId }
                    val debtorName = debt?.name ?: "مجهول"
                    DashboardItem.DebtOp(
                        id = inst.id,
                        name = debtorName,
                        amount = inst.amountPaid,
                        date = inst.date,
                        typeLabel = if (lang == "ar") "دفعة مستردة من دين 💰" else "Debt Installment Received",
                        notes = inst.notes.ifBlank { if (lang == "ar") "دفعة من ${debtorName}" else "Payment from ${debtorName}" },
                        color = Color(0xFF10B981),
                        icon = Icons.Default.Payments
                    )
                }
            val directOwedPaid = personalDebts.filter { it.isOwedToMe && it.isPaid && it.date in periodStart..periodEnd && installments.none { inst -> inst.refId == it.id && inst.refType == "PERSONAL_DEBT" } }
                .map { debt ->
                    DashboardItem.DebtOp(
                        id = debt.id,
                        name = debt.name,
                        amount = debt.amount,
                        date = debt.date,
                        typeLabel = if (lang == "ar") "استرداد دين كامل 🏁" else "Full Debt Recovered",
                        notes = debt.notes.ifBlank { if (lang == "ar") "تم استرداد الدين بالكامل من ${debt.name}" else "Fully recovered from ${debt.name}" },
                        color = Color(0xFF10B981),
                        icon = Icons.Default.Check
                    )
                }
            (matchingInstallments + directOwedPaid).sortedBy { it.date }
        } else if (categoryGroupFilter == "PAID_DEBTS") {
            val owedByMeDebtIds = personalDebts.filter { !it.isOwedToMe }.map { it.id }.toSet()
            val matchingInstallmentsPaid = installments.filter { it.refType == "PERSONAL_DEBT" && it.date in periodStart..periodEnd && it.refId in owedByMeDebtIds }
                .map { inst ->
                    val debt = personalDebts.find { it.id == inst.refId }
                    val creditorName = debt?.name ?: "مجهول"
                    DashboardItem.DebtOp(
                        id = inst.id,
                        name = creditorName,
                        amount = inst.amountPaid,
                        date = inst.date,
                        typeLabel = if (lang == "ar") "تسديد دفعة من دين 💸" else "Debt Installment Paid",
                        notes = inst.notes.ifBlank { if (lang == "ar") "تسديد دفعة لـ ${creditorName}" else "Payment to ${creditorName}" },
                        color = Color(0xFFEF4444),
                        icon = Icons.Default.Payments
                    )
                }
            val directOwedByMePaid = personalDebts.filter { !it.isOwedToMe && it.isPaid && it.date in periodStart..periodEnd && installments.none { inst -> inst.refId == it.id && inst.refType == "PERSONAL_DEBT" } }
                .map { debt ->
                    DashboardItem.DebtOp(
                        id = debt.id,
                        name = debt.name,
                        amount = debt.amount,
                        date = debt.date,
                        typeLabel = if (lang == "ar") "سداد دين كامل 🏁" else "Full Debt Settled",
                        notes = debt.notes.ifBlank { if (lang == "ar") "تم سداد الدين بالكامل لـ ${debt.name}" else "Fully settled to ${debt.name}" },
                        color = Color(0xFFEF4444),
                        icon = Icons.Default.Check
                    )
                }
            (matchingInstallmentsPaid + directOwedByMePaid).sortedBy { it.date }
        } else {
            filteredForDashboard.map { DashboardItem.Trans(it) }
        }
    }

    val periodStartingBalance = remember(transactions, personalDebts, pocketInit, bankInit, goodsInit, personalInit, periodStart) {
        val totalInitialVal = pocketInit + bankInit + goodsInit + personalInit
        if (periodStart == 0L) {
            totalInitialVal
        } else {
            val preTransactionsChange = transactions.filter { it.date < periodStart }
                .sumOf { it.profit }
            val preLentDebts = personalDebts.filter { it.isOwedToMe && it.date < periodStart }.sumOf { it.amount }
            val preBorrowedDebts = personalDebts.filter { !it.isOwedToMe && it.date < periodStart }.sumOf { it.amount }
            totalInitialVal + preTransactionsChange - preLentDebts + preBorrowedDebts
        }
    }

    val paidDebtsFiltered = remember(personalDebts, installments, periodStart, periodEnd) {
        val paidInstallments = installments.filter { it.refType == "PERSONAL_DEBT" && it.date in periodStart..periodEnd && personalDebts.any { d -> d.id == it.refId && !d.isOwedToMe } }.sumOf { it.amountPaid }
        val directPaid = personalDebts.filter { !it.isOwedToMe && it.isPaid && it.date in periodStart..periodEnd && installments.none { inst -> inst.refId == it.id && inst.refType == "PERSONAL_DEBT" } }.sumOf { it.amount }
        paidInstallments + directPaid
    }

    val receivedDebtsFiltered = remember(personalDebts, installments, periodStart, periodEnd) {
        val receivedInstallments = installments.filter { it.refType == "PERSONAL_DEBT" && it.date in periodStart..periodEnd && personalDebts.any { d -> d.id == it.refId && d.isOwedToMe } }.sumOf { it.amountPaid }
        val directReceived = personalDebts.filter { it.isOwedToMe && it.isPaid && it.date in periodStart..periodEnd && installments.none { inst -> inst.refId == it.id && inst.refType == "PERSONAL_DEBT" } }.sumOf { it.amount }
        receivedInstallments + directReceived
    }

    val periodLentDebtsOutflow = remember(personalDebts, periodStart, periodEnd) {
        personalDebts.filter { it.isOwedToMe && it.date in periodStart..periodEnd }.sumOf { it.amount }
    }

    val periodBorrowedDebtsInflow = remember(personalDebts, periodStart, periodEnd) {
        personalDebts.filter { !it.isOwedToMe && it.date in periodStart..periodEnd }.sumOf { it.amount }
    }

    val periodFinalBalance = periodStartingBalance + incomeFiltered - expensesFiltered + receivedDebtsFiltered - paidDebtsFiltered + periodBorrowedDebtsInflow - periodLentDebtsOutflow

    // Percentage of Profit allocations
    val totalSumForPercent = incomeFiltered + expensesFiltered

    val incomePercent = if (totalSumForPercent > 0) (incomeFiltered / totalSumForPercent).toFloat() else 0.5f
    val expensePercent = if (totalSumForPercent > 0) (expensesFiltered / totalSumForPercent).toFloat() else 0.5f

    // Premium theme-aligned palette
    val budgePurple = MaterialTheme.colorScheme.primary
    val budgePurpleLight = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val budgeCoral = Color(0xFFE53935)
    val budgeBlue = MaterialTheme.colorScheme.secondary
    val budgeBlueSoft = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
    val budgeGrayBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    val isLiquidTheme = com.example.ui.theme.LocalIsLiquidTheme.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val themedCardBgColor = if (isLiquidTheme) {
        if (isDark) Color(0x501E1E2E) else Color(0xCCFFFFFF)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val themedCardBorder = if (isLiquidTheme) {
        androidx.compose.foundation.BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                colors = if (isDark) {
                    listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.04f),
                        Color(0xFF007AFF).copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.08f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.12f),
                        Color(0xFF007AFF).copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.35f)
                    )
                }
            )
        )
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    }

    // Fluid animations for the custom drawing slices
    val incomeSweepAngle by animateFloatAsState(
        targetValue = incomePercent * 360f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "IncomeSweep"
    )
    val expenseSweepAngle by animateFloatAsState(
        targetValue = expensePercent * 360f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ExpenseSweep"
    )


    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isLiquidTheme) Modifier else {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
            if (subTabMode == "BUDGE") {
                // 2. THE SIGNATURE CENTRAL DIAL WHEEL (Budge Space Ring Card)
                item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isLiquidTheme) {
                                Modifier.border(themedCardBorder, RoundedCornerShape(32.dp))
                            } else {
                                Modifier.shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(32.dp),
                                    ambientColor = budgePurple.copy(alpha = 0.2f),
                                    spotColor = budgePurple.copy(alpha = 0.3f)
                                )
                            }
                        ),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = themedCardBgColor
                    ),
                    border = if (isLiquidTheme) null else themedCardBorder
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Segmented Header with pill toggles
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(budgePurple.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DonutLarge,
                                            contentDescription = "Split indicator icon",
                                            tint = budgePurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = translate("stat_split_title", lang),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = translate("net_earnings", lang),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Selection Switcher (TODAY, MONTH, ALL, CUSTOM)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(budgeGrayBackground)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("TODAY", "MONTH", "ALL", "CUSTOM").forEach { pillKey ->
                                    val active = timeFilter == pillKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(
                                                if (active) budgePurple else Color.Transparent
                                            )
                                            .clickable { timeFilter = pillKey }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (pillKey) {
                                                "TODAY" -> translate("p_today", lang)
                                                "MONTH" -> translate("p_month", lang)
                                                "CUSTOM" -> translate("p_custom", lang)
                                                else -> translate("p_all", lang)
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // If MONTH is selected, show Month Navigation Controls and Relative Buttons
                        if (timeFilter == "MONTH") {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Month navigation header: Arrows and Current Name
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { monthOffset -= 1 },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(budgePurple.copy(alpha = 0.08f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = "Previous Month",
                                            tint = budgePurple
                                        )
                                    }

                                    Text(
                                        text = getMonthNameWithYear(monthOffset, lang),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = { monthOffset += 1 },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(budgePurple.copy(alpha = 0.08f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Next Month",
                                            tint = budgePurple
                                        )
                                    }
                                }

                                // Quick Relative Month buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Button: الشهر الماضي (Last Month)
                                    Button(
                                        onClick = { monthOffset -= 1 },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = budgePurple.copy(alpha = 0.05f),
                                            contentColor = budgePurple
                                        ),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = translate("prev_month_lbl", lang),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Button: الشهر الحالي (Current Month)
                                    if (monthOffset != 0) {
                                        Button(
                                            onClick = { monthOffset = 0 },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                                                contentColor = MaterialTheme.colorScheme.secondary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = if (lang == "ar") "الحالي" else "Actuel",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Button: الشهر القادم (Next Month)
                                    Button(
                                        onClick = { monthOffset += 1 },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = budgePurple.copy(alpha = 0.05f),
                                            contentColor = budgePurple
                                        ),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = translate("next_month_lbl", lang),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // If CUSTOM is selected, show Custom Date Range Picker Fields
                        if (timeFilter == "CUSTOM") {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val context = LocalContext.current
                                val openDatePicker = { isStart: Boolean ->
                                    val calendar = Calendar.getInstance()
                                    val currentSelection = if (isStart) customStartDate else customEndDate
                                    if (currentSelection != null) {
                                        calendar.timeInMillis = currentSelection
                                    }
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedCal = Calendar.getInstance()
                                            selectedCal.set(Calendar.YEAR, year)
                                            selectedCal.set(Calendar.MONTH, month)
                                            selectedCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                            if (isStart) {
                                                selectedCal.set(Calendar.HOUR_OF_DAY, 0)
                                                selectedCal.set(Calendar.MINUTE, 0)
                                                selectedCal.set(Calendar.SECOND, 0)
                                                selectedCal.set(Calendar.MILLISECOND, 0)
                                                customStartDate = selectedCal.timeInMillis
                                            } else {
                                                selectedCal.set(Calendar.HOUR_OF_DAY, 23)
                                                selectedCal.set(Calendar.MINUTE, 59)
                                                selectedCal.set(Calendar.SECOND, 59)
                                                selectedCal.set(Calendar.MILLISECOND, 999)
                                                customEndDate = selectedCal.timeInMillis
                                            }
                                            // Re-trigger the custom filter to ensure it computes on valid selection
                                            val oldFilter = timeFilter
                                            timeFilter = "" // force state re-evaluation
                                            timeFilter = oldFilter
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Start Date Select Card
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                            .clickable { openDatePicker(true) }
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = translate("custom_from", lang),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatCustomDate(customStartDate, lang),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (customStartDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // End Date Select Card
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                            .clickable { openDatePicker(false) }
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = translate("custom_to", lang),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatCustomDate(customEndDate, lang),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (customEndDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Apply Button if selected custom dates are valid
                                if (customStartDate != null || customEndDate != null) {
                                    Button(
                                        onClick = {
                                            // Ensure end date is not before start date if both selected
                                            val start = customStartDate
                                            val end = customEndDate
                                            if (start != null && end != null && end < start) {
                                                // Swap if inverted
                                                customStartDate = end
                                                customEndDate = start
                                            }
                                            // Re-trigger the custom filter to ensure it computes on valid selection
                                            val oldFilter = timeFilter
                                            timeFilter = "" // force state re-evaluation
                                            timeFilter = oldFilter
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ProfitGreen
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = translate("apply_filter", lang),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Single-screen main dashboard summary card restructured vertically to center the diagram and place stats below
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Premium Allocation Donut Ring with Detailed Legend
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Component: Amplified Donut Ring Chart with beautiful nested inner layers
                                    val localDensity = androidx.compose.ui.platform.LocalDensity.current
                                    val emptyPrimaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    Box(
                                        modifier = Modifier
                                            .size(150.dp)
                                            .weight(1.21f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .pointerInput(totalSumForPercent, incomePercent, expensePercent) {
                                                    detectTapGestures { offset ->
                                                        val centerX = size.width / 2f
                                                        val centerY = size.height / 2f
                                                        val dx = offset.x - centerX
                                                        val dy = offset.y - centerY
                                                        val distance = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                                                        
                                                        val radius = size.width / 2f
                                                        if (distance <= radius) {
                                                            val angleRad = Math.atan2(dy.toDouble(), dx.toDouble())
                                                            var angleDeg = Math.toDegrees(angleRad).toFloat()
                                                            
                                                            // Normalize to 12 o'clock start (0 to 360 clockwise)
                                                            var normalizedAngle = angleDeg + 90f
                                                            if (normalizedAngle < 0f) {
                                                                normalizedAngle += 360f
                                                            }
                                                            
                                                            if (totalSumForPercent > 0) {
                                                                val currentIncSweep = incomePercent * 360f
                                                                val currentExpSweep = expensePercent * 360f
                                                                
                                                                if (normalizedAngle in 0f..currentIncSweep) {
                                                                    categoryGroupFilter = if (categoryGroupFilter == "INCOME") "ALL" else "INCOME"
                                                                } else if (normalizedAngle in currentIncSweep..(currentIncSweep + currentExpSweep)) {
                                                                    categoryGroupFilter = if (categoryGroupFilter == "EXPENSE") "ALL" else "EXPENSE"
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                        ) {
                                            val centerX = size.width / 2f
                                            val centerY = size.height / 2f
                                            val insetPx = 4.dp.toPx()
                                            val radius = (size.width - insetPx * 2) / 2f
                                            val shiftDp = 5.dp.toPx()

                                            if (totalSumForPercent <= 0) {
                                                // If there's no data, draw a single beautiful solid primary circle with centered info
                                                drawCircle(
                                                    color = emptyPrimaryColor,
                                                    radius = radius,
                                                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                                                )
                                                
                                                drawIntoCanvas { canvas ->
                                                    val paint = android.graphics.Paint().apply {
                                                        color = android.graphics.Color.WHITE
                                                        textSize = 10.sp.toPx()
                                                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                                        textAlign = android.graphics.Paint.Align.CENTER
                                                        isAntiAlias = true
                                                    }
                                                    val label = if (lang == "ar") "لا توجد معاملات" else "No Data"
                                                    canvas.nativeCanvas.drawText(label, centerX, centerY + 3.dp.toPx(), paint)
                                                }
                                            } else {
                                                val incPercentageVal = Math.round(incomePercent * 100).toInt()
                                                val expPercentageVal = Math.round(expensePercent * 100).toInt()

                                                // --- INCOME SLICE ---
                                                if (incomePercent > 0f) {
                                                    val middleAngle = -90f + incomeSweepAngle / 2f
                                                    val rad = Math.toRadians(middleAngle.toDouble())
                                                    val shiftX = (shiftDp * Math.cos(rad)).toFloat()
                                                    val shiftY = (shiftDp * Math.sin(rad)).toFloat()

                                                    val arcTopLeft = androidx.compose.ui.geometry.Offset(
                                                        centerX - radius + shiftX,
                                                        centerY - radius + shiftY
                                                    )
                                                    val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)

                                                    drawArc(
                                                        brush = Brush.linearGradient(
                                                            listOf(Color(0xFF34D399), Color(0xFF059669))
                                                        ),
                                                        startAngle = -90f,
                                                        sweepAngle = incomeSweepAngle,
                                                        useCenter = true,
                                                        topLeft = arcTopLeft,
                                                        size = arcSize
                                                    )
                                                }

                                                // --- EXPENSE SLICE ---
                                                if (expensePercent > 0f) {
                                                    val middleAngle = -90f + incomeSweepAngle + expenseSweepAngle / 2f
                                                    val rad = Math.toRadians(middleAngle.toDouble())
                                                    val shiftX = (shiftDp * Math.cos(rad)).toFloat()
                                                    val shiftY = (shiftDp * Math.sin(rad)).toFloat()

                                                    val arcTopLeft = androidx.compose.ui.geometry.Offset(
                                                        centerX - radius + shiftX,
                                                        centerY - radius + shiftY
                                                    )
                                                    val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)

                                                    drawArc(
                                                        brush = Brush.linearGradient(
                                                            listOf(Color(0xFFFB7185), Color(0xFFE11D48))
                                                        ),
                                                        startAngle = -90f + incomeSweepAngle,
                                                        sweepAngle = expenseSweepAngle,
                                                        useCenter = true,
                                                        topLeft = arcTopLeft,
                                                        size = arcSize
                                                    )
                                                }

                                                // --- DRAW LABELS AND PERCENTAGES DIRECTLY ON SLICES ---
                                                drawIntoCanvas { canvas ->
                                                    val paintBig = android.graphics.Paint().apply {
                                                        color = android.graphics.Color.WHITE
                                                        textSize = 9.5.sp.toPx()
                                                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                                        textAlign = android.graphics.Paint.Align.CENTER
                                                        isAntiAlias = true
                                                    }
                                                    val paintSmall = android.graphics.Paint().apply {
                                                        color = android.graphics.Color.argb(230, 255, 255, 255)
                                                        textSize = 9.sp.toPx()
                                                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                                                        textAlign = android.graphics.Paint.Align.CENTER
                                                        isAntiAlias = true
                                                    }

                                                    if (incomePercent >= 0.08f) {
                                                        val middleAngle = -90f + incomeSweepAngle / 2f
                                                        val rad = Math.toRadians(middleAngle.toDouble())
                                                        val sX = (shiftDp * Math.cos(rad)).toFloat()
                                                        val sY = (shiftDp * Math.sin(rad)).toFloat()
                                                        
                                                        val tX = centerX + sX + (radius * 0.52f * Math.cos(rad)).toFloat()
                                                        val tY = centerY + sY + (radius * 0.52f * Math.sin(rad)).toFloat()

                                                        val label = if (lang == "ar") "الدخل" else "Revenus"
                                                        val pct = "$incPercentageVal%"

                                                        canvas.nativeCanvas.drawText(label, tX, tY - 1.dp.toPx(), paintBig)
                                                        canvas.nativeCanvas.drawText(pct, tX, tY + 11.dp.toPx(), paintSmall)
                                                    }

                                                    if (expensePercent >= 0.08f) {
                                                        val middleAngle = -90f + incomeSweepAngle + expenseSweepAngle / 2f
                                                        val rad = Math.toRadians(middleAngle.toDouble())
                                                        val sX = (shiftDp * Math.cos(rad)).toFloat()
                                                        val sY = (shiftDp * Math.sin(rad)).toFloat()
                                                        
                                                        val tX = centerX + sX + (radius * 0.52f * Math.cos(rad)).toFloat()
                                                        val tY = centerY + sY + (radius * 0.52f * Math.sin(rad)).toFloat()

                                                        val label = if (lang == "ar") "المصاريف" else "Dépenses"
                                                        val pct = "$expPercentageVal%"

                                                        canvas.nativeCanvas.drawText(label, tX, tY - 1.dp.toPx(), paintBig)
                                                        canvas.nativeCanvas.drawText(pct, tX, tY + 11.dp.toPx(), paintSmall)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Right Component: Legend lists (Highly interactive with custom ripple selections)
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        // Collected Income detail
                                        val isIncomeActive = categoryGroupFilter == "INCOME"
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isIncomeActive) ProfitGreen.copy(alpha = 0.15f)
                                                    else ProfitGreen.copy(alpha = 0.05f)
                                                )
                                                .border(
                                                    width = if (isIncomeActive) 2.dp else 1.dp,
                                                    color = if (isIncomeActive) ProfitGreen else ProfitGreen.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    categoryGroupFilter = if (isIncomeActive) "ALL" else "INCOME"
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF059669)))
                                                Text(
                                                    text = if (lang == "ar") "الدخل المحصل" else "Collected Income",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = formatWithLoc(incomeFiltered, lang),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF059669)
                                            )
                                        }

                                        // Paid Expenses detail
                                        val isExpenseActive = categoryGroupFilter == "EXPENSE"
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isExpenseActive) budgeCoral.copy(alpha = 0.15f)
                                                    else budgeCoral.copy(alpha = 0.05f)
                                                )
                                                .border(
                                                    width = if (isExpenseActive) 2.dp else 1.dp,
                                                    color = if (isExpenseActive) budgeCoral else budgeCoral.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    categoryGroupFilter = if (isExpenseActive) "ALL" else "EXPENSE"
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFDC2626)))
                                                Text(
                                                    text = if (lang == "ar") "المصاريف المدفوعة" else "Paid Expenses",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = formatWithLoc(expensesFiltered, lang),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                    }
                                }
                            }

                            // Minimal horizontal Divider
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 2. High-Fidelity 7-Day Performance Trend Chart (Full width)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (lang == "ar") "النشاط اليومي (7 أيام)" else "7-Day Activity",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Modern Responsive Vertical Column Chart Container
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val maxVal = dailyTrendPoints.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(100.0) ?: 100.0

                                    // Main bars container (including background grid lines and average lines)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                    ) {
                                        // Background canvas for grid and horizontal average lines
                                        val canvasGridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val height = size.height
                                            val width = size.width
                                            // Draw horizontal grid lines (3 divisions)
                                            val divisions = 3
                                            for (i in 1..divisions) {
                                                val y = height * (i / (divisions + 1f))
                                                drawLine(
                                                    color = canvasGridColor,
                                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                                    end = androidx.compose.ui.geometry.Offset(width, y),
                                                    strokeWidth = 1.dp.toPx()
                                                )
                                            }

                                            // Horizontal Average Income Line (Crisp, highly visible)
                                            if (dailyAverageIncome > 0) {
                                                val avgIncLineY = height * (1f - (dailyAverageIncome / maxVal).toFloat().coerceIn(0f, 1f))
                                                drawLine(
                                                    color = Color(0xFF047857).copy(alpha = 0.6f), // Bold Emerald Green
                                                    start = androidx.compose.ui.geometry.Offset(0f, avgIncLineY),
                                                    end = androidx.compose.ui.geometry.Offset(width, avgIncLineY),
                                                    strokeWidth = 1.5.dp.toPx(),
                                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                                )
                                            }

                                            // Horizontal Average Expense Line (Crisp, highly visible)
                                            if (dailyAverageExpense > 0) {
                                                val avgExpLineY = height * (1f - (dailyAverageExpense / maxVal).toFloat().coerceIn(0f, 1f))
                                                drawLine(
                                                    color = Color(0xFFBE123C).copy(alpha = 0.6f), // Bold Dark Rose
                                                    start = androidx.compose.ui.geometry.Offset(0f, avgExpLineY),
                                                    end = androidx.compose.ui.geometry.Offset(width, avgExpLineY),
                                                    strokeWidth = 1.5.dp.toPx(),
                                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                                )
                                            }
                                        }

                                        // The vertical column bars arranged side-by-side
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            dailyTrendPoints.forEachIndexed { index, point ->
                                                val dayPart = point.dayLabel
                                                val datePart = point.dateLabel
                                                val inc = point.income
                                                val exp = point.expense
                                                val dayTransactions = point.transactions

                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .zIndex(if (selectedDayIndex == index) 10f else 1f)
                                                        .clickable {
                                                            selectedDayIndex = if (selectedDayIndex == index) null else index
                                                        },
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Bottom
                                                ) {
                                                    // The dual vertical bars row (placed side-by-side)
                                                    Row(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 2.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.Bottom
                                                    ) {
                                                        // Income Bar (Green)
                                                        val incFraction = (inc / maxVal).toFloat().coerceIn(0f, 1f)
                                                        if (inc > 0) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .fillMaxHeight(incFraction)
                                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                                    .background(
                                                                        Brush.verticalGradient(
                                                                            colors = listOf(Color(0xFF34D399), Color(0xFF059669))
                                                                        )
                                                                    )
                                                            )
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .height(4.dp)
                                                                    .clip(RoundedCornerShape(2.dp))
                                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                            )
                                                        }

                                                        // Expense Bar (Rose)
                                                        val expFraction = (exp / maxVal).toFloat().coerceIn(0f, 1f)
                                                        if (exp > 0) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .fillMaxHeight(expFraction)
                                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                                    .background(
                                                                        Brush.verticalGradient(
                                                                            colors = listOf(Color(0xFFFB7185), Color(0xFFE11D48))
                                                                        )
                                                                    )
                                                            )
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .height(4.dp)
                                                                    .clip(RoundedCornerShape(2.dp))
                                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                            )
                                                        }
                                                    }

                                                    // Interactive details tooltip anchor (shown above the column)
                                                    if (selectedDayIndex == index) {
                                                        androidx.compose.ui.window.Popup(
                                                            alignment = Alignment.TopCenter,
                                                            offset = androidx.compose.ui.unit.IntOffset(0, -50),
                                                            onDismissRequest = { selectedDayIndex = null },
                                                            properties = androidx.compose.ui.window.PopupProperties(
                                                                focusable = true,
                                                                dismissOnBackPress = true,
                                                                dismissOnClickOutside = true
                                                            )
                                                        ) {
                                                            Card(
                                                                colors = CardDefaults.cardColors(
                                                                    containerColor = Color(0xFF1E293B) // Rich Dark Slate Charcoal
                                                                ),
                                                                shape = RoundedCornerShape(12.dp),
                                                                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                                                                modifier = Modifier
                                                                    .width(290.dp)
                                                                    .border(1.dp, Color(0xFF475569), RoundedCornerShape(12.dp))
                                                            ) {
                                                                Column(
                                                                    modifier = Modifier.padding(12.dp),
                                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                                ) {
                                                                    // Header: Day and Date
                                                                    Row(
                                                                        modifier = Modifier.fillMaxWidth(),
                                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                                        verticalAlignment = Alignment.CenterVertically
                                                                    ) {
                                                                        Text(
                                                                            text = if (lang == "ar") "$dayPart ($datePart)" else "$dayPart, $datePart",
                                                                            fontSize = 12.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = Color.White
                                                                        )
                                                                        Text(
                                                                            text = if (lang == "ar") "التفاصيل" else "Details",
                                                                            fontSize = 10.sp,
                                                                            color = MaterialTheme.colorScheme.primary,
                                                                            fontWeight = FontWeight.Bold
                                                                        )
                                                                    }
                                                                    HorizontalDivider(color = Color(0xFF475569), thickness = 0.5.dp)

                                                                    // Day values summary
                                                                    Row(
                                                                        modifier = Modifier.fillMaxWidth(),
                                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                                    ) {
                                                                        Text(
                                                                            text = if (lang == "ar") "الربح: ${formatWithLoc(inc, lang)}" else "Inc: ${formatWithLoc(inc, lang)}",
                                                                            fontSize = 11.sp,
                                                                            fontWeight = FontWeight.SemiBold,
                                                                            color = Color(0xFF34D399)
                                                                        )
                                                                        Text(
                                                                            text = if (lang == "ar") "المصاريف: ${formatWithLoc(exp, lang)}" else "Exp: ${formatWithLoc(exp, lang)}",
                                                                            fontSize = 11.sp,
                                                                            fontWeight = FontWeight.SemiBold,
                                                                            color = Color(0xFFFB7185)
                                                                        )
                                                                    }

                                                                    HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)

                                                                    // Operations Listing
                                                                    if (dayTransactions.isEmpty()) {
                                                                        Text(
                                                                            text = if (lang == "ar") "لا توجد عمليات مسجلة" else "No transactions",
                                                                            fontSize = 11.sp,
                                                                            color = Color.Gray,
                                                                            modifier = Modifier.fillMaxWidth(),
                                                                            textAlign = TextAlign.Center
                                                                        )
                                                                    } else {
                                                                        Column(
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .heightIn(max = 140.dp)
                                                                                .verticalScroll(rememberScrollState()),
                                                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                                                        ) {
                                                                            dayTransactions.forEach { trx ->
                                                                                val isExp = trx.category == "EXPENSE"
                                                                                val isMaint = isMaintenance(trx.category)
                                                                                val isSl = isSales(trx.category)
                                                                                
                                                                                val typeLabel = if (lang == "ar") {
                                                                                    if (isMaint) "صيانة" else if (isSl) "بيع" else if (isExp) "مصروف" else "أخرى"
                                                                                } else {
                                                                                    if (isMaint) "Repair" else if (isSl) "Sale" else if (isExp) "Expense" else "Other"
                                                                                }

                                                                                val indicatorColor = if (isExp) Color(0xFFFB7185) else if (isMaint) Color(0xFF34D399) else Color(0xFF60A5FA)
                                                                                val amountText = if (isExp) {
                                                                                    "-${formatWithLoc(trx.costPrice, lang)}"
                                                                                } else {
                                                                                    "+${formatWithLoc(trx.sellingPrice, lang)}"
                                                                                }

                                                                                Row(
                                                                                    modifier = Modifier
                                                                                        .fillMaxWidth()
                                                                                        .clip(RoundedCornerShape(6.dp))
                                                                                        .background(Color(0xFF334155))
                                                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                                                ) {
                                                                                    Row(
                                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                                        modifier = Modifier.weight(1f)
                                                                                    ) {
                                                                                        // Color dot indicator
                                                                                        Box(
                                                                                            modifier = Modifier
                                                                                                .size(6.dp)
                                                                                                .clip(CircleShape)
                                                                                                .background(indicatorColor)
                                                                                        )
                                                                                        Column {
                                                                                            val titleDisplay = if (trx.deviceModel.isNotEmpty()) {
                                                                                                "${trx.title} (${trx.deviceModel})"
                                                                                            } else {
                                                                                                trx.title
                                                                                            }
                                                                                            Text(
                                                                                                text = titleDisplay,
                                                                                                fontSize = 11.sp,
                                                                                                color = Color.White,
                                                                                                fontWeight = FontWeight.Bold,
                                                                                                maxLines = 1,
                                                                                                overflow = TextOverflow.Ellipsis
                                                                                            )
                                                                                            Text(
                                                                                                text = typeLabel,
                                                                                                fontSize = 8.5.sp,
                                                                                                color = indicatorColor.copy(alpha = 0.85f),
                                                                                                fontWeight = FontWeight.Medium
                                                                                            )
                                                                                        }
                                                                                    }
                                                                                    Text(
                                                                                        text = amountText,
                                                                                        fontSize = 11.sp,
                                                                                        color = indicatorColor,
                                                                                        fontWeight = FontWeight.Black,
                                                                                        textAlign = TextAlign.End
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
                                    }

                                    // Base divider represented as the X-axis line
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                        thickness = 1.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // X-axis Day & Date Labels
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        dailyTrendPoints.forEach { point ->
                                            val dayPart = point.dayLabel
                                            val datePart = point.dateLabel
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = dayPart,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Text(
                                                    text = datePart,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Average income metadata card
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ProfitGreen.copy(alpha = 0.05f))
                                            .border(0.5.dp, ProfitGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF34D399)))
                                        Text(
                                            text = if (lang == "ar") "معدل ربح: ${formatWithLoc(dailyAverageIncome, lang)}" else "Avg Prof: ${formatWithLoc(dailyAverageIncome, lang)}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Average expense metadata card
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(budgeCoral.copy(alpha = 0.05f))
                                            .border(0.5.dp, budgeCoral.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFB7185)))
                                        Text(
                                            text = if (lang == "ar") "معدل صرف: ${formatWithLoc(dailyAverageExpense, lang)}" else "Avg Exp: ${formatWithLoc(dailyAverageExpense, lang)}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                        // Minimal horizontal Divider
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                thickness = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Bottom Section: statistics grid utilizing the edges fully
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Starting Balance Capsule (Sleek Horizontal Row spanning complete width)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                        .clickable { showStartingBalanceHelp = true }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (lang == "ar") "الرصيد الابتدائي للفترة 🏁" else "Starting Period Balance 🏁",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            imageVector = Icons.Default.HelpOutline,
                                            contentDescription = "Help Info",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = formatWithLoc(periodStartingBalance, lang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // 2. Interactive 2x2 Grid for Income, Expenses, and Debts (Clickable filters)
                                // Row 1: Income & Expenses
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Income Card
                                    val isIncomeActive = categoryGroupFilter == "INCOME"
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isIncomeActive) ProfitGreen.copy(alpha = 0.12f)
                                                else ProfitGreen.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                width = if (isIncomeActive) 2.dp else 1.dp,
                                                color = if (isIncomeActive) ProfitGreen else ProfitGreen.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                categoryGroupFilter = if (isIncomeActive) "ALL" else "INCOME"
                                            }
                                            .padding(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = "Income Icon",
                                                    tint = ProfitGreen,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                if (isIncomeActive) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(ProfitGreen)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = if (lang == "ar") "الدخل المحصل" else "Collected Income",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isIncomeActive) ProfitGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = formatWithLoc(incomeFiltered, lang),
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Black,
                                                color = ProfitGreen
                                            )
                                        }
                                    }

                                    // Expenses Card
                                    val isExpenseActive = categoryGroupFilter == "EXPENSE"
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isExpenseActive) budgeCoral.copy(alpha = 0.12f)
                                                else budgeCoral.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                width = if (isExpenseActive) 2.dp else 1.dp,
                                                color = if (isExpenseActive) budgeCoral else budgeCoral.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                categoryGroupFilter = if (isExpenseActive) "ALL" else "EXPENSE"
                                            }
                                            .padding(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = "Expense Icon",
                                                    tint = budgeCoral,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                if (isExpenseActive) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(budgeCoral)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = if (lang == "ar") "المصاريف المدفوعة" else "Paid Expenses",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isExpenseActive) budgeCoral else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = formatWithLoc(expensesFiltered, lang),
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Black,
                                                color = budgeCoral
                                            )
                                        }
                                    }
                                }

                                // Row 2: Debts Recovered & Debts Paid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Received Debts Card (ديون مستردة)
                                    val isDebtsActive = categoryGroupFilter == "RECEIVED_DEBTS"
                                    val debtsColor = Color(0xFF0D9488) // Beautiful teal
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isDebtsActive) debtsColor.copy(alpha = 0.12f)
                                                else debtsColor.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                width = if (isDebtsActive) 2.dp else 1.dp,
                                                color = if (isDebtsActive) debtsColor else debtsColor.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                categoryGroupFilter = if (isDebtsActive) "ALL" else "RECEIVED_DEBTS"
                                            }
                                            .padding(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountBalanceWallet,
                                                    contentDescription = "Debts Recovered Icon",
                                                    tint = debtsColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = { showReceivedDebtsHelp = true },
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.HelpOutline,
                                                            contentDescription = "Help Info",
                                                            tint = debtsColor.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    if (isDebtsActive) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(debtsColor)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = if (lang == "ar") "ديون مستردة" else "Debts Recovered",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDebtsActive) debtsColor else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = formatWithLoc(receivedDebtsFiltered, lang),
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Black,
                                                color = debtsColor
                                            )
                                        }
                                    }

                                    // Paid Debts Card (ديون مسددة)
                                    val isPaidDebtsActive = categoryGroupFilter == "PAID_DEBTS"
                                    val paidDebtsColor = Color(0xFFE11D48) // Beautiful rose//orange-red
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isPaidDebtsActive) paidDebtsColor.copy(alpha = 0.12f)
                                                else paidDebtsColor.copy(alpha = 0.05f)
                                            )
                                            .border(
                                                width = if (isPaidDebtsActive) 2.dp else 1.dp,
                                                color = if (isPaidDebtsActive) paidDebtsColor else paidDebtsColor.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                categoryGroupFilter = if (isPaidDebtsActive) "ALL" else "PAID_DEBTS"
                                            }
                                            .padding(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ReceiptLong,
                                                    contentDescription = "Debts Settled Icon",
                                                    tint = paidDebtsColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = { showPaidDebtsHelp = true },
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.HelpOutline,
                                                            contentDescription = "Help Info",
                                                            tint = paidDebtsColor.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    if (isPaidDebtsActive) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(paidDebtsColor)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = if (lang == "ar") "ديون مسددة" else "Debts Settled",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPaidDebtsActive) paidDebtsColor else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = formatWithLoc(paidDebtsFiltered, lang),
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Black,
                                                color = paidDebtsColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showStartingBalanceHelp) {
                            AlertDialog(
                                onDismissRequest = { showStartingBalanceHelp = false },
                                title = {
                                    Text(
                                        text = if (lang == "ar") "حول الرصيد الابتدائي ℹ️" else "About Starting Balance",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                },
                                text = {
                                    Text(
                                        text = if (lang == "ar") {
                                            "الرصيد الابتدائي يمثل مجموع القيمة المتوفرة في جميع محافظك (الصندوق، البنك، السلعة، المصروف) في بداية هذه الفترة المحددة.\n\n" +
                                            "الرصيد النهائي = الرصيد الابتدائي + إجمالي الدخل - المصاريف + الديون المستلمة - الديون المدفوعة."
                                        } else {
                                            "The Starting Balance represents the sum of your pockets/wallets at the beginning of the selected period.\n\n" +
                                            "Final Balance = Starting Balance + Total Income - Expenses + Received Debts - Paid Debts."
                                        },
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = { showStartingBalanceHelp = false }) {
                                        Text(text = if (lang == "ar") "حسناً" else "OK", fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }

                        if (showReceivedDebtsHelp) {
                            AlertDialog(
                                onDismissRequest = { showReceivedDebtsHelp = false },
                                title = {
                                    Text(
                                        text = if (lang == "ar") "الديون المستلمة ℹ️" else "Received Debts Help",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = ProfitGreen
                                    )
                                },
                                text = {
                                    Text(
                                        text = if (lang == "ar") {
                                            "الديون المستلمة تمثل الديون التي قمت أنت بإقراضها للآخرين (أموال لنا في ذمة الآخرين).\n\n" +
                                            "عند سداد هذا الدين أو أجزاء منه، فإنك تستلم مالاً يدخل رصيد ورشتك ويزيد من التدفق النقدي الإيجابي لورشتك."
                                        } else {
                                            "Received Debts represent money you lent to others (money owed to us).\n\n" +
                                            "When these debts are repaid, the cash enters your workshop balance and increases your net cashflow."
                                        },
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = { showReceivedDebtsHelp = false }) {
                                        Text(text = if (lang == "ar") "حسناً" else "OK", fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }

                        if (showPaidDebtsHelp) {
                            AlertDialog(
                                onDismissRequest = { showPaidDebtsHelp = false },
                                title = {
                                    Text(
                                        text = if (lang == "ar") "الديون المدفوعة ℹ️" else "Paid Debts Help",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = budgeCoral
                                    )
                                },
                                text = {
                                    Text(
                                        text = if (lang == "ar") {
                                            "الديون المدفوعة تمثل المبالغ التي قمت أنت باقتراضها من الموردين أو الآخرين (أموال علينا للآخرين).\n\n" +
                                            "عند سدادك لهذه الديون، فإنك تدفع مبالغ لخصمها من رصيد ورشتك لتسوية الالتزامات المالية والديون المترتبة عليك."
                                        } else {
                                            "Paid Debts represent loans or liabilities you borrowed from suppliers or others (money we owe to others).\n\n" +
                                            "Paying off these debts deducts cash from your workshop balance to settle your obligations."
                                        },
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = { showPaidDebtsHelp = false }) {
                                        Text(text = if (lang == "ar") "حسناً" else "OK", fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }
                    }
                }
            }


            // 5. SIGNATURE SHORTCUT SHINY BUTTONS (Budge Action Blocks)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Visit Departments Block
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .shadow(4.dp, RoundedCornerShape(18.dp))
                            .clickable { onNavigateToSections() },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, budgePurple.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            budgePurple,
                                            budgePurple.copy(alpha = 0.82f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = translate("visit_sections", lang),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Full History block
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .shadow(4.dp, RoundedCornerShape(18.dp))
                            .clickable { onNavigateToTransactions() },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, budgeBlue.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            budgeBlue,
                                            budgeBlue.copy(alpha = 0.82f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatListBulleted,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = translate("visit_transactions", lang),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 6. DETAILED TRANSACTION STREAM (Budge Segment List Header)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = translate("recent_filter_ops", lang),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Compact Pill Buttons toggling Repairs Vs Sales Vs Income Vs Expenses
                    LazyRow(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(14.dp))
                            .background(budgeGrayBackground)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(listOf("ALL", "MAINTENANCE", "SALES", "INCOME", "EXPENSE", "RECEIVED_DEBTS", "PAID_DEBTS")) { key ->
                            val selected = categoryGroupFilter == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
                                    )
                                    .clickable { categoryGroupFilter = key }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .then(
                                        if (selected) Modifier.shadow(1.dp, RoundedCornerShape(11.dp)) else Modifier
                                    )
                            ) {
                                Text(
                                    text = when (key) {
                                        "MAINTENANCE" -> translate("f_maint", lang)
                                        "SALES" -> translate("f_sales", lang)
                                        "INCOME" -> translate("income_lbl", lang)
                                        "EXPENSE" -> translate("expenses_lbl", lang)
                                        "RECEIVED_DEBTS" -> if (lang == "ar") "ديون مستردة" else "Debts Received"
                                        "PAID_DEBTS" -> if (lang == "ar") "ديون مسددة" else "Debts Settled"
                                        else -> translate("f_all", lang)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (selected) budgePurple else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Scrollable operations stream (Budge item cards)
            if (dashboardDisplayItems.isEmpty()) {
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
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = translate("no_trx_found", lang),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                items(dashboardDisplayItems.reversed().take(15)) { item ->
                    when (item) {
                        is DashboardItem.Trans -> {
                            val trx = item.trx
                            val trColor = when (trx.category) {
                                "SCREEN" -> Color(0xFF10B981)
                                "SERVICE" -> budgePurple
                                "PARTS" -> budgeBlue
                                "ACCESSORY" -> Color(0xFFF59E0B)
                                "REFURB" -> Color(0xFF8BC34A)
                                "EXPENSE" -> Color(0xFFEF4444)
                                "INVENTORY" -> Color(0xFF8D6E63)
                                else -> Color(0xFF6B7280)
                            }

                            val trIcon = when (trx.category) {
                                "SCREEN" -> Icons.Default.AspectRatio
                                "SERVICE" -> Icons.Default.Memory
                                "PARTS" -> Icons.Default.Build
                                "ACCESSORY" -> Icons.Default.ShoppingBag
                                "REFURB" -> Icons.Default.Autorenew
                                "EXPENSE" -> Icons.Default.Payments
                                "INVENTORY" -> Icons.Default.Inventory
                                else -> Icons.Default.PhoneAndroid
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(2.dp, RoundedCornerShape(22.dp))
                                    .clickable { onTransactionClicked(trx) },
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(trColor.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = trIcon,
                                                contentDescription = null,
                                                tint = trColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = trx.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val catLabel = when (trx.category) {
                                                    "SCREEN" -> translate("cat_screen", lang)
                                                    "SERVICE" -> translate("cat_service", lang)
                                                    "PARTS" -> translate("cat_parts", lang)
                                                    "ACCESSORY" -> translate("cat_accessory", lang)
                                                    "REFURB" -> translate("cat_refurb", lang)
                                                    "EXPENSE" -> translate("cat_expense", lang)
                                                    "INVENTORY" -> translate("cat_inventory", lang)
                                                    else -> translate("cat_other", lang)
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(trColor.copy(alpha = 0.08f))
                                                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                                ) {
                                                    Text(
                                                        text = catLabel,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = trColor
                                                    )
                                                }

                                                if (trx.deviceModel.isNotBlank()) {
                                                    Text(
                                                        text = trx.deviceModel,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            
                                            if (trx.notes.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = trx.notes,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // Primary amount: Selling price (or Cost for pure expenses)
                                        val isExpense = trx.category == "EXPENSE"
                                        val primaryAmount = if (isExpense) trx.costPrice else trx.sellingPrice
                                        val primaryColor = if (isExpense) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                                        val prefix = if (isExpense) "-" else "+"
                                        Text(
                                            text = prefix + formatWithLoc(primaryAmount, lang),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = primaryColor
                                        )

                                        // Item Cost (سعر شراء القطعة - تكاليف القطعة)
                                        if (trx.costPrice > 0.0 && !isExpense) {
                                            Text(
                                                text = "الشراء: -${formatWithLoc(trx.costPrice, lang)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEF4444).copy(alpha = 0.85f)
                                            )
                                        }

                                        // Profit display
                                        val isStockTransfer = trx.category == "INVENTORY" || (trx.category == "REFURB" && !trx.isDelivered)
                                        if (!isExpense && !isStockTransfer) {
                                            val netProfit = trx.sellingPrice - trx.costPrice
                                            Text(
                                                text = "الربح: +${formatWithLoc(netProfit, lang)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF10B981)
                                            )
                                        } else if (isStockTransfer) {
                                            Text(
                                                text = if (trx.category == "INVENTORY") "مخزن 📦" else "استثمار ♻️",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF8D6E63)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = formatShortDate(trx.date),
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        is DashboardItem.DebtOp -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(2.dp, RoundedCornerShape(22.dp)),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(item.color.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = null,
                                                tint = item.color,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(item.color.copy(alpha = 0.08f))
                                                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                                ) {
                                                    Text(
                                                        text = item.typeLabel,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = item.color
                                                    )
                                                }
                                            }
                                            
                                            if (item.notes.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = item.notes,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        val isRecovered = item.color == Color(0xFF10B981)
                                        Text(
                                            text = (if (isRecovered) "+" else "-") + formatWithLoc(item.amount, lang),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = item.color
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = formatShortDate(item.date),
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            } else {
                // Here is the dedicated, high-fidelity stats view!
                // 1. Time filter Selector Card
                item {
                    val budgePurple = MaterialTheme.colorScheme.primary
                    val budgeGrayBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (lang == "ar") "تصفية فترة الإحصائيات 📅" else "Période des Statistiques 📅",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // Selector Row with pills
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(budgeGrayBackground)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("TODAY", "MONTH", "ALL", "CUSTOM").forEach { pillKey ->
                                    val active = timeFilter == pillKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(
                                                if (active) budgePurple else Color.Transparent
                                            )
                                            .clickable { timeFilter = pillKey }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (pillKey) {
                                                "TODAY" -> translate("p_today", lang)
                                                "MONTH" -> translate("p_month", lang)
                                                "CUSTOM" -> translate("p_custom", lang)
                                                else -> translate("p_all", lang)
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Month Navigation
                            if (timeFilter == "MONTH") {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { monthOffset -= 1 },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(budgePurple.copy(alpha = 0.08f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = "Previous Month",
                                            tint = budgePurple
                                        )
                                    }

                                    Text(
                                        text = getMonthNameWithYear(monthOffset, lang),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(
                                        onClick = { monthOffset += 1 },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(budgePurple.copy(alpha = 0.08f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Next Month",
                                            tint = budgePurple
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Metrics 2x2 grid
                item {
                    val initialBalancesForStats = if (timeFilter == "ALL") (pocketInit + bankInit + goodsInit + personalInit) else 0.0
                    val statsRevenue = timeFilteredTransactions.filter { it.isDelivered && it.category != "DEBT" }.sumOf { it.sellingPrice } + timeFilteredTransactions.filter { it.category == "DEBT" && it.profit > 0 }.sumOf { it.profit } + initialBalancesForStats
                    val statsPartsCost = timeFilteredTransactions.filter { it.category != "EXPENSE" && it.category != "DEBT" && !it.isDelivered }.sumOf { it.costPrice }
                    val statsExpensesCost = timeFilteredTransactions.filter { it.category == "EXPENSE" }.sumOf { it.costPrice } + timeFilteredTransactions.filter { it.category == "DEBT" && it.profit < 0 }.sumOf { -it.profit }
                    val statsTotalProfit = timeFilteredTransactions.sumOf { it.profit } + initialBalancesForStats

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (lang == "ar") "مؤشرات الأداء المالي 📈" else "Indicateurs de Performance 📈",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatsGridItem(
                                title = if (lang == "ar") "المجموع المحصل" else "Total Encaissé",
                                amount = statsRevenue,
                                color = Color(0xFF4CAF50),
                                icon = Icons.Default.AddChart,
                                modifier = Modifier.weight(1f),
                                lang = lang
                            )

                            StatsGridItem(
                                title = if (lang == "ar") "تكاليف قطع الغيار" else "Coût des Pièces",
                                amount = statsPartsCost,
                                color = Color(0xFF0288D1),
                                icon = Icons.Default.Build,
                                modifier = Modifier.weight(1f),
                                lang = lang
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatsGridItem(
                                title = if (lang == "ar") "مصاريف وديون" else "Dépenses/Dettes",
                                amount = statsExpensesCost,
                                color = Color(0xFFE53935),
                                icon = Icons.Default.Payments,
                                modifier = Modifier.weight(1f),
                                lang = lang
                            )

                            StatsGridItem(
                                title = if (lang == "ar") "الربح الصافي" else "Bénéfice Net",
                                amount = statsTotalProfit,
                                color = MaterialTheme.colorScheme.primary,
                                icon = Icons.Default.AccountBalanceWallet,
                                isBold = true,
                                modifier = Modifier.weight(1f),
                                lang = lang
                            )
                        }
                    }
                }

                // 3. Comparison Chart Card
                item {
                    val initialBalancesForStats = if (timeFilter == "ALL") (pocketInit + bankInit + goodsInit + personalInit) else 0.0
                    val statsRevenue = timeFilteredTransactions.filter { it.isDelivered && it.category != "DEBT" }.sumOf { it.sellingPrice } + timeFilteredTransactions.filter { it.category == "DEBT" && it.profit > 0 }.sumOf { it.profit } + initialBalancesForStats
                    val statsPartsCost = timeFilteredTransactions.filter { it.category != "EXPENSE" && it.category != "DEBT" && !it.isDelivered }.sumOf { it.costPrice }
                    val statsExpensesCost = timeFilteredTransactions.filter { it.category == "EXPENSE" }.sumOf { it.costPrice } + timeFilteredTransactions.filter { it.category == "DEBT" && it.profit < 0 }.sumOf { -it.profit }
                    val statsTotalProfit = timeFilteredTransactions.sumOf { it.profit } + initialBalancesForStats

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (lang == "ar") "مخطط بياني للمقارنة 📊" else "Graphique de Comparaison 📊",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            FinancialBarChart(
                                revenue = statsRevenue,
                                partsCost = statsPartsCost,
                                expenses = statsExpensesCost,
                                profit = statsTotalProfit,
                                lang = lang
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun StatsGridItem(
    title: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isBold: Boolean = false,
    modifier: Modifier = Modifier,
    lang: String
) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.12f)),
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatWithLoc(amount, lang),
                fontSize = if (isBold) 15.sp else 13.sp,
                fontWeight = if (isBold) FontWeight.Black else FontWeight.ExtraBold,
                color = if (isBold) MaterialTheme.colorScheme.onSurface else color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Struct to store decorative information for the cal representation

// Helper methods to keep calculations pristine
private fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isSameMonth(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
}

private fun isMaintenance(category: String): Boolean {
    return category == "SCREEN" || category == "SERVICE" || category == "PARTS" || category == "OTHER"
}

private fun isSales(category: String): Boolean {
    return category == "ACCESSORY" || category == "REFURB" || category == "INVENTORY"
}

private fun getWelcomeGreeting(lang: String): String {
    return when (lang) {
        "fr" -> "Bienvenue à Warshati 🛠"
        "en" -> "Welcome to Warshati 🛠"
        else -> "أهلاً بك في ورشتي 🛠"
    }
}

private fun getSubGreeting(lang: String): String {
    return when (lang) {
        "fr" -> "Votre assistant de gestion d'Atelier"
        "en" -> "Your budget-focused workshop assistant"
        else -> "مساعدك المالي الذكي لإدارة المحل والاستثمار"
    }
}

private fun formatWithLoc(amount: Double, lang: String): String {
    return try {
        val symbols = java.text.DecimalFormatSymbols(Locale.US)
        val formatted = java.text.DecimalFormat("#,##0", symbols).format(amount)
        "\u200E$formatted DA\u200E"
    } catch (e: Exception) {
        "\u200E${amount.toLong()} DA\u200E"
    }
}

private fun formatShortDate(timestamp: Long): String {
    return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun translate(key: String, lang: String): String {
    val dic = mapOf(
        "ar" to mapOf(
            "today_profit" to "صافي أرباح اليوم 💰",
            "expenses_lbl" to "المصاريف 💸",
            "income_lbl" to "الدخل 💵",
            "month_profit" to "صافي أرباح الشهر 📈",
            "profit_net_lbl" to "صافي الربح",
            "ops_unit" to "عملية",
            "stat_split_title" to "مقارنة الدخل والمصاريف 📊",
            "ops_maint" to "الصيانة والخدمات",
            "ops_sales" to "مبيعات واكسسوارات  ",
            "net_earnings" to "صافي الأرباح المفلترة",
            "recent_filter_ops" to "سجل العمليات الأخير 📝",
            "f_all" to "الكل",
            "f_maint" to "صيانة فقط",
            "f_sales" to "بيع فقط",
            "p_today" to "اليوم",
            "p_month" to "الشهر",
            "p_all" to "الكل",
            "p_custom" to "مخصص",
            "custom_from" to "مِن 📅",
            "custom_to" to "إلى 📅",
            "apply_filter" to "تطبيق الفلتر ✅",
            "prev_month_lbl" to "👈 الشهر الماضي",
            "next_month_lbl" to "الشهر القادم 👉",
            "no_trx_found" to "لا توجد عمليات لهذه الفئة في هذه الفترة.",
            "visit_sections" to "تصفح الأقسام",
            "visit_transactions" to "سجل العمليات",
            "cat_screen" to "شاشات",
            "cat_service" to "فلاش وسوفت",
            "cat_parts" to "قطع غيار",
            "cat_accessory" to "اكسسوارات",
            "cat_refurb" to "استثمار",
            "cat_expense" to "مصروف شخصي",
            "cat_inventory" to "شراء مخزون",
            "cat_other" to "أخرى"
        ),
        "fr" to mapOf(
            "today_profit" to "Bénéfice d'Aujourd'hui 💰",
            "expenses_lbl" to "Dépenses 💸",
            "income_lbl" to "Revenus 💵",
            "month_profit" to "Bénéfice du Mois 📈",
            "profit_net_lbl" to "Bénéfice Net",
            "ops_unit" to "op",
            "stat_split_title" to "Revenus vs Dépenses 📊",
            "ops_maint" to "Réparations",
            "ops_sales" to "Ventes & Accessoires",
            "net_earnings" to "Bénéfice Filtré",
            "recent_filter_ops" to "Opérations Récentes 📝",
            "f_all" to "Tous",
            "f_maint" to "Réparations",
            "f_sales" to "Ventes",
            "p_today" to "Aujourd'hui",
            "p_month" to "Mois",
            "p_all" to "Tous",
            "p_custom" to "Perso",
            "custom_from" to "De 📅",
            "custom_to" to "À 📅",
            "apply_filter" to "Filtrer ✅",
            "prev_month_lbl" to "👈 Mois préc.",
            "next_month_lbl" to "Mois suiv. 👉",
            "no_trx_found" to "Aucune transaction trouvée pour ce filtre.",
            "visit_sections" to "Voir Rayons",
            "visit_transactions" to "Historique",
            "cat_screen" to "Écrans",
            "cat_service" to "Flash/Soft",
            "cat_parts" to "Pièces",
            "cat_accessory" to "Accs",
            "cat_refurb" to "Invest",
            "cat_expense" to "Dépense",
            "cat_inventory" to "Achat Stock",
            "cat_other" to "Autre"
        ),
        "en" to mapOf(
            "today_profit" to "Today's Net Profit 💰",
            "expenses_lbl" to "Expenses 💸",
            "income_lbl" to "Income 💵",
            "month_profit" to "This Month's Profit 📈",
            "profit_net_lbl" to "Net Profit",
            "ops_unit" to "ops",
            "stat_split_title" to "Income vs Expenses 📊",
            "ops_maint" to "Repairs/Service",
            "ops_sales" to "Sales & Accs",
            "net_earnings" to "Filtered Net Profit",
            "recent_filter_ops" to "Recent Operations 📝",
            "f_all" to "All",
            "f_maint" to "Repairs",
            "f_sales" to "Sales",
            "p_today" to "Today",
            "p_month" to "Month",
            "p_all" to "All Time",
            "p_custom" to "Custom",
            "custom_from" to "From 📅",
            "custom_to" to "To 📅",
            "apply_filter" to "Apply Filter ✅",
            "prev_month_lbl" to "👈 Prev Month",
            "next_month_lbl" to "Next Month 👉",
            "no_trx_found" to "No transactions logged in this period.",
            "visit_sections" to "View Departments",
            "visit_transactions" to "Full Logs",
            "cat_screen" to "Screens",
            "cat_service" to "Software",
            "cat_parts" to "Spare Parts",
            "cat_accessory" to "Accessories",
            "cat_refurb" to "Investments",
            "cat_expense" to "Personal Expense",
            "cat_inventory" to "Buy Stock",
            "cat_other" to "Other"
        )
    )
    return dic[lang]?.get(key) ?: dic["ar"]?.get(key) ?: key
}

private fun isSameMonthOffset(time1: Long, offset: Int): Boolean {
    val calOffset = Calendar.getInstance()
    calOffset.add(Calendar.MONTH, offset)
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    return cal1.get(Calendar.YEAR) == calOffset.get(Calendar.YEAR) &&
           cal1.get(Calendar.MONTH) == calOffset.get(Calendar.MONTH)
}

private fun getMonthNameWithYear(offset: Int, lang: String): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, offset)
    val sdf = SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
    return sdf.format(cal.time)
}

private fun formatCustomDate(timestamp: Long?, lang: String): String {
    if (timestamp == null) {
        return when (lang) {
            "ar" -> "اختر تاريخاً"
            "fr" -> "Sélectionner"
            else -> "Choose date"
        }
    }
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val sdf = SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault())
    return sdf.format(cal.time)
}

sealed interface DashboardItem {
    data class Trans(val trx: com.example.data.model.WorkshopTransaction) : DashboardItem
    data class DebtOp(
        val id: Int,
        val name: String,
        val amount: Double,
        val date: Long,
        val typeLabel: String,
        val notes: String,
        val color: Color,
        val icon: androidx.compose.ui.graphics.vector.ImageVector
    ) : DashboardItem
}

