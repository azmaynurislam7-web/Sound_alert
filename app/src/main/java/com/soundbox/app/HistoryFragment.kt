package com.soundbox.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private val databaseRef = FirebaseDatabase.getInstance().reference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        recyclerView = view.findViewById(R.id.recyclerHistory)
        tvEmpty = view.findViewById(R.id.tvEmptyHistory)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        loadHistory()

        return view
    }

    private fun loadHistory() {
        val prefs = requireContext().getSharedPreferences("SoundboxPrefs", Context.MODE_PRIVATE)
        
        // Real-time synchronization from Firebase Realtime Database with local fallback
        databaseRef.child("soundbox_transactions").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val transactionList = mutableListOf<TransactionItem>()
                val jsonArray = JSONArray()

                for (child in snapshot.children) {
                    val id = child.child("paymentId").getValue(String::class.java) ?: child.key ?: ""
                    val amount = child.child("amount").getValue(Double::class.java) ?: child.child("amount").getValue(Long::class.java)?.toDouble() ?: 0.0
                    val time = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

                    transactionList.add(TransactionItem(id, amount, time))

                    val obj = org.json.JSONObject().apply {
                        put("id", id)
                        put("amount", amount)
                        put("time", time)
                    }
                    jsonArray.put(obj)
                }

                // Update local storage cache with cloud data
                prefs.edit().putString("payment_history", jsonArray.toString()).apply()

                transactionList.reverse()

                if (!isAdded) return

                if (transactionList.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = HistoryAdapter(transactionList)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Fallback to local storage if network fails
                val historyJson = prefs.getString("payment_history", "[]")
                val jsonArray = JSONArray(historyJson)

                val transactionList = mutableListOf<TransactionItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    transactionList.add(
                        TransactionItem(
                            id = obj.getString("id"),
                            amount = obj.getDouble("amount"),
                            time = obj.getLong("time")
                        )
                    )
                }

                transactionList.reverse()

                if (!isAdded) return

                if (transactionList.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = HistoryAdapter(transactionList)
                }
            }
        })
    }

    data class TransactionItem(val id: String, val amount: Double, val time: Long)

    class HistoryAdapter(private val items: List<TransactionItem>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvItemAmount: TextView = view.findViewById(R.id.tvItemAmount)
            val tvItemId: TextView = view.findViewById(R.id.tvItemId)
            val tvItemTime: TextView = view.findViewById(R.id.tvItemTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvItemAmount.text = "₹ %.2f".format(item.amount)
            holder.tvItemId.text = "ID: ${item.id}"
            
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            holder.tvItemTime.text = sdf.format(Date(item.time))
        }

        override fun getItemCount() = items.size
    }
}
