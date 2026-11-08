object ConfigManager {
    private const val PREF_NAME = "SoundboxConfig"
    private const val KEY_ID = "key_id"
    private const val KEY_SECRET = "key_secret"
    private const val LAST_PAYMENT_ID = "last_payment_id"

    fun saveCredentials(context: Context, keyId: String, keySecret: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ID, keyId).putString(KEY_SECRET, keySecret).apply()
    }

    fun getKeyId(context: Context): String? = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_ID, null)

    fun getKeySecret(context: Context): String? = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_SECRET, null)

    fun getLastPaymentId(context: Context): String? =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(LAST_PAYMENT_ID, null)

    fun setLastPaymentId(context: Context, paymentId: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString(LAST_PAYMENT_ID, paymentId).apply()
    }
}
