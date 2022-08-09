package com.termux.gui.protocol.shared.v0

import android.webkit.ConsoleMessage
import android.webkit.WebView

interface WebEventListener {
    fun onNavigation(url: String)
    fun onHTTPError(url: String, code: Int)
    fun onReceivedError(url: String, description: String, code: Int)
    fun onRenderProcessGone(v: WebView)
    
    
    fun onProgressChanged(progress: Int)
    fun onConsoleMessage(m: ConsoleMessage)
}