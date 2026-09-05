package com.example.data.providers

import com.example.domain.model.PaymentProvider

class HaloPesaProvider(
    override val id: String = "halopesa",
    override val displayName: String = "HaloPesa (Halotel)",
    override val brandColorHex: Long = 0xFFFF7A00, // Halotel Orange
    override val shortCode: String = "*150*88#",
    override val currency: String = "TZS",
    override val phonePrefixes: List<String> = listOf("061", "062"),
    override val description: String = "Halotel Tanzania HaloPesa",
    val deepUssdPattern: String = "*150*88*1*{phone}*{amount}#"
) : PaymentProvider {

    override fun buildUssdRequest(phoneNumber: String, amount: Long): String {
        val sanitized = sanitizePhoneNumber(phoneNumber)
        return deepUssdPattern
            .replace("{phone}", sanitized)
            .replace("{amount}", amount.toString())
    }

    fun buildRootUssdRequest(): String = shortCode
}
