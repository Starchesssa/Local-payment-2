package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionByIdOnce(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE reference = :reference AND status = 'SUCCESS' LIMIT 1")
    suspend fun findSuccessfulByReference(reference: String): TransactionEntity?

    @Query(
        """
        SELECT * FROM transactions 
        WHERE providerId = :providerId 
          AND amount = :amount 
          AND status IN ('PENDING', 'PROCESSING', 'TIMEOUT', 'UNKNOWN')
          AND createdAt >= :minTimestamp 
          AND createdAt <= :maxTimestamp
        ORDER BY createdAt DESC
        """
    )
    suspend fun findMatchableTransactions(
        providerId: String,
        amount: Long,
        minTimestamp: Long,
        maxTimestamp: Long
    ): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE status IN ('PENDING', 'PROCESSING') ORDER BY createdAt DESC")
    suspend fun getActiveTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()
}
