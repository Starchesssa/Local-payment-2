package com.example.data.sms

import com.example.domain.model.SmsConfirmation

class YasSmsParser : SmsParser {
    override val providerId: String = "yas_tigo"

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()
        return s.contains("TIGO") || s.contains("YAS") ||
                (b.contains("TIGO") && (b.contains("KUMBUKUMBU") || b.contains("UMETUMA"))) ||
                (b.contains("YAS") && b.contains("PESA"))
    }

    override fun parse(sender: String, body: String): SmsConfirmation? {
        if (!canParse(sender, body)) return null

        val isDeclined = body.contains("umeshindikana", ignoreCase = true) ||
                body.contains("failed", ignoreCase = true) ||
                body.contains("halitoshi", ignoreCase = true) ||
                body.contains("insufficient", ignoreCase = true)

        val isSuccess = !isDeclined && (
                body.contains("Umetuma", ignoreCase = true) ||
                body.contains("transferred", ignoreCase = true) ||
                body.contains("umefanikiwa", ignoreCase = true)
        )

        val refRegex = Regex("""(?:Kumbukumbu\s*No|Ref\s*No|Kumbukumbu)[:\s]+([A-Z0-9]{6,14})""", RegexOption.IGNORE_CASE)
        val reference = refRegex.find(body)?.groupValues?.get(1)

        val amount = extractAmount(body)
        val phone = extractPhoneNumber(body)

        val balanceRegex = Regex("""(?i)(?:salio.*?jipya\s*ni|new\s*balance\s*is)\s*(?:TZS|TSH|SH)?\s*([\d,]+)""")
        val balance = balanceRegex.find(body)?.groupValues?.get(1)?.let { "TZS $it" }

        val failureReason = if (isDeclined) {
            when {
                body.contains("halitoshi", ignoreCase = true) || body.contains("insufficient", ignoreCase = true) ->
                    "Salio la Tigo Pesa halitoshi"
                else -> "Muamala wa Tigo Pesa umeshindikana"
            }
        } else null

        return SmsConfirmation(
            providerId = providerId,
            reference = reference,
            amount = amount,
            phoneNumber = phone,
            isSuccess = isSuccess,
            isDeclined = isDeclined,
            failureReason = failureReason,
            availableBalance = balance,
            messageText = body,
            senderAddress = sender
        )
    }
}
