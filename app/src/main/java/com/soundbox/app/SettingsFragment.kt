package com.soundbox.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SettingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        auth = FirebaseAuth.getInstance()
        val currentUserId = auth.currentUser?.uid

        val switchReadReceipts = view.findViewById<Switch>(R.id.switchReadReceipts)
        val switchTyping = view.findViewById<Switch>(R.id.switchTyping)

        // Reference to user settings in Firebase
        val userSettingsRef = currentUserId?.let {
            FirebaseDatabase.getInstance().getReference("Users").child(it).child("Settings")
        }

        // Save Read Receipts preference
        switchReadReceipts?.setOnCheckedChangeListener { _, isChecked ->
            userSettingsRef?.child("readReceipts")?.setValue(isChecked)
        }

        // Save Typing Indicator preference
        switchTyping?.setOnCheckedChangeListener { _, isChecked ->
            userSettingsRef?.child("typingIndicator")?.setValue(isChecked)
        }

        return view
    }
}
