package com.parentcontrol.child

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.parentcontrol.child.admin.DeviceAdminReceiver
import com.parentcontrol.child.databinding.ActivitySetupBinding
import com.parentcontrol.child.firebase.FirebaseSync
import com.parentcontrol.child.pairing.PairingActivity
import com.parentcontrol.child.repository.RulesRepository
import com.parentcontrol.child.service.MonitoringService

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var firebaseSync: FirebaseSync

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        devicePolicyManager = getSystemService(DevicePolicyManager::class.java)
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        firebaseSync = FirebaseSync(this)

        // If not paired yet, go to pairing screen
        if (!firebaseSync.isPaired()) {
            startActivity(Intent(this, PairingActivity::class.java))
            finish()
            return
        }

        binding.btnGrantAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.btnGrantUsageAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        binding.btnGrantDeviceAdmin.setOnClickListener {
            startActivity(
                Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        getString(R.string.device_admin_explanation))
                }
            )
        }

        ContextCompat.startForegroundService(this, Intent(this, MonitoringService::class.java))
    }

    override fun onResume() {
        super.onResume()
        if (firebaseSync.isPaired()) {
            updatePermissionStatus()
        }
    }

    private fun updatePermissionStatus() {
        val accessibilityOk = isAccessibilityEnabled()
        val usageOk = isUsageAccessGranted()
        val adminOk = devicePolicyManager.isAdminActive(adminComponent)

        binding.statusAccessibility.text = if (accessibilityOk) "✓ Granted" else "Not granted"
        binding.statusUsageAccess.text = if (usageOk) "✓ Granted" else "Not granted"
        binding.statusDeviceAdmin.text = if (adminOk) "✓ Active" else "Not active"

        binding.btnGrantAccessibility.isEnabled = !accessibilityOk
        binding.btnGrantUsageAccess.isEnabled = !usageOk
        binding.btnGrantDeviceAdmin.isEnabled = !adminOk

        binding.tvOverallStatus.text = if (accessibilityOk && usageOk && adminOk) {
            "All permissions granted. Device is protected."
        } else {
            "Grant all permissions below to activate parental controls."
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(AccessibilityManager::class.java)
        return am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun isUsageAccessGranted(): Boolean {
        val appOps = getSystemService(AppOpsManager::class.java)
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
