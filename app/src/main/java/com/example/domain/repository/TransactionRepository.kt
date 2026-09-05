package com.example.domain.repository

import com.example.domain.model.Transaction
import com.example.domain.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>>
    fun getTransactionById(id: String): Flow<Transaction?>
    suspend fun getTransactionByIdOnce(id: String): Transaction?
    suspend fun createTransaction(transaction: Transaction): Result<Transaction>
    suspend fun updateTransactionStatus(
        id: String,
        newStatus: TransactionStatus,
        reference: String? = null,
        failureReason: String? = null,
        rawSms: String? = null
    ): Result<Transaction>
    suspend fun deleteTransaction(id: String)
    suspend fun clearHistory()
}
