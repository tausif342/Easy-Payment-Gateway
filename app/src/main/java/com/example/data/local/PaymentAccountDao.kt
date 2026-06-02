package com.example.data.local

import androidx.room.*
import com.example.data.model.PaymentAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentAccountDao {
    @Query("SELECT * FROM payment_accounts ORDER BY timestamp DESC")
    fun getAllPaymentAccountsFlow(): Flow<List<PaymentAccount>>

    @Query("SELECT * FROM payment_accounts")
    suspend fun getAllPaymentAccounts(): List<PaymentAccount>

    @Query("SELECT * FROM payment_accounts WHERE id = :id LIMIT 1")
    suspend fun getPaymentAccountById(id: String): PaymentAccount?

    @Query("SELECT * FROM payment_accounts WHERE projectId = :projectId")
    suspend fun getAccountsByProject(projectId: String): List<PaymentAccount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentAccount(account: PaymentAccount)

    @Delete
    suspend fun deletePaymentAccount(account: PaymentAccount)

    @Query("DELETE FROM payment_accounts")
    suspend fun clearAllPaymentAccounts()
}
