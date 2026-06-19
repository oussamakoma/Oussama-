package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.PersonalDebt
import com.example.data.model.WorkshopTransaction
import com.example.data.model.InstallmentPayment
import com.example.data.model.RefurbishedDevice
import com.example.data.model.MaintenanceExpense

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE personal_debts ADD COLUMN wallet TEXT NOT NULL DEFAULT 'محفظة المحل'")
    }
}

@Database(entities = [WorkshopTransaction::class, PersonalDebt::class, InstallmentPayment::class, RefurbishedDevice::class, MaintenanceExpense::class], version = 11, exportSchema = false)
abstract class WorkshopDatabase : RoomDatabase() {
    abstract val transactionDao: TransactionDao
    abstract val personalDebtDao: PersonalDebtDao
    abstract val installmentPaymentDao: InstallmentPaymentDao
    abstract val refurbishedDeviceDao: RefurbishedDeviceDao
    abstract val maintenanceExpenseDao: MaintenanceExpenseDao
}
