package com.termux.gui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.lang.ref.WeakReference
import java.util.*

class GUIActivity : AppCompatActivity() {

    companion object {
        val newActivities: MutableList<WeakReference<GUIActivity>> = Collections.synchronizedList(LinkedList())
    }
    
    
    data class GUITheme(val statusBarColor: Int, val colorPrimary: Int, var windowBackground: Int, val textColor: Int, val colorAccent: Int)
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gui)
        setTheme(R.style.Theme_TermuxGUI)
        newActivities.add(WeakReference(this))
    }
    
    
}