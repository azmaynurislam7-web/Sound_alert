package com.soundbox.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale

class SoundAlertService : Service(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private val CHANNEL_ID = "SoundboxChannel"

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Soundbox is active and monitoring payments...")
        startForeground(1, notification)

        if (!isRunning) {
            isRunning = true
            startPolling()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Soundbox Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Razorpay Soundbox")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
    }

    private fun startPolling() {
        serviceScope.launch {
            while (isRunning) {
                try {
                    checkRazorpayPayments()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000)
            }
        }
    }

    private fun checkRazorpayPayments() {
        val prefs = getSharedPreferences("SoundboxPrefs", Context.MODE_PRIVATE)
        val keyId = prefs.getString("razorpay_key", null)
        val keySecret = prefs.getString("webhook_secret", null)

        if (keyId.isNullOrEmpty() || keySecret.isNullOrEmpty()) return

        val credentials = Credentials.basic(keyId, keySecret)
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.razorpay.com/v1/payments?count=1")
            .get()
            .header("Authorization", credentials)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonObject = JSONObject(responseBody ?: "")
                val items = jsonObject.getJSONArray("items")

                if (items.length() > 0) {
                    val latestPayment = items.getJSONObject(0)
                    val paymentId = latestPayment.getString("id")
                    val status = latestPayment.getString("status")
                    val amountInPaise = latestPayment.getInt("amount")
                    val amountInRupees = amountInPaise / 100

                    val lastSavedId = prefs.getString("last_payment_id", null)

                    if (paymentId != lastSavedId && status == "captured") {
                        prefs.edit().putString("last_payment_id", paymentId).apply()
                        speakOut("Rupees $amountInRupees received successfully")
                    }
                }
            }
        }
    }

    private fun speakOut(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.ENGLISH
        }
    }

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
