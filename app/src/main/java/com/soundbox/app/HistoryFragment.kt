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
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView

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

        if (transactionList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.adapter = HistoryAdapter(transactionList)
        }
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
