package com.example.data.sms

import com.example.domain.model.SmsConfirmation

interface SmsParser {
    val providerId: String

    fun canParse(sender: String, body: String): Boolean

    fun parse(sender: String, body: String): SmsConfirmation?

    /**
     * Helper to extract standardized amount from strings like "TZS 10,000" or "Tsh. 10,000" or "10000"
     */
    fun extractAmount(text: String): Long? {
        val regex = Regex("""(?i)(?:TZS|TSH|TSHS|SH)\s*[:.]?\s*([\d,]+)""")
        val match = regex.find(text)
        if (match != null) {
            val numStr = match.groupValues[1].replace(",", "").trim()
            return numStr.toLongOrNull()
        }
        return null
    }

    /**
     * Helper to extract phone number from text
     */
    fun extractPhoneNumber(text: String): String? {
        // Look for 07XXXXXXXX, 06XXXXXXXX or +255XXXXXXXXX
        val regex = Regex("""(?:\+?255|0)([67]\d{8})""")
        val match = regex.find(text)
        return match?.let { "0" + it.groupValues[1] }
    }
}
