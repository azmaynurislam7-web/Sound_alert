package com.soundbox.app

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class DashboardFragment : Fragment() {

    private lateinit var tvTotalCollection: TextView
    private lateinit var tvSuccessfulPayments: TextView
    private lateinit var tvLatestTransaction: TextView
    private lateinit var tvServiceStatus: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        
        tvServiceStatus = view.findViewById(R.id.tvServiceStatus)
        tvTotalCollection = view.findViewById(R.id.tvTotalCollection)
        tvSuccessfulPayments = view.findViewById(R.id.tvSuccessfulPayments)
        tvLatestTransaction = view.findViewById(R.id.tvLatestTransaction)

        tvServiceStatus.text = "🟢 Background Listener Active"

        loadDashboardData()

        // Real-time UI auto-refresh every 3 seconds to reflect new payments instantly
        updateRunnable = object : Runnable {
            override fun run() {
                loadDashboardData()
                handler.postDelayed(this, 3000)
            }
        }
        handler.postDelayed(updateRunnable, 3000)

        return view
    }

    private fun loadDashboardData() {
        val context = context ?: return
        val prefs = context.getSharedPreferences("SoundboxPrefs", Context.MODE_PRIVATE)

        val totalCollection = prefs.getFloat("total_collection", 0f)
        val paymentCount = prefs.getInt("payment_count", 0)
        val latestTx = prefs.getString("latest_tx_details", "Waiting for incoming webhook...")

        tvTotalCollection.text = String.format("₹ %.2f", totalCollection)
        tvSuccessfulPayments.text = "$paymentCount Successful Payments"
        tvLatestTransaction.text = latestTx
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
    }
}
