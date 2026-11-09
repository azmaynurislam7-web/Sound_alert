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

class SettingsFragment : Fragment() {
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
        switchPinLock.isChecked = prefs.getBoolean("is_pin_enabled", true)
        switchBiometric.isChecked = prefs.getBoolean("is_biometric_enabled", false)

        // Save switch states instantly on change
        switchPinLock.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("is_pin_enabled", isChecked).apply()
            Toast.makeText(requireContext(), if (isChecked) "PIN Lock Enabled" else "PIN Lock Disabled", Toast.LENGTH_SHORT).show()
        }

        switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("is_biometric_enabled", isChecked).apply()
            Toast.makeText(requireContext(), if (isChecked) "Biometric Lock Enabled" else "Biometric Lock Disabled", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            val keyId = etKeyId.text.toString().trim()
            val keySecret = etSecret.text.toString().trim()

            prefs.edit().apply {
                putString("webhook_url", etWebhook.text.toString().trim())
                putString("razorpay_key", keyId)
                putString("webhook_secret", keySecret)
                apply()
            }

            // Save hone ke turant baad background service start kar dein with ACTION_START
            if (keyId.isNotEmpty() && keySecret.isNotEmpty()) {
                val serviceIntent = Intent(requireContext(), SoundAlertService::class.java).apply {
                    action = SoundAlertService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireActivity().startForegroundService(serviceIntent)
                } else {
                    requireActivity().startService(serviceIntent)
                }
            } else {
                // Agar credentials khali hain toh service stop karne ke liye intent bhej sakte hain
                val stopIntent = Intent(requireContext(), SoundAlertService::class.java).apply {
                    action = SoundAlertService.ACTION_STOP
                }
                requireActivity().startService(stopIntent)
            }

            Toast.makeText(requireContext(), "Configurations Updated & Service Started!", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
