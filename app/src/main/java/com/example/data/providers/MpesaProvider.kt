package com.example.data.providers

import com.example.domain.model.PaymentProvider

class MpesaProvider(
    override val id: String = "mpesa",
    override val displayName: String = "M-Pesa (Vodacom)",
    override val brandColorHex: Long = 0xFFE60000, // Vodacom Red
    override val shortCode: String = "*150*00#",
    override val currency: String = "TZS",
    override val phonePrefixes: List<String> = listOf("074", "075", "076", "079"),
    override val description: String = "Vodacom Tanzania Mobile Money",
    val deepUssdPattern: String = "*150*00*1*1*{phone}*{amount}#"
) : PaymentProvider {

    override fun buildUssdRequest(phoneNumber: String, amount: Long): String {
        val sanitized = sanitizePhoneNumber(phoneNumber)
        return deepUssdPattern
            .replace("{phone}", sanitized)
            .replace("{amount}", amount.toString())
    }

    fun buildRootUssdRequest(): String = shortCode
}
