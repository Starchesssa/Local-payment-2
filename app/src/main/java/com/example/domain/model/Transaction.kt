package com.example.domain.model

data class Transaction(
    val id: String,
    val providerId: String,
    val providerName: String,
    val phoneNumber: String,
    val amount: Long,
    val currency: String = "TZS",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: TransactionStatus = TransactionStatus.PENDING,
    val reference: String? = null,
    val failureReason: String? = null,
    val ussdCode: String = "",
    val simSlotIndex: Int? = null,
    val simCarrierName: String? = null,
    val rawSmsMessage: String? = null
)
