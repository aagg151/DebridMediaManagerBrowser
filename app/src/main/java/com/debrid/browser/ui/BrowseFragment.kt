package com.debrid.browser.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.debrid.browser.App
import com.debrid.browser.databinding.FragmentBrowseBinding
import com.debrid.browser.download.DownloadManagerHelper

/** Full-screen WebView wrapping the Debrid Media Manager site for browse / search / organize. */
class BrowseFragment : Fragment() {

    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!

    // Real-Debrid login opens a popup window; we host it on top of the main WebView.
    private var popupWeb: WebView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowseBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        CookieManager.getInstance().setAcceptCookie(true)
        val web = binding.webView
        configure(web)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)

        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                binding.progress.visibility = View.VISIBLE
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progress.visibility = View.GONE
                CookieManager.getInstance().flush()
            }
        }
        web.webChromeClient = mainChromeClient()

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

    @SuppressLint("SetJavaScriptEnabled")
    private fun configure(web: WebView) {
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)      // needed for the RD login popup
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            userAgentString = userAgentString.replace("; wv", "") // pose as a real browser
        }
    }

    private fun mainChromeClient() = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            binding.progress.progress = newProgress
        }

        /** The DMM "Login with Real-Debrid" → "Authorize" step opens a new window. Host it. */
        override fun onCreateWindow(
            view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
        ): Boolean {
            val root = binding.root as FrameLayout
            val popup = WebView(requireContext())
            configure(popup)
            CookieManager.getInstance().setAcceptThirdPartyCookies(popup, true)
            popup.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
            popup.webViewClient = WebViewClient()
            popup.webChromeClient = object : WebChromeClient() {
                // Popup closed (RD approved / cancelled) → remove it and refresh the main page.
                override fun onCloseWindow(window: WebView?) = closePopup(reloadMain = true)
            }
            popup.setDownloadListener { url, _, cd, _, _ ->
                DownloadManagerHelper.enqueue(requireContext(), url, guessName(url, cd))
            }
            closePopup(reloadMain = false) // ensure only one popup at a time
            popupWeb = popup
            root.addView(popup)

            (resultMsg.obj as WebView.WebViewTransport).webView = popup
            resultMsg.sendToTarget()
            return true
        }

        override fun onCloseWindow(window: WebView?) { closePopup(reloadMain = true) }
    }

    private fun closePopup(reloadMain: Boolean) {
        val had = popupWeb != null
        popupWeb?.let {
            (binding.root as FrameLayout).removeView(it)
            it.destroy()
        }
        popupWeb = null
        CookieManager.getInstance().flush()
        // Give DMM's device-flow poll a moment to store credentials, then refresh so the
        // main page shows the logged-in state. (Pull-to-refresh also works if needed.)
        if (had && reloadMain) _binding?.webView?.postDelayed({ _binding?.webView?.reload() }, 3000)
    }

    private fun guessName(url: String, contentDisposition: String?): String {
        contentDisposition?.let {
            Regex("filename=\"?([^\";]+)\"?").find(it)?.groupValues?.get(1)?.let { n ->
                if (n.isNotBlank()) return n
            }
        }
        return url.substringAfterLast('/').substringBefore('?').ifBlank { "download.bin" }
    }

    /** @return true if Back was handled (closed a popup or navigated WebView history). */
    fun onBackPressed(): Boolean {
        if (popupWeb != null) { closePopup(reloadMain = true); return true }
        val web = _binding?.webView ?: return false
        return if (web.canGoBack()) { web.goBack(); true } else false
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroyView() {
        popupWeb?.destroy()
        popupWeb = null
        _binding?.webView?.destroy()
        _binding = null
        super.onDestroyView()
    }
}
