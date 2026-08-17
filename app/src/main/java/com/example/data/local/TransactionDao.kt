package com.example.data.local

import androidx.room.*
import com.example.data.model.WorkshopTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM workshop_transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<WorkshopTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: WorkshopTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: WorkshopTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: WorkshopTransaction)

    @Query("DELETE FROM workshop_transactions WHERE category = 'REFURB' AND date IN (:dates)")
    suspend fun deleteRefurbTransactionsByDates(dates: List<Long>)

    @Query("DELETE FROM workshop_transactions WHERE category = 'REFURB' AND deviceModel = :deviceModel")
    suspend fun deleteRefurbTransactionsByModel(deviceModel: String)

    @Query("DELETE FROM workshop_transactions WHERE category = 'REFURB' AND date = :date")
    suspend fun deleteRefurbTransactionByDate(date: Long)

    @Query("SELECT * FROM workshop_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Int): WorkshopTransaction?

    @Query("DELETE FROM workshop_transactions")
    suspend fun deleteAllTransactions()
}
