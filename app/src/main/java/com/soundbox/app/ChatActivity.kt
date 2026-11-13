package com.soundbox.app

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0
)

class ChatActivity : AppCompatActivity() {

    private lateinit var tvChatUserName: TextView
    private lateinit var recyclerViewChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton

    private lateinit var chatAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()
    private lateinit var databaseReference: DatabaseReference

    private var receiverId: String? = null
    private var receiverName: String? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var chatId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        receiverId = intent.getStringExtra("receiverId")
        receiverName = intent.getStringExtra("receiverName")

        tvChatUserName = findViewById(R.id.tvChatUserName)
        recyclerViewChat = findViewById(R.id.recyclerViewChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        tvChatUserName.text = receiverName ?: "Chat"

        recyclerViewChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        chatAdapter = MessageAdapter(messageList, currentUserId ?: "")
        recyclerViewChat.adapter = chatAdapter

        if (currentUserId != null && receiverId != null) {
            chatId = if (currentUserId < receiverId!!) {
                "${currentUserId}_$receiverId"
            } else {
                "${receiverId}_$currentUserId"
            }
            loadMessages()
        }

        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val messageText = etMessage.text.toString().trim()
        if (messageText.isNotEmpty() && currentUserId != null && receiverId != null) {
            val messageMap = Message(
                senderId = currentUserId,
                receiverId = receiverId!!,
                message = messageText,
                timestamp = System.currentTimeMillis()
            )

            databaseReference = FirebaseDatabase.getInstance().getReference("Chats")
                .child(chatId)
                .child("messages")
                .push()

            databaseReference.setValue(messageMap).addOnSuccessListener {
                etMessage.setText("")
            }
        }
    }

    private fun loadMessages() {
        FirebaseDatabase.getInstance().getReference("Chats")
            .child(chatId)
            .child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for (data in snapshot.children) {
                        val message = data.getValue(Message::class.java)
                        if (message != null) {
                            messageList.add(message)
                        }
                    }
                    chatAdapter.notifyDataSetChanged()
                    if (messageList.isNotEmpty()) {
                        recyclerViewChat.scrollToPosition(messageList.size - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
