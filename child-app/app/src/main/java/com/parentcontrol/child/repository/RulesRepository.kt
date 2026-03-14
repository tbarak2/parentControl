package com.parentcontrol.child.repository

import android.content.Context
import android.content.SharedPreferences

class RulesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Core block check ---

    fun isBlocked(packageName: String): Boolean {
        if (packageName == PARENT_CONTROL_PACKAGE) return false
        if (isEmergencyApp(packageName)) return false
        if (isTemporarilyUnlocked(packageName)) return false
        return getBlockedApps().contains(packageName) ||
               getLimitBlockedApps().contains(packageName) ||
               isScheduleBlocking()
    }

    // --- Permanently blocked apps ---

    fun getBlockedApps(): Set<String> =
        prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()

    fun setBlockedApps(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_BLOCKED_APPS, packages).apply()
    }

    // --- Emergency contacts / apps ---

    fun getEmergencyContacts(): Set<String> =
        prefs.getStringSet(KEY_EMERGENCY_CONTACTS, emptySet()) ?: emptySet()

    fun setEmergencyContacts(contacts: Set<String>) {
        prefs.edit().putStringSet(KEY_EMERGENCY_CONTACTS, contacts).apply()
    }

    private fun isEmergencyApp(packageName: String): Boolean {
        return packageName in DIALER_PACKAGES
    }

    // --- Time limits ---

    fun getTimeLimits(): Map<String, Int> {
        val raw = prefs.getString(KEY_TIME_LIMITS, null) ?: return emptyMap()
        return raw.split(";").mapNotNull {
            val parts = it.split("=")
            if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: return@mapNotNull null)
            else null
        }.toMap()
    }

    fun setTimeLimits(limits: Map<String, Int>) {
        prefs.edit().putString(KEY_TIME_LIMITS, limits.entries.joinToString(";") { "${it.key}=${it.value}" }).apply()
    }

    // --- Limit-blocked (auto-clears at midnight) ---

    fun getLimitBlockedApps(): Set<String> {
        if (prefs.getString(KEY_LIMIT_BLOCKED_DATE, null) != todayString()) {
            prefs.edit().remove(KEY_LIMIT_BLOCKED_APPS).putString(KEY_LIMIT_BLOCKED_DATE, todayString()).apply()
            return emptySet()
        }
        return prefs.getStringSet(KEY_LIMIT_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    fun setLimitBlockedApps(packages: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_LIMIT_BLOCKED_APPS, packages)
            .putString(KEY_LIMIT_BLOCKED_DATE, todayString())
            .apply()
    }

    // --- Schedule ---

    fun getSchedule(): ScheduleRule? {
        val enabled = prefs.getBoolean(KEY_SCHEDULE_ENABLED, false)
        val start = prefs.getString(KEY_SCHEDULE_START, null) ?: return null
        val end = prefs.getString(KEY_SCHEDULE_END, null) ?: return null
        return ScheduleRule(enabled, start, end)
    }

    fun setSchedule(rule: ScheduleRule?) {
        prefs.edit().apply {
            putBoolean(KEY_SCHEDULE_ENABLED, rule?.enabled ?: false)
            if (rule != null) {
                putString(KEY_SCHEDULE_START, rule.startBlock)
                putString(KEY_SCHEDULE_END, rule.endBlock)
            }
        }.apply()
    }

    fun isScheduleBlocking(): Boolean = prefs.getBoolean(KEY_SCHEDULE_ACTIVE, false)

    fun setScheduleBlocking(blocking: Boolean) {
        prefs.edit().putBoolean(KEY_SCHEDULE_ACTIVE, blocking).apply()
    }

    // --- Temporary unlocks (approved by parent) ---

    fun isTemporarilyUnlocked(packageName: String): Boolean {
        val expiryKey = KEY_TEMP_UNLOCK_PREFIX + packageName
        val expiresAt = prefs.getLong(expiryKey, 0L)
        if (expiresAt == 0L) return false
        return if (System.currentTimeMillis() < expiresAt) {
            true
        } else {
            prefs.edit().remove(expiryKey).apply()
            false
        }
    }

    fun setTemporaryUnlock(packageName: String, durationMinutes: Int) {
        val expiresAt = System.currentTimeMillis() + durationMinutes * 60_000L
        prefs.edit().putLong(KEY_TEMP_UNLOCK_PREFIX + packageName, expiresAt).apply()
    }

    fun clearTemporaryUnlock(packageName: String) {
        prefs.edit().remove(KEY_TEMP_UNLOCK_PREFIX + packageName).apply()
    }

    private fun todayString(): String {
        val cal = java.util.Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    companion object {
        private const val PREFS_NAME = "parent_control_rules"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_EMERGENCY_CONTACTS = "emergency_contacts"
        private const val KEY_TIME_LIMITS = "time_limits"
        private const val KEY_LIMIT_BLOCKED_APPS = "limit_blocked_apps"
        private const val KEY_LIMIT_BLOCKED_DATE = "limit_blocked_date"
        private const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
        private const val KEY_SCHEDULE_START = "schedule_start"
        private const val KEY_SCHEDULE_END = "schedule_end"
        private const val KEY_SCHEDULE_ACTIVE = "schedule_active"
        private const val KEY_TEMP_UNLOCK_PREFIX = "temp_unlock_"
        const val PARENT_CONTROL_PACKAGE = "com.parentcontrol.child"
        val DIALER_PACKAGES = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.miui.securitycenter"
        )
    }
}
