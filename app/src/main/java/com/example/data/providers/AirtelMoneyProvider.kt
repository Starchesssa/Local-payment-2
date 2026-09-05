package com.example.data.providers

import com.example.domain.model.PaymentProvider

class AirtelMoneyProvider(
    override val id: String = "airtel_money",
    override val displayName: String = "Airtel Money",
    override val brandColorHex: Long = 0xFFFF0000, // Airtel Red
    override val shortCode: String = "*150*60#",
    override val currency: String = "TZS",
    override val phonePrefixes: List<String> = listOf("068", "069", "078"),
    override val description: String = "Airtel Tanzania Mobile Money",
    val deepUssdPattern: String = "*150*60*1*{phone}*{amount}#"
) : PaymentProvider {

    override fun buildUssdRequest(phoneNumber: String, amount: Long): String {
        val sanitized = sanitizePhoneNumber(phoneNumber)
        return deepUssdPattern
            .replace("{phone}", sanitized)
            .replace("{amount}", amount.toString())
    }

    fun buildRootUssdRequest(): String = shortCode
}
