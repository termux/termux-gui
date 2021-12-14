package com.termux.gui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.termux.gui.databinding.ActivityGuiConfigBinding

class GUIConfigActivity : AppCompatActivity() {
    
    val settings = Settings()
    var b: ActivityGuiConfigBinding? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings.load(this)
        
        b = ActivityGuiConfigBinding.inflate(layoutInflater)
        b!!.serviceTimeout.setText(settings.timeout.toString(), TextView.BufferType.EDITABLE)
        
        b!!.serviceBackground.isChecked = settings.background
        
        setContentView(b!!.root)
    }


    override fun onPause() {
        super.onPause()
        try {
            settings.timeout = Integer.parseInt(b!!.serviceTimeout.text.toString())
        } catch(_: NumberFormatException) {}
        settings.background = b!!.serviceBackground.isChecked
        settings.save(this)
    }
    
    
}