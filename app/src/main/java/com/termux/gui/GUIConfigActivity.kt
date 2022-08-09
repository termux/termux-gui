package com.termux.gui

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NotificationManagerCompat
import com.termux.gui.databinding.ActivityGuiConfigBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class GUIConfigActivity : AppCompatActivity() {
    
    private var b: ActivityGuiConfigBinding? = null
    
    companion object {
        private const val OPEN_FILE = 1
    }
    
    @Suppress("DEPRECATION")
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
        
        val open = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
            if (uri != null) {
                val app = applicationContext
                Thread {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        ProcessBuilder().command("logcat", "-d").start().inputStream.use { inp ->
                            inp.copyTo(out)
                        }
                    }
                    runBlocking { 
                        launch(Dispatchers.Main) {
                            Toast.makeText(app, R.string.logcat_written, Toast.LENGTH_SHORT).show()
                        }
                    }
                    // also clear logcat after dumping
                    ProcessBuilder().command("logcat", "-c").start()
                }.start()
                
            }
        }

        b!!.logcat.setOnClickListener {
            open.launch("Termux:GUI logcat")
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