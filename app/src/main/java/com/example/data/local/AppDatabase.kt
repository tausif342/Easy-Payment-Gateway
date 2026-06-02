package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.SmsTransaction
import com.example.data.model.SyncLog

import com.example.data.model.Project
import com.example.data.model.PaymentAccount

@Database(entities = [SmsTransaction::class, SyncLog::class, Project::class, PaymentAccount::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsTransactionDao(): SmsTransactionDao
    abstract fun syncLogDao(): SyncLogDao
    abstract fun projectDao(): ProjectDao
    abstract fun paymentAccountDao(): PaymentAccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_gateway_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
