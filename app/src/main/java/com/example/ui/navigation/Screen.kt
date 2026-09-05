package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Payment : Screen("payment")
    data class PaymentProgress(val transactionId: String) : Screen("progress/$transactionId") {
        companion object {
            const val ROUTE_PATTERN = "progress/{transactionId}"
        }
    }
    data class Result(val transactionId: String) : Screen("result/$transactionId") {
        companion object {
            const val ROUTE_PATTERN = "result/{transactionId}"
        }
    }
    object History : Screen("history")
    data class TransactionDetails(val transactionId: String) : Screen("details/$transactionId") {
        companion object {
            const val ROUTE_PATTERN = "details/{transactionId}"
        }
    }
}
