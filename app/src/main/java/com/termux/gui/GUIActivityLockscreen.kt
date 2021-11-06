package com.termux.gui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager

class GUIActivityLockscreen : GUIActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT < 27) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        super.onCreate(savedInstanceState)
    }
}