package com.parentcontrol.child.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.parentcontrol.child.firebase.FirebaseSync
import com.parentcontrol.child.repository.RulesRepository
import com.parentcontrol.child.ui.BlockedActivity

class AppBlockerAccessibilityService : AccessibilityService() {

    private lateinit var rulesRepository: RulesRepository
    private lateinit var firebaseSync: FirebaseSync

    override fun onServiceConnected() {
        super.onServiceConnected()
        rulesRepository = RulesRepository(applicationContext)
        firebaseSync = FirebaseSync(applicationContext)
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

        if (rulesRepository.isBlocked(packageName)) {
            firebaseSync.logBlockedAttempt(packageName)
            launchBlockedScreen(packageName)
        }
    }

    override fun onInterrupt() {}

    private fun launchBlockedScreen(packageName: String) {
        startActivity(
            Intent(this, BlockedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(BlockedActivity.EXTRA_PACKAGE_NAME, packageName)
            }
        )
    }
}
