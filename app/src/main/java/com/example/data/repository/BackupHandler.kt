package com.example.data.repository

import com.example.data.model.PersonalDebt
import com.example.data.model.WorkshopTransaction
import java.io.BufferedReader
import java.io.StringReader

object BackupHandler {

    /**
     * Converts transactions and debts into a single, unified CSV spreadsheet format.
     */
    fun createCsvBackup(transactions: List<WorkshopTransaction>, debts: List<PersonalDebt>): String {
        val sb = StringBuilder()
        
        // CSV Metadata Headers
        sb.append("TYPE,TITLE_OR_NAME,CATEGORY_OR_DEBTTYPE,COST_OR_AMOUNT,SELLING_OR_PAID,DATE_TIMESTAMP,NOTES,DEVICE_MODEL,CUSTOMER_NAME,CREDIT_AMOUNT,CREDIT_PAID_DEPOSIT,WALLET,IS_DELIVERED\n")
        
        // Serialize Transactions
        transactions.forEach { t ->
            val cleanTitle = t.title.replace(",", ";").replace("\n", " ")
            val cleanNotes = t.notes.replace(",", ";").replace("\n", " ")
            val cleanModel = t.deviceModel.replace(",", ";").replace("\n", " ")
            val cleanCustomer = t.customerName.replace(",", ";").replace("\n", " ")
            
            sb.append("TRANSACTION,")
              .append("$cleanTitle,")
              .append("${t.category},")
              .append("${t.costPrice},")
              .append("${t.sellingPrice},")
              .append("${t.date},")
              .append("$cleanNotes,")
              .append("$cleanModel,")
              .append("$cleanCustomer,")
              .append("${t.creditAmount},")
              .append("${t.creditPaid},")
              .append("${t.wallet},")
              .append("${t.isDelivered}\n")
        }
        
        // Serialize Personal Debts
        debts.forEach { d ->
            val cleanName = d.name.replace(",", ";").replace("\n", " ")
            val cleanNotes = d.notes.replace(",", ";").replace("\n", " ")
            val debtTypeStr = if (d.isOwedToMe) "OWED_TO_ME" else "OWED_BY_ME"
            val paidStr = if (d.isPaid) "PAID" else "UNPAID"
            
            sb.append("DEBT,")
              .append("$cleanName,")
              .append("$debtTypeStr,")
              .append("${d.amount},")
              .append("$paidStr,")
              .append("${d.date},")
              .append("$cleanNotes,")
              .append(",,,")  // empty columns matching transaction fields
              .append("\n")
        }
        
        return sb.toString()
    }

    /**
     * Parses a unified CSV backup file back into transactions and personal debts list.
     */
    fun parseCsvBackup(csvData: String): Pair<List<WorkshopTransaction>, List<PersonalDebt>> {
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
                    val wallet = cols.getOrNull(11) ?: "مصروف الشهر"
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
                            isDelivered = isDelivered
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
                            notes = notes
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
