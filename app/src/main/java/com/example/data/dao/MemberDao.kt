package com.example.data.dao

import androidx.room.*
import com.example.data.model.Member
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY id DESC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchMembers(query: String): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Int): Member?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Update
    suspend fun updateMember(member: Member)

    @Delete
    suspend fun deleteMember(member: Member)

    @Query("SELECT COUNT(*) FROM members WHERE isActive = 1 AND activeEndDate >= :currentTime")
    fun getActiveMembersCountFlow(currentTime: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM members WHERE isActive = 0 OR activeEndDate < :currentTime")
    fun getExpiredMembersCountFlow(currentTime: Long): Flow<Int>
}
