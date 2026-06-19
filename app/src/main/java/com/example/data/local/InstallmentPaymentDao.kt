package com.example.data.local

import androidx.room.*
import com.example.data.model.InstallmentPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentPaymentDao {
    @Query("SELECT * FROM installment_payments ORDER BY date DESC")
    fun getAllInstallments(): Flow<List<InstallmentPayment>>

    @Query("SELECT * FROM installment_payments WHERE refId = :refId AND refType = :refType ORDER BY date ASC")
    fun getInstallmentsForRef(refId: Int, refType: String): Flow<List<InstallmentPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: InstallmentPayment): Long

    @Delete
    suspend fun deleteInstallment(installment: InstallmentPayment)

    @Query("DELETE FROM installment_payments WHERE refId = :refId AND refType = :refType")
    suspend fun deleteInstallmentsForRef(refId: Int, refType: String)

    @Query("DELETE FROM installment_payments")
    suspend fun deleteAllInstallments()
}
