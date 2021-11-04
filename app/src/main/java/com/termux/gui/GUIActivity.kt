package com.termux.gui

import android.app.PictureInPictureParams
import android.os.Bundle
import android.util.Rational
import androidx.appcompat.app.AppCompatActivity
import java.lang.ref.WeakReference
import java.util.*

open class GUIActivity : AppCompatActivity() {

    
    
    
    data class GUITheme(val statusBarColor: Int, val colorPrimary: Int, var windowBackground: Int, val textColor: Int, val colorAccent: Int)
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gui)
        if (intent.getBooleanExtra("pip", false)) {
            println("pip")
            setTheme(R.style.Theme_TermuxGUI_NoAnimation)
            enterPictureInPictureMode()
            overridePendingTransition(0,0)
        } else {
            setTheme(R.style.Theme_TermuxGUI)
        }
        
    }
    
    
}