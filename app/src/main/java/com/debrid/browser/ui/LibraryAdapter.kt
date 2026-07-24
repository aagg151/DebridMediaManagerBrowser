package com.debrid.browser.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.debrid.browser.Util
import com.debrid.browser.data.TorrentItem
import com.debrid.browser.databinding.ItemLibraryBinding

class LibraryAdapter(
    private val onOpen: (TorrentItem) -> Unit,
    private val onDelete: (TorrentItem) -> Unit
) : ListAdapter<TorrentItem, LibraryAdapter.VH>(DIFF) {

    inner class VH(val b: ItemLibraryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemLibraryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.b) {
            title.text = item.filename
            val statusText = if (item.isReady) "Ready" else "${item.status} · ${item.progress}%"
            subtitle.text = "$statusText · ${Util.formatBytes(item.bytes)}"
            statusDot.setColorFilter(
                if (item.isReady) 0xFF2E7D32.toInt() else 0xFFF9A825.toInt()
            )
            root.setOnClickListener { onOpen(item) }
            deleteButton.setOnClickListener { onDelete(item) }
            root.isEnabled = true
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TorrentItem>() {
            override fun areItemsTheSame(a: TorrentItem, b: TorrentItem) = a.id == b.id
            override fun areContentsTheSame(a: TorrentItem, b: TorrentItem) = a == b
        }
    }
}
