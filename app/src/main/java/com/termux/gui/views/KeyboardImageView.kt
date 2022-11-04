package com.termux.gui.views

import android.content.Context
import android.view.inputmethod.*
import androidx.appcompat.widget.AppCompatImageView
import com.termux.gui.protocol.shared.v0.RawInputConnection

class KeyboardImageView(c: Context) : AppCompatImageView(c) {
    
    private var con: RawInputConnection? = null
    
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = EditorInfo.TYPE_NULL
        val c = RawInputConnection(this)
        con = c
        return c
    }
    
    override fun setOnKeyListener(l: OnKeyListener?) {
        con?.setOnKeyListener(l)
        super.setOnKeyListener(l)
    }
    
}