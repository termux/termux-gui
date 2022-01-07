package com.termux.gui

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonElement
import java.io.Serializable
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.collections.HashMap

open class GUIActivity : AppCompatActivity() {
    
    
    
    
    companion object {
        private const val THEME_KEY = "gui_theme"
        private const val DATA_KEY = "gui_data"
    }
    val usedIds: TreeSet<Int> = TreeSet()
    init {
        usedIds.add(R.id.root)
    }
    var theme: GUITheme? = null
        set(t) {
            field = t
            if (t != null) {
                window.decorView.background = ColorDrawable(t.windowBackground)
                window.statusBarColor = t.statusBarColor
            }
        }
    
    data class ActivityData(var autopip: Boolean = false) : Serializable
    var data = ActivityData()
    
    data class GUITheme(val statusBarColor: Int, val colorPrimary: Int, var windowBackground: Int, val textColor: Int, val colorAccent: Int) : Serializable
    var eventQueue : LinkedBlockingQueue<ConnectionHandler.Event>? = null

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("oncreate activity")
        if (intent.getBooleanExtra("pip", false)) {
            println("pip")
            setTheme(R.style.Theme_TermuxGUI_NoAnimation)
            @Suppress("DEPRECATION")
            enterPictureInPictureMode()
            overridePendingTransition(0,0)
        }
        setContentView(R.layout.activity_gui)
        if (savedInstanceState != null) {
            theme = savedInstanceState.getSerializable(THEME_KEY) as? GUITheme?
            val d = savedInstanceState.getSerializable(DATA_KEY) as? ActivityData
            if (d != null) {
                data = d
            }
        }
    }
    
    fun configToJson(conf: Configuration?): JsonElement? {
        val c: Configuration = conf ?: resources.configuration ?: return ConnectionHandler.gson.toJsonTree(emptyArray<Any>())
        val m = HashMap<String, Any>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            m["dark_mode"] = c.isNightModeActive
        }
        val l = c.locales.get(0)
        m["country"] = l.country
        m["language"] = l.language
        m["orientation"] = when (c.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> "landscape"
            Configuration.ORIENTATION_PORTRAIT -> "portrait"
            else -> ""
        }
        m["keyboardHidden"] = when (c.keyboardHidden) {
            Configuration.KEYBOARDHIDDEN_NO -> false
            Configuration.KEYBOARDHIDDEN_YES -> true
            else -> true
        }
        m["screenwidth"] = c.screenWidthDp
        m["screenheight"] = c.screenHeightDp
        m["fontscale"] = c.fontScale
        m["density"] = resources.displayMetrics.density
        return ConnectionHandler.gson.toJsonTree(m)
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        eventQueue?.offer(ConnectionHandler.Event("config", configToJson(newConfig)))
    }

    @Suppress("DEPRECATION")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        val ev = eventQueue
        if (ev != null) {
            try {
                ev.add(ConnectionHandler.Event("pipchanged", ConnectionHandler.gson.toJsonTree(isInPictureInPictureMode)))
            } catch (ignored: Exception) {}
        }
    }

    @Suppress("DEPRECATION")
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val ev = eventQueue
        if (ev != null) {
            try {
                ev.add(ConnectionHandler.Event("UserLeaveHint", null))
            } catch (ignored: Exception) {}
            if (data.autopip) {
                enterPictureInPictureMode()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(THEME_KEY, theme)
        outState.putSerializable(DATA_KEY, data)
    }
    
    
    @Suppress("UNCHECKED_CAST")
    fun <T> findViewReimplemented(id: Int) : T? {
        return findViewById(id)
    }
    
    
}