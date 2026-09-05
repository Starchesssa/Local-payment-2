package com.example.data.sms

import com.example.domain.model.SmsConfirmation

class AirtelMoneySmsParser : SmsParser {
    override val providerId: String = "airtel_money"

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.uppercase()
        val b = body.uppercase()
        return s.contains("AIRTEL") || (b.contains("AIRTEL") && (b.contains("TXN ID") || b.contains("SENT")))
    }

    override fun parse(sender: String, body: String): SmsConfirmation? {
        if (!canParse(sender, body)) return null

        val isDeclined = body.contains("failed", ignoreCase = true) ||
                body.contains("insufficient", ignoreCase = true) ||
                body.contains("halitoshi", ignoreCase = true) ||
                body.contains("umeshindikana", ignoreCase = true)

        val isSuccess = !isDeclined && (
                body.contains("sent", ignoreCase = true) ||
                body.contains("umetuma", ignoreCase = true) ||
                body.contains("successfully", ignoreCase = true)
        )

        val refRegex = Regex("""(?:Txn\s*ID|Kumbukumbu|Ref)[:\s]+([A-Z0-9]{6,14})""", RegexOption.IGNORE_CASE)
        val reference = refRegex.find(body)?.groupValues?.get(1)

        val amount = extractAmount(body)
        val phone = extractPhoneNumber(body)

        val balanceRegex = Regex("""(?i)(?:balance|salio)\s*(?:is|ni)?\s*(?:TZS|TSH|SH)?\s*([\d,]+)""")
        val balance = balanceRegex.find(body)?.groupValues?.get(1)?.let { "TZS $it" }

        val failureReason = if (isDeclined) {
            when {
                body.contains("insufficient", ignoreCase = true) || body.contains("halitoshi", ignoreCase = true) ->
                    "Salio halitoshi / Insufficient balance"
                else -> "Transaction failed on Airtel Money"
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
