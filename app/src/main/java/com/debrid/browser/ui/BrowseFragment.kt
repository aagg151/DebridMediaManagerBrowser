package com.debrid.browser.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.debrid.browser.App
import com.debrid.browser.databinding.FragmentBrowseBinding
import com.debrid.browser.download.DownloadManagerHelper

/** Full-screen WebView wrapping the Debrid Media Manager site for browse / search / organize. */
class BrowseFragment : Fragment() {

    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowseBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val web = binding.webView
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            userAgentString = userAgentString.replace("; wv", "") // pose as a real browser
        }

        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                binding.progress.visibility = View.VISIBLE
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progress.visibility = View.GONE
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progress.progress = newProgress
            }
        }

        // Route any direct file download triggered inside the page to the native queue.
        web.setDownloadListener { url, _, contentDisposition, _, _ ->
            val name = guessName(url, contentDisposition)
            DownloadManagerHelper.enqueue(requireContext(), url, name)
        }

        binding.swipeRefresh.setOnRefreshListener {
            web.reload()
            binding.swipeRefresh.isRefreshing = false
        }

        if (savedInstanceState == null) {
            web.loadUrl(App.instance.prefs.dmmUrl)
        }
    }

    private fun guessName(url: String, contentDisposition: String?): String {
        contentDisposition?.let {
            Regex("filename=\"?([^\";]+)\"?").find(it)?.groupValues?.get(1)?.let { n ->
                if (n.isNotBlank()) return n
            }
        }
        return url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" }
    }

    /** @return true if the WebView handled Back (navigated within its own history). */
    fun onBackPressed(): Boolean {
        val web = _binding?.webView ?: return false
        return if (web.canGoBack()) { web.goBack(); true } else false
    }

    override fun onDestroyView() {
        _binding?.webView?.destroy()
        _binding = null
        super.onDestroyView()
    }
}
