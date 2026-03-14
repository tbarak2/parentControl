package com.parentcontrol.parent.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    var isBlocked: Boolean = false,
    val dailyLimitMinutes: Int? = null
)
