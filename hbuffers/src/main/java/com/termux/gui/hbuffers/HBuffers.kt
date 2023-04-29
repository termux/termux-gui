package com.termux.gui.hbuffers

import android.hardware.HardwareBuffer
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.os.Build
import android.view.Surface

class HBuffers {
    
    companion object {
        
        external fun sendHardwareBuffer(fd: Int, b: HardwareBuffer): Int
        private external fun nativeHardwareBufferToEGLImageKHR(disp: Long, b: HardwareBuffer): Long
        
        init {
            // Only load if the API level is high enough.
            if (Build.VERSION.SDK_INT >= 26) {
                System.loadLibrary("hbuffers")
            }
        }
        
        

        fun hardwareBufferToEGLImageKHR(d: EGLDisplay, b: HardwareBuffer): Long {
            return if (Build.VERSION.SDK_INT >= 26) {
                nativeHardwareBufferToEGLImageKHR(d.nativeHandle, b)
            } else {
                0
            }
        }
        
        
        
    }
}