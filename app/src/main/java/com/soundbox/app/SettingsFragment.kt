package com.soundbox.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.google.firebase.database.FirebaseDatabase

class SettingsFragment : Fragment() {
    private val databaseRef = FirebaseDatabase.getInstance().reference
    private var isUpdatingSwitch = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val etWebhook = view.findViewById<EditText>(R.id.etWebhookUrl)
        val etKeyId = view.findViewById<EditText>(R.id.etRazorpayKey)
        val etSecret = view.findViewById<EditText>(R.id.etWebhookSecret)
        val btnSave = view.findViewById<Button>(R.id.btnSaveConfig)

        // Security Toggles
        val switchPinLock = view.findViewById<SwitchCompat>(R.id.switchPinLock)
        val switchBiometric = view.findViewById<SwitchCompat>(R.id.switchBiometric)

        val prefs = requireActivity().getSharedPreferences("SoundboxPrefs", Context.MODE_PRIVATE)
        
        // Load existing saved configurations
        etWebhook.setText(prefs.getString("webhook_url", ""))
        etKeyId.setText(prefs.getString("razorpay_key", ""))
        etSecret.setText(prefs.getString("webhook_secret", ""))

        // Load security toggle states
        isUpdatingSwitch = true
        switchPinLock.isChecked = prefs.getBoolean("is_pin_enabled", false)
        switchBiometric.isChecked = prefs.getBoolean("is_biometric_enabled", false)
        isUpdatingSwitch = false

        // Save switch states instantly on change by opening full-screen PinActivity smoothly
        switchPinLock.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitch) return@setOnCheckedChangeListener

            val intent = Intent(requireContext(), PinActivity::class.java).apply {
                putExtra("action_mode", if (isChecked) "set_pin" else "disable_pin")
            }
            startActivity(intent)
        }

        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitch) return@setOnCheckedChangeListener
            prefs.edit().putBoolean("is_biometric_enabled", isChecked).apply()
            databaseRef.child("soundbox_settings").child("is_biometric_enabled").setValue(isChecked)
            Toast.makeText(requireContext(), if (isChecked) "Biometric Lock Enabled" else "Biometric Lock Disabled", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            val webhookUrl = etWebhook.text.toString().trim()
            val keyId = etKeyId.text.toString().trim()
            val keySecret = etSecret.text.toString().trim()

            prefs.edit().apply {
                putString("webhook_url", webhookUrl)
                putString("razorpay_key", keyId)
                putString("webhook_secret", keySecret)
                apply()
            }

            // Sync configuration settings to Firebase Realtime Database
            val settingsData = hashMapOf(
                "webhookUrl" to webhookUrl,
                "razorpayKey" to keyId,
                "webhookSecret" to keySecret,
                "isPinEnabled" to switchPinLock.isChecked,
                "isBiometricEnabled" to switchBiometric.isChecked
            )
            databaseRef.child("soundbox_settings").updateChildren(settingsData as Map<String, Any>)

            // Real backend service start/stop control based on credentials input
            if (keyId.isNotEmpty() && keySecret.isNotEmpty()) {
                val serviceIntent = Intent(requireContext(), SoundAlertService::class.java).apply {
                    action = SoundAlertService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireActivity().startForegroundService(serviceIntent)
                } else {
                    requireActivity().startService(serviceIntent)
                }
                Toast.makeText(requireContext(), "Configurations Saved & Monitoring Started!", Toast.LENGTH_SHORT).show()
            } else {
                val stopIntent = Intent(requireContext(), SoundAlertService::class.java).apply {
                    action = SoundAlertService.ACTION_STOP
                }
                requireActivity().startService(stopIntent)
                Toast.makeText(requireContext(), "Credentials empty! Monitoring Stopped.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // Refresh toggle state when returning from PinActivity
        val prefs = requireActivity().getSharedPreferences("SoundboxPrefs", Context.MODE_PRIVATE)
        val switchPinLock = view?.findViewById<SwitchCompat>(R.id.switchPinLock)
        if (switchPinLock != null) {
            isUpdatingSwitch = true
            switchPinLock.isChecked = prefs.getBoolean("is_pin_enabled", false)
            isUpdatingSwitch = false
        }
    }
}
