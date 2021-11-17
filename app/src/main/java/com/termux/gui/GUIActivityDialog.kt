package com.termux.gui

import android.os.Bundle

class GUIActivityDialog : GUIActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(intent.getBooleanExtra("canceloutside", true))
    }
    
    
    
}
