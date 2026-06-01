package com.example.data.dao

import androidx.room.*
import com.example.data.model.SubscriptionHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionHistoryDao {
    @Query("SELECT * FROM subscription_history ORDER BY renewDate DESC")
    fun getAllHistory(): Flow<List<SubscriptionHistory>>

    @Query("SELECT * FROM subscription_history WHERE memberId = :memberId ORDER BY renewDate DESC")
    fun getHistoryByMemberId(memberId: Int): Flow<List<SubscriptionHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: SubscriptionHistory)
}
