package com.soundbox.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            // Permissions handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkManageExternalStorage()
        requestAllPermissions()
        requestBatteryOptimizationExemption()

        val navChats = findViewById<LinearLayout>(R.id.navChats)
        val navVoices = findViewById<LinearLayout>(R.id.navVoices)
        val navSettings = findViewById<LinearLayout>(R.id.navSettings)

        if (savedInstanceState == null) {
            loadFragment(ChatsFragment())
            updateNavSelection(0)
        }

        navChats.setOnClickListener {
            loadFragment(ChatsFragment())
            updateNavSelection(0)
        }

        navVoices.setOnClickListener {
            loadFragment(CallsFragment())
            updateNavSelection(1)
        }

        navSettings.setOnClickListener {
            loadFragment(SettingsFragment())
            updateNavSelection(2)
        }
    }

    private fun updateNavSelection(index: Int) {
        val iconChats = findViewById<ImageView>(R.id.iconChats)
        val iconVoices = findViewById<ImageView>(R.id.iconVoices)
        val iconSettings = findViewById<ImageView>(R.id.iconSettings)

        val textChats = findViewById<TextView>(R.id.textChats)
        val textVoices = findViewById<TextView>(R.id.textVoices)
        val textSettings = findViewById<TextView>(R.id.textSettings)

        // WhatsApp style labels
        textChats.text = "Chats"
        textVoices.text = "Calls"

        val activeColor = Color.parseColor("#34D399")
        val inactiveColor = Color.parseColor("#9CA3AF")

        ImageViewCompat.setImageTintList(iconChats, ColorStateList.valueOf(if (index == 0) activeColor else inactiveColor))
        textChats.setTextColor(if (index == 0) activeColor else inactiveColor)

        ImageViewCompat.setImageTintList(iconVoices, ColorStateList.valueOf(if (index == 1) activeColor else inactiveColor))
        textVoices.setTextColor(if (index == 1) activeColor else inactiveColor)

        ImageViewCompat.setImageTintList(iconSettings, ColorStateList.valueOf(if (index == 2) activeColor else inactiveColor))
        textSettings.setTextColor(if (index == 2) activeColor else inactiveColor)
    }

    private fun checkManageExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }

    private fun requestAllPermissions() {
        val permissionList = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionList.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionList.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissionList.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        requestPermissionLauncher.launch(permissionList.toTypedArray())
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
