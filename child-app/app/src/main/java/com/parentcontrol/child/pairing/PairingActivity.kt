package com.parentcontrol.child.pairing

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.parentcontrol.child.SetupActivity
import com.parentcontrol.child.databinding.ActivityPairingBinding
import com.parentcontrol.child.model.DeviceInfo

class PairingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPairingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPairingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPair.setOnClickListener {
            val code = binding.etPairingCode.text.toString().trim()
            if (code.length != 6) {
                binding.etPairingCode.error = "Enter the 6-digit code"
                return@setOnClickListener
            }
            submitPairingCode(code)
        }
    }

    private fun submitPairingCode(code: String) {
        setLoading(true)
        // Step 1: sign in anonymously to get a Firebase UID for this child device
        Firebase.auth.signInAnonymously()
            .addOnSuccessListener { authResult ->
                val childId = authResult.user?.uid ?: run {
                    showError("Authentication failed")
                    return@addOnSuccessListener
                }
                validateCode(code, childId)
            }
            .addOnFailureListener { e ->
                showError("Auth error: ${e.message}")
            }
    }

    private fun validateCode(code: String, childId: String) {
        val db = Firebase.firestore
        db.collection("pairingCodes").document(code).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    showError("Invalid pairing code")
                    return@addOnSuccessListener
                }
                val used = doc.getBoolean("used") ?: false
                if (used) {
                    showError("This code has already been used")
                    return@addOnSuccessListener
                }
                val expiresAt = doc.getTimestamp("expiresAt")
                if (expiresAt != null && expiresAt.toDate().before(java.util.Date())) {
                    showError("Pairing code has expired")
                    return@addOnSuccessListener
                }
                val parentUid = doc.getString("parentUid") ?: run {
                    showError("Invalid pairing code data")
                    return@addOnSuccessListener
                }
                registerChildDevice(code, parentUid, childId)
            }
            .addOnFailureListener { e ->
                showError("Failed to validate code: ${e.message}")
            }
    }

    private fun registerChildDevice(code: String, parentUid: String, childId: String) {
        val db = Firebase.firestore
        val deviceInfo = DeviceInfo.current()
        val batch = db.batch()

        // Write child device info
        val childRef = db.document("families/$parentUid/children/$childId")
        batch.set(childRef, mapOf(
            "deviceInfo" to mapOf(
                "model" to deviceInfo.model,
                "androidVersion" to deviceInfo.androidVersion,
                "lastSeen" to com.google.firebase.Timestamp.now()
            ),
            "childId" to childId
        ))

        // Create initial empty rules
        val rulesRef = db.document("families/$parentUid/children/$childId/rules/current")
        batch.set(rulesRef, mapOf("blockedApps" to emptyList<String>()))

        // Mark pairing code as used
        val codeRef = db.collection("pairingCodes").document(code)
        batch.update(codeRef, "used", true)

        batch.commit()
            .addOnSuccessListener {
                savePairingLocally(parentUid, childId)
                uploadInstalledApps(parentUid, childId)
            }
            .addOnFailureListener { e ->
                showError("Pairing failed: ${e.message}")
            }
    }

    private fun savePairingLocally(parentUid: String, childId: String) {
        getSharedPreferences("pairing_prefs", Context.MODE_PRIVATE).edit()
            .putString("parentUid", parentUid)
            .putString("childId", childId)
            .putBoolean("isPaired", true)
            .apply()
    }

    private fun uploadInstalledApps(parentUid: String, childId: String) {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { mapOf("packageName" to it.packageName, "appName" to pm.getApplicationLabel(it).toString()) }

        Firebase.firestore
            .document("families/$parentUid/children/$childId/installedApps/list")
            .set(mapOf("apps" to apps))
            .addOnCompleteListener {
                // Done — navigate to SetupActivity
                startActivity(Intent(this, SetupActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
    }

    private fun showError(message: String) {
        setLoading(false)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnPair.isEnabled = !loading
        binding.etPairingCode.isEnabled = !loading
    }
}
