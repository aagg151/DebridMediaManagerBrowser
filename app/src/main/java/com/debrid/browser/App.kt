package com.debrid.browser

import android.app.Application
import com.debrid.browser.data.CollectionsStore
import com.debrid.browser.data.Prefs
import com.debrid.browser.data.RealDebridApi
import com.debrid.browser.data.Tmdb

/** App-wide singletons: preferences + Real-Debrid client + local collections + TMDB. */
class App : Application() {

    val prefs: Prefs by lazy { Prefs(this) }
    val api: RealDebridApi by lazy { RealDebridApi(prefs) }
    val collections: CollectionsStore by lazy { CollectionsStore(this) }
    val tmdb: Tmdb by lazy { Tmdb(prefs) }

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
