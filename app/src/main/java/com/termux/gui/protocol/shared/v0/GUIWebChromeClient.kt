package com.termux.gui.protocol.shared.v0

import android.net.Uri
import android.os.Message
import android.webkit.*

@Suppress("OVERRIDE_DEPRECATION")
class GUIWebChromeClient(val l: WebEventListener) : WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        l.onProgressChanged(newProgress)
    }

    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
    }

    override fun onRequestFocus(view: WebView?) {
        super.onRequestFocus(view)
    }

    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
    }

    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        return super.onJsAlert(view, url, message, result)
    }

    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        return super.onJsConfirm(view, url, message, result)
    }

    override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
        return super.onJsPrompt(view, url, message, defaultValue, result)
    }

    override fun onJsBeforeUnload(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        return super.onJsBeforeUnload(view, url, message, result)
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
        callback?.invoke(origin, false, true)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        request?.deny()
    }

    
    override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
        onConsoleMessage(ConsoleMessage(message,sourceID, lineNumber, ConsoleMessage.MessageLevel.LOG))
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (consoleMessage != null)
            l.onConsoleMessage(consoleMessage)
        return true
    }

    override fun getVisitedHistory(callback: ValueCallback<Array<String>>?) {
        super.getVisitedHistory(callback)
    }

    override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
        filePathCallback?.onReceiveValue(null)
        return true
    }
}