package com.termux.gui

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.graphics.Matrix
import android.net.LocalSocket
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.termux.gui.protocol.v0.GUIRecyclerViewAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.DataOutputStream
import java.io.FileDescriptor
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.collections.HashMap
import kotlin.math.roundToInt

class Util {
    companion object {
        
        fun getTaskInfo(tasks: LinkedList<ActivityManager.AppTask>, task: ActivityManager.AppTask): ActivityManager.RecentTaskInfo? {
            try {
                return task.taskInfo
            } catch (e: IllegalArgumentException) {
                tasks.remove(task)
            }
            return null
        }
        
        @Suppress("DEPRECATION")
        fun getTaskId(info: ActivityManager.RecentTaskInfo): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                info.taskId
            } else {
                info.id
            }
        }
        
        fun toPX(a: Context, dip: Int): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip.toFloat(), a.resources.displayMetrics).roundToInt()
        }
        
        fun sendMessageFd(w: DataOutputStream, ret: String, s: LocalSocket, fd: FileDescriptor) {
            s.setFileDescriptorsForSend(arrayOf(fd))
            val bytes = ret.toByteArray(StandardCharsets.UTF_8)
            w.writeInt(bytes.size)
            w.write(bytes)
            w.flush()
        }

        fun sendMessage(w: DataOutputStream, ret: String) {
            val bytes = ret.toByteArray(StandardCharsets.UTF_8)
            w.writeInt(bytes.size)
            w.write(bytes)
            w.flush()
        }

        fun generateViewID(rand: Random, a: GUIActivity): Int {
            return generateViewIDRaw(rand, a.usedIds)
        }

        fun generateViewIDRaw(rand: Random, usedIds: MutableSet<Int>): Int {
            var id = rand.nextInt(Integer.MAX_VALUE)
            while (usedIds.contains(id)) {
                id = rand.nextInt(Integer.MAX_VALUE)
            }
            usedIds.add(id)
            return id
        }

        fun runOnUIThreadBlocking(r: Runnable) {
            var e: Exception? = null
            runBlocking(Dispatchers.Main) {
                launch {
                    try {
                        r.run()
                    } catch (ex: java.lang.Exception) {
                        e = ex
                    }
                }
            }
            val ex = e
            if (ex != null) {
                throw ex
            }
        }
        
        fun setViewActivity(a: GUIActivity, v: View, parent: Int?, recid: Int?, recindex: Int?) {
            val t = a.theme
            if (t != null) {
                if (v is TextView) {
                    v.setTextColor(t.textColor)
                }
            }
            if (parent == null) {
                if (t != null) {
                    v.setBackgroundColor(t.windowBackground)
                }
                val fl = a.findViewReimplemented(R.id.root, recid, recindex) as? FrameLayout
                fl?.removeAllViews()
                fl?.addView(v)
            } else {
                val g = a.findViewReimplemented<ViewGroup>(parent, recid, recindex)
                if (g is LinearLayout) {
                    val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1F)
                    if (g.orientation == LinearLayout.VERTICAL) {
                        p.width = LinearLayout.LayoutParams.MATCH_PARENT
                    } else {
                        p.height = LinearLayout.LayoutParams.MATCH_PARENT
                    }
                    v.layoutParams = p
                }
                g?.addView(v)
            }
        }
        
        fun setClickListener(v: View, aid: String, enabled: Boolean, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
            if (enabled) {
                if (v is CompoundButton) {
                    val map = HashMap<String, Any>()
                    map["id"] = v.id
                    map["aid"] = aid
                    v.setOnClickListener {
                        map["set"] = v.isChecked
                        eventQueue.offer(ConnectionHandler.Event("click", ConnectionHandler.gson.toJsonTree(map)))
                    }
                } else {
                    val map = HashMap<String, Any>()
                    map["id"] = v.id
                    map["aid"] = aid
                    val ev = ConnectionHandler.Event("click", ConnectionHandler.gson.toJsonTree(map))
                    v.setOnClickListener { eventQueue.offer(ev) }
                }
            } else {
                v.setOnClickListener(null)
            }
        }

        fun setLongClickListener(v: View, aid: String, enabled: Boolean, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
            if (enabled) {
                val map = HashMap<String, Any>()
                map["id"] = v.id
                map["aid"] = aid
                val ev = ConnectionHandler.Event("longClick", ConnectionHandler.gson.toJsonTree(map))
                v.setOnLongClickListener { eventQueue.offer(ev) }
            } else {
                v.setOnLongClickListener(null)
            }
        }

        fun setFocusChangeListener(v: View, aid: String, enabled: Boolean, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
            if (enabled) {
                val map = HashMap<String, Any>()
                map["id"] = v.id
                map["aid"] = aid
                v.setOnFocusChangeListener { _, hasFocus -> 
                    map["focus"] = hasFocus
                    eventQueue.offer(ConnectionHandler.Event("focusChange", ConnectionHandler.gson.toJsonTree(map)))
                }
            } else {
                v.onFocusChangeListener = null
            }
        }
        
        fun setCheckedListener(v: RadioGroup, aid: String, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
            val args = HashMap<String, Any>()
            args["aid"] = aid
            args["id"] = v.id
            v.setOnCheckedChangeListener { _, checked ->
                args["selected"] = checked
                eventQueue.offer(ConnectionHandler.Event("selected", ConnectionHandler.gson.toJsonTree(args)))
            }
        }

        fun setSpinnerListener(v: Spinner, aid: String, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
            v.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                val args = HashMap<String, Any?>()
                init {
                    args["aid"] = aid
                    args["id"] = v.id
                }
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    args["selected"] = (view as? TextView)?.text?.toString()
                    eventQueue.offer(ConnectionHandler.Event("itemselected", ConnectionHandler.gson.toJsonTree(args)))
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    args["selected"] = null
                    eventQueue.offer(ConnectionHandler.Event("itemselected", ConnectionHandler.gson.toJsonTree(args)))
                }
            }
        }

        fun setRefreshListener(v: SwipeRefreshLayout, aid: String, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
            val args = HashMap<String, Any>()
            args["aid"] = aid
            args["id"] = v.id
            val ev = ConnectionHandler.Event("refresh", ConnectionHandler.gson.toJsonTree(args))
            v.setOnRefreshListener {
                if (v.isRefreshing) {
                    eventQueue.offer(ev)
                }
            }
        }
        
        fun setTabSelectedListener(v: TabLayout, aid: String, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
            val args = HashMap<String, Any>()
            args["aid"] = aid
            args["id"] = v.id
            v.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    if (tab != null) {
                        args["selected"] = tab.position
                        eventQueue.offer(ConnectionHandler.Event("itemselected", ConnectionHandler.gson.toJsonTree(args)))
                    }
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }
        
        private val TOUCH_EVENT_MAP: Map<Int, String>
        init {
            val map = HashMap<Int,String>()
            map[MotionEvent.ACTION_DOWN] = "down"
            map[MotionEvent.ACTION_UP] = "up"
            map[MotionEvent.ACTION_POINTER_DOWN] = "pointer_down"
            map[MotionEvent.ACTION_POINTER_UP] = "pointer_up"
            map[MotionEvent.ACTION_CANCEL] = "cancel"
            map[MotionEvent.ACTION_MOVE] = "move"
            TOUCH_EVENT_MAP = Collections.unmodifiableMap(map)
        }
        
        fun setTextWatcher(v: TextView, aid: String, enabled: Boolean, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
            if (enabled) {
                v.addTextChangedListener(object: TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if (s != null) {
                            val map = HashMap<String, Any>()
                            map["id"] = v.id
                            map["aid"] = aid
                            map["text"] = s.toString()
                            eventQueue.offer(ConnectionHandler.Event("text", ConnectionHandler.gson.toJsonTree(map)))
                        }
                    }
                })
            }
        }
        
        
        
        @SuppressLint("ClickableViewAccessibility")
        fun setTouchListener(v: View, aid: String, enabled: Boolean, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
            if (enabled) {
                val map = HashMap<String, Any>()
                map["id"] = v.id
                map["aid"] = aid
                data class PointerData(val x: Int, val y: Int, val id: Int)
                v.setOnTouchListener { _, event ->
                    val mapped = TOUCH_EVENT_MAP[event.actionMasked]
                    if (mapped != null) {
                        map["action"] = mapped
                        map["index"] = event.actionIndex
                        map["time"] = event.eventTime
                        val pd = LinkedList<PointerData>()
                        val rev = Matrix()
                        var inv = false
                        if (v is ImageView) {
                            inv = v.imageMatrix.invert(rev)
                        }
                        for (i in 0 until event.pointerCount) {
                            if (inv) {
                                // if it is an ImageView, automatically transform from view coordinates to image coordinates
                                val pos = FloatArray(2)
                                pos[0] = event.getX(i)
                                pos[1] = event.getY(i)
                                rev.mapPoints(pos)
                                pd.add(PointerData(pos[0].roundToInt(), pos[1].roundToInt(), event.getPointerId(i)))
                            } else {
                                pd.add(PointerData(event.getX(i).roundToInt(), event.getY(i).roundToInt(), event.getPointerId(i)))
                            }
                        }
                        for (i in 0 until event.historySize) {
                            val pdh = LinkedList<PointerData>()
                            for (a in 0 until event.pointerCount) {
                                if (inv) {
                                    // if it is an ImageView, automatically transform from view coordinates to image coordinates
                                    val pos = FloatArray(2)
                                    pos[0] = event.getHistoricalX(a, i)
                                    pos[1] = event.getHistoricalY(a, i)
                                    rev.mapPoints(pos)
                                    pdh.add(PointerData(pos[0].roundToInt(), pos[1].roundToInt(), event.getPointerId(a)))
                                } else {
                                    pdh.add(PointerData(event.getHistoricalX(a, i).roundToInt(), event.getHistoricalY(a, i).roundToInt(), event.getPointerId(a)))
                                }
                            }
                            map["pointers"] = pdh
                            eventQueue.offer(ConnectionHandler.Event("touch", ConnectionHandler.gson.toJsonTree(map)))
                        }
                        map["pointers"] = pd
                        eventQueue.offer(ConnectionHandler.Event("touch", ConnectionHandler.gson.toJsonTree(map)))
                    }
                    true
                }
            } else {
                v.setOnTouchListener(null)
            }
        }
        
        
        fun removeViewRecursive(v: View?, usedIds: MutableSet<Int>, recyclerviews: HashMap<Int, GUIRecyclerViewAdapter>) {
            if (v != null) {
                if (v is ViewGroup) {
                    if (v is RecyclerView) {
                        val rv = recyclerviews[v.id]
                        if (rv != null) {
                            usedIds.removeAll(rv.exportViewList().map { it.id }.toSet())
                            recyclerviews.remove(v.id)
                        }
                    } else {
                        while (v.childCount > 0) {
                            removeViewRecursive(v.getChildAt(0), usedIds, recyclerviews)
                        }
                    }
                }
                val p = v.parent
                if (p is ViewGroup) {
                    p.removeView(v)
                }
                usedIds.remove(v.id)
            }
        }
    }
}