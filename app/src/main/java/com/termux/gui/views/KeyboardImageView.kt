package com.termux.gui.views

import android.content.Context
import android.view.inputmethod.*
import androidx.appcompat.widget.AppCompatImageView
import com.termux.gui.protocol.shared.v0.RawInputConnection

/**
 * ImageView with InputConnection to get KeyEvents.
 */
class KeyboardImageView(c: Context) : AppCompatImageView(c) {
    
    
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = EditorInfo.TYPE_NULL
        return RawInputConnection()
    }
    
    override fun setOnKeyListener(l: OnKeyListener?) {
        super.setOnKeyListener(l)
    }
    
}