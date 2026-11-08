package com.soundbox.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val etWebhook = view.findViewById<EditText>(R.id.etWebhookUrl)
        val etKeyId = view.findViewById<EditText>(R.id.etRazorpayKey)
        val etSecret = view.findViewById<EditText>(R.id.etWebhookSecret)
        val btnSave = view.findViewById<Button>(R.id.btnSaveConfig)

        val prefs = requireActivity().getSharedPreferences("SoundboxPrefs", Context.MODE_PRIVATE)
        etWebhook.setText(prefs.getString("webhook_url", ""))
        etKeyId.setText(prefs.getString("razorpay_key", ""))
        etSecret.setText(prefs.getString("webhook_secret", ""))

        btnSave.setOnClickListener {
            prefs.edit().apply {
                putString("webhook_url", etWebhook.text.toString())
                putString("razorpay_key", etKeyId.text.toString())
                putString("webhook_secret", etSecret.text.toString())
                apply()
            }
            Toast.makeText(requireContext(), "Configurations Successfully Updated!", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
