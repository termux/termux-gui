package com.termux.gui.hbuffers

import android.os.Build

class HBuffers {
    
    companion object {
        
        
        init {
            // Only load if the API level is high enough.
            if (Build.VERSION.SDK_INT >= 26) {
                System.loadLibrary("hbuffers")
            }
        }
        
        
        
        
        
        
        
    }
}