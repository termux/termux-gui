package com.termux.gui.protocol.shared.v0

import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRenderer(private val l: SurfaceChangeListener, private val b: HardwareBuffer) : GLSurfaceView.Renderer {
    
    
    
    companion object {
        interface SurfaceChangeListener {
            fun onSurfaceChanged(width: Int, height: Int)
        }
    }
    
    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        l.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        
    }
}