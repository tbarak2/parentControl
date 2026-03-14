package com.parentcontrol.parent.requests

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.parentcontrol.parent.R
import com.parentcontrol.parent.databinding.ActivityUnlockRequestsBinding
import com.parentcontrol.parent.model.UnlockRequest

class UnlockRequestsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUnlockRequestsBinding
    private lateinit var adapter: UnlockRequestAdapter
    private var childId = ""
    private var parentUid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUnlockRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Unlock Requests"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        childId = intent.getStringExtra(EXTRA_CHILD_ID) ?: run { finish(); return }
        parentUid = Firebase.auth.currentUser?.uid ?: run { finish(); return }

        adapter = UnlockRequestAdapter(
            onApprove = { request, minutes -> approveRequest(request, minutes) },
            onDeny = { request -> denyRequest(request) }
        )
        binding.rvRequests.layoutManager = LinearLayoutManager(this)
        binding.rvRequests.adapter = adapter

        listenForRequests()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun listenForRequests() {
        Firebase.firestore
            .collection("families/$parentUid/children/$childId/unlockRequests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val requests = snapshot.documents.mapNotNull { doc ->
                    UnlockRequest(
                        packageName = doc.getString("packageName") ?: doc.id,
                        requestedAt = doc.getTimestamp("requestedAt")?.toDate()?.toString() ?: ""
                    )
                }
                adapter.submitList(requests)
                binding.tvEmpty.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
                if (requests.isNotEmpty()) showNotification(requests.size)
            }
    }

    private fun approveRequest(request: UnlockRequest, minutes: Int) {
        Firebase.firestore
            .document("families/$parentUid/children/$childId/unlockRequests/${request.packageName}")
            .update(mapOf(
                "status" to "approved",
                "approvedUntilMinutes" to minutes,
                "respondedAt" to com.google.firebase.Timestamp.now()
            ))
    }

    private fun denyRequest(request: UnlockRequest) {
        Firebase.firestore
            .document("families/$parentUid/children/$childId/unlockRequests/${request.packageName}")
            .update(mapOf(
                "status" to "denied",
                "respondedAt" to com.google.firebase.Timestamp.now()
            ))
    }

    private fun showNotification(count: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("unlock_requests", "Unlock Requests", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.notify(2001, NotificationCompat.Builder(this, "unlock_requests")
            .setContentTitle("Unlock Request")
            .setContentText("$count app unlock request(s) waiting for your approval")
            .setSmallIcon(R.drawable.ic_shield)
            .setAutoCancel(true)
            .build()
        )
    }

    companion object {
        const val EXTRA_CHILD_ID = "extra_child_id"
    }
}
