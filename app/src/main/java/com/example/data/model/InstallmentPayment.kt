package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installment_payments")
data class InstallmentPayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val refId: Int, // The target ID (can be transaction ID or personal debt ID)
    val refType: String, // "TRANSACTION" or "PERSONAL_DEBT"
    val amountPaid: Double,
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)
