package com.example.domain.model

interface PaymentProvider {
    val id: String
    val displayName: String
    val brandColorHex: Long
    val shortCode: String
    val currency: String
    val phonePrefixes: List<String>
    val description: String

    fun buildUssdRequest(
        phoneNumber: String,
        amount: Long
    ): String

    fun sanitizePhoneNumber(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.startsWith("255") && digits.length == 12 -> "0" + digits.substring(3)
            digits.startsWith("0") && digits.length == 10 -> digits
            digits.length == 9 -> "0$digits"
            else -> digits
        }
    }

    fun isPhonePrefixValid(phoneNumber: String): Boolean {
        val sanitized = sanitizePhoneNumber(phoneNumber)
        if (sanitized.length != 10) return false
        return phonePrefixes.any { sanitized.startsWith(it) }
    }
}
