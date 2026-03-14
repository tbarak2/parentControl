package com.parentcontrol.parent.pairing

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.parentcontrol.parent.databinding.ActivityPairingBinding
import java.util.Calendar

class PairingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPairingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPairingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Add Child Device"

        binding.btnGenerate.setOnClickListener { generateCode() }
    }

    private fun generateCode() {
        val parentUid = Firebase.auth.currentUser?.uid ?: return
        val code = (100000..999999).random().toString()
        val expiresAt = Calendar.getInstance().apply { add(Calendar.MINUTE, 15) }.time

        setLoading(true)
        Firebase.firestore.collection("pairingCodes").document(code)
            .set(mapOf(
                "parentUid" to parentUid,
                "expiresAt" to Timestamp(expiresAt),
                "used" to false
            ))
            .addOnSuccessListener {
                setLoading(false)
                binding.tvCode.text = code
                binding.tvCode.visibility = View.VISIBLE
                binding.tvInstructions.visibility = View.VISIBLE
                binding.tvExpiry.text = "This code expires in 15 minutes"
                binding.tvExpiry.visibility = View.VISIBLE
                binding.btnGenerate.text = "Generate New Code"
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnGenerate.isEnabled = !loading
    }
}
