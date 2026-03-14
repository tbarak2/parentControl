package com.parentcontrol.child.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.parentcontrol.child.databinding.ActivityBlockedBinding
import com.parentcontrol.child.firebase.FirebaseSync

class BlockedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockedBinding
    private lateinit var firebaseSync: FirebaseSync

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseSync = FirebaseSync(applicationContext)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "this app"

        binding.tvAppName.text = packageName

        binding.btnGoHome.setOnClickListener { goHome() }

        if (firebaseSync.isPaired()) {
            binding.btnRequestAccess.visibility = View.VISIBLE
            binding.btnRequestAccess.setOnClickListener {
                firebaseSync.sendUnlockRequest(packageName)
                binding.btnRequestAccess.isEnabled = false
                binding.btnRequestAccess.text = "Request Sent"
                Toast.makeText(this, "Your parent has been notified", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        // Intentionally blocked
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.onKeyDown(keyCode, event)
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }
}
