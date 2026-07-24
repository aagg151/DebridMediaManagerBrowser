package com.debrid.browser.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.debrid.browser.databinding.FragmentDownloadsBinding
import com.debrid.browser.download.DownloadManagerHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Native download queue backed by the system DownloadManager. Live progress; play completed files. */
class DownloadsFragment : Fragment() {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DownloadsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = DownloadsAdapter(
            onPlay = { entry ->
                if (entry.isComplete) {
                    // Prefer a shareable content:// URI so external players (VLC) can read it;
                    // fall back to the stored local URI for the built-in player.
                    val dm = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val uri = dm.getUriForDownloadedFile(entry.id)
                        ?: entry.localUri?.let { Uri.parse(it) }
                    if (uri != null) {
                        Playback.play(requireContext(), uri, entry.title)
                    } else {
                        Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Not finished yet", Toast.LENGTH_SHORT).show()
                }
            },
            onRemove = { entry ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Remove download?")
                    .setMessage(entry.title)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Remove") { _, _ ->
                        DownloadManagerHelper.remove(requireContext(), entry.id)
                        reloadOnce()
                    }
                    .show()
            }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { reloadOnce() }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Poll for live progress only while the tab is visible.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    reloadOnce()
                    delay(1500)
                }
            }
        }
    }

    private fun reloadOnce() {
        val list = DownloadManagerHelper.list(requireContext())
        adapter.submitList(list)
        binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.swipeRefresh.isRefreshing = false
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
