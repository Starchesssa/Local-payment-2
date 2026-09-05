package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.TransactionRepositoryImpl
import com.example.telephony.UssdManager

class PaymentViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
            val db = AppDatabase.getInstance(context.applicationContext)
            val repository = TransactionRepositoryImpl(db.transactionDao())
            val ussdManager = UssdManager(context.applicationContext)
            return PaymentViewModel(repository, ussdManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
