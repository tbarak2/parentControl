package com.parentcontrol.parent.usage

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.parentcontrol.parent.databinding.ActivityUsageBinding
import com.parentcontrol.parent.model.AppUsage
import java.util.Calendar

class UsageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsageBinding
    private lateinit var adapter: UsageAdapter
    private var childId = ""
    private var parentUid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Today's Usage"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        childId = intent.getStringExtra(EXTRA_CHILD_ID) ?: run { finish(); return }
        parentUid = Firebase.auth.currentUser?.uid ?: run { finish(); return }

        adapter = UsageAdapter()
        binding.rvUsage.layoutManager = LinearLayoutManager(this)
        binding.rvUsage.adapter = adapter

        loadUsage()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun loadUsage() {
        binding.progressBar.visibility = View.VISIBLE
        val today = todayString()
        Firebase.firestore
            .document("families/$parentUid/children/$childId/usageStats/$today")
            .get()
            .addOnSuccessListener { doc ->
                binding.progressBar.visibility = View.GONE
                @Suppress("UNCHECKED_CAST")
                val apps = doc.get("apps") as? Map<String, Any> ?: emptyMap()
                val usageList = apps.entries
                    .map { (pkg, minutes) ->
                        AppUsage(
                            packageName = pkg,
                            appName = pkg.substringAfterLast("."),
                            minutes = when (minutes) {
                                is Long -> minutes.toInt()
                                is Int -> minutes
                                else -> 0
                            }
                        )
                    }
                    .sortedByDescending { it.minutes }

                adapter.submitList(usageList)
                binding.tvEmpty.visibility = if (usageList.isEmpty()) View.VISIBLE else View.GONE
                binding.tvDate.text = "Date: $today"
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.tvEmpty.text = "Failed to load usage data"
                binding.tvEmpty.visibility = View.VISIBLE
            }
    }

    private fun todayString(): String {
        val cal = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    companion object {
        const val EXTRA_CHILD_ID = "extra_child_id"
    }
}
