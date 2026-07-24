package com.debrid.browser.data

import android.content.Context
import androidx.core.content.edit

/** Simple SharedPreferences wrapper for app settings. */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("debrid_browser", Context.MODE_PRIVATE)

    var apiToken: String
        get() = sp.getString(KEY_TOKEN, "") ?: ""
        set(value) = sp.edit { putString(KEY_TOKEN, value.trim()) }

    var dmmUrl: String
        get() = sp.getString(KEY_DMM_URL, DEFAULT_DMM_URL) ?: DEFAULT_DMM_URL
        set(value) = sp.edit { putString(KEY_DMM_URL, value.trim()) }

    /** When true, videos are handed off to an external player via an intent instead of the built-in one. */
    var preferExternalPlayer: Boolean
        get() = sp.getBoolean(KEY_EXTERNAL_PLAYER, false)
        set(value) = sp.edit { putBoolean(KEY_EXTERNAL_PLAYER, value) }

    val hasToken: Boolean get() = apiToken.isNotBlank()

    companion object {
        private const val KEY_TOKEN = "rd_api_token"
        private const val KEY_DMM_URL = "dmm_url"
        private const val KEY_EXTERNAL_PLAYER = "external_player"
        const val DEFAULT_DMM_URL = "https://debridmediamanager.com"
    }
}
