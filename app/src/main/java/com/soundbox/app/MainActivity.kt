package com.soundbox.app

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            startSoundService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkManageExternalStorage()
        requestAllPermissions()

        // Initialize Font Awesome for custom Telegram-style nav bar icons
        val typeface = try {
            Typeface.createFromAsset(assets, "fonts/fa-solid-900.ttf")
        } catch (e: Exception) {
            null
        }

        val iconChats = findViewById<TextView>(R.id.iconChats)
        val iconVoices = findViewById<TextView>(R.id.iconVoices)
        val iconSettings = findViewById<TextView>(R.id.iconSettings)

        if (typeface != null) {
            iconChats.typeface = typeface
            iconVoices.typeface = typeface
            iconSettings.typeface = typeface
        }

        val navChats = findViewById<LinearLayout>(R.id.navChats)
        val navVoices = findViewById<LinearLayout>(R.id.navVoices)
        val navSettings = findViewById<LinearLayout>(R.id.navSettings)

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
            updateNavSelection(0)
        }

        navChats.setOnClickListener {
            loadFragment(DashboardFragment())
            updateNavSelection(0)
        }

        navVoices.setOnClickListener {
            loadFragment(VoicePacksFragment())
            updateNavSelection(1)
        }

        navSettings.setOnClickListener {
            loadFragment(SettingsFragment())
            updateNavSelection(2)
        }
    }

    private fun updateNavSelection(index: Int) {
        val iconChats = findViewById<TextView>(R.id.iconChats)
        val iconVoices = findViewById<TextView>(R.id.iconVoices)
        val iconSettings = findViewById<TextView>(R.id.iconSettings)

        val activeColor = Color.parseColor("#34D399")
        val inactiveColor = Color.parseColor("#9CA3AF")

        iconChats.setTextColor(if (index == 0) activeColor else inactiveColor)
        iconVoices.setTextColor(if (index == 1) activeColor else inactiveColor)
        iconSettings.setTextColor(if (index == 2) activeColor else inactiveColor)
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

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun startSoundService() {
        val serviceIntent = Intent(this, SoundAlertService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}
