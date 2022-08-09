package com.termux.gui

import android.os.Bundle

class GUIActivityDialog : GUIActivity() {
    
    companion object {
        public const val CANCELOUTSIDE_KEY = "canceloutside"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(intent.getBooleanExtra(CANCELOUTSIDE_KEY, true))
    }
    
    
    
}
