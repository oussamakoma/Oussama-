package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workshop_transactions")
data class WorkshopTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // SCREEN, PARTS, ACCESSORY, SERVICE, REPAIR, OTHER
    val costPrice: Double, // ثمن الشراء / التكلفة
    val sellingPrice: Double, // ثمن البيع / سعر الخدمة
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val deviceModel: String = "", // e.g. "Redmi 9a"
    val customerName: String = "",
    val creditAmount: Double = 0.0, // إجمالي الكريدي على الزبون
    val creditPaid: Double = 0.0, // الكريدي المكتسب أو المستلم
    val wallet: String = "محفظة المحل",
    val dueDate: Long? = null,
    val isDelivered: Boolean = true,
    val affectBalance: Boolean = true
) {
    val profit: Double
        get() = if (category == "EXPENSE") -costPrice 
                else if (category == "DEBT") (sellingPrice - costPrice)
                else if (isDelivered) (sellingPrice - costPrice) 
                else -costPrice

    val creditRemaining: Double
        get() = if (creditAmount > creditPaid) creditAmount - creditPaid else 0.0
}
