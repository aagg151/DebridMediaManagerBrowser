package com.debrid.browser.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

/** Thin wrapper around the system DownloadManager for queuing + inspecting downloads. */
object DownloadManagerHelper {

    data class Entry(
        val id: Long,
        val title: String,
        val status: Int,          // DownloadManager.STATUS_*
        val bytesDownloaded: Long,
        val bytesTotal: Long,
        val localUri: String?,
        val mediaType: String?
    ) {
        val progress: Int
            get() = if (bytesTotal > 0) ((bytesDownloaded * 100) / bytesTotal).toInt() else 0
        val isComplete: Boolean get() = status == DownloadManager.STATUS_SUCCESSFUL
        val isFailed: Boolean get() = status == DownloadManager.STATUS_FAILED
    }

    /** Queue a direct URL for download into the public Movies directory. Returns the download id. */
    fun enqueue(context: Context, url: String, fileName: String): Long {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName)
            setDescription("Debrid Media Manager Browser")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "DebridBrowser/$fileName")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        return dm.enqueue(request)
    }

    /** Query all downloads (most recent first). */
    fun list(context: Context): List<Entry> {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query()
        val out = mutableListOf<Entry>()
        dm.query(query)?.use { c ->
            val idxId = c.getColumnIndex(DownloadManager.COLUMN_ID)
            val idxTitle = c.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val idxStatus = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val idxSoFar = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val idxTotal = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val idxLocal = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val idxMedia = c.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)
            while (c.moveToNext()) {
                out += Entry(
                    id = c.getLong(idxId),
                    title = c.getString(idxTitle) ?: "download",
                    status = c.getInt(idxStatus),
                    bytesDownloaded = c.getLong(idxSoFar),
                    bytesTotal = c.getLong(idxTotal),
                    localUri = c.getString(idxLocal),
                    mediaType = c.getString(idxMedia)
                )
            }
        }
        return out.sortedByDescending { it.id }
    }

    fun remove(context: Context, id: Long) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.remove(id)
    }
}
