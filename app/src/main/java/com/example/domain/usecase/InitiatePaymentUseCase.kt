package com.example.domain.usecase

import com.example.domain.model.PaymentProvider
import com.example.domain.model.SimInfo
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionStatus
import com.example.domain.repository.TransactionRepository
import com.example.telephony.UssdManager
import com.example.telephony.UssdResult
import java.util.UUID

sealed class PaymentExecutionOutcome {
    data class Success(val transaction: Transaction, val ussdResult: UssdResult) : PaymentExecutionOutcome()
    data class Failed(val transaction: Transaction, val reason: String) : PaymentExecutionOutcome()
}

class InitiatePaymentUseCase(
    private val transactionRepository: TransactionRepository,
    private val ussdManager: UssdManager
) {

    suspend operator fun invoke(
        provider: PaymentProvider,
        phoneNumber: String,
        amount: Long,
        selectedSim: SimInfo? = null,
        useRootUssdOnly: Boolean = false
    ): PaymentExecutionOutcome {
        val transactionId = UUID.randomUUID().toString()
        val sanitizedPhone = provider.sanitizePhoneNumber(phoneNumber)

        val ussdCode = if (useRootUssdOnly) {
            provider.shortCode
        } else {
            provider.buildUssdRequest(sanitizedPhone, amount)
        }

        // 1. Every payment MUST create a transaction with PENDING status before USSD begins
        val pendingTransaction = Transaction(
            id = transactionId,
            providerId = provider.id,
            providerName = provider.displayName,
            phoneNumber = sanitizedPhone,
            amount = amount,
            currency = provider.currency,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            status = TransactionStatus.PENDING,
            ussdCode = ussdCode,
            simSlotIndex = selectedSim?.slotIndex,
            simCarrierName = selectedSim?.carrierName
        )

        val createResult = transactionRepository.createTransaction(pendingTransaction)
        if (createResult.isFailure) {
            return PaymentExecutionOutcome.Failed(
                pendingTransaction,
                "Failed to initialize transaction record in local storage."
            )
        }

        // 2. Transition state to PROCESSING
        val processingResult = transactionRepository.updateTransactionStatus(
            id = transactionId,
            newStatus = TransactionStatus.PROCESSING
        )
        val currentTransaction = processingResult.getOrDefault(pendingTransaction)

        // 3. Initiate USSD via Telephony Manager
        val ussdResult = ussdManager.executeUssd(
            ussdCode = ussdCode,
            selectedSim = selectedSim,
            forceCarrierDialog = false
        )

        return when (ussdResult) {
            is UssdResult.ProgrammaticSuccess,
            is UssdResult.CarrierDialogLaunched,
            is UssdResult.DialerLaunched -> {
                PaymentExecutionOutcome.Success(currentTransaction, ussdResult)
            }
            is UssdResult.Failed -> {
                transactionRepository.updateTransactionStatus(
                    id = transactionId,
                    newStatus = TransactionStatus.FAILED,
                    failureReason = ussdResult.error.message
                )
                PaymentExecutionOutcome.Failed(currentTransaction, ussdResult.error.message)
            }
        }
    }
}
