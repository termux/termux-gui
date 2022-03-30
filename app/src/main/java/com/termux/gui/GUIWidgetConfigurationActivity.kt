package com.termux.gui

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.termux.gui.databinding.WidgetConfigurationLayoutBinding
import java.util.*

class GUIWidgetConfigurationActivity : AppCompatActivity() {

    
    
    var widgetid: Int? = null
    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        widgetid = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val p = GUIWidget.getIDMappingPrefs(this)
        if (widgetid == AppWidgetManager.INVALID_APPWIDGET_ID || widgetid == null || p == null) {
            finishAndRemoveTask()
            return
        }
        var mapped = UUID.randomUUID().toString()
        val already = p.getString(widgetid!!.toString(), null)
        if (already == null)
            p.edit().putInt(mapped, widgetid!!).putString(widgetid!!.toString(), mapped).commit()
        else
            mapped = already
        
        val b = WidgetConfigurationLayoutBinding.inflate(layoutInflater)
        b.widgetId.text = getString(R.string.show_widget_id, mapped)
        b.copy.setOnClickListener {
            getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(getString(R.string.widget_id), mapped))
            Toast.makeText(this, R.string.widget_id_copied, Toast.LENGTH_SHORT).show()
        }
        b.close.setOnClickListener { finishAndRemoveTask() }
        
        
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetid)
        setResult(Activity.RESULT_OK, resultValue)
        setContentView(b.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        val id = widgetid
        if (id != null) {
            AppWidgetManager.getInstance(this).updateAppWidget(id, GUIWidget.defaultViews(this))
        }
    }
    
    
}