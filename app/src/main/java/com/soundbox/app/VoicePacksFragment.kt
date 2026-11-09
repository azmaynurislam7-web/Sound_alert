package com.soundbox.app

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VoicePacksFragment : Fragment() {

    data class VoicePack(val id: String, val name: String, val desc: String)

    private val voiceList = listOf(
        VoicePack("hi_male", "Hindi (Default Male)", "Clear & loud UPI alert"),
        VoicePack("hi_female", "Hindi (Female Voice)", "Professional corporate tone"),
        VoicePack("en_us", "English (Global Accent)", "Standard English audio"),
        VoicePack("bengali", "Bengali (Regional)", "Local merchant preference")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_voice_packs, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewVoices)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = VoiceAdapter(voiceList) { voice ->
            // Save active voice in SharedPreferences
            val prefs = requireActivity().getSharedPreferences("SoundboxPrefs", Context.MODE_PRIVATE)
            prefs.edit().putString("active_voice", voice.id).apply()
            Toast.makeText(requireContext(), "${voice.name} Activated!", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    class VoiceAdapter(
        private val list: List<VoicePack>,
        private val onSelect: (VoicePack) -> Unit
    ) : RecyclerView.Adapter<VoiceAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvVoiceName)
            val tvDesc: TextView = view.findViewById(R.id.tvVoiceDesc)
            val btnPreview: Button = view.findViewById(R.id.btnPreview)
            val btnSelect: Button = view.findViewById(R.id.btnSelect)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_voice_pack, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val voice = list[position]
            holder.tvName.text = voice.name
            holder.tvDesc.text = voice.desc

            holder.btnPreview.setOnClickListener {
                Toast.init // Sample preview toast or MediaPlayer trigger
                Toast.makeText(holder.itemView.context, "Playing sample for ${voice.name}...", Toast.LENGTH_SHORT).show()
            }

            holder.btnSelect.setOnClickListener {
                onSelect(voice)
            }
        }

        override fun getItemCount() = list.size
    }
}
