package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "refurbished_devices")
data class RefurbishedDevice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceName: String,
    val serialNumber: String,
    val purchasePrice: Double,
    val salePrice: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isCreditSale: Boolean = false,
    val downPayment: Double = 0.0,
    val customerName: String? = null,
    val saleDate: Long? = null,
    val photoUri: String? = null,
    val saleNotes: String? = null
)
