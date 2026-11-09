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
                        speakOut(amountInRupees)
                    }
                }
            }
        }
    }

    private fun speakOut(amount: Int) {
        val prefs = getSharedPreferences("SoundboxPrefs", Context.MODE_PRIVATE)
        val activeVoiceId = prefs.getString("active_voice", "paytm_en") ?: "paytm_en"

        // Active voice pack ke hisab se dynamic message set hoga
        val textToSpeak = when (activeVoiceId) {
            "paytm_en" -> "Received $amount rupees successfully"
            "paytm_hi" -> "Paytm par $amount rupaye prapt hue"
            "paytm_bn" -> "Paytm a $amount taka paoya gelo"
            "phonepe_en" -> "Received Rs. $amount successfully"
            "phonepe_hi" -> "PhonePe par $amount rupaye mile"
            "gpay_en" -> "Google Pay: Received $amount rupees"
            "gpay_hi" -> "Google Pay par $amount rupaye prapt hue"
            "classic_bell" -> "Payment of $amount rupees received"
            "loud_alert" -> "Alert! $amount rupees credited"
            "fast_upi" -> "UPI $amount rupees received"
            "amazon_hi" -> "Amazon Pay se $amount rupaye prapt hue"
            "bhim_hi" -> "BHIM app par $amount rupaye mile"
            "mobikwik_en" -> "Mobikwik wallet credited with $amount rupees"
            "fampay_en" -> "Got $amount rupees successfully"
            "corporate_en" -> "Account credited with $amount rupees"
            "royal_tone" -> "Successfully received $amount rupees"
            "cyberpunk" -> "Transaction confirmed. $amount rupees received"
            "soft_bell" -> "Received $amount rupees"
            "dual_tone" -> "Payment alert: $amount rupees"
            "mega_volume" -> "Alert! Received $amount rupees"
            "gujarati_upi" -> "Paytm ma $amount rupiya malya"
            "tamil_upi" -> "Paytm-il $amount roobai vanthathu"
            "telugu_upi" -> "Paytm lo $amount rupayilu vachayi"
            "marathi_upi" -> "Paytm madhye $amount rupaye prapt"
            "punjabi_upi" -> "Paytm vich $amount paise aaye"
            else -> "Paytm par $amount rupaye prapt hue"
        }

        tts.speak(textToSpeak, TextToSpeech.QUEUE_ADD, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("en", "IN")
            tts.setSpeechRate(0.45f)
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
