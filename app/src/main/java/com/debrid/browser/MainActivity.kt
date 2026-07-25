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
import com.debrid.browser.ui.DiscoverFragment
import com.debrid.browser.ui.DownloadsFragment
import com.debrid.browser.ui.LibraryFragment
import com.debrid.browser.ui.SettingsActivity

/** Implemented by tabs that need to react immediately when Offline mode is toggled. */
interface OfflineAware {
    fun onOfflineChanged(offline: Boolean)
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val discover by lazy { DiscoverFragment() }
    private val browse by lazy { BrowseFragment() }
    private val library by lazy { LibraryFragment() }
    private val downloads by lazy { DownloadsFragment() }
    private var current: Fragment? = null

    private var currentTabId = R.id.nav_discover
    private val tabBackStack = ArrayDeque<Int>()
    private var suppressBackStackPush = false

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
            if (!suppressBackStackPush && item.itemId != currentTabId) {
                // Record the tab we're leaving so Back can return to it.
                tabBackStack.removeAll { it == item.itemId }
                tabBackStack.addLast(currentTabId)
            }
            currentTabId = item.itemId
            when (item.itemId) {
                R.id.nav_discover -> show(discover, "discover", R.string.tab_discover)
                R.id.nav_browse -> show(browse, "browse", R.string.tab_browse)
                R.id.nav_library -> show(library, "library", R.string.tab_library)
                R.id.nav_downloads -> show(downloads, "downloads", R.string.tab_downloads)
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
            val defaultTab =
                if (App.instance.prefs.offlineMode) R.id.nav_downloads else R.id.nav_discover
            currentTabId = defaultTab
            suppressBackStackPush = true
            binding.bottomNav.selectedItemId = defaultTab
            suppressBackStackPush = false
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 1) Walk the Browse WebView's own history (and close any login popup).
                if (currentTabId == R.id.nav_browse &&
                    (current as? BrowseFragment)?.onBackPressed() == true
                ) return
                // 2) Otherwise return to the previous tab (e.g. Discover), keeping its state.
                if (tabBackStack.isNotEmpty()) {
                    val prev = tabBackStack.removeLast()
                    suppressBackStackPush = true
                    binding.bottomNav.selectedItemId = prev
                    suppressBackStackPush = false
                    return
                }
                // 3) Nothing left → default behavior (leave the app).
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })
    }

    fun goToDownloads() {
        binding.bottomNav.selectedItemId = R.id.nav_downloads
    }

    /** Switch to the Browse (DMM) tab and load the given URL — used by Discover deep-links. */
    fun openInBrowse(url: String) {
        binding.bottomNav.selectedItemId = R.id.nav_browse
        (current as? BrowseFragment)?.loadUrl(url)
    }

    /**
     * Add/hide/show (never replace) so each tab's view — notably the Browse WebView and its
     * page/scroll/history — is retained when switching tabs. Reuses fragments restored by the
     * FragmentManager after recreation to avoid duplicates.
     */
    private fun show(fragment: Fragment, tag: String, titleRes: Int): Boolean {
        val fm = supportFragmentManager
        val existing = fm.findFragmentByTag(tag)
        val target = existing ?: fragment
        if (current === target) {
            supportActionBar?.setTitle(titleRes)
            return true
        }
        val tx = fm.beginTransaction()
        current?.let { tx.hide(it) }
        if (existing == null) tx.add(R.id.fragmentContainer, fragment, tag)
        tx.show(target)
        tx.commitNow()
        current = target
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
