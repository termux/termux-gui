package com.termux.gui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.termux.gui.databinding.JavascriptdialogBinding
import java.util.*

/**
 * The dialog for the user to enable JavaScript in a WebView.
 */
class GUIWebViewJavascriptDialog : AppCompatActivity() {
    
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    companion object {
        /**
         * A request to the Dialog, storing the lock object and the result boolean.
         */
        data class Request(var allow: Boolean? = null, val monitor: Object = Object())

        /**
         * Global for storing the requests.
         */
        val requestMap: MutableMap<String, Request> = Collections.synchronizedMap(HashMap())
        
        
    }

    private var b: JavascriptdialogBinding? = null
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // get the request that caused this Activity to be launched.
        val r = requestMap[intent.dataString]
        if (r == null) {
            finish()
            return
        }
        // set up the layout
        b = JavascriptdialogBinding.inflate(layoutInflater)
        
        b!!.javascriptAllow.setOnClickListener {
            synchronized(r.monitor) {
                r.allow = true
                r.monitor.notifyAll()
            }
            finish()
        }
        b!!.javascriptDeny.setOnClickListener {
            synchronized(r.monitor) {
                r.allow = false
                r.monitor.notifyAll()
            }
            finish()
        }
        
        setContentView(b!!.root)
    }


    override fun onDestroy() {
        super.onDestroy()
        // if the user dismissed the dialog, set the result to false
        if (isFinishing) {
            val r = requestMap[intent.dataString]
            if (r != null) {
                synchronized(r.monitor) {
                    if (r.allow == null) {
                        r.allow = false
                        r.monitor.notifyAll()
                    }
                }
            }
        }
    }
    
    
}