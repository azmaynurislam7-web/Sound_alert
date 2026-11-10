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
    private lateinit var tvSubtitle: TextView
    private lateinit var btnFingerprint: Button

    private var actionMode: String? = null
    private var firstEnteredPin: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val sharedPrefs = getSharedPreferences("SoundboxPrefs", MODE_PRIVATE)
        tvTitle = findViewById(R.id.tvPinTitle)
        tvSubtitle = findViewById(R.id.tvPinSubtitle)
        btnFingerprint = findViewById(R.id.btnFingerprint)

        actionMode = intent.getStringExtra("action_mode")

        pinDots = arrayOf(
            findViewById(R.id.pinDot1),
            findViewById(R.id.pinDot2),
            findViewById(R.id.pinDot3),
            findViewById(R.id.pinDot4)
        )

        when (actionMode) {
            "set_pin" -> {
                tvTitle.text = "Set New PIN"
                tvSubtitle.text = "Enter a 4-digit PIN for your Soundbox"
                btnFingerprint.visibility = View.GONE
                setupKeypadForSetPin(sharedPrefs)
            }
            "disable_pin" -> {
                tvTitle.text = "Verify PIN"
                tvSubtitle.text = "Enter current PIN to disable"
                btnFingerprint.visibility = View.GONE
                setupKeypadForDisablePin(sharedPrefs)
            }
            else -> {
                databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isPinEnabled = snapshot.child("is_pin_enabled").getValue(Boolean::class.java) ?: sharedPrefs.getBoolean("is_pin_enabled", false)
                        val isBiometricEnabled = snapshot.child("is_biometric_enabled").getValue(Boolean::class.java) ?: sharedPrefs.getBoolean("is_biometric_enabled", false)
                        val savedPin = snapshot.child("app_pin").getValue(String::class.java) ?: sharedPrefs.getString("app_pin", "")

                        sharedPrefs.edit()
                            .putBoolean("is_pin_enabled", isPinEnabled)
                            .putBoolean("is_biometric_enabled", isBiometricEnabled)
                            .putString("app_pin", savedPin)
                            .apply()

                        if (!isPinEnabled) {
                            navigateToMain()
                            return
                        }

                        tvTitle.text = "Enter your Soundbox PIN"
                        tvSubtitle.text = "Enter your 4-digit PIN to access"

                        if (isBiometricEnabled && !savedPin.isNullOrEmpty()) {
                            setupBiometricAuthentication()
                        }

                        setupKeypadForUnlock(savedPin)
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

                        setupKeypadForUnlock(savedPin)
                    }
                })
            }
        }
    }

    private fun setupKeypadCommonListeners(onDigitComplete: (String) -> Unit) {
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
                        val pin = enteredPinBuilder.toString()
                        enteredPinBuilder.clear()
                        updatePinDots()
                        onDigitComplete(pin)
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
    }

    private fun setupKeypadForUnlock(savedPin: String?) {
        setupKeypadCommonListeners { enteredPin ->
            if (enteredPin == savedPin || savedPin.isNullOrEmpty()) {
                navigateToMain()
            } else {
                Toast.makeText(this, "Incorrect PIN. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        btnFingerprint.setOnClickListener {
            setupBiometricAuthentication()
        }
    }

    private fun setupKeypadForSetPin(sharedPrefs: android.content.SharedPreferences) {
        setupKeypadCommonListeners { enteredPin ->
            if (firstEnteredPin == null) {
                firstEnteredPin = enteredPin
                tvTitle.text = "Confirm PIN"
                tvSubtitle.text = "Re-enter your 4-digit PIN"
            } else {
                if (firstEnteredPin == enteredPin) {
                    sharedPrefs.edit().putBoolean("is_pin_enabled", true).putString("app_pin", enteredPin).apply()
                    databaseRef.child("is_pin_enabled").setValue(true)
                    databaseRef.child("app_pin").setValue(enteredPin)
                    Toast.makeText(this, "PIN Set Successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "PINs do not match. Try again.", Toast.LENGTH_SHORT).show()
                    firstEnteredPin = null
                    tvTitle.text = "Set New PIN"
                    tvSubtitle.text = "Enter a 4-digit PIN for your Soundbox"
                }
            }
        }
    }

    private fun setupKeypadForDisablePin(sharedPrefs: android.content.SharedPreferences) {
        val savedPin = sharedPrefs.getString("app_pin", "")
        setupKeypadCommonListeners { enteredPin ->
            if (enteredPin == savedPin || savedPin.isNullOrEmpty()) {
                sharedPrefs.edit().putBoolean("is_pin_enabled", false).remove("app_pin").apply()
                databaseRef.child("is_pin_enabled").setValue(false)
                databaseRef.child("app_pin").removeValue()
                Toast.makeText(this, "PIN Lock Disabled", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Incorrect PIN. Cannot disable.", Toast.LENGTH_SHORT).show()
            }
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
