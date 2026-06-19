package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_debts")
data class PersonalDebt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // اسم الشخص أو الجهة الدائنة/المدينة
    val amount: Double, // قيمة الدين بالدينار الجزائري
    val isOwedToMe: Boolean, // true if ديون ليا (owed to me), false if ديون عليا (owed by me to others)
    val isPaid: Boolean = false, // هل تم سداد الدين بالكامل أم لا؟
    val wallet: String = "محفظة المحل", // محفظة المحل, حساب بنكي, etc.
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val dueDate: Long? = null
)
