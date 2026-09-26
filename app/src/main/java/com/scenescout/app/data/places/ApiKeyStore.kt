package com.scenescout.app.data.places

import android.content.Context

/**
 * The user's Google API key, pasted once in About > API key and kept in
 * private SharedPreferences. Lets the key be set or rotated without a
 * rebuild; BuildConfig.MAPS_API_KEY (CI secret) is the fallback.
 */
class ApiKeyStore(context: Context) {
    private val prefs =
        context.getSharedPreferences("scenescout", Context.MODE_PRIVATE)

    fun getKey(): String? =
        prefs.getString(KEY, null)?.trim()?.ifBlank { null }

    fun setKey(raw: String) {
        prefs.edit().putString(KEY, raw.trim()).apply()
    }

    fun clearKey() {
        prefs.edit().remove(KEY).apply()
    }

    fun hasKey(): Boolean = getKey() != null

    companion object {
        private const val KEY = "google_api_key"
    }
}
