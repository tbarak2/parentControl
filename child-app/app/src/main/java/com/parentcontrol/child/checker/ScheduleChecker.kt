package com.parentcontrol.child.checker

import android.content.Context
import com.parentcontrol.child.repository.RulesRepository
import java.util.Calendar

class ScheduleChecker(private val context: Context) {

    private val repo = RulesRepository(context)

    fun check() {
        val rule = repo.getSchedule()
        if (rule == null || !rule.enabled) {
            repo.setScheduleBlocking(false)
            return
        }
        repo.setScheduleBlocking(isCurrentlyBlocked(rule.startBlock, rule.endBlock))
    }

    private fun isCurrentlyBlocked(startStr: String, endStr: String): Boolean {
        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = parseTime(startStr)
        val endMinutes = parseTime(endStr)

        return if (startMinutes <= endMinutes) {
            // Same day window e.g. 08:00–17:00
            nowMinutes in startMinutes..endMinutes
        } else {
            // Overnight window e.g. 21:00–07:00
            nowMinutes >= startMinutes || nowMinutes <= endMinutes
        }
    }

    private fun parseTime(time: String): Int {
        val parts = time.split(":")
        return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
    }
}
