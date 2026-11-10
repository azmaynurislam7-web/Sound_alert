package com.soundbox.app

import android.app.AlertDialog
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

        // Save switch states instantly on change with custom PIN popup setup/disable flow
        switchPinLock.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSwitch) return@setOnCheckedChangeListener

            if (isChecked) {
                // Show dialog to enter and set a new PIN
                showSetPinDialog(switchPinLock, prefs)
            } else {
                // Show dialog to enter current PIN to disable
                showVerifyPinForDisableDialog(switchPinLock, prefs)
            }
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

    private fun showSetPinDialog(switchPinLock: SwitchCompat, prefs: android.content.SharedPreferences) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_set_pin, null)
        val etNewPin = dialogView.findViewById<EditText>(R.id.etNewPin)
        val etConfirmPin = dialogView.findViewById<EditText>(R.id.etConfirmPin)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Set Soundbox PIN")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel") { _, _ ->
                isUpdatingSwitch = true
                switchPinLock.isChecked = false
                isUpdatingSwitch = false
            }
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = etNewPin.text.toString().trim()
                val confirmPin = etConfirmPin.text.toString().trim()

                if (pin.length != 4) {
                    Toast.makeText(requireContext(), "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (pin != confirmPin) {
                    Toast.makeText(requireContext(), "PINs do not match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Save PIN to Firebase and SharedPreferences
                prefs.edit().putBoolean("is_pin_enabled", true).putString("app_pin", pin).apply()
                databaseRef.child("soundbox_settings").child("is_pin_enabled").setValue(true)
                databaseRef.child("soundbox_settings").child("app_pin").setValue(pin)

                Toast.makeText(requireContext(), "PIN Lock Enabled Successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showVerifyPinForDisableDialog(switchPinLock: SwitchCompat, prefs: android.content.SharedPreferences) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_verify_pin, null)
        val etCurrentPin = dialogView.findViewById<EditText>(R.id.etCurrentPin)
        val savedPin = prefs.getString("app_pin", "")

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Disable PIN Lock")
            .setView(dialogView)
            .setPositiveButton("Disable", null)
            .setNegativeButton("Cancel") { _, _ ->
                isUpdatingSwitch = true
                switchPinLock.isChecked = true
                isUpdatingSwitch = false
            }
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val enteredPin = etCurrentPin.text.toString().trim()

                if (enteredPin != savedPin && savedPin!!.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Incorrect PIN", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Disable and remove PIN from Firebase and SharedPreferences
                prefs.edit().putBoolean("is_pin_enabled", false).remove("app_pin").apply()
                databaseRef.child("soundbox_settings").child("is_pin_enabled").setValue(false)
                databaseRef.child("soundbox_settings").child("app_pin").removeValue()

                Toast.makeText(requireContext(), "PIN Lock Disabled & PIN Removed", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
