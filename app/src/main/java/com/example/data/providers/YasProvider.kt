package com.example.data.providers

import com.example.domain.model.PaymentProvider

class YasProvider(
    override val id: String = "yas_tigo",
    override val displayName: String = "Yas / Tigo Pesa",
    override val brandColorHex: Long = 0xFF00377B, // Tigo Deep Blue
    override val shortCode: String = "*150*01#",
    override val currency: String = "TZS",
    override val phonePrefixes: List<String> = listOf("065", "067", "071"),
    override val description: String = "Yas / Tigo Pesa Tanzania",
    val deepUssdPattern: String = "*150*01*1*1*{phone}*{amount}#"
) : PaymentProvider {

    override fun buildUssdRequest(phoneNumber: String, amount: Long): String {
        val sanitized = sanitizePhoneNumber(phoneNumber)
        return deepUssdPattern
            .replace("{phone}", sanitized)
            .replace("{amount}", amount.toString())
    }

    fun buildRootUssdRequest(): String = shortCode
}
