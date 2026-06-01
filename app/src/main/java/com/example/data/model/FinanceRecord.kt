package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finance_records")
data class FinanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "REVENUE", "EXPENSE"
    val category: String, // e.g., "اشتراك", "رواتب", "إيجار", "صيانة", "أخرى"
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val description: String = ""
)
