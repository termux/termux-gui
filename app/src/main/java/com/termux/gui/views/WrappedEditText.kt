package com.termux.gui.views

import android.annotation.SuppressLint
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import com.termux.gui.ConnectionHandler
import java.util.HashMap
import java.util.concurrent.LinkedBlockingQueue

@SuppressLint("ViewConstructor")
class WrappedEditText(private val a: Context, private val aid: Int, private val eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) : androidx.appcompat.widget.AppCompatEditText(a) {
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        return InputWrapper(super.onCreateInputConnection(outAttrs), true)
    }

    override fun isSuggestionsEnabled(): Boolean {
        return false
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        val consumed: Boolean = super.onTextContextMenuItem(id)
        val args = HashMap<String, Any>()
        args["aid"] = aid
        args["id"] = getId()
        println("contextmenu")
        when (id) {
            android.R.id.cut -> {
                println("cut")
                val t = text
                if (t != null) {
                    val selStart = selectionStart
                    val selEnd = selectionEnd
                    val min = 0.coerceAtLeast(selStart.coerceAtMost(selEnd))
                    val max = 0.coerceAtLeast(selStart.coerceAtLeast(selEnd))
                    args["text"] = t.subSequence(min, max)
                    eventQueue.offer(ConnectionHandler.Event("cut", ConnectionHandler.gson.toJsonTree(args)))
                }
            }
            android.R.id.paste -> {
                println("paste")
                val clip = a.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                if (clip.hasPrimaryClip() && clip.primaryClipDescription!!.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                    val item = clip.primaryClip!!.getItemAt(0)
                    args["text"] = item.text
                    eventQueue.offer(ConnectionHandler.Event("paste", ConnectionHandler.gson.toJsonTree(args)))
                }
            }
        }
        return consumed
    }

    private inner class InputWrapper(c: InputConnection?, mutable: Boolean) : InputConnectionWrapper(c, mutable) {
        private val args = HashMap<String, Any>()

        init {
            args["aid"] = aid
        }

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            println(text)
            return super.commitText(text, newCursorPosition)
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            println("delete $beforeLength $afterLength")
            return super.deleteSurroundingText(beforeLength, afterLength)
        }

        override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
            println(text)
            return true
            //return super.setComposingText(text, newCursorPosition)
        }


        override fun sendKeyEvent(e: KeyEvent?): Boolean {
            if (!args.containsKey("id")) {
                args["id"] = id
            }
            if (e != null) {
                args["key"] = e.unicodeChar
                args["shift"] = e.isShiftPressed
                args["ctrl"] = e.isCtrlPressed
                args["caps"] = e.isCapsLockOn
                args["alt"] = e.isAltPressed
                args["fn"] = e.isFunctionPressed
                args["meta"] = e.isMetaPressed
                args["num"] = e.isNumLockOn
                args["code"] = e.keyCode
                eventQueue.offer(ConnectionHandler.Event("input", ConnectionHandler.gson.toJsonTree(args)))
            }
            return true
        }
    }
    
}