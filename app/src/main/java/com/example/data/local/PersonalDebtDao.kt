package com.example.data.local

import androidx.room.*
import com.example.data.model.PersonalDebt
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalDebtDao {
    @Query("SELECT * FROM personal_debts ORDER BY date DESC")
    fun getAllDebts(): Flow<List<PersonalDebt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: PersonalDebt): Long

    @Update
    suspend fun updateDebt(debt: PersonalDebt)

    @Delete
    suspend fun deleteDebt(debt: PersonalDebt)

    @Query("DELETE FROM personal_debts")
    suspend fun deleteAllDebts()
}
