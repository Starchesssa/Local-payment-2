package com.example.data.repository

import com.example.data.database.TransactionDao
import com.example.data.database.TransactionEntity
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionStatus
import com.example.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactions(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionById(id: String): Flow<Transaction?> {
        return transactionDao.getTransactionById(id).map { it?.toDomain() }
    }

    override suspend fun getTransactionByIdOnce(id: String): Transaction? {
        return transactionDao.getTransactionByIdOnce(id)?.toDomain()
    }

    override suspend fun createTransaction(transaction: Transaction): Result<Transaction> {
        return try {
            val entity = TransactionEntity.fromDomain(transaction)
            transactionDao.insertTransaction(entity)
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTransactionStatus(
        id: String,
        newStatus: TransactionStatus,
        reference: String?,
        failureReason: String?,
        rawSms: String?
    ): Result<Transaction> {
        val existing = transactionDao.getTransactionByIdOnce(id)
            ?: return Result.failure(IllegalStateException("Transaction $id not found"))

        val currentStatus = try {
            TransactionStatus.valueOf(existing.status)
        } catch (_: Exception) {
            TransactionStatus.UNKNOWN
        }

        // State Machine validation
        if (!currentStatus.canTransitionTo(newStatus)) {
            return Result.failure(
                IllegalStateException("Invalid transition from $currentStatus to $newStatus")
            )
        }

        val updatedEntity = existing.copy(
            status = newStatus.name,
            updatedAt = System.currentTimeMillis(),
            reference = reference ?: existing.reference,
            failureReason = failureReason ?: existing.failureReason,
            rawSmsMessage = rawSms ?: existing.rawSmsMessage
        )

        transactionDao.updateTransaction(updatedEntity)
        return Result.success(updatedEntity.toDomain())
    }

    override suspend fun deleteTransaction(id: String) {
        transactionDao.deleteTransactionById(id)
    }

    override suspend fun clearHistory() {
        transactionDao.clearAllTransactions()
    }
}
