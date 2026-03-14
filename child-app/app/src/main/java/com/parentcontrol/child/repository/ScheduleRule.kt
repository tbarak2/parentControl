package com.parentcontrol.child.repository

data class ScheduleRule(
    val enabled: Boolean,
    val startBlock: String, // "HH:mm"
    val endBlock: String    // "HH:mm"
)
