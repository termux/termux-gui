package com.termux.gui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.termux.gui.databinding.ActivityGuiConfigBinding

class GUIConfigActivity : AppCompatActivity() {
    
    private var b: ActivityGuiConfigBinding? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        b = ActivityGuiConfigBinding.inflate(layoutInflater)
        b!!.serviceTimeout.setText(Settings.instance.timeout.toString(), TextView.BufferType.EDITABLE)
        b!!.serviceBackground.isChecked = Settings.instance.background
        b!!.loglevel.setText(Settings.instance.loglevel.toString(), TextView.BufferType.EDITABLE)
        b!!.channelDelete.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val m = NotificationManagerCompat.from(this)
                for (c in m.notificationChannels) {
                    m.deleteNotificationChannel(c.id)
                }
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            b!!.channelDelete.visibility = View.GONE
            b!!.channelDeleteDesc.visibility = View.GONE
        }
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