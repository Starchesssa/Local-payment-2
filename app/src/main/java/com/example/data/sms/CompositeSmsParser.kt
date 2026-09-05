package com.example.data.sms

import com.example.domain.model.SmsConfirmation

class CompositeSmsParser(
    private val parsers: List<SmsParser> = listOf(
        MpesaSmsParser(),
        AirtelMoneySmsParser(),
        YasSmsParser(),
        HaloPesaSmsParser()
    )
) {
    fun parse(sender: String, body: String): SmsConfirmation? {
        for (parser in parsers) {
            if (parser.canParse(sender, body)) {
                val parsed = parser.parse(sender, body)
                if (parsed != null) {
                    return parsed
                }
            }
        }
        return null
    }
}
