package com.example.data.dao

import androidx.room.*
import com.example.data.model.FinanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceRecordDao {
    @Query("SELECT * FROM finance_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<FinanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: FinanceRecord): Long

    @Delete
    suspend fun deleteRecord(record: FinanceRecord)

    @Query("SELECT SUM(amount) FROM finance_records WHERE type = 'REVENUE' AND date >= :startOfDay")
    fun getDailyRevenueFlow(startOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM finance_records WHERE type = 'EXPENSE' AND date >= :startOfDay")
    fun getDailyExpenseFlow(startOfDay: Long): Flow<Double?>
}
