package com.example.data.repository

import com.example.data.model.WorkshopTransaction
import com.example.data.model.PersonalDebt
import com.example.data.model.InstallmentPayment
import com.example.data.model.RefurbishedDevice
import com.example.data.model.MaintenanceExpense
import com.example.data.local.TransactionDao
import com.example.data.local.PersonalDebtDao
import com.example.data.local.InstallmentPaymentDao
import com.example.data.local.RefurbishedDeviceDao
import com.example.data.local.MaintenanceExpenseDao
import kotlinx.coroutines.flow.Flow

class WorkshopRepository(
    private val transactionDao: TransactionDao,
    private val personalDebtDao: PersonalDebtDao,
    private val installmentPaymentDao: InstallmentPaymentDao,
    private val refurbishedDeviceDao: RefurbishedDeviceDao,
    private val maintenanceExpenseDao: MaintenanceExpenseDao
) {
    // Existing fields...
    val allDevices: Flow<List<RefurbishedDevice>> = refurbishedDeviceDao.getAllDevices()
    val allExpenses: Flow<List<MaintenanceExpense>> = maintenanceExpenseDao.getAllExpensesFlow()
    
    fun getExpensesForDevice(deviceId: Int): Flow<List<MaintenanceExpense>> {
        return maintenanceExpenseDao.getExpensesForDevice(deviceId)
    }

    suspend fun insertDevice(device: RefurbishedDevice): Long = refurbishedDeviceDao.insertDevice(device)
    suspend fun updateDevice(device: RefurbishedDevice) = refurbishedDeviceDao.updateDevice(device)
    suspend fun getDeviceById(deviceId: Int): RefurbishedDevice? = refurbishedDeviceDao.getDeviceById(deviceId)
    suspend fun deleteDevice(device: RefurbishedDevice) = refurbishedDeviceDao.deleteDevice(device)
    
    suspend fun insertExpense(expense: MaintenanceExpense): Long = maintenanceExpenseDao.insertExpense(expense)
    suspend fun updateExpense(expense: MaintenanceExpense) = maintenanceExpenseDao.updateExpense(expense)
    suspend fun deleteExpense(expense: MaintenanceExpense) = maintenanceExpenseDao.deleteExpense(expense)
    // ...
    val allTransactions: Flow<List<WorkshopTransaction>> = transactionDao.getAllTransactions()
    val allDebts: Flow<List<PersonalDebt>> = personalDebtDao.getAllDebts()
    val allInstallments: Flow<List<InstallmentPayment>> = installmentPaymentDao.getAllInstallments()

    fun getInstallmentsForRef(refId: Int, refType: String): Flow<List<InstallmentPayment>> {
        return installmentPaymentDao.getInstallmentsForRef(refId, refType)
    }

    suspend fun insertInstallment(installment: InstallmentPayment) {
        installmentPaymentDao.insertInstallment(installment)
    }

    suspend fun deleteInstallment(installment: InstallmentPayment) {
        installmentPaymentDao.deleteInstallment(installment)
    }

    suspend fun deleteInstallmentsForRef(refId: Int, refType: String) {
        installmentPaymentDao.deleteInstallmentsForRef(refId, refType)
    }

    suspend fun clearAllInstallments() {
        installmentPaymentDao.deleteAllInstallments()
    }

    suspend fun insert(transaction: WorkshopTransaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun update(transaction: WorkshopTransaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun delete(transaction: WorkshopTransaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun getById(id: Int): WorkshopTransaction? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun clearAll() {
        transactionDao.deleteAllTransactions()
    }

    suspend fun insertDebt(debt: PersonalDebt): Long {
        return personalDebtDao.insertDebt(debt)
    }

    suspend fun updateDebt(debt: PersonalDebt) {
        personalDebtDao.updateDebt(debt)
    }

    suspend fun deleteDebt(debt: PersonalDebt) {
        personalDebtDao.deleteDebt(debt)
    }

    suspend fun clearAllDebts() {
        personalDebtDao.deleteAllDebts()
    }

    suspend fun clearAllDevices() {
        refurbishedDeviceDao.deleteAllDevices()
    }

    suspend fun clearAllExpenses() {
        maintenanceExpenseDao.deleteAllExpenses()
    }
}
