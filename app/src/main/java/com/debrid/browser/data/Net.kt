package com.debrid.browser.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.debrid.browser.App

object Net {

    /** True if a validated internet-capable network is currently available. */
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Offline mode is on, or there is simply no connectivity. */
    fun isOffline(context: Context): Boolean =
        App.instance.prefs.offlineMode || !isOnline(context)

    /** Whether the app is currently allowed to start downloads / make network calls. */
    fun downloadsAllowed(context: Context): Boolean =
        !App.instance.prefs.offlineMode && isOnline(context)
}
