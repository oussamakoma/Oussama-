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
    val affectBalance: Boolean = true,
    val isPrepaid: Boolean = false
) {
    val profit: Double
        get() {
            if (category == "EXPENSE") return -costPrice
            if (category == "DEBT") return (sellingPrice - costPrice)
            if (category == "REFURB") {
                if (title.startsWith("بيع")) {
                    if (creditAmount > 0.0) {
                        val totalPaid = sellingPrice - creditAmount + creditPaid
                        return totalPaid - costPrice
                    }
                    return sellingPrice - costPrice
                } else {
                    return 0.0
                }
            }
            if (isDelivered || isPrepaid) {
                if (creditAmount > 0.0) {
                    val totalPaid = sellingPrice - creditAmount + creditPaid
                    return totalPaid - costPrice
                } else {
                    return sellingPrice - costPrice
                }
            }
            return 0.0
        }
    val cashFlow: Double
        get() {
            if (category == "EXPENSE") return -costPrice
            if (category == "DEBT") return (sellingPrice - costPrice)
            if (category == "REFURB") {
                if (title.startsWith("بيع")) {
                    if (creditAmount > 0.0) {
                        return sellingPrice - creditAmount + creditPaid
                    }
                    return sellingPrice
                } else {
                    return -costPrice
                }
            }
            if (isDelivered || isPrepaid) {
                if (creditAmount > 0.0) {
                    val totalPaid = sellingPrice - creditAmount + creditPaid
                    return totalPaid - costPrice
                } else {
                    return sellingPrice - costPrice
                }
            }
            return -costPrice
        }



    val creditRemaining: Double
        get() = if (creditAmount > creditPaid) creditAmount - creditPaid else 0.0
}
