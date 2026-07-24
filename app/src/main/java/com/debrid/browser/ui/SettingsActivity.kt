package com.debrid.browser.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        binding.tokenInput.setText(prefs.apiToken)
        binding.urlInput.setText(prefs.dmmUrl)
        binding.externalPlayerSwitch.isChecked = prefs.preferExternalPlayer

        binding.saveButton.setOnClickListener { save() }
        binding.testButton.setOnClickListener { testToken() }
    }

    private fun save() {
        prefs.apiToken = binding.tokenInput.text?.toString().orEmpty()
        val url = binding.urlInput.text?.toString()?.trim().orEmpty()
        prefs.dmmUrl = url.ifBlank { Prefs.DEFAULT_DMM_URL }
        prefs.preferExternalPlayer = binding.externalPlayerSwitch.isChecked
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        finish()
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
