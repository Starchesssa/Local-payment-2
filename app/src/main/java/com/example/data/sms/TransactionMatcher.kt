package com.example.data.sms

import com.example.data.database.TransactionDao
import com.example.data.database.TransactionEntity
import com.example.domain.model.SmsConfirmation
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionStatus
import java.util.concurrent.TimeUnit

sealed class MatchResult {
    data class Matched(val transaction: Transaction, val isSuccess: Boolean) : MatchResult()
    data class Duplicate(val existingTransaction: Transaction, val reference: String) : MatchResult()
    data class Mismatched(val reason: String) : MatchResult()
    object NoCandidateFound : MatchResult()
}

class TransactionMatcher(
    private val transactionDao: TransactionDao,
    private val timeToleranceMinutes: Long = 15L
) {

    suspend fun matchAndApply(confirmation: SmsConfirmation): MatchResult {
        val ref = confirmation.reference

        // 1. Duplicate SMS Prevention
        if (!ref.isNullOrBlank()) {
            val duplicate = transactionDao.findSuccessfulByReference(ref)
            if (duplicate != null) {
                return MatchResult.Duplicate(duplicate.toDomain(), ref)
            }
        }

        // 2. Query candidates within time window
        val now = confirmation.timestamp
        val minTime = now - TimeUnit.MINUTES.toMillis(timeToleranceMinutes)
        val maxTime = now + TimeUnit.MINUTES.toMillis(5) // Allow small clock skew

        val candidates = if (confirmation.amount != null) {
            transactionDao.findMatchableTransactions(
                providerId = confirmation.providerId,
                amount = confirmation.amount,
                minTimestamp = minTime,
                maxTimestamp = maxTime
            )
        } else {
            transactionDao.getActiveTransactions()
                .filter { it.providerId == confirmation.providerId }
        }

        if (candidates.isEmpty()) {
            return MatchResult.NoCandidateFound
        }

        // 3. Find best match based on phone number and reference
        val bestCandidate = findBestCandidate(candidates, confirmation)
            ?: return MatchResult.Mismatched("Unable to confidently correlate SMS with a single pending transaction.")

        // 4. Verify state machine transition
        val currentStatus = try {
            TransactionStatus.valueOf(bestCandidate.status)
        } catch (_: Exception) {
            TransactionStatus.UNKNOWN
        }

        val targetStatus = when {
            confirmation.isDeclined -> TransactionStatus.DECLINED
            confirmation.isSuccess -> TransactionStatus.SUCCESS
            else -> TransactionStatus.UNKNOWN
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            return MatchResult.Mismatched(
                "Illegal state transition from $currentStatus to $targetStatus for transaction ${bestCandidate.id}"
            )
        }

        // 5. Update transaction in database
        val updatedEntity = bestCandidate.copy(
            status = targetStatus.name,
            reference = confirmation.reference ?: bestCandidate.reference,
            failureReason = confirmation.failureReason ?: bestCandidate.failureReason,
            updatedAt = System.currentTimeMillis(),
            rawSmsMessage = confirmation.messageText
        )
        transactionDao.updateTransaction(updatedEntity)

        return MatchResult.Matched(updatedEntity.toDomain(), targetStatus == TransactionStatus.SUCCESS)
    }

    private fun findBestCandidate(
        candidates: List<TransactionEntity>,
        confirmation: SmsConfirmation
    ): TransactionEntity? {
        val smsPhoneDigits = confirmation.phoneNumber?.filter { it.isDigit() }?.takeLast(9)

        // Exact match by phone and amount
        if (smsPhoneDigits != null) {
            val phoneMatch = candidates.firstOrNull { candidate ->
                val candidateDigits = candidate.phoneNumber.filter { it.isDigit() }.takeLast(9)
                candidateDigits == smsPhoneDigits
            }
            if (phoneMatch != null) return phoneMatch
        }

        // If phone wasn't in SMS, but exactly one candidate matches provider and amount within time window
        if (candidates.size == 1 && confirmation.amount != null && candidates[0].amount == confirmation.amount) {
            return candidates[0]
        }

        return null
    }
}
