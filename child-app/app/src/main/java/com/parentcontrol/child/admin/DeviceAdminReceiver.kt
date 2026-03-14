package com.parentcontrol.child.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        // Device admin activated — app is now protected from uninstallation
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // TODO Phase 2: notify parent via FCM that device admin was removed
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.getString(com.parentcontrol.child.R.string.device_admin_disable_warning)
}
