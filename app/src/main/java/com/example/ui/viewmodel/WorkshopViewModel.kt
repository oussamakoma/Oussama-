package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.WorkshopTransaction
import com.example.data.model.PersonalDebt
import com.example.data.model.InstallmentPayment
import com.example.data.model.RefurbishedDevice
import com.example.data.model.MaintenanceExpense
import com.example.data.repository.WorkshopRepository
import com.example.data.repository.SettingsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateFilter(val displayNameAr: String) {
    ALL("الكل"),
    TODAY("اليوم"),
    WEEK("هذا الأسبوع"),
    MONTH("هذا الشهر")
}

enum class DeliveryFilter(val displayNameAr: String) {
    ALL("الكل"),
    DELIVERED("تم التسليم"),
    NOT_DELIVERED("لم يتم التسليم")
}

class WorkshopViewModel(
    private val repository: WorkshopRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    // App Preferences integrated directly
    val appLanguage: StateFlow<String> = settingsManager.appLanguage
    val appTheme: StateFlow<String> = settingsManager.appTheme
    val darkMode: StateFlow<String> = settingsManager.darkMode

    fun setAppLanguage(lang: String) {
        settingsManager.setAppLanguage(lang)
    }

    fun setAppTheme(theme: String) {
        settingsManager.setAppTheme(theme)
    }

    fun setDarkMode(mode: String) {
        settingsManager.setDarkMode(mode)
    }

    // Filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val walletPocketInit: StateFlow<Double> = settingsManager.walletPocketInit
    val walletBankInit: StateFlow<Double> = settingsManager.walletBankInit
    val walletGoodsInit: StateFlow<Double> = settingsManager.walletGoodsInit
    val walletPersonalInit: StateFlow<Double> = settingsManager.walletPersonalInit

    val walletPocketName: StateFlow<String> = settingsManager.walletPocketName
    val walletBankName: StateFlow<String> = settingsManager.walletBankName
    val walletGoodsName: StateFlow<String> = settingsManager.walletGoodsName
    val walletPersonalName: StateFlow<String> = settingsManager.walletPersonalName

    val walletPocketInclude: StateFlow<Boolean> = settingsManager.walletPocketInclude
    val walletBankInclude: StateFlow<Boolean> = settingsManager.walletBankInclude
    val walletGoodsInclude: StateFlow<Boolean> = settingsManager.walletGoodsInclude
    val walletPersonalInclude: StateFlow<Boolean> = settingsManager.walletPersonalInclude

    fun setWalletPocketInclude(include: Boolean) = settingsManager.setWalletPocketInclude(include)
    fun setWalletBankInclude(include: Boolean) = settingsManager.setWalletBankInclude(include)
    fun setWalletGoodsInclude(include: Boolean) = settingsManager.setWalletGoodsInclude(include)
    fun setWalletPersonalInclude(include: Boolean) = settingsManager.setWalletPersonalInclude(include)

    fun setWalletPocketInit(amount: Double) = settingsManager.setWalletPocketInit(amount)
    fun setWalletBankInit(amount: Double) = settingsManager.setWalletBankInit(amount)
    fun setWalletGoodsInit(amount: Double) = settingsManager.setWalletGoodsInit(amount)
    fun setWalletPersonalInit(amount: Double) = settingsManager.setWalletPersonalInit(amount)

    fun setWalletPocketName(name: String) = settingsManager.setWalletPocketName(name)
    fun setWalletBankName(name: String) = settingsManager.setWalletBankName(name)
    fun setWalletGoodsName(name: String) = settingsManager.setWalletGoodsName(name)
    fun setWalletPersonalName(name: String) = settingsManager.setWalletPersonalName(name)

    fun updateTransaction(transaction: WorkshopTransaction) {
        viewModelScope.launch {
            repository.update(transaction)
        }
    }

    fun toggleTransactionDelivery(transaction: WorkshopTransaction) {
        viewModelScope.launch {
            repository.update(transaction.copy(isDelivered = !transaction.isDelivered))
        }
    }

    fun renameWalletInTransactions(oldName: String, newName: String) {
        viewModelScope.launch {
            try {
                repository.allTransactions.first().forEach { transaction ->
                    val walletMatch = transaction.wallet == oldName || (oldName == "محفظة المحل" && transaction.wallet.isBlank())
                    if (walletMatch) {
                        repository.update(transaction.copy(wallet = newName))
                    }
                }
            } catch (e: Exception) {
                // Ignore or log
            }
        }
    }

    private val _selectedCategory = MutableStateFlow<String?>("ALL") // "ALL" or specific category
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _dateFilter = MutableStateFlow(DateFilter.ALL)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    private val _deliveryFilter = MutableStateFlow(DeliveryFilter.ALL)
    val deliveryFilter: StateFlow<DeliveryFilter> = _deliveryFilter.asStateFlow()

    // Retrieve transactions from database
    private val rawTransactionsFlow: Flow<List<WorkshopTransaction>> = repository.allTransactions

    // Retrieve personal debts from database
    val debtsFlow: StateFlow<List<PersonalDebt>> = repository.allDebts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val installmentsFlow: StateFlow<List<InstallmentPayment>> = repository.allInstallments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transactionsFlow: StateFlow<List<WorkshopTransaction>> = combine(
        rawTransactionsFlow, debtsFlow, installmentsFlow
    ) { baseTransactions, debts, installments ->
        val debtTransactions = debts.map { debt ->
            val actualPaidAmount = if (debt.isPaid) debt.amount else installments.filter { it.refId == debt.id && it.refType == "PERSONAL_DEBT" }.sumOf { it.amountPaid }
            val isFullyPaid = debt.isPaid || actualPaidAmount >= debt.amount
            WorkshopTransaction(
                id = -debt.id, 
                title = "دين شخصي: ${debt.name}",
                category = "DEBT",
                costPrice = if (debt.isOwedToMe) debt.amount else actualPaidAmount,
                sellingPrice = if (debt.isOwedToMe) actualPaidAmount else debt.amount,
                date = debt.date,
                notes = debt.notes,
                customerName = debt.name,
                wallet = debt.wallet,
                dueDate = debt.dueDate,
                isDelivered = isFullyPaid, 
                affectBalance = true
            )
        }
        (baseTransactions + debtTransactions).sortedByDescending { it.date }
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    // Filtered transaction list
    val filteredTransactions: StateFlow<List<WorkshopTransaction>> = combine(
        transactionsFlow,
        _searchQuery,
        _selectedCategory,
        _dateFilter,
        _deliveryFilter
    ) { transactions, query, cat, dateFltr, deliveryFltr ->
        transactions.filter { transaction ->
            // Search Match
            val matchesSearch = query.isEmpty() ||
                    transaction.title.contains(query, ignoreCase = true) ||
                    transaction.deviceModel.contains(query, ignoreCase = true) ||
                    transaction.customerName.contains(query, ignoreCase = true)

            // Category Match
            val matchesCategory = cat == "ALL" || transaction.category == cat

            // Date Match
            val matchesDate = when (dateFltr) {
                DateFilter.ALL -> true
                DateFilter.TODAY -> isToday(transaction.date)
                DateFilter.WEEK -> isThisWeek(transaction.date)
                DateFilter.MONTH -> isThisMonth(transaction.date)
            }

            // Delivery Match
            val matchesDelivery = when (deliveryFltr) {
                DeliveryFilter.ALL -> true
                DeliveryFilter.DELIVERED -> transaction.isDelivered
                DeliveryFilter.NOT_DELIVERED -> !transaction.isDelivered
            }

            matchesSearch && matchesCategory && matchesDate && matchesDelivery
        }
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Statistics Flow updated to differentiate parts cost and general expenses
    private val _totalInitialBalanceFlow: StateFlow<Double> = combine(
        listOf<Flow<Any>>(
            walletPocketInit, walletBankInit, walletGoodsInit, walletPersonalInit,
            walletPocketInclude, walletBankInclude, walletGoodsInclude, walletPersonalInclude
        )
    ) { array ->
        val p = array[0] as Double
        val b = array[1] as Double
        val g = array[2] as Double
        val pe = array[3] as Double
        val pInc = array[4] as Boolean
        val bInc = array[5] as Boolean
        val gInc = array[6] as Boolean
        val peInc = array[7] as Boolean
        (if (pInc) p else 0.0) + (if (bInc) b else 0.0) + (if (gInc) g else 0.0) + (if (peInc) pe else 0.0)
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val statsFlow: StateFlow<WorkshopStats> = combine(
        transactionsFlow,
        _selectedCategory,
        _dateFilter,
        _totalInitialBalanceFlow,
        combine(walletPersonalName, debtsFlow, installmentsFlow) { name, debts, inst ->
            Triple(name, debts, inst)
        }
    ) { transactions, cat, dateFltr, totalInitialBalance, debtsAndName ->
        val (personalNameVal, debts, installments) = debtsAndName
        val filteredForStats = transactions.filter { transaction ->
            val matchesDate = when (dateFltr) {
                DateFilter.ALL -> true
                DateFilter.TODAY -> isToday(transaction.date)
                DateFilter.WEEK -> isThisWeek(transaction.date)
                DateFilter.MONTH -> isThisMonth(transaction.date)
            }
            matchesDate
        }

        val partsCost = filteredForStats.filter { 
            it.category != "EXPENSE" && 
            it.category != "DEBT" &&
            (it.isDelivered || it.isPrepaid) && 
            !(it.category == "REFURB" && it.title.startsWith("بيع"))
        }.sumOf { it.costPrice }

        val personalExpenses = filteredForStats.filter { 
            it.category == "EXPENSE" && 
            (it.wallet == personalNameVal || it.wallet == "مصروف شخصي" || it.wallet == "مصروفي شخصي" || it.wallet == "مصروفي الشخصي")
        }.sumOf { it.costPrice }

        val shopExpenses = filteredForStats.filter { 
            it.category == "EXPENSE" && 
            !(it.wallet == personalNameVal || it.wallet == "مصروف شخصي" || it.wallet == "مصروفي شخصي" || it.wallet == "مصروفي الشخصي")
        }.sumOf { it.costPrice } + 
        filteredForStats.filter { it.category == "DEBT" && it.profit < 0 }.sumOf { -it.profit }

        val expensesCost = personalExpenses + shopExpenses
        
        val totalRevenue = filteredForStats.filter { (it.isDelivered || it.isPrepaid) && it.category != "DEBT" }.sumOf { it.sellingPrice } + filteredForStats.filter { it.category == "DEBT" && it.profit > 0 }.sumOf { it.profit }
        
        val workshopNetProfit = totalRevenue - partsCost - shopExpenses

        // Net Profit = Sum of individual transaction profits matching pocket balance changes exactly
        val totalProfit = filteredForStats.sumOf { 
            if (it.category == "REFURB" && it.title.startsWith("بيع")) it.sellingPrice else it.profit
        }
        val count = filteredForStats.size

        val workshopDebts = filteredForStats.sumOf { it.creditRemaining }

        val personalDebtsOwedToMe = debts.filter { 
            it.isOwedToMe && !it.isPaid && (dateFltr == DateFilter.ALL || when (dateFltr) {
                DateFilter.TODAY -> isToday(it.date)
                DateFilter.WEEK -> isThisWeek(it.date)
                DateFilter.MONTH -> isThisMonth(it.date)
                else -> true
            })
        }.sumOf { debt -> 
            debt.amount - installments.filter { it.refId == debt.id && it.refType == "PERSONAL_DEBT" }.sumOf { it.amountPaid } 
        }

        val personalDebtsOwedByMe = debts.filter { 
            !it.isOwedToMe && !it.isPaid && (dateFltr == DateFilter.ALL || when (dateFltr) {
                DateFilter.TODAY -> isToday(it.date)
                DateFilter.WEEK -> isThisWeek(it.date)
                DateFilter.MONTH -> isThisMonth(it.date)
                else -> true
            })
        }.sumOf { debt -> 
            debt.amount - installments.filter { it.refId == debt.id && it.refType == "PERSONAL_DEBT" }.sumOf { it.amountPaid } 
        }

        WorkshopStats(
            totalCost = partsCost + expensesCost,
            partsCost = partsCost,
            expensesCost = expensesCost,
            personalExpenses = personalExpenses,
            shopExpenses = shopExpenses,
            workshopNetProfit = workshopNetProfit,
            totalRevenue = totalRevenue,
            totalProfit = totalProfit,
            transactionCount = count,
            workshopDebts = workshopDebts,
            personalDebtsOwedToMe = personalDebtsOwedToMe,
            personalDebtsOwedByMe = personalDebtsOwedByMe
        )
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WorkshopStats()
    )

    // Dynamic Budget Categories
    private val _budgetLimitsUpdateTrigger = MutableStateFlow(0)

    val budgetCategories: StateFlow<List<com.example.ui.BudgetCategory>> = combine(
        transactionsFlow,
        _budgetLimitsUpdateTrigger
    ) { transactions, _ ->
            // Assume we want current month
            val currentMonthTransactions = transactions.filter { isThisMonth(it.date) }
            val lastMonthTransactions = transactions.filter { isLastMonth(it.date) }
            
            // Calculate spent per category for current month
            val currentMonthSpent = currentMonthTransactions
                .groupBy { transaction -> getBudgetCategoryForTransaction(transaction) }
                .filterKeys { it != null }
                .mapValues { (_, transList) -> transList.sumOf { it.costPrice } }

            // Calculate spent per category for last month
            val lastMonthSpent = lastMonthTransactions
                .groupBy { transaction -> getBudgetCategoryForTransaction(transaction) }
                .filterKeys { it != null }
                .mapValues { (_, transList) -> transList.sumOf { it.costPrice } }

            // Combine spending with all default categories to ensure visibility
            val allCategoryNames = (defaultBudgetLimits.keys + currentMonthSpent.keys + lastMonthSpent.keys).filterNotNull().toSet()

            allCategoryNames.map { nonNullCatName ->
                val spent = currentMonthSpent[nonNullCatName] ?: 0.0
                val defaultLimit = defaultBudgetLimits[nonNullCatName] ?: 10000.0
                val total = settingsManager.getBudgetLimit(nonNullCatName, defaultLimit)
                
                val lastSpent = lastMonthSpent[nonNullCatName] ?: 0.0
                val diffPercent = if (lastSpent > 0) {
                    ((spent - lastSpent) / lastSpent) * 100
                } else if (spent > 0) {
                    100.0
                } else {
                    0.0
                }

                val percentageChange = if (diffPercent > 0) {
                    "+${"%.1f".format(diffPercent)}%"
                } else if (diffPercent < 0) {
                    "${"%.1f".format(diffPercent)}%"
                } else {
                    "0.0%"
                }

                com.example.ui.BudgetCategory(
                    name = nonNullCatName,
                    spent = spent,
                    total = total,
                    percentageChange = percentageChange,
                    lastMonthSpent = lastSpent
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val defaultBudgetLimits = mapOf(
        "الغذاء" to 14000.0,
        "الأسرة" to 1250.0,
        "مواصلات" to 5000.0,
        "إنترنت" to 3000.0,
        "فواتير" to 5000.0,
        "قهوة" to 2000.0,
        "صحة وعلاج" to 5000.0,
        "إيجار" to 20000.0,
        "تسوق" to 5000.0,
        "العناية الشخصية" to 3000.0,
        "كوسميتيك" to 2000.0,
        "حلاقة" to 1500.0,
        "أخرى" to 5000.0
    )

    fun updateBudgetLimit(category: String, limit: Double) {
        settingsManager.setBudgetLimit(category, limit)
        _budgetLimitsUpdateTrigger.value += 1
    }

    private fun getBudgetCategoryForTransaction(transaction: WorkshopTransaction): String? {
        val categoriesMap = mapOf(
            "FOOD" to "الغذاء",
            "FAMILY" to "الأسرة",
            "TRANSPORT" to "مواصلات",
            "INTERNET" to "إنترنت",
            "BILLS" to "فواتير",
            "COFFEE" to "قهوة",
            "HEALTH" to "صحة وعلاج",
            "RENT" to "إيجار",
            "SHOPPING" to "تسوق"
        )
        
        return if (transaction.category == "EXPENSE") {
            // For EXPENSE category, infer the budget category from the title
            val titleMappings = mapOf(
                "غذاء" to "الغذاء",
                "طعام" to "الغذاء",
                "صدقة" to "الأسرة",
                "أسرة" to "الأسرة",
                "سكن" to "إيجار",
                "ايجار" to "إيجار",
                "إيجار" to "إيجار",
                "انترنت" to "إنترنت",
                "إنترنت" to "إنترنت",
                "نت" to "إنترنت",
                "مواصلات" to "مواصلات",
                "فواتير" to "فواتير",
                "قهوة" to "قهوة",
                "صحة" to "صحة وعلاج",
                "علاج" to "صحة وعلاج",
                "تسوق" to "تسوق",
                "عناية" to "العناية الشخصية",
                "كوسميتيك" to "كوسميتيك",
                "حلاقة" to "حلاقة",
                "حلاق" to "حلاقة",
                "تجميل" to "كوسميتيك",
                "خرى" to "أخرى",
                "أخرى" to "أخرى",
                "الخرى" to "أخرى",
                "الأخرى" to "أخرى",
                "علبة" to "أخرى",
                "كرتون" to "أخرى",
                "كرطون" to "أخرى"
            )
            
            // Normalize title for more robust matching
            val normalizedTitle = transaction.title.replace(Regex("[^\\p{L}\\p{Nd}]+"), " ").lowercase()
            
            // Find matching category from titleMappings based on transaction title
            titleMappings.entries.find { it.key in normalizedTitle }?.value
            // Or match directly against defaultBudgetLimits keys as a fallback
                ?: defaultBudgetLimits.keys.find { it in transaction.title }
        } else {
            categoriesMap[transaction.category]
        }
    }

    // Update getTransactionsForBudgetCategory to use current month by default or custom range if needed
    fun getTransactionsForBudgetCategory(categoryName: String): Flow<List<WorkshopTransaction>> {
        return transactionsFlow.map { transactions ->
            transactions.filter { 
                getBudgetCategoryForTransaction(it) == categoryName && isThisMonth(it.date)
            }
        }
    }

    /**
     * Calculates budget categories for a specific date range.
     * Use this for custom range analysis in the Pie Chart.
     */
    fun getCategoriesForRange(start: Long, end: Long): Flow<List<com.example.ui.BudgetCategory>> {
        return transactionsFlow.map { allTransactions ->
            val rangeTransactions = allTransactions.filter { it.date in start..end }
            
            // For comparison, we use the same duration before the start date
            val duration = end - start
            val prevStart = start - duration
            val prevEnd = start
            val prevRangeTransactions = allTransactions.filter { it.date in prevStart..prevEnd }

            val currentSpentMap = rangeTransactions
                .groupBy { getBudgetCategoryForTransaction(it) }
                .filterKeys { it != null }
                .mapValues { it.value.sumOf { t -> t.costPrice } }

            val prevSpentMap = prevRangeTransactions
                .groupBy { getBudgetCategoryForTransaction(it) }
                .filterKeys { it != null }
                .mapValues { it.value.sumOf { t -> t.costPrice } }

            // Combine all categories that have spending in either range
            val allCatNames = (currentSpentMap.keys + prevSpentMap.keys).filterNotNull().toSet()

            allCatNames.map { catName ->
                val spent = currentSpentMap[catName] ?: 0.0
                val lastSpent = prevSpentMap[catName] ?: 0.0
                val defaultLimit = defaultBudgetLimits[catName] ?: 10000.0
                val totalLimit = settingsManager.getBudgetLimit(catName, defaultLimit)

                val diffPercent = if (lastSpent > 0) {
                    ((spent - lastSpent) / lastSpent) * 100
                } else if (spent > 0) {
                    100.0
                } else {
                    0.0
                }

                val percentageChange = if (diffPercent > 0) {
                    "+${"%.1f".format(diffPercent)}%"
                } else if (diffPercent < 0) {
                    "${"%.1f".format(diffPercent)}%"
                } else {
                    "0.0%"
                }

                com.example.ui.BudgetCategory(
                    name = catName,
                    spent = spent,
                    total = totalLimit,
                    percentageChange = percentageChange,
                    lastMonthSpent = lastSpent
                )
            }.sortedByDescending { it.spent }
        }
    }

    // Actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category ?: "ALL"
    }

    fun setDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }

    fun setDeliveryFilter(filter: DeliveryFilter) {
        _deliveryFilter.value = filter
    }

    fun addTransaction(
        title: String,
        category: String,
        costPrice: Double,
        sellingPrice: Double,
        deviceModel: String = "",
        customerName: String = "",
        notes: String = "",
        creditAmount: Double = 0.0,
        creditPaid: Double = 0.0,
        wallet: String = "محفظة المحل",
        dueDate: Long? = null,
        date: Long = System.currentTimeMillis(),
        isDelivered: Boolean = true,
        affectBalance: Boolean = true,
        isPrepaid: Boolean = false
    ) {
        viewModelScope.launch {
            val transaction = WorkshopTransaction(
                title = title,
                category = category,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                deviceModel = deviceModel,
                customerName = customerName,
                notes = notes,
                creditAmount = creditAmount,
                creditPaid = creditPaid,
                wallet = wallet,
                date = date,
                dueDate = dueDate,
                isDelivered = isDelivered,
                affectBalance = affectBalance,
                isPrepaid = isPrepaid
            )
            val insertedId = repository.insert(transaction)
            if (creditAmount > 0.0) {
                // Initial down-payment: sellingPrice - creditAmount
                val initialDownpayment = sellingPrice - creditAmount
                if (initialDownpayment > 0.0) {
                    repository.insertInstallment(
                        InstallmentPayment(
                            refId = insertedId.toInt(),
                            refType = "TRANSACTION",
                            amountPaid = initialDownpayment,
                            notes = "دفعة أولى مقدمة عند الشراء",
                            date = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun addPersonalDebt(
        name: String,
        amount: Double,
        isOwedToMe: Boolean,
        wallet: String = "محفظة المحل",
        notes: String = "",
        dueDate: Long? = null
    ) {
        viewModelScope.launch {
            val debt = PersonalDebt(
                name = name,
                amount = amount,
                isOwedToMe = isOwedToMe,
                wallet = wallet,
                notes = notes,
                isPaid = false,
                dueDate = dueDate
            )
            val insertedId = repository.insertDebt(debt)
            // Storing an initial installment if needed, but usually personal debts are full amount.
        }
    }

    // Installment log helper methods
    fun addInstallment(refId: Int, refType: String, amountPaid: Double, notes: String = "") {
        viewModelScope.launch {
            repository.insertInstallment(
                InstallmentPayment(
                    refId = refId,
                    refType = refType,
                    amountPaid = amountPaid,
                    notes = notes,
                    date = System.currentTimeMillis()
                )
            )
        }
    }

    // Refurbished Devices
    val refurbishedDevicesFlow: StateFlow<List<RefurbishedDevice>> = repository.allDevices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allExpensesFlow: StateFlow<List<MaintenanceExpense>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addDevice(name: String, serialNumber: String, purchasePrice: Double, createdAt: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertDevice(RefurbishedDevice(deviceName = name, serialNumber = serialNumber, purchasePrice = purchasePrice, createdAt = createdAt))
            addTransaction(
                title = "شراء هاتف للاستثمار: $name",
                category = "REFURB",
                costPrice = purchasePrice,
                sellingPrice = 0.0,
                deviceModel = name,
                customerName = "",
                notes = "شراء هاتف للاستثمار تدوير. رقم تسلسلي: ${serialNumber.ifBlank { "غير مسجل" }}",
                creditAmount = 0.0,
                creditPaid = 0.0,
                wallet = walletPocketName.value,
                date = createdAt
            )
        }
    }

    fun updateDevice(device: RefurbishedDevice) {
        viewModelScope.launch {
            repository.updateDevice(device)
        }
    }

    fun deleteDevice(device: RefurbishedDevice) {
        viewModelScope.launch {
            // 1. Get all maintenance expenses for this device first
            val expensesList = repository.getExpensesForDeviceSync(device.id)
            
            // 2. Gather all dates to delete from transactions
            val datesToDelete = mutableListOf<Long>()
            datesToDelete.add(device.createdAt)
            device.saleDate?.let { datesToDelete.add(it) }
            expensesList.forEach { datesToDelete.add(it.date) }
            
            val uniqueDates = datesToDelete.distinct()
            
            // 3. Delete the corresponding transactions by model name first (bulletproof)
            repository.deleteRefurbTransactionsByModel(device.deviceName)
            
            // 3b. Also delete by dates as a fallback
            if (uniqueDates.isNotEmpty()) {
                repository.deleteRefurbTransactionsByDates(uniqueDates)
            }
            
            // 4. Delete maintenance expenses from local db
            repository.deleteExpensesForDevice(device.id)
            
            // 5. Delete the device itself
            repository.deleteDevice(device)
        }
    }

    fun addMaintenanceExpense(deviceId: Int, partName: String, cost: Double, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.insertExpense(MaintenanceExpense(deviceId = deviceId, partName = partName, cost = cost, date = date))
            val device = repository.getDeviceById(deviceId)
            val deviceModelN = device?.deviceName ?: "هاتف استثمار"
            addTransaction(
                title = "تجهيز هاتف استثمار ($deviceModelN): $partName",
                category = "REFURB",
                costPrice = cost,
                sellingPrice = 0.0,
                deviceModel = deviceModelN,
                customerName = "",
                notes = "تكلفة قطعة غيار/تجهيز للهاتف المستثمر",
                creditAmount = 0.0,
                creditPaid = 0.0,
                wallet = walletPocketName.value,
                date = date
            )
        }
    }

    fun deleteExpense(expense: MaintenanceExpense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            repository.deleteRefurbTransactionByDate(expense.date)
        }
    }

    fun getMaintenanceExpensesFlow(deviceId: Int): Flow<List<MaintenanceExpense>> {
        return repository.getExpensesForDevice(deviceId)
    }

    fun sellDevice(
        deviceId: Int, 
        salePrice: Double, 
        isCredit: Boolean = false, 
        downPayment: Double = 0.0, 
        customerName: String? = null, 
        saleDate: Long = System.currentTimeMillis(), 
        costPrice: Double = 0.0,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val device = repository.getDeviceById(deviceId)
            device?.let {
                repository.updateDevice(it.copy(
                    salePrice = salePrice, 
                    isCreditSale = isCredit,
                    customerName = customerName,
                    downPayment = downPayment,
                    saleDate = saleDate,
                    saleNotes = notes
                ))

                // Record transaction
                val creditAmount = if (isCredit) salePrice - downPayment else 0.0
                val transNotes = buildString {
                    if (isCredit) {
                        append("بيع بالتقسيط (كريدي)")
                    } else {
                        append("بيع نقدي")
                    }
                    if (!notes.isNullOrBlank()) {
                        append(" • ملاحظة: $notes")
                    }
                }
                
                addTransaction(
                    title = "بيع هاتف استثمار: ${it.deviceName}",
                    category = "REFURB",
                    costPrice = costPrice, // Pass the total furbishing of the device as costPrice (shows up as strikethrough 'Tach'tib')
                    sellingPrice = salePrice,
                    deviceModel = it.deviceName,
                    customerName = customerName ?: "",
                    notes = transNotes,
                    creditAmount = creditAmount,
                    creditPaid = 0.0,
                    wallet = walletPocketName.value,
                    date = saleDate
                )

                // Add to Debts if credit
                if (isCredit && creditAmount > 0) {
                    addPersonalDebt(
                        name = customerName ?: "زبون غير معروف",
                        amount = creditAmount,
                        isOwedToMe = true,
                        notes = "باقي تقسيط هاتف ${it.deviceName}" + (if (!notes.isNullOrBlank()) " ($notes)" else "")
                    )
                }
            }
        }
    }

    fun restoreDevice(deviceId: Int) {
        viewModelScope.launch {
            val device = repository.getDeviceById(deviceId)
            device?.let {
                // Delete sale transaction
                it.saleDate?.let { sDate ->
                    repository.deleteRefurbTransactionByDate(sDate)
                }
                repository.updateDevice(it.copy(
                    salePrice = null,
                    isCreditSale = false,
                    customerName = null,
                    downPayment = 0.0,
                    saleDate = null
                ))
            }
        }
    }

    fun deleteInstallment(installment: InstallmentPayment) {
        viewModelScope.launch {
            repository.deleteInstallment(installment)
        }
    }

    fun togglePersonalDebtPaid(debt: PersonalDebt) {
        viewModelScope.launch {
            val newPaid = !debt.isPaid
            repository.updateDebt(debt.copy(isPaid = newPaid))
            if (newPaid) {
                // Auto insert installment payment for complete debt repayment
                repository.insertInstallment(
                    InstallmentPayment(
                        refId = debt.id,
                        refType = "PERSONAL_DEBT",
                        amountPaid = debt.amount,
                        date = System.currentTimeMillis(),
                        notes = "تسديد كامل للدين"
                    )
                )
            } else {
                // If toggled back to unpaid status, remove installment records
                repository.deleteInstallmentsForRef(debt.id, "PERSONAL_DEBT")
            }
        }
    }

    fun updateDebt(debt: PersonalDebt) {
        viewModelScope.launch {
            repository.updateDebt(debt)
        }
    }

    fun deletePersonalDebt(debt: PersonalDebt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
            repository.deleteInstallmentsForRef(debt.id, "PERSONAL_DEBT")
        }
    }

    fun clearAllDebts() {
        viewModelScope.launch {
            repository.clearAllDebts()
            repository.clearAllInstallments() // Clean everything
        }
    }

    fun deleteTransaction(transaction: WorkshopTransaction) {
        viewModelScope.launch {
            repository.delete(transaction)
            repository.deleteInstallmentsForRef(transaction.id, "TRANSACTION")
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun clearAllApplicationData() {
        viewModelScope.launch {
            repository.clearAll()
            repository.clearAllDebts()
            repository.clearAllInstallments()
            repository.clearAllDevices()
            repository.clearAllExpenses()
        }
    }

    // Helper functions for date matching
    private fun isToday(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisWeek(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
    }

    fun importBackup(backupData: com.example.data.repository.AppBackupData) {
        viewModelScope.launch {
            // Bulk insert incoming objects preserving original IDs from backup
            backupData.transactions.forEach {
                repository.insert(it)
            }
            backupData.debts.forEach {
                repository.insertDebt(it)
            }
            backupData.devices.forEach {
                repository.insertDevice(it)
            }
            backupData.expenses.forEach {
                repository.insertExpense(it)
            }
            backupData.installments.forEach {
                repository.insertInstallment(it)
            }
        }
    }

    private fun isThisMonth(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    private fun isLastMonth(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance()
        cal1.add(Calendar.MONTH, -1)
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }
}

data class WorkshopStats(
    val totalCost: Double = 0.0,
    val partsCost: Double = 0.0,
    val expensesCost: Double = 0.0,
    val personalExpenses: Double = 0.0,
    val shopExpenses: Double = 0.0,
    val workshopNetProfit: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val transactionCount: Int = 0,
    val workshopDebts: Double = 0.0,
    val personalDebtsOwedToMe: Double = 0.0,
    val personalDebtsOwedByMe: Double = 0.0
)

class WorkshopViewModelFactory(
    private val repository: WorkshopRepository,
    private val settingsManager: SettingsManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkshopViewModel::class.java)) {
            return WorkshopViewModel(repository, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
