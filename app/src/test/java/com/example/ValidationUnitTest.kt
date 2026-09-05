package com.example

import com.example.data.providers.MpesaProvider
import com.example.domain.usecase.ValidatePaymentUseCase
import com.example.domain.usecase.ValidationResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidationUnitTest {

    private lateinit var validateUseCase: ValidatePaymentUseCase
    private val mpesaProvider = MpesaProvider()

    @Before
    fun setUp() {
        validateUseCase = ValidatePaymentUseCase()
    }

    @Test
    fun `valid payment data passes validation`() {
        val result = validateUseCase(
            provider = mpesaProvider,
            rawPhoneNumber = "0754123456",
            amountText = "10000"
        )
        assertTrue("Valid input should pass", result is ValidationResult.Valid)
    }

    @Test
    fun `null provider fails validation`() {
        val result = validateUseCase(
            provider = null,
            rawPhoneNumber = "0754123456",
            amountText = "10000"
        )
        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertTrue(invalid.providerError != null)
    }

    @Test
    fun `invalid phone number fails validation`() {
        val shortPhoneResult = validateUseCase(
            provider = mpesaProvider,
            rawPhoneNumber = "0754",
            amountText = "10000"
        )
        assertTrue((shortPhoneResult as ValidationResult.Invalid).phoneError != null)

        val invalidPrefixResult = validateUseCase(
            provider = mpesaProvider,
            rawPhoneNumber = "0222123456", // Landline prefix
            amountText = "10000"
        )
        assertTrue((invalidPrefixResult as ValidationResult.Invalid).phoneError != null)
    }

    @Test
    fun `negative or zero amount fails validation`() {
        val zeroResult = validateUseCase(
            provider = mpesaProvider,
            rawPhoneNumber = "0754123456",
            amountText = "0"
        )
        assertTrue((zeroResult as ValidationResult.Invalid).amountError != null)

        val negativeResult = validateUseCase(
            provider = mpesaProvider,
            rawPhoneNumber = "0754123456",
            amountText = "-500"
        )
        assertTrue((negativeResult as ValidationResult.Invalid).amountError != null)
    }

    @Test
    fun `amount with commas parses correctly`() {
        val result = validateUseCase(
            provider = mpesaProvider,
            rawPhoneNumber = "0754123456",
            amountText = "10,000"
        )
        assertTrue(result is ValidationResult.Valid)
    }
}
