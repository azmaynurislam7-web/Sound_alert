package com.soundbox.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PinActivity : AppCompatActivity() {
    private val DEFAULT_PIN = "1234" // Default PIN, user can change later in settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val etPin = findViewById<EditText>(R.id.etPin)
        val btnUnlock = findViewById<Button>(R.id.btnUnlock)

        btnUnlock.setOnClickListener {
            val enteredPin = etPin.text.toString()
            val sharedPrefs = getSharedPreferences("SoundboxPrefs", MODE_PRIVATE)
            val savedPin = sharedPrefs.getString("app_pin", DEFAULT_PIN)

            if (enteredPin == savedPin) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Galat PIN! Dubara koshish karein.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
