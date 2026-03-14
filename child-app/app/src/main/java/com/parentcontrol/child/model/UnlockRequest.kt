package com.parentcontrol.child.model

data class UnlockRequest(
    val packageName: String,
    val status: String, // "pending", "approved", "denied"
    val approvedUntilMinutes: Int = 0
)
