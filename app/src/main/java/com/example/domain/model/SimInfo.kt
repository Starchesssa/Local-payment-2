package com.example.domain.model

data class SimInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val isDefault: Boolean = false
)
