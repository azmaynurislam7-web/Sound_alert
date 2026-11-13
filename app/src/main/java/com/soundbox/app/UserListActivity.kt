package com.soundbox.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val bio: String = ""
)

class UserListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<User>()
    private lateinit var databaseReference: DatabaseReference
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        recyclerView = findViewById(R.id.recyclerViewUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        userAdapter = UserAdapter(userList) { user ->
            // Click karne par Chat Activity open hogi (Receiver ID aur Name pass hoga)
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("receiverId", user.uid)
                putExtra("receiverName", user.username)
            }
            startActivity(intent)
        }
        recyclerView.adapter = userAdapter

        fetchUsers()
    }

    private fun fetchUsers() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Users")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (data in snapshot.children) {
                    val user = data.getValue(User::class.java)
                    // Khud ki profile ko list se hata kar baaki saare users dikhayein
                    if (user != null && user.uid != currentUserId) {
                        userList.add(user)
                    }
                }
                userAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Error handling agar zaroori ho
            }
        })
    }
}

// RecyclerView Adapter for Users List
class UserAdapter(
    private val users: List<User>,
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvUserBio: TextView = itemView.findViewById(R.id.tvUserBio)
        val tvUserInitial: TextView = itemView.findViewById(R.id.tvUserInitial)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.tvUsername.text = user.username
        holder.tvUserBio.text = if (user.bio.isNotEmpty()) user.bio else "Hey there! I am using Chat Wave."
        holder.tvUserInitial.text = user.username.take(1).uppercase()

        holder.itemView.setOnClickListener { onClick(user) }
    }

    override fun getItemCount(): Int = users.size
}
