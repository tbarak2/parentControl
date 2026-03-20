package com.parentcontrol.child.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.parentcontrol.child.R
import com.parentcontrol.child.checker.ScheduleChecker
import com.parentcontrol.child.firebase.FirebaseSync
import com.parentcontrol.child.worker.UsageWorker
import java.util.concurrent.TimeUnit

class MonitoringService : Service() {

    private lateinit var firebaseSync: FirebaseSync
    private lateinit var scheduleChecker: ScheduleChecker
    private val scheduleHandler = Handler(Looper.getMainLooper())
    private val scheduleRunnable = object : Runnable {
        override fun run() {
            scheduleChecker.check()
            scheduleHandler.postDelayed(this, SCHEDULE_CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        firebaseSync = FirebaseSync(applicationContext)
        scheduleChecker = ScheduleChecker(applicationContext)

        if (firebaseSync.isPaired()) {
            firebaseSync.startListening()
            firebaseSync.uploadInstalledApps(applicationContext)
            scheduleHandler.post(scheduleRunnable)
            scheduleUsageWorker()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        scheduleHandler.removeCallbacks(scheduleRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun scheduleUsageWorker() {
        val request = PeriodicWorkRequestBuilder<UsageWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "usage_upload",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Parental Controls",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps parental controls active in the background"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Parental Controls Active")
            .setContentText("This device is protected")
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setSilent(true)
            .build()

    companion object {
        const val CHANNEL_ID = "monitoring_channel"
        const val NOTIFICATION_ID = 1001
        private const val SCHEDULE_CHECK_INTERVAL_MS = 60_000L
    }
}
