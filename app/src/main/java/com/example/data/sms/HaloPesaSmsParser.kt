package com.example.data.sms

import com.example.domain.model.SmsConfirmation

class HaloPesaSmsParser : SmsParser {
    override val providerId: String = "halopesa"

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()
        return s.contains("HALO") || s.contains("HALOTEL") ||
                (b.contains("HALOPESA") || b.contains("HALOTEL"))
    }

    override fun parse(sender: String, body: String): SmsConfirmation? {
        if (!canParse(sender, body)) return null

        val isDeclined = body.contains("haukufanikiwa", ignoreCase = true) ||
                body.contains("failed", ignoreCase = true) ||
                body.contains("halitoshi", ignoreCase = true) ||
                body.contains("insufficient", ignoreCase = true)

        val isSuccess = !isDeclined && (
                body.contains("Umefanikiwa", ignoreCase = true) ||
                body.contains("Successfully sent", ignoreCase = true) ||
                body.contains("umetuma", ignoreCase = true)
        )

        val refRegex = Regex("""(?:Kumbukumbu|Ref)[:\s]+([A-Z0-9]{6,14})""", RegexOption.IGNORE_CASE)
        val reference = refRegex.find(body)?.groupValues?.get(1)

        val amount = extractAmount(body)
        val phone = extractPhoneNumber(body)

        val balanceRegex = Regex("""(?i)(?:salio\s*lako\s*ni|new\s*balance\s*is)\s*(?:TZS|TSH|SH)?\s*([\d,]+)""")
        val balance = balanceRegex.find(body)?.groupValues?.get(1)?.let { "TZS $it" }

        val failureReason = if (isDeclined) {
            when {
                body.contains("halitoshi", ignoreCase = true) -> "Salio la HaloPesa halitoshi"
                else -> "Muamala wa HaloPesa haukufanikiwa"
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
