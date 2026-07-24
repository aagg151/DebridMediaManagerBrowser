package com.debrid.browser

import android.app.Application
import com.debrid.browser.data.Prefs
import com.debrid.browser.data.RealDebridApi

/** App-wide singletons: preferences + Real-Debrid client. */
class App : Application() {

    val prefs: Prefs by lazy { Prefs(this) }
    val api: RealDebridApi by lazy { RealDebridApi(prefs) }

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
