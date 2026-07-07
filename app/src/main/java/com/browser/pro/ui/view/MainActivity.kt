package com.browser.pro.ui.view

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.browser.pro.databinding.ActivityMainBinding
import com.browser.pro.ui.viewmodel.BrowserViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: BrowserViewModel
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        fileUploadCallback?.onReceiveValue(uris.toTypedArray())
        fileUploadCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[BrowserViewModel::class.java]

        setupUI()
        setupObservers()

        if (viewModel.tabs.value.isNullOrEmpty()) {
            viewModel.createNewTab(this, "https://www.google.com")
        }
    }

    private fun setupUI() {
        binding.etAddressBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val input = binding.etAddressBar.text.toString().trim()
                if (input.isNotEmpty()) {
                    val url = if (URLUtil.isValidUrl(input)) input else "https://www.google.com/search?q=$input"
                    viewModel.currentTab.value?.webView?.loadUrl(url)
                }
                true
            } else false
        }

        binding.btnBack.setOnClickListener { viewModel.currentTab.value?.webView?.let { if (it.canGoBack()) it.goBack() } }
        binding.btnForward.setOnClickListener { viewModel.currentTab.value?.webView?.let { if (it.canGoForward()) it.goForward() } }
        binding.btnRefresh.setOnClickListener { viewModel.currentTab.value?.webView?.reload() }
        binding.btnHome.setOnClickListener { viewModel.currentTab.value?.webView?.loadUrl("https://www.google.com") }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.currentTab.value?.webView?.reload()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun setupObservers() {
        viewModel.currentTab.observe(this) { tab ->
            tab?.let { setupWebView(it.webView) }
        }
        viewModel.tabs.observe(this) { list ->
            binding.tvTabCount.text = list.size.toString()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView) {
        binding.webViewContainer.removeAllViews()
        binding.webViewContainer.addView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                binding.webProgress.visibility = View.VISIBLE
                url?.let { binding.etAddressBar.setText(it) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.webProgress.visibility = View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.webProgress.progress = newProgress
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback = filePathCallback
                fileChooserLauncher.launch("*/*")
                return true
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimetype)
                addRequestHeader("User-Agent", userAgent)
                setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype))
            }
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(this, "Downloading file...", Toast.LENGTH_SHORT).show()
        }
    }
}
 