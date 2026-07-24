package com.debrid.browser

import java.util.Locale

object Util {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var i = 0
        while (value >= 1024 && i < units.lastIndex) {
            value /= 1024
            i++
        }
        return String.format(Locale.US, if (i == 0) "%.0f %s" else "%.2f %s", value, units[i])
    }
}
