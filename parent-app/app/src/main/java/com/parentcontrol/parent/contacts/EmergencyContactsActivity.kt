package com.parentcontrol.parent.contacts

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.parentcontrol.parent.databinding.ActivityEmergencyContactsBinding

class EmergencyContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyContactsBinding
    private var childId = ""
    private var parentUid = ""
    private val contacts = mutableListOf<String>()
    private lateinit var adapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Emergency Contacts"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        childId = intent.getStringExtra(EXTRA_CHILD_ID) ?: run { finish(); return }
        parentUid = Firebase.auth.currentUser?.uid ?: run { finish(); return }

        adapter = ContactsAdapter { phone -> removeContact(phone) }
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter

        binding.btnAddContact.setOnClickListener {
            val phone = binding.etPhone.text.toString().trim()
            if (phone.isEmpty()) return@setOnClickListener
            addContact(phone)
            binding.etPhone.setText("")
        }

        loadContacts()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun loadContacts() {
        Firebase.firestore
            .document("families/$parentUid/children/$childId/rules/current")
            .get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val loaded = doc.get("emergencyContacts") as? List<String> ?: emptyList()
                contacts.clear()
                contacts.addAll(loaded)
                adapter.submitList(contacts.toList())
            }
    }

    private fun addContact(phone: String) {
        if (contacts.contains(phone)) {
            Toast.makeText(this, "Already added", Toast.LENGTH_SHORT).show()
            return
        }
        contacts.add(phone)
        saveContacts()
    }

    private fun removeContact(phone: String) {
        contacts.remove(phone)
        saveContacts()
    }

    private fun saveContacts() {
        Firebase.firestore
            .document("families/$parentUid/children/$childId/rules/current")
            .update("emergencyContacts", contacts)
            .addOnSuccessListener {
                adapter.submitList(contacts.toList())
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        const val EXTRA_CHILD_ID = "extra_child_id"
    }
}
