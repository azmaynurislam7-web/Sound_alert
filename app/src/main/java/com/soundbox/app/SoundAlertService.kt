class SoundAlertService : Service(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startPolling()
        }
        return START_STICKY
    }

    private fun startPolling() {
        serviceScope.launch {
            while (isRunning) {
                try {
                    checkRazorpayPayments()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5000) // Har 5 second mein check karega
            }
        }
    }

    private fun checkRazorpayPayments() {
        val keyId = ConfigManager.getKeyId(this)
        val keySecret = ConfigManager.getKeySecret(this)

        if (keyId.isNullOrEmpty() || keySecret.isNullOrEmpty()) return

        val credentials = Credentials.basic(keyId, keySecret)
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.razorpay.v1/payments?count=1")
            .get()
            .header("Authorization", credentials)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonObject = JSONObject(responseBody ?: "")
                val items = jsonObject.getJSONArray("items")

                if (items.length() > 0) {
                    val latestPayment = items.getJSONObject(0)
                    val paymentId = latestPayment.getString("id")
                    val status = latestPayment.getString("status")
                    val amountInPaise = latestPayment.getInt("amount")
                    val amountInRupees = amountInPaise / 100

                    val lastSavedId = ConfigManager.getLastPaymentId(this)

                    // Agar naya payment hai aur status captured/success hai
                    if (paymentId != lastSavedId && status == "captured") {
                        ConfigManager.setLastPaymentId(this, paymentId)
                        
                        // Voice announcement
                        speakOut("Rupees $amountInRupees received successfully")
                        
                        // Yahan aap local database ya history mein save kar sakte hain
                    }
                }
            }
        }
    }

    private fun speakOut(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.ENGLISH
        }
    }

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
