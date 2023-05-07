package com.termux.gui.protocol.shared.v0

import android.content.Context
import android.graphics.Bitmap
import android.os.SharedMemory
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.RemoteViews
import com.termux.gui.GUIActivity
import com.termux.gui.R
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.collections.HashMap
import kotlin.reflect.KClass

/**
 * A collection of data classes used by the procotol implementations.
 */
class DataClasses {
    /**
     * A shared memory bitmap buffer.
     */
    data class SharedBuffer(
        /**
         * The Bitmap the buffer contents will be blitted into.
         */
        val btm: Bitmap,
        /**
         * The shared memory object, if supported by the Android version.
         */
        val shm: SharedMemory?,
        /**
         * The buffer of the shared memory.
         */
        val buff: ByteBuffer,
        /**
         * The shared memory file descriptor, if SharedMemory isn't supported by the Android version.
         */
        val fd: Int?)

    /**
     * A tree of remote Views.
     */
    data class RemoteLayoutRepresentation(
        var root: RemoteViews?,
        val viewCount: HashMap<KClass<*>, Int> = HashMap())

    /**
     * State of an Activity
     */
    data class ActivityState(
        /**
         * The id of the connection thread
         */
        val connectionID: Long) {
        /**
         * The underlying Activity.
         */
        @Volatile private var _a: GUIActivity? = null

        /**
         * Whether onSaveInstanceState has already been called on teh Activity.
         */
        @Volatile var saved: Boolean = false

        /**
         * A Queue of pending Runnables for the Activity.
         */
        val queued: LinkedBlockingQueue<(activity: GUIActivity) -> Unit> = LinkedBlockingQueue<(activity: GUIActivity) -> Unit>(100)

        /**
         * Set the Activity, if the connection ID lines up with the connection ID of this state object.
         */
        var a: GUIActivity? get() {return _a} set(value) {
            if (value != null) {
                if (value.connection == connectionID) {
                    _a = value
                } else {
                    Log.d("AID", "connection doesn't match")
                }
            } else {
                _a = null
            }
        }
    }

    /**
     * Representing an overlay Activity.
     */
    data class Overlay(val context: Context) {
        val usedIds: TreeSet<Int> = TreeSet()
        var theme: GUIActivity.GUITheme? = null
        var sendTouch = false
        val root = OverlayView(context)
        init {
            usedIds.add(R.id.root)
            root.id = R.id.root
        }
        inner class OverlayView(c: Context) : FrameLayout(c) {
            var interceptListener : ((MotionEvent) -> Unit)? = null
            override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
                val int = interceptListener
                if (int != null && ev != null) {
                    int(ev)
                }
                return false
            }
            fun inside(ev: MotionEvent) : Boolean {
                val loc = IntArray(2)
                getLocationOnScreen(loc)
                val x = ev.rawX
                val y = ev.rawY
                if (x < loc[0] || x > loc[0]+width || y < loc[1] || y > loc[1]+height) {
                    return false
                }
                return true
            }
            
            @Suppress("UNCHECKED_CAST")
            fun <T> findViewReimplemented(id: Int) : T? {
                return findViewById(id)
            }
        }
    }


}