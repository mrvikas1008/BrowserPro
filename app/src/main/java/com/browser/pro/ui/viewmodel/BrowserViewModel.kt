package com.browser.pro.ui.viewmodel

import android.app.Application
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

data class TabModel(val id: String, var title: String, var url: String, val webView: WebView)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    val tabs = MutableLiveData<MutableList<TabModel>>(mutableListOf())
    val currentTab = MutableLiveData<TabModel?>()

    fun createNewTab(context: android.content.Context, url: String) {
        val webView = WebView(context)
        val tab = TabModel(java.util.UUID.randomUUID().toString(), "New Tab", url, webView)
        val currentList = tabs.value ?: mutableListOf()
        currentList.add(tab)
        tabs.value = currentList
        currentTab.value = tab
    }
}
 