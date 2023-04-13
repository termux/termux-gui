package com.termux.gui

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonElement
import java.io.Serializable
import java.util.*

/**
 * Base class for custom user Activities.
 */
open class GUIActivity : AppCompatActivity() {


    interface Listener {
        fun onConfigurationChanged(a: GUIActivity, newConfig: Configuration)
        fun onPictureInPictureModeChanged(a: GUIActivity, isInPictureInPictureMode: Boolean)
        fun onUserLeaveHint(a: GUIActivity)
        fun onBackButton(a: GUIActivity)
    }
    
    
    companion object {
        private val TAG: String? = GUIActivity::class.java.canonicalName
        private const val THEME_KEY = "gui_theme"
        private const val DATA_KEY = "gui_data"
        public const val INTERCEPT_KEY = "intercept"
        public const val PIP_KEY = "pip"
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
    
    data class ActivityData(var autopip: Boolean = false, var backEvent: Boolean = false, var secure: Boolean = false) : Serializable
    var data = ActivityData()
    
    data class GUITheme(val statusBarColor: Int, val colorPrimary: Int, var windowBackground: Int, val textColor: Int, val colorAccent: Int) : Serializable
    var listener: Listener? = null
    
    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.log(2, TAG, "oncreate activity")
        if (intent.getBooleanExtra(PIP_KEY, false)) {
            Logger.log(2, TAG, "pip")
            setTheme(R.style.Theme_TermuxGUI_NoAnimation)
            @Suppress("DEPRECATION")
            enterPictureInPictureMode()
            overridePendingTransition(0,0)
        }
        if (intent.getBooleanExtra(INTERCEPT_KEY, false)) {
            data.backEvent = true
        }
        setContentView(R.layout.activity_gui)
        if (savedInstanceState != null) {
            theme = savedInstanceState.getSerializable(THEME_KEY) as? GUITheme?
            val d = savedInstanceState.getSerializable(DATA_KEY) as? ActivityData
            if (d != null) {
                data = d
                setSecure(data.secure)
            }
        }
    }
    
    fun setSecure(secure: Boolean) {
        data.secure = secure
        var bit = 0
        if (secure) {
            bit = WindowManager.LayoutParams.FLAG_SECURE
        }
        window.setFlags(bit, WindowManager.LayoutParams.FLAG_SECURE)
    }
    
    val aid: Int? get() {return intent.dataString?.split('-')?.get(1)?.toInt()}
    val connection: Long? get() {return intent.dataString?.split('-')?.get(0)?.toLong()}

    override fun onBackPressed() {
        if (data.backEvent) {
            listener?.onBackButton(this)
        } else {
            super.onBackPressed()
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
        listener?.onConfigurationChanged(this, newConfig)
    }

    @Suppress("DEPRECATION")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        listener?.onPictureInPictureModeChanged(this, isInPictureInPictureMode)
    }

    @Suppress("DEPRECATION")
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (data.autopip) {
            enterPictureInPictureMode()
        }
        listener?.onUserLeaveHint(this)
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
    
    private fun destroyWebViews(v: View?) {
        if (v == null) return
        if (v is WebView) {
            val p = v.parent as ViewGroup
            p.removeView(v)
            Util.destroyWebView(v)
            return
        }
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                destroyWebViews(v.getChildAt(i))
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        destroyWebViews(findViewById<ViewGroup>(R.id.root))
    }
    
}