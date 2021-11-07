package com.termux.gui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.io.DataOutputStream
import java.util.concurrent.LinkedBlockingQueue

open class GUIActivity : AppCompatActivity() {

    
    
    
    data class GUITheme(val statusBarColor: Int, val colorPrimary: Int, var windowBackground: Int, val textColor: Int, val colorAccent: Int)
    var eventQueue : LinkedBlockingQueue<String>? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gui)
        if (intent.getBooleanExtra("pip", false)) {
            println("pip")
            setTheme(R.style.Theme_TermuxGUI_NoAnimation)
            @Suppress("DEPRECATION")
            enterPictureInPictureMode()
            overridePendingTransition(0,0)
        } else {
            setTheme(R.style.Theme_TermuxGUI)
        }
        
    }
    
    @Suppress("DEPRECATION")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        val ev = eventQueue
        if (ev != null) {
            try {
                ev.add(ConnectionHandler.gson.toJson(ConnectionHandler.Event("pipchanged", ConnectionHandler.gson.toJsonTree(isInPictureInPictureMode))))
            } catch (ignored: Exception) {}
        }
    }


    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val ev = eventQueue
        if (ev != null) {
            try {
                ev.add(ConnectionHandler.gson.toJson(ConnectionHandler.Event("UserLeaveHint", null)))
            } catch (ignored: Exception) {}
        }
    }
    
}