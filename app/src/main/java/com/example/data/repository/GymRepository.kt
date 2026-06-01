package com.example.data.repository

import com.example.data.dao.FinanceRecordDao
import com.example.data.dao.MemberDao
import com.example.data.dao.SubscriptionHistoryDao
import com.example.data.model.FinanceRecord
import com.example.data.model.Member
import com.example.data.model.SubscriptionHistory
import kotlinx.coroutines.flow.Flow

class GymRepository(
    private val memberDao: MemberDao,
    private val subscriptionHistoryDao: SubscriptionHistoryDao,
    private val financeRecordDao: FinanceRecordDao
) {
    fun getAllMembers(): Flow<List<Member>> = memberDao.getAllMembers()

    fun searchMembers(query: String): Flow<List<Member>> = memberDao.searchMembers(query)

    suspend fun getMemberById(id: Int): Member? = memberDao.getMemberById(id)

    suspend fun insertMember(member: Member): Long = memberDao.insertMember(member)

    suspend fun updateMember(member: Member) = memberDao.updateMember(member)

    suspend fun deleteMember(member: Member) = memberDao.deleteMember(member)

    fun getActiveMembersCount(currentTime: Long): Flow<Int> = memberDao.getActiveMembersCountFlow(currentTime)

    fun getExpiredMembersCount(currentTime: Long): Flow<Int> = memberDao.getExpiredMembersCountFlow(currentTime)

    fun getAllHistory(): Flow<List<SubscriptionHistory>> = subscriptionHistoryDao.getAllHistory()

    fun getHistoryByMemberId(memberId: Int): Flow<List<SubscriptionHistory>> =
        subscriptionHistoryDao.getHistoryByMemberId(memberId)

    suspend fun insertSubscriptionHistory(history: SubscriptionHistory) =
        subscriptionHistoryDao.insertHistory(history)

    fun getAllFinanceRecords(): Flow<List<FinanceRecord>> = financeRecordDao.getAllRecords()

    suspend fun insertFinanceRecord(record: FinanceRecord): Long = financeRecordDao.insertRecord(record)

    suspend fun deleteFinanceRecord(record: FinanceRecord) = financeRecordDao.deleteRecord(record)

    fun getDailyRevenue(startOfDay: Long): Flow<Double?> = financeRecordDao.getDailyRevenueFlow(startOfDay)

    fun getDailyExpense(startOfDay: Long): Flow<Double?> = financeRecordDao.getDailyExpenseFlow(startOfDay)
}
