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

    /** TMDB v3 API key, used by the native Discover screen. */
    var tmdbKey: String
        get() = sp.getString(KEY_TMDB, "") ?: ""
        set(value) = sp.edit { putString(KEY_TMDB, value.trim()) }

    val hasTmdbKey: Boolean get() = tmdbKey.isNotBlank()

    /** When true, videos are handed off to an external player via an intent instead of the built-in one. */
    var preferExternalPlayer: Boolean
        get() = sp.getBoolean(KEY_EXTERNAL_PLAYER, false)
        set(value) = sp.edit { putBoolean(KEY_EXTERNAL_PLAYER, value) }

    /**
     * Offline mode: the app makes no network requests and starts no downloads; only
     * already-downloaded videos are usable (via the Downloads tab).
     */
    var offlineMode: Boolean
        get() = sp.getBoolean(KEY_OFFLINE, false)
        set(value) = sp.edit { putBoolean(KEY_OFFLINE, value) }

    /** Prefer playing in VLC (best format support) when it is installed. Default on. */
    var preferVlc: Boolean
        get() = sp.getBoolean(KEY_PREFER_VLC, true)
        set(value) = sp.edit { putBoolean(KEY_PREFER_VLC, value) }

    val hasToken: Boolean get() = apiToken.isNotBlank()

    companion object {
        private const val KEY_TOKEN = "rd_api_token"
        private const val KEY_DMM_URL = "dmm_url"
        private const val KEY_TMDB = "tmdb_key"
        private const val KEY_EXTERNAL_PLAYER = "external_player"
        private const val KEY_OFFLINE = "offline_mode"
        private const val KEY_PREFER_VLC = "prefer_vlc"
        const val DEFAULT_DMM_URL = "https://debridmediamanager.com"
    }
}
