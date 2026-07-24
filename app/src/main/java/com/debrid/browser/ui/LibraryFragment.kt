package com.debrid.browser.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.debrid.browser.App
import com.debrid.browser.Util
import com.debrid.browser.data.TorrentFile
import com.debrid.browser.data.TorrentInfo
import com.debrid.browser.data.TorrentItem
import com.debrid.browser.databinding.FragmentLibraryBinding
import com.debrid.browser.download.DownloadManagerHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** Native Real-Debrid library: browse torrents, then play / download / delete individual files. */
class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val api get() = App.instance.api
    private lateinit var adapter: LibraryAdapter

    private var all: List<TorrentItem> = emptyList()
    private var sortMode = SortMode.DATE

    private enum class SortMode { DATE, NAME, SIZE }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = LibraryAdapter(onOpen = ::openTorrent, onDelete = ::confirmDelete)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { refresh() }

        binding.filterInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = applyFilterSort()
        })

        binding.sortButton.setOnClickListener { cycleSort() }
        binding.retryButton.setOnClickListener { refresh() }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (all.isEmpty()) refresh()
    }

    private fun refresh() {
        if (!App.instance.prefs.hasToken) {
            showEmpty(getString(com.debrid.browser.R.string.no_token_hint))
            binding.swipeRefresh.isRefreshing = false
            return
        }
        binding.swipeRefresh.isRefreshing = true
        binding.emptyView.visibility = View.GONE
        lifecycleScope.launch {
            try {
                all = api.getTorrents()
                applyFilterSort()
                if (all.isEmpty()) showEmpty(getString(com.debrid.browser.R.string.library_empty))
            } catch (e: Exception) {
                showEmpty(e.message ?: "Failed to load library")
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun cycleSort() {
        sortMode = when (sortMode) {
            SortMode.DATE -> SortMode.NAME
            SortMode.NAME -> SortMode.SIZE
            SortMode.SIZE -> SortMode.DATE
        }
        binding.sortButton.text = getString(com.debrid.browser.R.string.sort_prefix, sortMode.name)
        applyFilterSort()
    }

    private fun applyFilterSort() {
        val q = binding.filterInput.text?.toString()?.trim()?.lowercase().orEmpty()
        var list = if (q.isBlank()) all else all.filter { it.filename.lowercase().contains(q) }
        list = when (sortMode) {
            SortMode.DATE -> list.sortedByDescending { it.added }
            SortMode.NAME -> list.sortedBy { it.filename.lowercase() }
            SortMode.SIZE -> list.sortedByDescending { it.bytes }
        }
        adapter.submitList(list)
        binding.emptyView.visibility = if (list.isEmpty() && all.isNotEmpty()) View.GONE else binding.emptyView.visibility
    }

    private fun showEmpty(msg: String) {
        adapter.submitList(emptyList())
        binding.emptyText.text = msg
        binding.emptyView.visibility = View.VISIBLE
    }

    // ---- Actions ----------------------------------------------------------

    private fun openTorrent(item: TorrentItem) {
        if (!item.isReady) {
            Toast.makeText(requireContext(), "Not ready yet (${item.status}, ${item.progress}%)", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val info = api.getTorrentInfo(item.id)
                val selected = info.files.filter { it.selected }
                if (selected.isEmpty()) {
                    Toast.makeText(requireContext(), "No files available", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                if (selected.size == 1) chooseAction(info, selected.first())
                else showFilePicker(info, selected)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showFilePicker(info: TorrentInfo, files: List<TorrentFile>) {
        val labels = files.map { "${it.displayName}  (${Util.formatBytes(it.bytes)})" }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select a file")
            .setItems(labels) { _, which -> chooseAction(info, files[which]) }
            .show()
    }

    private fun chooseAction(info: TorrentInfo, file: TorrentFile) {
        val options = arrayOf("Play", "Download")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(file.displayName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> resolveAnd(info, file) { url, name -> play(url, name) }
                    1 -> resolveAnd(info, file) { url, name ->
                        DownloadManagerHelper.enqueue(requireContext(), url, name)
                        Toast.makeText(requireContext(), "Download started: $name", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    /** Unrestrict the file's restricted link into a direct URL, then run [action]. */
    private fun resolveAnd(info: TorrentInfo, file: TorrentFile, action: (url: String, name: String) -> Unit) {
        val restricted = api.restrictedLinkFor(info, file)
        if (restricted == null) {
            Toast.makeText(requireContext(), "No link for this file", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val direct = api.unrestrict(restricted)
                val name = direct.filename.ifBlank { file.displayName }
                action(direct.download, name)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun play(url: String, name: String) {
        if (App.instance.prefs.preferExternalPlayer) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(url), "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
                return
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "No external player found, using built-in", Toast.LENGTH_SHORT).show()
            }
        }
        startActivity(PlayerActivity.intent(requireContext(), url, name))
    }

    private fun confirmDelete(item: TorrentItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete torrent?")
            .setMessage(item.filename)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        api.deleteTorrent(item.id)
                        all = all.filterNot { it.id == item.id }
                        applyFilterSort()
                        Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
