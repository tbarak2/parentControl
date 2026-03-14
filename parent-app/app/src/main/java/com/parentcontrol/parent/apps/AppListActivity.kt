package com.parentcontrol.parent.apps

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.parentcontrol.parent.contacts.EmergencyContactsActivity
import com.parentcontrol.parent.databinding.ActivityAppListBinding
import com.parentcontrol.parent.model.AppInfo
import com.parentcontrol.parent.requests.UnlockRequestsActivity
import com.parentcontrol.parent.schedule.ScheduleActivity
import com.parentcontrol.parent.usage.UsageActivity

class AppListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppListBinding
    private lateinit var adapter: AppListAdapter
    private var childId = ""
    private var parentUid = ""
    private var currentTimeLimits = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Manage Apps"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        childId = intent.getStringExtra(EXTRA_CHILD_ID) ?: run { finish(); return }
        parentUid = Firebase.auth.currentUser?.uid ?: run { finish(); return }

        adapter = AppListAdapter(
            onToggleBlock = { app, isBlocked -> toggleAppBlock(app.packageName, isBlocked) },
            onSetLimit = { app -> showTimeLimitDialog(app) }
        )
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = adapter

        binding.btnViewUsage.setOnClickListener {
            startActivity(Intent(this, UsageActivity::class.java).putExtra(UsageActivity.EXTRA_CHILD_ID, childId))
        }
        binding.btnSchedule.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java).putExtra(ScheduleActivity.EXTRA_CHILD_ID, childId))
        }
        binding.btnUnlockRequests.setOnClickListener {
            startActivity(Intent(this, UnlockRequestsActivity::class.java).putExtra(UnlockRequestsActivity.EXTRA_CHILD_ID, childId))
        }
        binding.btnEmergencyContacts.setOnClickListener {
            startActivity(Intent(this, EmergencyContactsActivity::class.java).putExtra(EmergencyContactsActivity.EXTRA_CHILD_ID, childId))
        }

        loadApps()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun loadApps() {
        binding.progressBar.visibility = View.VISIBLE
        val db = Firebase.firestore
        val appsRef = db.document("families/$parentUid/children/$childId/installedApps/list")
        val rulesRef = db.document("families/$parentUid/children/$childId/rules/current")

        appsRef.get().addOnSuccessListener { appsDoc ->
            rulesRef.addSnapshotListener { rulesDoc, _ ->
                binding.progressBar.visibility = View.GONE
                @Suppress("UNCHECKED_CAST")
                val installedApps = appsDoc.get("apps") as? List<Map<String, String>> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val blockedApps = (rulesDoc?.get("blockedApps") as? List<String>)?.toSet() ?: emptySet()
                @Suppress("UNCHECKED_CAST")
                val timeLimitsRaw = rulesDoc?.get("timeLimits") as? Map<String, Any> ?: emptyMap()
                currentTimeLimits = timeLimitsRaw.mapValues { (_, v) ->
                    when (v) { is Long -> v.toInt(); is Int -> v; else -> 0 }
                }.toMutableMap()

                adapter.submitList(installedApps
                    .map {
                        val pkg = it["packageName"] ?: ""
                        AppInfo(pkg, it["appName"] ?: pkg, blockedApps.contains(pkg), currentTimeLimits[pkg])
                    }
                    .filter { it.packageName.isNotEmpty() }
                    .sortedBy { it.appName }
                )
            }
        }.addOnFailureListener {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Failed to load apps", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleAppBlock(packageName: String, shouldBlock: Boolean) {
        val rulesRef = Firebase.firestore.document("families/$parentUid/children/$childId/rules/current")
        rulesRef.get().addOnSuccessListener { doc ->
            @Suppress("UNCHECKED_CAST")
            val current = (doc.get("blockedApps") as? List<String>)?.toMutableList() ?: mutableListOf()
            if (shouldBlock) { if (!current.contains(packageName)) current.add(packageName) }
            else current.remove(packageName)
            rulesRef.update("blockedApps", current)
        }
    }

    private fun showTimeLimitDialog(app: AppInfo) {
        val input = EditText(this).apply {
            hint = "Minutes per day (0 = no limit)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            val current = app.dailyLimitMinutes
            if (current != null && current > 0) setText(current.toString())
        }
        AlertDialog.Builder(this)
            .setTitle("Daily Limit — ${app.appName}")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val minutes = input.text.toString().toIntOrNull() ?: 0
                saveTimeLimit(app.packageName, minutes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveTimeLimit(packageName: String, minutes: Int) {
        if (minutes <= 0) currentTimeLimits.remove(packageName) else currentTimeLimits[packageName] = minutes
        Firebase.firestore.document("families/$parentUid/children/$childId/rules/current")
            .update("timeLimits", currentTimeLimits)
            .addOnSuccessListener {
                val msg = if (minutes > 0) "Limit set: $minutes min/day" else "Limit removed"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        const val EXTRA_CHILD_ID = "extra_child_id"
    }
}
