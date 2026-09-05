package com.example.data.providers

import com.example.domain.model.PaymentProvider

object ProviderRegistry {

    private val providers: MutableMap<String, PaymentProvider> = mutableMapOf(
        "mpesa" to MpesaProvider(),
        "airtel_money" to AirtelMoneyProvider(),
        "yas_tigo" to YasProvider(),
        "halopesa" to HaloPesaProvider()
    )

    fun getAllProviders(): List<PaymentProvider> = providers.values.toList()

    fun getProvider(id: String): PaymentProvider? = providers[id]

    fun getDefaultProvider(): PaymentProvider = providers.values.first()

    /**
     * Allows dynamic updates or custom provider registration in one central location.
     */
    fun registerOrUpdateProvider(provider: PaymentProvider) {
        providers[provider.id] = provider
    }

    /**
     * Determines the suggested provider based on the phone number prefix.
     */
    fun detectProviderForPhone(phoneNumber: String): PaymentProvider? {
        val clean = sanitizePhone(phoneNumber)
        return providers.values.firstOrNull { it.isPhonePrefixValid(clean) }
    }

    fun sanitizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.startsWith("255") && digits.length == 12 -> "0" + digits.substring(3)
            digits.startsWith("0") && digits.length == 10 -> digits
            digits.length == 9 -> "0$digits"
            else -> digits
        }
    }
}
