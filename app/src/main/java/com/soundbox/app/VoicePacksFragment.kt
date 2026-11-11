package com.soundbox.app

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class VoicePacksFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech

    data class VoicePack(val id: String, val name: String, val desc: String, val avatarRes: Int)

    private val voiceList = listOf(
        VoicePack("paytm_en", "Paytm English", "Standard 2s Alert", android.R.drawable.ic_menu_myplaces),
        VoicePack("paytm_hi", "Paytm Hindi", "Paytm par 10 rupaye prapt hue", android.R.drawable.ic_menu_gallery),
        VoicePack("paytm_bn", "Paytm Bengali", "Paytm a 10 taka paoya gelo", android.R.drawable.ic_menu_camera),
        VoicePack("phonepe_en", "PhonePe English", "Received Rs. 10 successfully", android.R.drawable.ic_menu_call),
        VoicePack("phonepe_hi", "PhonePe Hindi", "PhonePe par 20 rupaye mile", android.R.drawable.ic_menu_compass),
        VoicePack("gpay_en", "Google Pay En", "Google Pay: Received 50 rupees", android.R.drawable.ic_menu_manage),
        VoicePack("gpay_hi", "Google Pay Hi", "Google Pay par 100 rupaye prapt hue", android.R.drawable.ic_menu_directions),
        VoicePack("classic_bell", "Classic Bell", "Traditional Soundbox Beep + Tone", android.R.drawable.ic_menu_mapmode),
        VoicePack("loud_alert", "Loud Alert", "High volume tone for noisy shops", android.R.drawable.ic_menu_my_calendar),
        VoicePack("fast_upi", "Fast UPI Tone", "Quick notification sound", android.R.drawable.ic_menu_recent_history),
        VoicePack("amazon_hi", "Amazon Pay", "Amazon Pay se 500 rupaye prapt hue", android.R.drawable.ic_menu_search),
        VoicePack("bhim_hi", "BHIM UPI", "BHIM app par payment mila", android.R.drawable.ic_menu_send),
        VoicePack("mobikwik_en", "Mobikwik", "Mobikwik wallet credited", android.R.drawable.ic_menu_slideshow),
        VoicePack("fampay_en", "Fampay Teen", "Fun style payment alert", android.R.drawable.ic_menu_sort_by_size),
        VoicePack("corporate_en", "Corporate", "Professional chime alert", android.R.drawable.ic_menu_today),
        VoicePack("royal_tone", "Royal Tone", "Premium bass sound alert", android.R.drawable.ic_menu_upload),
        VoicePack("cyberpunk", "Cyberpunk", "Futuristic electronic beep", android.R.drawable.ic_menu_slideshow),
        VoicePack("soft_bell", "Soft Bell", "Gentle background chime", android.R.drawable.ic_menu_slideshow),
        VoicePack("dual_tone", "Dual Tone", "Double beep payment notification", android.R.drawable.ic_menu_slideshow),
        VoicePack("mega_volume", "Mega Volume", "Max amplification sound pack", android.R.drawable.ic_menu_help),
        VoicePack("gujarati_upi", "Gujarati UPI", "Paytm ma rupiya malya", android.R.drawable.ic_menu_info_details),
        VoicePack("tamil_upi", "Tamil UPI", "Paytm-il roobai vanthathu", android.R.drawable.ic_menu_preferences),
        VoicePack("telugu_upi", "Telugu UPI", "Paytm lo rupayilu vachayi", android.R.drawable.ic_menu_report_image),
        VoicePack("marathi_upi", "Marathi UPI", "Paytm madhye rupaye prapt", android.R.drawable.ic_menu_share),
        VoicePack("punjabi_upi", "Punjabi UPI", "Paytm vich paise aaye", android.R.drawable.ic_menu_myplaces)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_voice_packs, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewVoices)

        tts = TextToSpeech(requireContext(), this)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerView.adapter = VoiceAdapter(
            voiceList,
            onSelect = { voice ->
                val prefs = requireActivity().getSharedPreferences("SoundboxPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("active_voice", voice.id).apply()
                Toast.makeText(requireContext(), "${voice.name} Activated!", Toast.LENGTH_SHORT).show()
                recyclerView.adapter?.notifyDataSetChanged()
            },
            onPreview = { voice ->
                tts.speak(voice.desc, TextToSpeech.QUEUE_FLUSH, null, null)
            },
            context = requireContext()
        )

        return view
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("en", "IN")
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    class VoiceAdapter(
        private val list: List<VoicePack>,
        private val onSelect: (VoicePack) -> Unit,
        private val onPreview: (VoicePack) -> Unit,
        private val context: Context
    ) : RecyclerView.Adapter<VoiceAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvVoiceName)
            val tvVoiceStatus: TextView = view.findViewById(R.id.tvVoiceStatus)
            val imgVoiceAvatar: ImageView = view.findViewById(R.id.imgVoiceAvatar)
            val cardContainer: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cardVoiceContainer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_voice_pack, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val voice = list[position]
            holder.tvName.text = voice.name
            holder.imgVoiceAvatar.setImageResource(voice.avatarRes)

            val prefs = context.getSharedPreferences("SoundboxPrefs", Context.MODE_PRIVATE)
            val activeVoiceId = prefs.getString("active_voice", "paytm_en")
            val isActive = (activeVoiceId == voice.id)

            if (isActive) {
                holder.tvVoiceStatus.text = "Active"
                holder.tvVoiceStatus.setTextColor(android.graphics.Color.parseColor("#00E676"))
                holder.cardContainer.strokeColor = android.graphics.Color.parseColor("#00E676")
                holder.cardContainer.strokeWidth = 3
            } else {
                holder.tvVoiceStatus.text = "Tap to Select"
                holder.tvVoiceStatus.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                holder.cardContainer.strokeColor = android.graphics.Color.parseColor("#262626")
                holder.cardContainer.strokeWidth = 1
            }

            holder.itemView.setOnClickListener {
                onSelect(voice)
                onPreview(voice)
            }
        }

        override fun getItemCount() = list.size
    }
}
