package com.soundbox.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SoundAlertService : Service() {
    private val CHANNEL_ID = "SoundboxServiceChannel"
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startListeningToDatabase()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Razorpay Soundbox Active")
            .setContentText("Background mein payment listen ho rahi hai...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }

    private fun startListeningToDatabase() {
        // Firebase Realtime Database reference
        // Yahan 'payments' node par listener lagaya gaya hai
        val database = Firebase.database
        val ref = database.getReference("payments")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Jab bhi database mein koi naya data aayega ya change hoga yeh run hoga
                // Aap yahan amount check karke sound play kar sakte hain
                playAlertSound()
            }

            override fun onCancelled(error: DatabaseError) {
                // Error handle karne ke liye
            }
        })
    }

    private fun playAlertSound() {
        try {
            // Phone ki default notification sound play karega (Aap chahein toh custom MP3 bhi laga sakte hain)
            val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, notificationUri)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Soundbox Foreground Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }
}
