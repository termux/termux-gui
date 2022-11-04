package com.termux.gui.hbuffers

import android.hardware.HardwareBuffer
import android.os.Build
import android.view.Surface

class HBuffers {
    
    companion object {
        
        private external fun nativeRenderBuffer(surface: Surface, buffer: HardwareBuffer): Boolean
        private external fun nativeCleanup()
        
        init {
            // Only load if the API level is high enough.
            if (Build.VERSION.SDK_INT >= 26) {
                System.loadLibrary("hbuffers")
            }
        }
        
        fun renderBuffer(surface: Surface, buffer: HardwareBuffer): Boolean {
            return if (Build.VERSION.SDK_INT >= 26) {
                nativeRenderBuffer(surface, buffer)
            } else {
                false
            }
        }

        fun cleanupRenderer() {
            if (Build.VERSION.SDK_INT >= 26) {
                nativeCleanup()
            }
        }
        
        
        
    }
}