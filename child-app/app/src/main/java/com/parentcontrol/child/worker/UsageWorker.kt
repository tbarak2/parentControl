package com.parentcontrol.child.worker

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.parentcontrol.child.repository.RulesRepository
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class UsageWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("pairing_prefs", Context.MODE_PRIVATE)
        val parentUid = prefs.getString("parentUid", null) ?: return Result.success()
        val childId = prefs.getString("childId", null) ?: return Result.success()

        val appUsage = readTodayUsage()
        if (appUsage.isEmpty()) return Result.success()

        uploadUsage(parentUid, childId, appUsage)
        enforceLimits(appUsage)
        return Result.success()
    }

    private fun readTodayUsage(): Map<String, Long> {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            cal.timeInMillis,
            System.currentTimeMillis()
        )
        return stats
            .filter { it.totalTimeInForeground > 60_000 } // at least 1 minute
            .associate { it.packageName to it.totalTimeInForeground / 60_000 } // convert to minutes
    }

    private suspend fun uploadUsage(parentUid: String, childId: String, appUsage: Map<String, Long>) {
        val today = todayString()
        try {
            Firebase.firestore
                .document("families/$parentUid/children/$childId/usageStats/$today")
                .set(mapOf(
                    "apps" to appUsage,
                    "lastUpdated" to Timestamp.now()
                ))
                .await()
        } catch (_: Exception) {
            // Ignore upload errors — will retry next run
        }
    }

    private fun enforceLimits(appUsage: Map<String, Long>) {
        val repo = RulesRepository(context)
        val limits = repo.getTimeLimits()
        if (limits.isEmpty()) return

        val nowLimitBlocked = repo.getLimitBlockedApps().toMutableSet()
        limits.forEach { (pkg, limitMinutes) ->
            val used = appUsage[pkg] ?: 0L
            if (used >= limitMinutes) {
                nowLimitBlocked.add(pkg)
            }
        }
        repo.setLimitBlockedApps(nowLimitBlocked)
    }

    private fun todayString(): String {
        val cal = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}
