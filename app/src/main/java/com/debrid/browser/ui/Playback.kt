package com.debrid.browser.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.debrid.browser.App
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Decides how to play a video: VLC (best codec/container support), a generic external
 * player, or the built-in Media3 player — based on user preference and what's installed.
 */
object Playback {

    const val VLC_PACKAGE = "org.videolan.vlc"

    fun isVlcInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(VLC_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun play(context: Context, uri: Uri, title: String?) {
        val prefs = App.instance.prefs
        when {
            prefs.preferVlc && isVlcInstalled(context) -> startVlc(context, uri, title)
            prefs.preferVlc && !prefs.preferExternalPlayer -> promptInstallVlc(context, uri, title)
            prefs.preferExternalPlayer -> startGenericExternal(context, uri, title)
            else -> startBuiltIn(context, uri, title)
        }
    }

    private fun startVlc(context: Context, uri: Uri, title: String?) {
        try {
            context.startActivity(vlcIntent(uri, title))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Couldn't open VLC, using built-in player", Toast.LENGTH_SHORT).show()
            startBuiltIn(context, uri, title)
        }
    }

    private fun vlcIntent(uri: Uri, title: String?): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setPackage(VLC_PACKAGE)
            setDataAndType(uri, "video/*")
            if (!title.isNullOrBlank()) putExtra("title", title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun startGenericExternal(context: Context, uri: Uri, title: String?) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                if (!title.isNullOrBlank()) putExtra("title", title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No external player found, using built-in", Toast.LENGTH_SHORT).show()
            startBuiltIn(context, uri, title)
        }
    }

    private fun startBuiltIn(context: Context, uri: Uri, title: String?) {
        context.startActivity(PlayerActivity.intent(context, uri.toString(), title))
    }

    private fun promptInstallVlc(context: Context, uri: Uri, title: String?) {
        MaterialAlertDialogBuilder(context)
            .setTitle("VLC not installed")
            .setMessage("VLC plays formats the built-in player can't. Install it, or play now in the built-in player?")
            .setNeutralButton("Cancel", null)
            .setNegativeButton("Built-in") { _, _ -> startBuiltIn(context, uri, title) }
            .setPositiveButton("Install VLC") { _, _ -> openVlcInStore(context) }
            .show()
    }

    fun openVlcInStore(context: Context) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$VLC_PACKAGE"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$VLC_PACKAGE")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
