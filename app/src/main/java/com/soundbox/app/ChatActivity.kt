package com.soundbox.app

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {

    private lateinit var tvChatUserEmail: TextView
    private lateinit var recyclerViewChat: RecyclerView
    private lateinit var etMessageInput: EditText
    private lateinit var btnSend: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()

    private var receiverUid: String? = null
    private var receiverEmail: String? = null
    private var senderUid: String? = null
    private var senderRoom: String? = null
    private var receiverRoom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        receiverUid = intent.getStringExtra("userId")
        receiverEmail = intent.getStringExtra("userEmail")

        auth = FirebaseAuth.getInstance()
        senderUid = auth.currentUser?.uid

        // Unique chat rooms for both users to maintain real-time sync
        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        tvChatUserEmail = findViewById(R.id.tvChatUserEmail)
        recyclerViewChat = findViewById(R.id.recyclerViewChat)
        etMessageInput = findViewById(R.id.etMessageInput)
        btnSend = findViewById(R.id.btnSend)

        tvChatUserEmail.text = receiverEmail ?: "Chat"

        recyclerViewChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        messageAdapter = MessageAdapter(this, messageList)
        recyclerViewChat.adapter = messageAdapter

        databaseReference = FirebaseDatabase.getInstance().getReference()

        // Load messages in real-time
        databaseReference.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messageList.clear()
                    for (postSnapshot in snapshot.children) {
                        val message = postSnapshot.getValue(Message::class.java)
                        if (message != null) {
                            messageList.add(message)
                        }
                    }
                    messageAdapter.notifyDataSetChanged()
                    if (messageList.isNotEmpty()) {
                        recyclerViewChat.scrollToPosition(messageList.size - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        btnSend.setOnClickListener {
            val messageText = etMessageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                val messageObject = Message(messageText, senderUid)

                databaseReference.child("chats").child(senderRoom!!).child("messages").push()
                    .setValue(messageObject).addOnSuccessListener {
                        databaseReference.child("chats").child(receiverRoom!!).child("messages").push()
                            .setValue(messageObject)
                    }

                etMessageInput.setText("")
            }
        }
    }
}

data class Message(
    val message: String? = "",
    val senderId: String? = ""
)
