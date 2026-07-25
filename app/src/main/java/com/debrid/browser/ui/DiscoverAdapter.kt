package com.debrid.browser.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.debrid.browser.R
import com.debrid.browser.data.DiscoverItem
import com.debrid.browser.databinding.ItemPosterBinding

class DiscoverAdapter(
    private val onClick: (DiscoverItem) -> Unit
) : ListAdapter<DiscoverItem, DiscoverAdapter.VH>(DIFF) {

    inner class VH(val b: ItemPosterBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemPosterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.b) {
            title.text = item.title
            year.text = item.year
            poster.load(item.posterUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_poster_placeholder)
                error(R.drawable.ic_poster_placeholder)
            }
            root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DiscoverItem>() {
            override fun areItemsTheSame(a: DiscoverItem, b: DiscoverItem) =
                a.tmdbId == b.tmdbId && a.isMovie == b.isMovie
            override fun areContentsTheSame(a: DiscoverItem, b: DiscoverItem) = a == b
        }
    }
}
