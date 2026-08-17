package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.local.WorkshopDatabase
import com.example.data.repository.WorkshopRepository
import com.example.data.repository.SettingsManager

class WorkshopApplication : Application() {
    val database: WorkshopDatabase by lazy {
        Room.databaseBuilder(
            this,
            WorkshopDatabase::class.java,
            "workshop_database"
        )
        .addMigrations(com.example.data.local.MIGRATION_10_11, com.example.data.local.MIGRATION_11_12)
        .fallbackToDestructiveMigration() // Simple migration support
        .build()
    }
    
    val repository: WorkshopRepository by lazy {
        WorkshopRepository(
            database.transactionDao, 
            database.personalDebtDao, 
            database.installmentPaymentDao,
            database.refurbishedDeviceDao,
            database.maintenanceExpenseDao
        )
    }

    val settingsManager: SettingsManager by lazy {
        SettingsManager(applicationContext)
    }
}

