package com.example.data.sms

import com.example.domain.model.SmsConfirmation

class MpesaSmsParser : SmsParser {
    override val providerId: String = "mpesa"

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()
        return s.contains("MPESA") || s.contains("M-PESA") || s.contains("VODACOM") ||
                (b.contains("M-PESA") && (b.contains("IMETHIBITISHWA") || b.contains("CONFIRMED")))
    }

    override fun parse(sender: String, body: String): SmsConfirmation? {
        if (!canParse(sender, body)) return null

        val isDeclined = body.contains("umeshindikana", ignoreCase = true) ||
                body.contains("failed", ignoreCase = true) ||
                body.contains("insufficient", ignoreCase = true) ||
                body.contains("halitoshi", ignoreCase = true) ||
                body.contains("imekataliwa", ignoreCase = true)

        val isSuccess = !isDeclined && (
                body.contains("Imethibitishwa", ignoreCase = true) ||
                body.contains("Confirmed", ignoreCase = true) ||
                body.contains("imetumwa kwa", ignoreCase = true) ||
                body.contains("sent to", ignoreCase = true)
        )

        // Reference: typically first word like "5KA782910J" or after "Ref"
        val refRegex = Regex("""^([A-Z0-9]{8,12})\b""", RegexOption.IGNORE_CASE)
        val altRefRegex = Regex("""(?:Ref|Kumbukumbu|Transaction ID)[:\s]+([A-Z0-9]{6,14})""", RegexOption.IGNORE_CASE)
        val reference = refRegex.find(body.trim())?.groupValues?.get(1)
            ?: altRefRegex.find(body)?.groupValues?.get(1)

        val amount = extractAmount(body)
        val phone = extractPhoneNumber(body)

        // Extract balance if available
        val balanceRegex = Regex("""(?i)(?:salio.*?la.*?ni|balance.*?is)\s*(?:TZS|TSH|SH)?\s*([\d,]+)""")
        val balanceMatch = balanceRegex.find(body)
        val balance = balanceMatch?.groupValues?.get(1)?.let { "TZS $it" }

        val failureReason = if (isDeclined) {
            when {
                body.contains("insufficient", ignoreCase = true) || body.contains("halitoshi", ignoreCase = true) ->
                    "Salio halitoshi / Insufficient funds"
                body.contains("incorrect pin", ignoreCase = true) || body.contains("namba ya siri", ignoreCase = true) ->
                    "PIN si sahihi / Incorrect PIN"
                else -> "Transaction was declined by M-Pesa"
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
