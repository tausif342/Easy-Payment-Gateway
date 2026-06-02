package com.example.data.local

import androidx.room.*
import com.example.data.model.SmsTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsTransactionDao {
    @Query("SELECT * FROM sms_transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<SmsTransaction>>

    @Query("SELECT * FROM sms_transactions WHERE txnId = :txnId LIMIT 1")
    suspend fun getTransactionByTxnId(txnId: String): SmsTransaction?

    @Query("SELECT EXISTS(SELECT 1 FROM sms_transactions WHERE txnId = :txnId)")
    suspend fun existsByTxnId(txnId: String): Boolean

    @Query("SELECT * FROM sms_transactions WHERE syncStatus = :status ORDER BY timestamp ASC")
    suspend fun getTransactionsBySyncStatus(status: String): List<SmsTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: SmsTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: SmsTransaction)

    @Query("UPDATE sms_transactions SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, status: String)

    @Query("DELETE FROM sms_transactions")
    suspend fun clearAllTransactions()
}
