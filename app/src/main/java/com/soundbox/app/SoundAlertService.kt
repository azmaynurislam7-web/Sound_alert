package com.soundbox.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

class SoundAlertService : Service(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var audioManager: AudioManager
    private var serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private val CHANNEL_ID = "SoundboxChannel"
    private val databaseRef = FirebaseDatabase.getInstance().reference

    companion object {
        const val ACTION_START = "com.soundbox.app.ACTION_START"
        const val ACTION_STOP = "com.soundbox.app.ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
                return START_NOT_STICKY
            }
            else -> {
                val notification = createNotification("Soundbox is active and monitoring payments...")
                startForeground(1, notification)

                if (!isRunning) {
                    isRunning = true
                    if (!serviceScope.isActive) {
                        serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
                    }
                    startPolling()
                }
            }
        }
        return START_STICKY
    }

    private fun stopMonitoring() {
        isRunning = false
        serviceScope.cancel()
        serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SoundAlertService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Razorpay Soundbox")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startPolling() {
        serviceScope.launch {
            while (isRunning) {
                try {
                    checkAndResetDailyCollection()
                    checkRazorpayPayments()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000)
            }
        }
    }

    private fun checkAndResetDailyCollection() {
        val prefs = getSharedPreferences("SoundboxPrefs", Context.MODE_PRIVATE)
        val lastSavedDay = prefs.getInt("last_saved_day", -1)
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        if (lastSavedDay != -1 && lastSavedDay != currentDay) {
            prefs.edit()
                .putFloat("total_collection", 0f)
                .putInt("payment_count", 0)
                .putInt("last_saved_day", currentDay)
                .apply()
        } else if (lastSavedDay == -1) {
            prefs.edit().putInt("last_saved_day", currentDay).apply()
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

                        // Real-time calculation and saving to Local SharedPreferences
                        val currentTotal = prefs.getFloat("total_collection", 0f)
                        val newTotal = currentTotal + amountInRupees
                        val currentCount = prefs.getInt("payment_count", 0) + 1

                        val historyJson = prefs.getString("payment_history", "[]")
                        val jsonArray = JSONArray(historyJson)
                        val timestamp = System.currentTimeMillis()
                        
                        val newTx = JSONObject().apply {
                            put("id", paymentId)
                            put("amount", amountInRupees)
                            put("time", timestamp)
                        }
                        jsonArray.put(newTx)

                        prefs.edit()
                            .putFloat("total_collection", newTotal)
                            .putInt("payment_count", currentCount)
                            .putString("payment_history", jsonArray.toString())
                            .putString("latest_tx_details", "₹ $amountInRupees received successfully (ID: $paymentId)")
                            .apply()

                        // Real-time saving to Firebase Realtime Database
                        val transactionData = hashMapOf(
                            "paymentId" to paymentId,
                            "amount" to amountInRupees,
                            "status" to status,
                            "timestamp" to timestamp
                        )

                        databaseRef.child("soundbox_transactions").child(paymentId).setValue(transactionData)

                        val summaryData = hashMapOf(
                            "totalCollection" to newTotal,
                            "paymentCount" to currentCount,
                            "lastUpdated" to timestamp
                        )

                        databaseRef.child("soundbox_summary").child("stats").setValue(summaryData)

                        maximizeVolumeAndRouteAudio()
                        speakOut(amountInRupees)
                    }
                }
            }
        }
    }

    private fun maximizeVolumeAndRouteAudio() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
    }

    private fun speakOut(amount: Int) {
        val prefs = getSharedPreferences("SoundboxPrefs", Context.MODE_PRIVATE)
        val activeVoiceId = prefs.getString("active_voice", "paytm_en") ?: "paytm_en"

        val locale = when {
            activeVoiceId.contains("_hi") || activeVoiceId.contains("amazon") || activeVoiceId.contains("bhim") -> Locale("hi", "IN")
            activeVoiceId.contains("_bn") -> Locale("bn", "IN")
            activeVoiceId.contains("gujarati") -> Locale("gu", "IN")
            activeVoiceId.contains("tamil") -> Locale("ta", "IN")
            activeVoiceId.contains("telugu") -> Locale("te", "IN")
            activeVoiceId.contains("marathi") -> Locale("mr", "IN")
            activeVoiceId.contains("punjabi") -> Locale("pa", "IN")
            else -> Locale("en", "IN")
        }
        
        tts.language = locale

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

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        tts.setAudioAttributes(audioAttributes)

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
