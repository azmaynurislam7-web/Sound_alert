package com.soundbox.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DirectReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput != null) {
            val replyText = remoteInput.getCharSequence("key_text_reply")?.toString()
            val receiverId = intent.getStringExtra("senderId")
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            if (!replyText.isNullOrEmpty() && !receiverId.isNullOrEmpty() && currentUserId != null) {
                val chatId = if (currentUserId < receiverId) {
                    "$currentUserId_$receiverId"
                } else {
                    "$receiverId_$currentUserId"
                }

                val messageMap = mapOf(
                    "senderId" to currentUserId,
                    "receiverId" to receiverId,
                    "message" to replyText,
                    "timestamp" to System.currentTimeMillis()
                )

                FirebaseDatabase.getInstance().getReference("Chats")
                    .child(chatId)
                    .child("messages")
                    .push()
                    .setValue(messageMap)
            }
        }
    }
}
