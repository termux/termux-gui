package com.termux.gui.views

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.termux.gui.protocol.shared.v0.RawInputConnection

class KeyboardGLSurfaceView(c: Context) : GLSurfaceView(c) {
    
    init {
        
        setEGLContextClientVersion(2)
    }
    
    
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = EditorInfo.TYPE_NULL
        return RawInputConnection()
    }
    
    override fun setOnKeyListener(l: OnKeyListener?) {
        super.setOnKeyListener(l)
    }
    
}