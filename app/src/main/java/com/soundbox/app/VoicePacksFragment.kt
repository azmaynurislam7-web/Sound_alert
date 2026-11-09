package com.soundbox.app

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class VoicePacksFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech

    data class VoicePack(val id: String, val name: String, val desc: String)

    private val voiceList = listOf(
        VoicePack("paytm_en", "Paytm Style - English", "Standard 2s Alert"),
        VoicePack("paytm_hi", "Paytm Style - Hindi", "Paytm par 10 rupaye prapt hue"),
        VoicePack("paytm_bn", "Paytm Style - Bengali", "Paytm a 10 taka paoya gelo"),
        VoicePack("phonepe_en", "PhonePe Style - English", "Received Rs. 10 successfully"),
        VoicePack("phonepe_hi", "PhonePe Style - Hindi", "PhonePe par 20 rupaye mile"),
        VoicePack("gpay_en", "Google Pay Style - English", "Google Pay: Received 50 rupees"),
        VoicePack("gpay_hi", "Google Pay Style - Hindi", "Google Pay par 100 rupaye prapt hue"),
        VoicePack("classic_bell", "Classic Bell Sound", "Traditional Soundbox Beep + Tone"),
        VoicePack("loud_alert", "Loud Payment Alert", "High volume tone for noisy shops"),
        VoicePack("fast_upi", "Fast UPI Tone", "Quick notification sound"),
        VoicePack("amazon_hi", "Amazon Pay Style", "Amazon Pay se 500 rupaye prapt hue"),
        VoicePack("bhim_hi", "BHIM UPI Voice", "BHIM app par payment mila"),
        VoicePack("mobikwik_en", "Mobikwik Alert", "Mobikwik wallet credited"),
        VoicePack("fampay_en", "Fampay Teen Tone", "Fun style payment alert"),
        VoicePack("corporate_en", "Corporate Executive", "Professional chime alert"),
        VoicePack("royal_tone", "Royal Soundbox Tone", "Premium bass sound alert"),
        VoicePack("cyberpunk", "Cyberpunk Alert", "Futuristic electronic beep"),
        VoicePack("soft_bell", "Soft Bell Tone", "Gentle background chime"),
        VoicePack("dual_tone", "Dual Tone Alert", "Double beep payment notification"),
        VoicePack("mega_volume", "Mega Volume Alert", "Max amplification sound pack"),
        VoicePack("gujarati_upi", "Gujarati UPI Tone", "Paytm ma rupiya malya"),
        VoicePack("tamil_upi", "Tamil UPI Tone", "Paytm-il roobai vanthathu"),
        VoicePack("telugu_upi", "Telugu UPI Tone", "Paytm lo rupayilu vachayi"),
        VoicePack("marathi_upi", "Marathi UPI Tone", "Paytm madhye rupaye prapt"),
        VoicePack("punjabi_upi", "Punjabi UPI Tone", "Paytm vich paise aaye")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_voice_packs, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewVoices)

        // TextToSpeech initialize kar rahe hain
        tts = TextToSpeech(requireContext(), this)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = VoiceAdapter(
            voiceList,
            onSelect = { voice ->
                val prefs = requireActivity().getSharedPreferences("SoundboxPrefs", Context.MODE_PRIVATE)
                prefs.edit().putString("active_voice", voice.id).apply()
                Toast.makeText(requireContext(), "${voice.name} Activated!", Toast.LENGTH_SHORT).show()
            },
            onPreview = { voice ->
                // Preview click par voice bolkar sunayega
                tts.speak(voice.desc, TextToSpeech.QUEUE_FLUSH, null, null)
            }
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
        private val onPreview: (VoicePack) -> Unit
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
                onPreview(voice)
            }

            holder.btnSelect.setOnClickListener {
                onSelect(voice)
            }
        }

        override fun getItemCount() = list.size
    }
}
