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

        // Listen to rules
        db.document("families/$parentUid/children/$childId/rules/current")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                @Suppress("UNCHECKED_CAST")
                repo.setBlockedApps((snapshot.get("blockedApps") as? List<String> ?: emptyList()).toSet())

                @Suppress("UNCHECKED_CAST")
                val timeLimitsRaw = snapshot.get("timeLimits") as? Map<String, Any> ?: emptyMap()
                repo.setTimeLimits(timeLimitsRaw.mapValues { (_, v) -> when (v) { is Long -> v.toInt(); is Int -> v; else -> 0 } })

                @Suppress("UNCHECKED_CAST")
                val scheduleRaw = snapshot.get("schedule") as? Map<String, Any>
                if (scheduleRaw != null) {
                    repo.setSchedule(ScheduleRule(
                        enabled = scheduleRaw["enabled"] as? Boolean ?: false,
                        startBlock = scheduleRaw["startBlock"] as? String ?: "21:00",
                        endBlock = scheduleRaw["endBlock"] as? String ?: "07:00"
                    ))
                }

                @Suppress("UNCHECKED_CAST")
                val contacts = (snapshot.get("emergencyContacts") as? List<String> ?: emptyList()).toSet()
                repo.setEmergencyContacts(contacts)

                updateLastSeen(parentUid, childId)
            }

        // Listen to unlock requests approved by parent
        db.collection("families/$parentUid/children/$childId/unlockRequests")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                snapshot.documentChanges.forEach { change ->
                    val doc = change.document
                    val status = doc.getString("status") ?: return@forEach
                    val packageName = doc.id
                    if (status == "approved") {
                        val minutes = (doc.getLong("approvedUntilMinutes") ?: 30L).toInt()
                        repo.setTemporaryUnlock(packageName, minutes)
                    } else if (status == "denied") {
                        repo.clearTemporaryUnlock(packageName)
                    }
                }
            }
    }

    fun uploadInstalledApps(context: Context) {
        val parentUid = prefs.getString("parentUid", null) ?: return
        val childId = prefs.getString("childId", null) ?: return
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { mapOf("packageName" to it.packageName, "appName" to pm.getApplicationLabel(it).toString()) }
        db.document("families/$parentUid/children/$childId/installedApps/list")
            .set(mapOf("apps" to apps))
    }

    fun sendUnlockRequest(packageName: String) {
        val parentUid = prefs.getString("parentUid", null) ?: return
        val childId = prefs.getString("childId", null) ?: return
        db.document("families/$parentUid/children/$childId/unlockRequests/$packageName")
            .set(mapOf(
                "status" to "pending",
                "packageName" to packageName,
                "requestedAt" to Timestamp.now()
            ))
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
