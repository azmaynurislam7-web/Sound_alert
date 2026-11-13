package com.soundbox.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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
        holder.tvUserInitial.text = if (user.username.isNotEmpty()) user.username.take(1).uppercase() else "U"

        holder.itemView.setOnClickListener { onClick(user) }
    }

    override fun getItemCount(): Int = users.size
}
