package com.soundbox.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class PinActivity : AppCompatActivity() {

    private val DEFAULT_PIN = "1234"
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val sharedPrefs = getSharedPreferences("SoundboxPrefs", MODE_PRIVATE)
        val isPinEnabled = sharedPrefs.getBoolean("is_pin_enabled", true) // Default enabled
        val isBiometricEnabled = sharedPrefs.getBoolean("is_biometric_enabled", false)

        val pinContainer = findViewById<LinearLayout>(R.id.pinContainer) // Optional: layout container agar hide karna ho
        val etPin = findViewById<EditText>(R.id.etPin)
        val btnUnlock = findViewById<Button>(R.id.btnUnlock)

        // 1. Agar Security PIN disabled hai, toh seedha MainActivity par bhej dein
        if (!isPinEnabled) {
            navigateToMain()
            return
        }

        // 2. Agar Biometric enabled hai, toh app khulte hi fingerprint prompt trigger karein
        if (isBiometricEnabled) {
            setupBiometricAuthentication()
        }

        // Manual PIN Unlock Click Listener
        btnUnlock.setOnClickListener {
            val enteredPin = etPin.text.toString()
            val savedPin = sharedPrefs.getString("app_pin", DEFAULT_PIN)

            if (enteredPin == savedPin) {
                navigateToMain()
            } else {
                Toast.makeText(this, "Galat PIN! Dubara koshish karein.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBiometricAuthentication() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Biometric Verified!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Agar user fingerprint cancel kar de, toh PIN enter karne ka option khula rahega
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Fingerprint not recognized.", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Soundbox Security Lock")
            .setSubtitle("Touch your fingerprint sensor to unlock")
            .setNegativeButtonText("Use PIN")
            .build()

        // Prompt launch karein
        biometricPrompt.authenticate(promptInfo)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
