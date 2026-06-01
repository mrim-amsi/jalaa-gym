package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val joinDate: Long = System.currentTimeMillis(),
    val profileImageUri: String? = null,
    val activeEndDate: Long = 0,
    val currentSubscriptionType: String? = null,
    val isActive: Boolean = false
)
