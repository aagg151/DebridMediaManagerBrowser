package com.debrid.browser

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.debrid.browser.databinding.ActivityMainBinding
import com.debrid.browser.ui.BrowseFragment
import com.debrid.browser.ui.DownloadsFragment
import com.debrid.browser.ui.LibraryFragment
import com.debrid.browser.ui.SettingsActivity

/** Implemented by tabs that need to react immediately when Offline mode is toggled. */
interface OfflineAware {
    fun onOfflineChanged(offline: Boolean)
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val browse by lazy { BrowseFragment() }
    private val library by lazy { LibraryFragment() }
    private val downloads by lazy { DownloadsFragment() }
    private var current: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Edge-to-edge (default on Android 15 / targetSdk 35): keep the toolbar below the
        // status bar and the bottom nav above the gesture/navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updatePadding(top = bars.top)
            binding.bottomNav.updatePadding(bottom = bars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100
            )
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_browse -> show(browse, R.string.tab_browse)
                R.id.nav_library -> show(library, R.string.tab_library)
                R.id.nav_downloads -> show(downloads, R.string.tab_downloads)
                else -> false
            }
        }

        // Offline-mode toggle.
        binding.offlineSwitch.isChecked = App.instance.prefs.offlineMode
        binding.offlineSwitch.setOnCheckedChangeListener { _, checked ->
            App.instance.prefs.offlineMode = checked
            Toast.makeText(
                this,
                getString(if (checked) R.string.offline_on_toast else R.string.offline_off_toast),
                Toast.LENGTH_SHORT
            ).show()
            (current as? OfflineAware)?.onOfflineChanged(checked)
            if (checked) goToDownloads()
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId =
                if (App.instance.prefs.offlineMode) R.id.nav_downloads else R.id.nav_browse
        }

        // Let the Browse tab's WebView consume Back for in-page navigation.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (current === browse && browse.onBackPressed()) return
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })
    }

    fun goToDownloads() {
        binding.bottomNav.selectedItemId = R.id.nav_downloads
    }

    private fun show(fragment: Fragment, titleRes: Int): Boolean {
        if (current === fragment) return true
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        current = fragment
        supportActionBar?.setTitle(titleRes)
        return true
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
