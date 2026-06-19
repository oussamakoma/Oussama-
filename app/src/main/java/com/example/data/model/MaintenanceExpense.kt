package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance_expenses")
data class MaintenanceExpense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: Int, // Refers to RefurbishedDevice.id
    val partName: String,
    val cost: Double,
    val date: Long = System.currentTimeMillis()
)
