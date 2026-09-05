package com.example.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val parser = CompositeSmsParser()
                val confirmation = parser.parse(sender, fullBody)
                if (confirmation != null) {
                    val db = AppDatabase.getInstance(appContext)
                    val matcher = TransactionMatcher(db.transactionDao())
                    matcher.matchAndApply(confirmation)
                }
            } catch (_: Exception) {
                // Ignore error, never crash system broadcast
            } finally {
                pendingResult.finish()
            }
        }
    }
}
