package com.debrid.browser.ui

import android.app.DownloadManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.debrid.browser.Util
import com.debrid.browser.databinding.ItemDownloadBinding
import com.debrid.browser.download.DownloadManagerHelper.Entry

class DownloadsAdapter(
    private val onPlay: (Entry) -> Unit,
    private val onRemove: (Entry) -> Unit
) : ListAdapter<Entry, DownloadsAdapter.VH>(DIFF) {

    inner class VH(val b: ItemDownloadBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = getItem(position)
        with(holder.b) {
            title.text = e.title
            status.text = statusLabel(e)
            progressBar.progress = e.progress
            progressBar.visibility = if (e.isComplete || e.isFailed) View.GONE else View.VISIBLE
            playButton.visibility = if (e.isComplete) View.VISIBLE else View.GONE
            playButton.setOnClickListener { onPlay(e) }
            removeButton.setOnClickListener { onRemove(e) }
        }
    }

    private fun statusLabel(e: Entry): String = when (e.status) {
        DownloadManager.STATUS_PENDING -> "Pending…"
        DownloadManager.STATUS_RUNNING ->
            "${e.progress}% · ${Util.formatBytes(e.bytesDownloaded)} / ${Util.formatBytes(e.bytesTotal)}"
        DownloadManager.STATUS_PAUSED -> "Paused · ${e.progress}%"
        DownloadManager.STATUS_SUCCESSFUL -> "Completed · ${Util.formatBytes(e.bytesTotal)}"
        DownloadManager.STATUS_FAILED -> "Failed"
        else -> "Unknown"
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Entry>() {
            override fun areItemsTheSame(a: Entry, b: Entry) = a.id == b.id
            override fun areContentsTheSame(a: Entry, b: Entry) = a == b
        }
    }
}
