package com.parentcontrol.child.firebase

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.parentcontrol.child.repository.RulesRepository
import com.parentcontrol.child.repository.ScheduleRule

class FirebaseSync(private val context: Context) {

    private val db = Firebase.firestore
    private val prefs = context.getSharedPreferences("pairing_prefs", Context.MODE_PRIVATE)

    fun isPaired(): Boolean = prefs.getBoolean("isPaired", false)

    fun startListening() {
        val parentUid = prefs.getString("parentUid", null) ?: return
        val childId = prefs.getString("childId", null) ?: return
        val repo = RulesRepository(context)

        db.document("families/$parentUid/children/$childId/rules/current")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                // Blocked apps
                @Suppress("UNCHECKED_CAST")
                val blockedApps = snapshot.get("blockedApps") as? List<String> ?: emptyList()
                repo.setBlockedApps(blockedApps.toSet())

                // Time limits
                @Suppress("UNCHECKED_CAST")
                val timeLimitsRaw = snapshot.get("timeLimits") as? Map<String, Any> ?: emptyMap()
                val timeLimits = timeLimitsRaw.mapValues { (_, v) ->
                    when (v) {
                        is Long -> v.toInt()
                        is Int -> v
                        else -> 0
                    }
                }
                repo.setTimeLimits(timeLimits)

                // Schedule
                @Suppress("UNCHECKED_CAST")
                val scheduleRaw = snapshot.get("schedule") as? Map<String, Any>
                if (scheduleRaw != null) {
                    val rule = ScheduleRule(
                        enabled = scheduleRaw["enabled"] as? Boolean ?: false,
                        startBlock = scheduleRaw["startBlock"] as? String ?: "21:00",
                        endBlock = scheduleRaw["endBlock"] as? String ?: "07:00"
                    )
                    repo.setSchedule(rule)
                }

                updateLastSeen(parentUid, childId)
            }
    }

    fun logBlockedAttempt(packageName: String) {
        val parentUid = prefs.getString("parentUid", null) ?: return
        val childId = prefs.getString("childId", null) ?: return
        db.collection("families/$parentUid/children/$childId/blockedAttempts")
            .add(mapOf("packageName" to packageName, "timestamp" to Timestamp.now()))
    }

    private fun updateLastSeen(parentUid: String, childId: String) {
        db.document("families/$parentUid/children/$childId")
            .update("deviceInfo.lastSeen", Timestamp.now())
    }
}
