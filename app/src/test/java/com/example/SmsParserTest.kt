package com.example

import com.example.data.sms.AirtelMoneySmsParser
import com.example.data.sms.CompositeSmsParser
import com.example.data.sms.HaloPesaSmsParser
import com.example.data.sms.MpesaSmsParser
import com.example.data.sms.YasSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    private val compositeParser = CompositeSmsParser()

    @Test
    fun `mpesa successful swahili sms extracts details correctly`() {
        val parser = MpesaSmsParser()
        val sms = "5KA782910J Imethibitishwa. TSh 10,000 imetumwa kwa 0754123456 - JOHN DOE tarehe 05/09/2026. Salio lako la M-Pesa ni TSh 45,000."
        val confirmation = parser.parse("MPESA", sms)

        assertNotNull(confirmation)
        assertEquals("mpesa", confirmation!!.providerId)
        assertEquals("5KA782910J", confirmation.reference)
        assertEquals(10000L, confirmation.amount)
        assertEquals("0754123456", confirmation.phoneNumber)
        assertTrue(confirmation.isSuccess)
        assertFalse(confirmation.isDeclined)
    }

    @Test
    fun `mpesa failed insufficient funds sms extracts failure`() {
        val parser = MpesaSmsParser()
        val sms = "5KA782910F Muamala umeshindikana. Salio lako halitoshi kufanya muamala huu wa TSh 10,000."
        val confirmation = parser.parse("MPESA", sms)

        assertNotNull(confirmation)
        assertFalse(confirmation!!.isSuccess)
        assertTrue(confirmation.isDeclined)
        assertTrue(confirmation.failureReason!!.contains("halitoshi", ignoreCase = true))
    }

    @Test
    fun `airtel money successful sms extracts details`() {
        val parser = AirtelMoneySmsParser()
        val sms = "Txn ID: AM8392019. You have sent TZS 10,000 to 0784123456 on 05/09/2026. Your new balance is TZS 25,000."
        val confirmation = parser.parse("AirtelMoney", sms)

        assertNotNull(confirmation)
        assertEquals("airtel_money", confirmation!!.providerId)
        assertEquals("AM8392019", confirmation.reference)
        assertEquals(10000L, confirmation.amount)
        assertEquals("0784123456", confirmation.phoneNumber)
        assertTrue(confirmation.isSuccess)
    }

    @Test
    fun `yas tigo pesa successful sms extracts details`() {
        val parser = YasSmsParser()
        val sms = "Kumbukumbu No: 82910283. Umetuma Tsh 10,000 kwenda 0713123456 - MARY JANE. Salio lako jipya ni Tsh 30,000."
        val confirmation = parser.parse("TigoPesa", sms)

        assertNotNull(confirmation)
        assertEquals("yas_tigo", confirmation!!.providerId)
        assertEquals("82910283", confirmation.reference)
        assertEquals(10000L, confirmation.amount)
        assertEquals("0713123456", confirmation.phoneNumber)
        assertTrue(confirmation.isSuccess)
    }

    @Test
    fun `halopesa successful sms extracts details`() {
        val parser = HaloPesaSmsParser()
        val sms = "Kumbukumbu: HP928192. Umefanikiwa kutuma Tsh 15,000 kwenda 0622123456 tarehe 05/09/2026. Salio lako ni Tsh 15,000."
        val confirmation = parser.parse("HALOPESA", sms)

        assertNotNull(confirmation)
        assertEquals("halopesa", confirmation!!.providerId)
        assertEquals("HP928192", confirmation.reference)
        assertEquals(15000L, confirmation.amount)
        assertEquals("0622123456", confirmation.phoneNumber)
        assertTrue(confirmation.isSuccess)
    }

    @Test
    fun `composite parser delegates to matching provider`() {
        val sms = "5KA782910J Confirmed. You have sent TZS 20,000 to 0754123456."
        val confirmation = compositeParser.parse("MPESA", sms)

        assertNotNull(confirmation)
        assertEquals("mpesa", confirmation!!.providerId)
        assertEquals(20000L, confirmation.amount)
    }
}
