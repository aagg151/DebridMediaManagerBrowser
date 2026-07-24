package com.debrid.browser.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.debrid.browser.databinding.ActivityPlayerBinding

/** Built-in Media3 / ExoPlayer full-screen video player. */
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        savedInstanceState?.let {
            currentPosition = it.getLong(KEY_POS, 0L)
            playWhenReady = it.getBoolean(KEY_PWR, true)
        }
    }

    private fun initPlayer() {
        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            Toast.makeText(this, "No video URL", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        val exo = ExoPlayer.Builder(this).build()
        binding.playerView.player = exo
        binding.playerView.keepScreenOn = true

        val title = intent.getStringExtra(EXTRA_TITLE)
        val item = MediaItem.Builder()
            .setUri(Uri.parse(url))
            .apply { if (!title.isNullOrBlank()) setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder().setTitle(title).build()) }
            .build()

        exo.setMediaItem(item)
        exo.playWhenReady = playWhenReady
        exo.seekTo(currentPosition)
        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(this@PlayerActivity, "Playback error: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
            }
        })
        exo.prepare()
        player = exo
    }

    private fun releasePlayer() {
        player?.let {
            currentPosition = it.currentPosition
            playWhenReady = it.playWhenReady
            it.release()
        }
        player = null
    }

    override fun onStart() {
        super.onStart()
        initPlayer()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        if (player == null) initPlayer()
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_POS, currentPosition)
        outState.putBoolean(KEY_PWR, playWhenReady)
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
    }

    companion object {
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_TITLE = "extra_title"
        private const val KEY_POS = "pos"
        private const val KEY_PWR = "pwr"

        fun intent(context: Context, url: String, title: String?): Intent =
            Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
            }
    }
}
