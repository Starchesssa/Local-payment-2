package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.TransactionDao
import com.example.data.database.TransactionEntity
import com.example.data.sms.MatchResult
import com.example.data.sms.TransactionMatcher
import com.example.domain.model.SmsConfirmation
import com.example.domain.model.TransactionStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionMatcherTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao
    private lateinit var matcher: TransactionMatcher

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.transactionDao()
        matcher = TransactionMatcher(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `matching sms updates pending transaction to success`() = runBlocking {
        val txId = "tx-123"
        val now = System.currentTimeMillis()
        val entity = TransactionEntity(
            id = txId,
            providerId = "mpesa",
            providerName = "M-Pesa (Vodacom)",
            phoneNumber = "0754123456",
            amount = 10000L,
            currency = "TZS",
            createdAt = now,
            updatedAt = now,
            status = TransactionStatus.PROCESSING.name,
            reference = null,
            failureReason = null,
            ussdCode = "*150*00#",
            simSlotIndex = 0,
            simCarrierName = "Vodacom",
            rawSmsMessage = null
        )
        dao.insertTransaction(entity)

        val sms = SmsConfirmation(
            providerId = "mpesa",
            reference = "REF999888",
            amount = 10000L,
            phoneNumber = "0754123456",
            isSuccess = true,
            isDeclined = false,
            messageText = "5KA782910J Imethibitishwa. TSh 10,000 imetumwa...",
            senderAddress = "MPESA"
        )

        val result = matcher.matchAndApply(sms)
        assertTrue(result is MatchResult.Matched)
        val matched = result as MatchResult.Matched
        assertTrue(matched.isSuccess)
        assertEquals(TransactionStatus.SUCCESS, matched.transaction.status)
        assertEquals("REF999888", matched.transaction.reference)

        // Verify persisted in DB
        val updated = dao.getTransactionByIdOnce(txId)
        assertEquals(TransactionStatus.SUCCESS.name, updated?.status)
        assertEquals("REF999888", updated?.reference)
    }

    @Test
    fun `duplicate sms is detected and rejected from re-applying`() = runBlocking {
        val txId = "tx-original"
        val now = System.currentTimeMillis()
        val entity = TransactionEntity(
            id = txId,
            providerId = "mpesa",
            providerName = "M-Pesa (Vodacom)",
            phoneNumber = "0754123456",
            amount = 10000L,
            currency = "TZS",
            createdAt = now,
            updatedAt = now,
            status = TransactionStatus.SUCCESS.name,
            reference = "DUP12345",
            failureReason = null,
            ussdCode = "*150*00#",
            simSlotIndex = 0,
            simCarrierName = "Vodacom",
            rawSmsMessage = null
        )
        dao.insertTransaction(entity)

        val duplicateSms = SmsConfirmation(
            providerId = "mpesa",
            reference = "DUP12345",
            amount = 10000L,
            phoneNumber = "0754123456",
            isSuccess = true,
            isDeclined = false,
            messageText = "Duplicate message",
            senderAddress = "MPESA"
        )

        val result = matcher.matchAndApply(duplicateSms)
        assertTrue(result is MatchResult.Duplicate)
    }

    @Test
    fun `declined sms marks transaction as DECLINED`() = runBlocking {
        val txId = "tx-declined"
        val now = System.currentTimeMillis()
        val entity = TransactionEntity(
            id = txId,
            providerId = "mpesa",
            providerName = "M-Pesa (Vodacom)",
            phoneNumber = "0754123456",
            amount = 50000L,
            currency = "TZS",
            createdAt = now,
            updatedAt = now,
            status = TransactionStatus.PROCESSING.name,
            reference = null,
            failureReason = null,
            ussdCode = "*150*00#",
            simSlotIndex = 0,
            simCarrierName = "Vodacom",
            rawSmsMessage = null
        )
        dao.insertTransaction(entity)

        val declinedSms = SmsConfirmation(
            providerId = "mpesa",
            reference = null,
            amount = 50000L,
            phoneNumber = "0754123456",
            isSuccess = false,
            isDeclined = true,
            failureReason = "Salio halitoshi",
            messageText = "Muamala umeshindikana. Salio halitoshi.",
            senderAddress = "MPESA"
        )

        val result = matcher.matchAndApply(declinedSms)
        assertTrue(result is MatchResult.Matched)
        val matched = result as MatchResult.Matched
        assertEquals(TransactionStatus.DECLINED, matched.transaction.status)
    }
}
