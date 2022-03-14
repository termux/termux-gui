package com.termux.gui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.termux.gui.databinding.ActivityGuiConfigBinding

class GUIConfigActivity : AppCompatActivity() {
    
    private var b: ActivityGuiConfigBinding? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        b = ActivityGuiConfigBinding.inflate(layoutInflater)
        b!!.serviceTimeout.setText(Settings.instance.timeout.toString(), TextView.BufferType.EDITABLE)
        b!!.serviceBackground.isChecked = Settings.instance.background
        b!!.loglevel.setText(Settings.instance.loglevel.toString(), TextView.BufferType.EDITABLE)
        setContentView(b!!.root)
    }


    override fun onPause() {
        super.onPause()
        try {
            Settings.instance.timeout = Integer.parseInt(b!!.serviceTimeout.text.toString())
        } catch(_: NumberFormatException) {}
        try {
            Settings.instance.loglevel = Integer.parseInt(b!!.loglevel.text.toString())
        } catch(_: NumberFormatException) {}
        Settings.instance.background = b!!.serviceBackground.isChecked
        Settings.instance.save(this)
    }
    
    
}