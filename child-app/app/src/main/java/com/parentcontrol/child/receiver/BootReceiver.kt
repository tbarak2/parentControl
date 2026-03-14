package com.parentcontrol.child.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.parentcontrol.child.service.MonitoringService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.LOCKED_BOOT_COMPLETED") {
            context.startForegroundService(Intent(context, MonitoringService::class.java))
        }
    }
}
