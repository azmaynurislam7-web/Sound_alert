package com.soundbox.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val senderName = remoteMessage.data["senderName"] ?: "CW User"
        val messageBody = remoteMessage.data["body"] ?: "New message"
        val senderId = remoteMessage.data["senderId"] ?: ""

        showNotification(senderName, messageBody, senderId)
    }

    private fun showNotification(senderName: String, message: String, senderId: String) {
        val channelId = "cw_chat_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "CW Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Direct messages from Chat Wave"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // WhatsApp jaisa Direct Reply Intent
        val replyIntent = Intent(this, DirectReplyReceiver::class.java).apply {
            putExtra("senderId", senderId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            this, 0, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteInput = RemoteInput.Builder("key_text_reply")
            .setLabel("Type a reply...")
            .build()

        val action = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send, "Reply", replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // Notification builder with App Name "CW" & Sender Name
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("CW • $senderName")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(action)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
