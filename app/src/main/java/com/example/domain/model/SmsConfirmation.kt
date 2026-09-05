package com.example.domain.model

data class SmsConfirmation(
    val providerId: String,
    val reference: String?,
    val amount: Long?,
    val phoneNumber: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean,
    val isDeclined: Boolean,
    val failureReason: String? = null,
    val availableBalance: String? = null,
    val messageText: String,
    val senderAddress: String = ""
)
