package com.example

import com.example.data.providers.AirtelMoneyProvider
import com.example.data.providers.HaloPesaProvider
import com.example.data.providers.MpesaProvider
import com.example.data.providers.YasProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UssdCodeGenerationTest {

    @Test
    fun `mpesa provider generates valid deep ussd code`() {
        val mpesa = MpesaProvider()
        val code = mpesa.buildUssdRequest("0754123456", 10000L)
        assertEquals("*150*00*1*1*0754123456*10000#", code)
        assertTrue(code.startsWith("*150*00"))
        assertTrue(code.endsWith("#"))
    }

    @Test
    fun `airtel money provider generates valid deep ussd code`() {
        val airtel = AirtelMoneyProvider()
        val code = airtel.buildUssdRequest("0784123456", 5000L)
        assertEquals("*150*60*1*0784123456*5000#", code)
        assertTrue(code.startsWith("*150*60"))
        assertTrue(code.endsWith("#"))
    }

    @Test
    fun `yas provider generates valid deep ussd code`() {
        val yas = YasProvider()
        val code = yas.buildUssdRequest("0713123456", 25000L)
        assertEquals("*150*01*1*1*0713123456*25000#", code)
        assertTrue(code.startsWith("*150*01"))
        assertTrue(code.endsWith("#"))
    }

    @Test
    fun `halopesa provider generates valid deep ussd code`() {
        val halo = HaloPesaProvider()
        val code = halo.buildUssdRequest("0622123456", 15000L)
        assertEquals("*150*88*1*0622123456*15000#", code)
        assertTrue(code.startsWith("*150*88"))
        assertTrue(code.endsWith("#"))
    }

    @Test
    fun `phone number sanitization strips international prefix`() {
        val mpesa = MpesaProvider()
        val code = mpesa.buildUssdRequest("+255754123456", 1000L)
        assertEquals("*150*00*1*1*0754123456*1000#", code)
    }
}
