package com.example.data.repository

import com.example.data.model.PersonalDebt
import com.example.data.model.WorkshopTransaction
import com.example.data.model.RefurbishedDevice
import com.example.data.model.MaintenanceExpense
import com.example.data.model.InstallmentPayment
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.BufferedReader
import java.io.StringReader

data class AppBackupData(
    val transactions: List<WorkshopTransaction> = emptyList(),
    val debts: List<PersonalDebt> = emptyList(),
    val devices: List<RefurbishedDevice> = emptyList(),
    val expenses: List<MaintenanceExpense> = emptyList(),
    val installments: List<InstallmentPayment> = emptyList()
)

object BackupHandler {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        
    private val adapter = moshi.adapter(AppBackupData::class.java)

    /**
     * Converts all database entities into a JSON backup format.
     */
    fun createJsonBackup(
        transactions: List<WorkshopTransaction>,
        debts: List<PersonalDebt>,
        devices: List<RefurbishedDevice>,
        expenses: List<MaintenanceExpense>,
        installments: List<InstallmentPayment>
    ): String {
        val backupData = AppBackupData(
            transactions = transactions,
            debts = debts,
            devices = devices,
            expenses = expenses,
            installments = installments
        )
        return adapter.toJson(backupData) ?: "{}"
    }

    /**
     * Parses a JSON backup or falls back to legacy CSV parser.
     */
    fun parseBackup(data: String): AppBackupData {
        if (data.trimStart().startsWith("{")) {
            // JSON Format
            try {
                return adapter.fromJson(data) ?: AppBackupData()
            } catch (e: Exception) {
                e.printStackTrace()
                return AppBackupData()
            }
        } else {
            // Legacy CSV Format
            val (t, d) = parseLegacyCsvBackup(data)
            return AppBackupData(transactions = t, debts = d)
        }
    }

    /**
     * Legacy CSV Parser for backwards compatibility with old backups.
     * Generates new IDs since old backups didn't store them.
     */
    private fun parseLegacyCsvBackup(csvData: String): Pair<List<WorkshopTransaction>, List<PersonalDebt>> {
        val transactions = mutableListOf<WorkshopTransaction>()
        val debts = mutableListOf<PersonalDebt>()
                
        try {
            val reader = BufferedReader(StringReader(csvData))
            var line: String? = reader.readLine() // skip headers
                        
            while (reader.readLine().also { line = it } != null) {
                val row = line?.trim() ?: continue
                if (row.isEmpty()) continue
                                
                // Parse CSV columns splitting on comma
                val cols = row.split(",")
                if (cols.size < 7) continue
                                
                val type = cols[0]
                if (type == "TRANSACTION") {
                    val title = cols.getOrNull(1) ?: "صيانة"
                    val category = cols.getOrNull(2) ?: "OTHER"
                    val costPrice = cols.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                    val sellingPrice = cols.getOrNull(4)?.toDoubleOrNull() ?: 0.0
                    val date = cols.getOrNull(5)?.toLongOrNull() ?: System.currentTimeMillis()
                    val notes = cols.getOrNull(6) ?: ""
                    val deviceModel = cols.getOrNull(7) ?: ""
                    val customerName = cols.getOrNull(8) ?: ""
                    val creditAmount = cols.getOrNull(9)?.toDoubleOrNull() ?: 0.0
                    val creditPaid = cols.getOrNull(10)?.toDoubleOrNull() ?: 0.0
                    val wallet = cols.getOrNull(11) ?: "محفظة المحل"
                    val isDelivered = cols.getOrNull(12)?.toBoolean() ?: true
                                        
                    transactions.add(
                        WorkshopTransaction(
                            id = 0,
                            title = title,
                            category = category,
                            costPrice = costPrice,
                            sellingPrice = sellingPrice,
                            date = date,
                            notes = notes,
                            deviceModel = deviceModel,
                            customerName = customerName,
                            creditAmount = creditAmount,
                            creditPaid = creditPaid,
                            wallet = wallet,
                            isDelivered = isDelivered,
                            dueDate = null,
                            affectBalance = true,
                            isPrepaid = false
                        )
                    )
                } else if (type == "DEBT") {
                    val name = cols.getOrNull(1) ?: "دين مجهول"
                    val debtTypeStr = cols.getOrNull(2) ?: "OWED_TO_ME"
                    val amount = cols.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                    val paidStr = cols.getOrNull(4) ?: "UNPAID"
                    val date = cols.getOrNull(5)?.toLongOrNull() ?: System.currentTimeMillis()
                    val notes = cols.getOrNull(6) ?: ""
                                        
                    debts.add(
                        PersonalDebt(
                            id = 0,
                            name = name,
                            amount = amount,
                            isOwedToMe = debtTypeStr == "OWED_TO_ME",
                            isPaid = paidStr == "PAID",
                            date = date,
                            notes = notes,
                            wallet = "محفظة المحل",
                            dueDate = null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
                
        return Pair(transactions, debts)
    }
}
