package com.example.domain.usecase

import com.example.domain.model.PaymentProvider

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(
        val providerError: String? = null,
        val phoneError: String? = null,
        val amountError: String? = null
    ) : ValidationResult() {
        val hasErrors: Boolean
            get() = providerError != null || phoneError != null || amountError != null
    }
}

class ValidatePaymentUseCase {

    operator fun invoke(
        provider: PaymentProvider?,
        rawPhoneNumber: String,
        amountText: String
    ): ValidationResult {
        var providerError: String? = null
        var phoneError: String? = null
        var amountError: String? = null

        // 1. Provider validation
        if (provider == null) {
            providerError = "Please select a mobile-money provider."
        }

        // 2. Phone validation
        val digits = rawPhoneNumber.filter { it.isDigit() }
        val cleanPhone = when {
            digits.startsWith("255") && digits.length == 12 -> "0" + digits.substring(3)
            digits.startsWith("0") && digits.length == 10 -> digits
            digits.length == 9 -> "0$digits"
            else -> digits
        }

        if (cleanPhone.isBlank()) {
            phoneError = "Phone number is required."
        } else if (cleanPhone.length != 10 || !cleanPhone.startsWith("0")) {
            phoneError = "Enter a valid 10-digit mobile number (e.g. 0754123456)."
        } else if (provider != null && !provider.isPhonePrefixValid(cleanPhone)) {
            // Informative prefix check
            phoneError = "Number does not match ${provider.displayName} prefixes (${provider.phonePrefixes.joinToString()})."
        }

        // 3. Amount validation
        val amount = amountText.replace(",", "").trim().toLongOrNull()
        if (amount == null) {
            amountError = "Please enter a valid payment amount."
        } else if (amount <= 0) {
            amountError = "Amount must be greater than zero."
        } else if (amount < 100) {
            amountError = "Minimum transaction amount is 100 TZS."
        } else if (amount > 5_000_000) {
            amountError = "Maximum transaction limit is 5,000,000 TZS per payment."
        }

        return if (providerError == null && phoneError == null && amountError == null) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(providerError, phoneError, amountError)
        }
    }
}
