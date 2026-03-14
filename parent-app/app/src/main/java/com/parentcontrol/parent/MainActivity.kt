package com.parentcontrol.parent

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.parentcontrol.parent.apps.AppListActivity
import com.parentcontrol.parent.auth.LoginActivity
import com.parentcontrol.parent.dashboard.ChildDeviceAdapter
import com.parentcontrol.parent.databinding.ActivityMainBinding
import com.parentcontrol.parent.model.ChildDevice
import com.parentcontrol.parent.pairing.PairingActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ChildDeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "ParentControl"

        adapter = ChildDeviceAdapter { child ->
            startActivity(Intent(this, AppListActivity::class.java).apply {
                putExtra(AppListActivity.EXTRA_CHILD_ID, child.childId)
            })
        }
        binding.rvChildren.layoutManager = LinearLayoutManager(this)
        binding.rvChildren.adapter = adapter

        binding.fabAddChild.setOnClickListener {
            startActivity(Intent(this, PairingActivity::class.java))
        }

        listenForChildren()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_logout) {
            Firebase.auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun listenForChildren() {
        val parentUid = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("families/$parentUid/children")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val children = snapshot.documents.mapNotNull { doc ->
                    val childId = doc.getString("childId") ?: doc.id
                    val deviceInfo = doc.get("deviceInfo") as? Map<*, *>
                    val model = deviceInfo?.get("model") as? String ?: "Unknown device"
                    val lastSeen = deviceInfo?.get("lastSeen")
                    ChildDevice(childId = childId, deviceModel = model)
                }
                adapter.submitList(children)
                binding.tvEmpty.visibility = if (children.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
    }
}
