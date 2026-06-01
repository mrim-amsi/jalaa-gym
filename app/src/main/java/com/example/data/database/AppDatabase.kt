package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.MemberDao
import com.example.data.dao.SubscriptionHistoryDao
import com.example.data.dao.FinanceRecordDao
import com.example.data.model.Member
import com.example.data.model.SubscriptionHistory
import com.example.data.model.FinanceRecord

@Database(
    entities = [Member::class, SubscriptionHistory::class, FinanceRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun subscriptionHistoryDao(): SubscriptionHistoryDao
    abstract fun financeRecordDao(): FinanceRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jalaa_gym_database"
                )
                .fallbackToDestructiveMigration() // ensures safety during updates
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
