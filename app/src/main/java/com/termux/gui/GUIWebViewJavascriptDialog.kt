package com.termux.gui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.termux.gui.databinding.JavascriptdialogBinding
import java.util.*

class GUIWebViewJavascriptDialog : AppCompatActivity() {
    
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    companion object {
        data class Request(var allow: Boolean? = null, val monitor: Object = Object())
        
        val requestMap: MutableMap<String, Request> = Collections.synchronizedMap(HashMap())
        
        
    }

    private var b: JavascriptdialogBinding? = null
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val r = requestMap[intent.dataString]
        if (r == null) {
            finish()
            return
        }
        
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