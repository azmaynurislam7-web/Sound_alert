package com.soundbox.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.Executor

class PinActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val databaseRef = FirebaseDatabase.getInstance().reference.child("soundbox_settings")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val sharedPrefs = getSharedPreferences("SoundboxPrefs", MODE_PRIVATE)
        val etPin = findViewById<EditText>(R.id.etPin)
        val btnUnlock = findViewById<Button>(R.id.btnUnlock)

        // Fetch security configurations from Firebase Realtime Database
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isPinEnabled = snapshot.child("is_pin_enabled").getValue(Boolean::class.java) ?: sharedPrefs.getBoolean("is_pin_enabled", false)
                val isBiometricEnabled = snapshot.child("is_biometric_enabled").getValue(Boolean::class.java) ?: sharedPrefs.getBoolean("is_biometric_enabled", false)
                val savedPin = snapshot.child("app_pin").getValue(String::class.java) ?: sharedPrefs.getString("app_pin", "")

                // Sync with local SharedPreferences cache
                sharedPrefs.edit()
                    .putBoolean("is_pin_enabled", isPinEnabled)
                    .putBoolean("is_biometric_enabled", isBiometricEnabled)
                    .putString("app_pin", savedPin)
                    .apply()

                // Agar PIN lock enabled nahi hai toh direct main activity par navigate karein
                if (!isPinEnabled) {
                    navigateToMain()
                    return
                }

                // Agar PIN set nahi hai toh button text "Set PIN" kar dein
                if (savedPin.isNullOrEmpty()) {
                    btnUnlock.text = "Set PIN"
                }

                // Agar biometric enabled hai aur PIN pehle se set hai toh biometric prompt trigger karein
                if (isBiometricEnabled && !savedPin.isNullOrEmpty()) {
                    setupBiometricAuthentication()
                }

                btnUnlock.setOnClickListener {
                    val enteredPin = etPin.text.toString().trim()

                    if (enteredPin.isEmpty()) {
                        Toast.makeText(this@PinActivity, "Please enter PIN", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    if (savedPin.isNullOrEmpty()) {
                        // Naya PIN set karke Firebase aur local storage mein save karein
                        databaseRef.child("app_pin").setValue(enteredPin)
                        sharedPrefs.edit().putString("app_pin", enteredPin).apply()
                        Toast.makeText(this@PinActivity, "PIN Set Successfully", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    } else {
                        if (enteredPin == savedPin) {
                            navigateToMain()
                        } else {
                            Toast.makeText(this@PinActivity, "Incorrect PIN. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Fallback to local SharedPreferences if offline
                val isPinEnabled = sharedPrefs.getBoolean("is_pin_enabled", false)
                val isBiometricEnabled = sharedPrefs.getBoolean("is_biometric_enabled", false)
                val savedPin = sharedPrefs.getString("app_pin", "")

                if (!isPinEnabled) {
                    navigateToMain()
                    return
                }

                if (isBiometricEnabled && !savedPin.isNullOrEmpty()) {
                    setupBiometricAuthentication()
                }

                btnUnlock.setOnClickListener {
                    val enteredPin = etPin.text.toString().trim()
                    if (enteredPin.isEmpty()) {
                        Toast.makeText(this@PinActivity, "Please enter your PIN", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (enteredPin == savedPin) {
                        navigateToMain()
                    } else {
                        Toast.makeText(this@PinActivity, "Incorrect PIN. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun setupBiometricAuthentication() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Biometric Verified", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Fingerprint not recognized", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Soundbox Security Lock")
            .setSubtitle("Touch your fingerprint sensor to unlock")
            .setNegativeButtonText("Use PIN")
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
