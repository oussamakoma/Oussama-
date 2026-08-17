package com.example.data.local

import androidx.room.*
import com.example.data.model.MaintenanceExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceExpenseDao {
    @Query("SELECT * FROM maintenance_expenses WHERE deviceId = :deviceId")
    fun getExpensesForDevice(deviceId: Int): Flow<List<MaintenanceExpense>>

    @Query("SELECT * FROM maintenance_expenses WHERE deviceId = :deviceId")
    suspend fun getExpensesForDeviceSync(deviceId: Int): List<MaintenanceExpense>

    @Query("SELECT * FROM maintenance_expenses")
    fun getAllExpensesFlow(): Flow<List<MaintenanceExpense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: MaintenanceExpense): Long

    @Update
    suspend fun updateExpense(expense: MaintenanceExpense)

    @Delete
    suspend fun deleteExpense(expense: MaintenanceExpense)

    @Query("DELETE FROM maintenance_expenses WHERE deviceId = :deviceId")
    suspend fun deleteExpensesForDevice(deviceId: Int)

    @Query("DELETE FROM maintenance_expenses")
    suspend fun deleteAllExpenses()
}
