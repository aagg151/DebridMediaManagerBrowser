package com.debrid.browser.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.debrid.browser.App
import com.debrid.browser.data.Prefs
import com.debrid.browser.databinding.ActivitySettingsBinding
import kotlinx.coroutines.launch

/** Real-Debrid token + site URL + player preference. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val prefs get() = App.instance.prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updatePadding(top = bars.top)
            binding.root.updatePadding(bottom = bars.bottom)
            insets
        }

        binding.tokenInput.setText(prefs.apiToken)
        binding.urlInput.setText(prefs.dmmUrl)
        binding.vlcSwitch.isChecked = prefs.preferVlc
        binding.externalPlayerSwitch.isChecked = prefs.preferExternalPlayer

        binding.saveButton.setOnClickListener { save() }
        binding.testButton.setOnClickListener { testToken() }
        binding.getTokenButton.setOnClickListener { openTokenPage() }
        binding.pasteButton.setOnClickListener { pasteFromClipboard() }
        binding.installVlcButton.setOnClickListener { Playback.openVlcInStore(this) }
    }

    private fun save() {
        prefs.apiToken = binding.tokenInput.text?.toString().orEmpty()
        val url = binding.urlInput.text?.toString()?.trim().orEmpty()
        prefs.dmmUrl = url.ifBlank { Prefs.DEFAULT_DMM_URL }
        prefs.preferVlc = binding.vlcSwitch.isChecked
        prefs.preferExternalPlayer = binding.externalPlayerSwitch.isChecked
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun openTokenPage() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://real-debrid.com/apitoken")))
            Toast.makeText(this, "Copy the token, then return and tap Paste", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "No browser found: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun pasteFromClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty()
        if (text.isBlank()) {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            return
        }
        binding.tokenInput.setText(text)
        binding.tokenInput.setSelection(text.length)
        Toast.makeText(this, "Pasted — now tap Test connection or Save", Toast.LENGTH_SHORT).show()
    }

    private fun testToken() {
        val token = binding.tokenInput.text?.toString()?.trim().orEmpty()
        if (token.isBlank()) {
            Toast.makeText(this, "Enter a token first", Toast.LENGTH_SHORT).show()
            return
        }
        // Persist before testing so the API client picks it up.
        prefs.apiToken = token
        binding.testButton.isEnabled = false
        lifecycleScope.launch {
            try {
                val user = App.instance.api.getUser()
                val kind = if (user.premium) "premium" else "free"
                Toast.makeText(
                    this@SettingsActivity,
                    "Connected as ${user.username} ($kind), expires ${user.expiration}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.testButton.isEnabled = true
            }
        }
    }
}
