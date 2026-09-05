package com.example.domain.model

enum class TransactionStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    DECLINED,
    FAILED,
    CANCELLED,
    TIMEOUT,
    UNKNOWN;

    /**
     * Enforces a strict state machine transition.
     * Prevents transactions from randomly jumping between unrelated states.
     */
    fun canTransitionTo(next: TransactionStatus): Boolean {
        if (this == next) return true
        return when (this) {
            PENDING -> next in setOf(PROCESSING, CANCELLED, FAILED)
            PROCESSING -> next in setOf(SUCCESS, DECLINED, FAILED, TIMEOUT, CANCELLED, UNKNOWN)
            TIMEOUT -> next in setOf(SUCCESS, DECLINED, FAILED, PROCESSING, UNKNOWN, CANCELLED)
            UNKNOWN -> next in setOf(SUCCESS, DECLINED, FAILED, CANCELLED)
            SUCCESS, DECLINED, FAILED, CANCELLED -> false // Terminal states
        }
    }

    val isTerminal: Boolean
        get() = this in setOf(SUCCESS, DECLINED, FAILED, CANCELLED)
}
