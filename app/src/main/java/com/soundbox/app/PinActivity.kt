package com.soundbox.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
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

    private val enteredPinBuilder = StringBuilder()
    private lateinit var pinDots: Array<View>
    private lateinit var tvTitle: TextView
    private lateinit var btnFingerprint: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val sharedPrefs = getSharedPreferences("SoundboxPrefs", MODE_PRIVATE)
        tvTitle = findViewById(R.id.tvPinTitle)

        // 4 PIN Dot indicators mapping (ensure your activity_pin.xml has these ids or matching views)
        pinDots = arrayOf(
            findViewById(R.id.pinDot1),
            findViewById(R.id.pinDot2),
            findViewById(R.id.pinDot3),
            findViewById(R.id.pinDot4)
        )

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

                if (!isPinEnabled) {
                    navigateToMain()
                    return
                }

                if (savedPin.isNullOrEmpty()) {
                    tvTitle.text = "Set your Soundbox PIN"
                } else {
                    tvTitle.text = "Enter your Soundbox PIN"
                }

                if (isBiometricEnabled && !savedPin.isNullOrEmpty()) {
                    setupBiometricAuthentication()
                }

                setupKeypad(savedPin)
            }

            override fun onCancelled(error: DatabaseError) {
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

                setupKeypad(savedPin)
            }
        })
    }

    private fun setupKeypad(savedPin: String?) {
        val sharedPrefs = getSharedPreferences("SoundboxPrefs", MODE_PRIVATE)

        val numberButtons = listOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9"
        )

        for ((id, digit) in numberButtons) {
            findViewById<Button>(id)?.setOnClickListener {
                if (enteredPinBuilder.length < 4) {
                    enteredPinBuilder.append(digit)
                    updatePinDots()

                    if (enteredPinBuilder.length == 4) {
                        verifyOrSetPin(savedPin, sharedPrefs)
                    }
                }
            }
        }

        findViewById<View>(R.id.btnBackspace)?.setOnClickListener {
            if (enteredPinBuilder.isNotEmpty()) {
                enteredPinBuilder.deleteCharAt(enteredPinBuilder.length - 1)
                updatePinDots()
            }
        }

        btnFingerprint = findViewById(R.id.btnFingerprint)
        btnFingerprint.setOnClickListener {
            setupBiometricAuthentication()
        }
    }

    private fun updatePinDots() {
        val length = enteredPinBuilder.length
        for (i in pinDots.indices) {
            if (i < length) {
                pinDots[i].setBackgroundResource(R.drawable.bg_pin_dot_filled)
            } else {
                pinDots[i].setBackgroundResource(R.drawable.bg_pin_dot_empty)
            }
        }
    }

    private fun verifyOrSetPin(savedPin: String?, sharedPrefs: android.content.SharedPreferences) {
        val enteredPin = enteredPinBuilder.toString()

        if (savedPin.isNullOrEmpty()) {
            databaseRef.child("app_pin").setValue(enteredPin)
            sharedPrefs.edit().putString("app_pin", enteredPin).apply()
            Toast.makeText(this, "PIN Set Successfully", Toast.LENGTH_SHORT).show()
            navigateToMain()
        } else {
            if (enteredPin == savedPin) {
                navigateToMain()
            } else {
                Toast.makeText(this, "Incorrect PIN. Please try again.", Toast.LENGTH_SHORT).show()
                enteredPinBuilder.clear()
                updatePinDots()
            }
        }
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
