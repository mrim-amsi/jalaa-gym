package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscription_history")
data class SubscriptionHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val memberId: Int,
    val memberName: String,
    val type: String, // "Monthly", "3 Months", "6 Months", "1 Year"
    val startDate: Long,
    val endDate: Long,
    val pricePaid: Double,
    val renewDate: Long = System.currentTimeMillis()
)
