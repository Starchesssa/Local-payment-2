package com.example.domain.usecase

import com.example.data.sms.CompositeSmsParser
import com.example.data.sms.MatchResult
import com.example.data.sms.TransactionMatcher

class ProcessManualSmsUseCase(
    private val smsParser: CompositeSmsParser,
    private val transactionMatcher: TransactionMatcher
) {
    suspend operator fun invoke(sender: String, messageBody: String): MatchResult {
        val parsed = smsParser.parse(sender, messageBody)
            ?: return MatchResult.Mismatched("The entered SMS could not be identified as a valid mobile-money confirmation.")
        return transactionMatcher.matchAndApply(parsed)
    }
}
