package com.termux.gui.protocol.shared.v0

import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.*

class RawInputConnection(private val keyListener: View.OnKeyListener) : InputConnection {
    
    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
        return null
    }

    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? {
        return null
    }

    override fun getSelectedText(flags: Int): CharSequence? {
        return null
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        return 0
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
        return null
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        return false
    }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
        return false
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        return false
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        return false
    }

    override fun finishComposingText(): Boolean {
        return false
    }

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        return false
    }

    override fun commitCompletion(text: CompletionInfo?): Boolean {
        return false
    }

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean {
        return false
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        return false
    }

    override fun performEditorAction(editorAction: Int): Boolean {
        return false
    }

    override fun performContextMenuAction(id: Int): Boolean {
        return false
    }

    override fun beginBatchEdit(): Boolean {
        return false
    }

    override fun endBatchEdit(): Boolean {
        return false
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        keyListener.onKey(null, KeyEvent.KEYCODE_UNKNOWN, event)
        return true
    }

    override fun clearMetaKeyStates(states: Int): Boolean {
        return true
    }

    override fun reportFullscreenMode(enabled: Boolean): Boolean {
        return false
    }

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
        return false
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        return false
    }

    override fun getHandler(): Handler? {
        return null
    }

    override fun closeConnection() {}

    override fun commitContent(inputContentInfo: InputContentInfo, flags: Int, opts: Bundle?): Boolean {
        return false
    }
}