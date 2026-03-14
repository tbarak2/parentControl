package com.parentcontrol.parent.schedule

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.parentcontrol.parent.databinding.ActivityScheduleBinding

class ScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleBinding
    private var childId = ""
    private var parentUid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Blocked Time Schedule"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        childId = intent.getStringExtra(EXTRA_CHILD_ID) ?: run { finish(); return }
        parentUid = Firebase.auth.currentUser?.uid ?: run { finish(); return }

        binding.btnSave.setOnClickListener { saveSchedule() }
        loadCurrentSchedule()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun loadCurrentSchedule() {
        Firebase.firestore
            .document("families/$parentUid/children/$childId/rules/current")
            .get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val schedule = doc.get("schedule") as? Map<String, Any> ?: return@addOnSuccessListener
                binding.switchEnabled.isChecked = schedule["enabled"] as? Boolean ?: false
                binding.tpStart.apply {
                    val parts = (schedule["startBlock"] as? String ?: "21:00").split(":")
                    hour = parts[0].toIntOrNull() ?: 21
                    minute = parts[1].toIntOrNull() ?: 0
                }
                binding.tpEnd.apply {
                    val parts = (schedule["endBlock"] as? String ?: "07:00").split(":")
                    hour = parts[0].toIntOrNull() ?: 7
                    minute = parts[1].toIntOrNull() ?: 0
                }
            }
    }

    private fun saveSchedule() {
        val startTime = "%02d:%02d".format(binding.tpStart.hour, binding.tpStart.minute)
        val endTime = "%02d:%02d".format(binding.tpEnd.hour, binding.tpEnd.minute)
        val enabled = binding.switchEnabled.isChecked

        val rulesRef = Firebase.firestore
            .document("families/$parentUid/children/$childId/rules/current")

        rulesRef.update(mapOf(
            "schedule" to mapOf(
                "enabled" to enabled,
                "startBlock" to startTime,
                "endBlock" to endTime
            )
        )).addOnSuccessListener {
            Toast.makeText(this, "Schedule saved", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val EXTRA_CHILD_ID = "extra_child_id"
    }
}
