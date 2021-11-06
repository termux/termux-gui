package com.termux.gui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class GUIActivity : AppCompatActivity() {

    
    
    
    data class GUITheme(val statusBarColor: Int, val colorPrimary: Int, var windowBackground: Int, val textColor: Int, val colorAccent: Int)
    
    
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
    
    
}