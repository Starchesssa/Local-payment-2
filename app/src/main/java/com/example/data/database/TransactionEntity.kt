package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionStatus

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["reference"], unique = false),
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val providerId: String,
    val providerName: String,
    val phoneNumber: String,
    val amount: Long,
    val currency: String,
    val createdAt: Long,
    val updatedAt: Long,
    val status: String,
    val reference: String?,
    val failureReason: String?,
    val ussdCode: String,
    val simSlotIndex: Int?,
    val simCarrierName: String?,
    val rawSmsMessage: String?
) {
    fun toDomain(): Transaction {
        val parsedStatus = try {
            TransactionStatus.valueOf(status)
        } catch (_: Exception) {
            TransactionStatus.UNKNOWN
        }
        return Transaction(
            id = id,
            providerId = providerId,
            providerName = providerName,
            phoneNumber = phoneNumber,
            amount = amount,
            currency = currency,
            createdAt = createdAt,
            updatedAt = updatedAt,
            status = parsedStatus,
            reference = reference,
            failureReason = failureReason,
            ussdCode = ussdCode,
            simSlotIndex = simSlotIndex,
            simCarrierName = simCarrierName,
            rawSmsMessage = rawSmsMessage
        )
    }

    companion object {
        fun fromDomain(transaction: Transaction): TransactionEntity {
            return TransactionEntity(
                id = transaction.id,
                providerId = transaction.providerId,
                providerName = transaction.providerName,
                phoneNumber = transaction.phoneNumber,
                amount = transaction.amount,
                currency = transaction.currency,
                createdAt = transaction.createdAt,
                updatedAt = transaction.updatedAt,
                status = transaction.status.name,
                reference = transaction.reference,
                failureReason = transaction.failureReason,
                ussdCode = transaction.ussdCode,
                simSlotIndex = transaction.simSlotIndex,
                simCarrierName = transaction.simCarrierName,
                rawSmsMessage = transaction.rawSmsMessage
            )
        }
    }
}
