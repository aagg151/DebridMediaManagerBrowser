package com.debrid.browser.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.debrid.browser.App
import com.debrid.browser.MainActivity
import com.debrid.browser.OfflineAware
import com.debrid.browser.R
import com.debrid.browser.data.DiscoverItem
import com.debrid.browser.data.Genre
import com.debrid.browser.data.Net
import com.debrid.browser.databinding.FragmentDiscoverBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Native browse/search backed by TMDB — filter by type, genre and language. */
class DiscoverFragment : Fragment(), OfflineAware {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private val tmdb get() = App.instance.tmdb
    private lateinit var adapter: DiscoverAdapter

    private var isMovie = true
    private var genres: List<Genre> = emptyList()
    private var genreId: Int? = null
    private var language: String? = null   // "en", "hi", or null (All)
    private var query: String = ""

    private val items = mutableListOf<DiscoverItem>()
    private var page = 1
    private var loading = false
    private var canLoadMore = true
    private var loadJob: Job? = null
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = DiscoverAdapter(::openTitle)
        val glm = GridLayoutManager(requireContext(), 3)
        binding.recycler.layoutManager = glm
        binding.recycler.adapter = adapter
        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val last = glm.findLastVisibleItemPosition()
                if (!loading && canLoadMore && last >= adapter.itemCount - 6) loadMore()
            }
        })

        binding.typeGroup.check(R.id.typeMovies)
        binding.typeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            isMovie = checkedId == R.id.typeMovies
            genreId = null
            loadGenres()
            reload()
        }

        setupLanguageDropdown()

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString()?.trim().orEmpty()
                // Debounce typing before firing a search.
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(400)
                    reload()
                }
            }
        })

        binding.swipeRefresh.setOnRefreshListener { reload() }
        binding.emptyButton.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        start()
    }

    override fun onResume() {
        super.onResume()
        // Picking up a key just added in Settings, or coming back online.
        if (items.isEmpty()) start()
    }

    override fun onOfflineChanged(offline: Boolean) = start()

    private fun start() {
        if (!App.instance.prefs.hasTmdbKey) {
            showMessage(getString(R.string.discover_no_key), showSettings = true)
            return
        }
        if (Net.isOffline(requireContext())) {
            showMessage(getString(R.string.offline_no_connection), showSettings = false)
            return
        }
        if (genres.isEmpty()) loadGenres()
        reload()
    }

    private fun setupLanguageDropdown() {
        val labels = listOf(
            getString(R.string.all), getString(R.string.lang_english), getString(R.string.lang_hindi)
        )
        val codes = listOf<String?>(null, "en", "hi")
        binding.langInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
        )
        binding.langInput.setText(labels[0], false)
        binding.langInput.setOnItemClickListener { _, _, pos, _ ->
            language = codes[pos]
            reload()
        }
    }

    private fun loadGenres() {
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                genres = tmdb.genres(isMovie)
                val labels = listOf(getString(R.string.all)) + genres.map { it.name }
                binding.genreInput.setAdapter(
                    ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
                )
                binding.genreInput.setText(getString(R.string.all), false)
                binding.genreInput.setOnItemClickListener { _, _, pos, _ ->
                    genreId = if (pos == 0) null else genres[pos - 1].id
                    reload()
                }
            } catch (_: Exception) { /* genres are optional; ignore */ }
        }
    }

    private fun reload() {
        val b = _binding ?: return
        page = 1
        canLoadMore = true
        items.clear()
        adapter.submitList(emptyList())
        b.emptyView.visibility = View.GONE
        load(first = true)
    }

    private fun loadMore() {
        page += 1
        load(first = false)
    }

    private fun load(first: Boolean) {
        if (!App.instance.prefs.hasTmdbKey) { showMessage(getString(R.string.discover_no_key), true); return }
        loading = true
        if (first) binding.progress.visibility = View.VISIBLE
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = if (query.isNotBlank()) tmdb.search(isMovie, query, page)
                else tmdb.discover(isMovie, genreId, language, page)
                if (result.isEmpty()) canLoadMore = false
                items.addAll(result)
                adapter.submitList(items.toList())
                if (items.isEmpty()) showMessage(getString(R.string.discover_empty), showSettings = false)
                else binding.emptyView.visibility = View.GONE
            } catch (e: Exception) {
                if (first && items.isEmpty()) showMessage(e.message ?: "TMDB error", showSettings = false)
                else Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            } finally {
                loading = false
                _binding?.progress?.visibility = View.GONE
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun showMessage(msg: String, showSettings: Boolean) {
        _binding?.let { b ->
            adapter.submitList(emptyList())
            b.emptyText.text = msg
            b.emptyButton.visibility = if (showSettings) View.VISIBLE else View.GONE
            b.emptyView.visibility = View.VISIBLE
            b.progress.visibility = View.GONE
            b.swipeRefresh.isRefreshing = false
        }
    }

    /** Resolve the IMDb id and open the matching DMM page in the Browse tab to grab it. */
    private fun openTitle(item: DiscoverItem) {
        Toast.makeText(requireContext(), "Opening ${item.title}…", Toast.LENGTH_SHORT).show()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val imdb = tmdb.imdbId(item)
                if (imdb == null) {
                    Toast.makeText(requireContext(), "No IMDb match for this title", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val base = App.instance.prefs.dmmUrl.trimEnd('/')
                val path = if (item.isMovie) "movie" else "show"
                (activity as? MainActivity)?.openInBrowse("$base/$path/$imdb")
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        searchJob?.cancel()
        _binding = null
        super.onDestroyView()
    }
}
