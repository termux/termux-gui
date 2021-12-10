package com.termux.gui.protocol.v0

import android.annotation.SuppressLint
import android.app.*
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.net.LocalSocket
import android.net.Uri
import android.os.Build
import android.os.SharedMemory
import android.provider.Settings
import android.view.*
import android.widget.*
import com.termux.gui.*
import com.termux.gui.Util.Companion.runOnUIThreadBlocking
import com.termux.gui.protocol.v0.HandleActivityAndTask.Companion.handleActivityTaskMessage
import com.termux.gui.protocol.v0.HandleView.Companion.handleView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileDescriptor
import java.lang.reflect.InvocationTargetException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.collections.HashMap


class V0(val app: Context) {
    data class SharedBuffer(val btm: Bitmap, val shm: SharedMemory?, val buff: ByteBuffer, val fd: Int?)
    data class WidgetRepresentation(val usedIds: TreeSet<Int> = TreeSet(), var root: RemoteViews?, var theme: GUIActivity.GUITheme?)
    data class ActivityState(var a: GUIActivity?, @Volatile var saved: Boolean = false, val queued: LinkedBlockingQueue<(activity: GUIActivity) -> Unit> = LinkedBlockingQueue<(activity: GUIActivity) -> Unit>(100))
    data class Overlay(val context: Context) {
        val usedIds: TreeSet<Int> = TreeSet()
        val recyclerviews = HashMap<Int, GUIRecyclerViewAdapter>()
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
            fun <T> findViewReimplemented(id: Int, recid: Int?, recindex: Int?) : T? {
                if (recid != null && recindex != null) {
                    val rec = recyclerviews[recid]
                    if (rec != null) {
                        val el = rec.getViewByIndex(recindex)
                        if (el != null) {
                            return el.v.findViewById<View>(id) as? T
                        }
                    }
                    return null
                }
                return findViewById(id)
            }
        }
    }

    private var activityID = 0
    private val rand = Random()



    @SuppressLint("ClickableViewAccessibility")
    @Suppress("DEPRECATION")
    fun handleConnection(ptype: Int, main: LocalSocket, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
        val inp = DataInputStream(main.inputStream)
        val out = DataOutputStream(main.outputStream)
        val am = app.getSystemService(ActivityManager::class.java)
        val wm = app.getSystemService(WindowManager::class.java)
        
        val tasks = LinkedList<ActivityManager.AppTask>()
        val activities = Collections.synchronizedMap(HashMap<String,ActivityState>())
        val buffers: MutableMap<Int, SharedBuffer> = HashMap()
        val widgets: MutableMap<Int,WidgetRepresentation> = HashMap()
        val overlays = Collections.synchronizedMap(HashMap<String,Overlay>())
        
        val lifecycleCallbacks = LifecycleListener(eventQueue, activities, tasks, am)
        App.APP?.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        val sysrec = SystemBroadcastReceiver(eventQueue)
        val filter = IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_LOCALE_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        App.APP?.registerReceiver(sysrec, filter)
        try {
            var msgbytes = ByteArray(0)
            
            // to use regex in the when clause
            operator fun Regex.contains(text: String?): Boolean = if (text == null) false else this.matches(text)
            while (!Thread.currentThread().isInterrupted) {
                val len = inp.readInt()
                // resize the buffer if needed
                if (len > msgbytes.size) {
                    msgbytes = ByteArray(len)
                }
                inp.readFully(msgbytes, 0, len)
                when (ptype) {
                    1 -> {
                        val msg = msgbytes.decodeToString(0, len, true)
                        val m = ConnectionHandler.gson.fromJson(msg, ConnectionHandler.Message::class.java)
                        //println(m?.method)
                        if (m?.method != null) {
                            if (handleActivityTaskMessage(m, activities, tasks, widgets, overlays, app, wm)) continue
                            if (handleView(m, activities, widgets, overlays, rand, out, app, eventQueue)) continue
                        }
                        when (m?.method) {
                            // Activity and Task methods
                            "newActivity" -> {
                                handleActivity(m, activities, tasks, wm, overlays, out, app, eventQueue)
                                continue
                            }
                            "finishActivity" -> {
                                val aid = m.params?.get("aid")?.asString
                                val a = activities[aid]
                                val o = overlays[aid]
                                if (a != null) {
                                    runOnUIThreadActivityStarted(a) {
                                        it.finish()
                                    }
                                }
                                if (o != null) {
                                    wm.removeView(o.root)
                                    overlays[aid] = null
                                }
                                continue
                            }
                            "addBuffer" -> {
                                val format = m.params?.get("format")?.asString
                                val w = m.params?.get("w")?.asInt
                                val h = m.params?.get("h")?.asInt
                                if (w != null && h != null && format == "ARGB888" && w > 0 && h > 0) {
                                    val bid = generateBufferID(rand, buffers)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                        println("creating buffer on API 27+")
                                        val shm = SharedMemory.create(bid.toString(), w * h * 4)
                                        val b = SharedBuffer(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888, true), shm, shm.mapReadOnly(), null)
                                        try {
                                            // this is a dirty trick to get the FileDescriptor of a SharedMemory object without using JNI or a higher API version.
                                            // this could break anytime, though it is still marked as public but discouraged, so that is unlikely, also given the implementation of the class.
                                            // noinspection DiscouragedPrivateApi
                                            val getFd = SharedMemory::class.java.getDeclaredMethod("getFd")
                                            val fdint = getFd.invoke(shm) as? Int
                                            if (fdint == null) {
                                                println("fd empty or not a Int")
                                                shm.close()
                                                SharedMemory.unmap(b.buff)
                                                b.btm.recycle()
                                                Util.sendMessage(out, ConnectionHandler.gson.toJson(-1))
                                                continue
                                            }
                                            val fdesc = FileDescriptor()
                                            val setInt =  FileDescriptor::class.java.getDeclaredMethod("setInt$", Int::class.java)
                                            setInt(fdesc, fdint)
                                            Util.sendMessageFd(out, ConnectionHandler.gson.toJson(bid), main, fdesc)
                                            buffers[bid] = b
                                        } catch (e: Exception) {
                                            SharedMemory.unmap(b.buff)
                                            b.shm?.close()
                                            b.btm.recycle()
                                            if (e is NoSuchMethodException || e is IllegalArgumentException || e is IllegalAccessException ||
                                                    e is InstantiationException || e is InvocationTargetException) {
                                                println("reflection exception")
                                                e.printStackTrace()
                                                Util.sendMessage(out, ConnectionHandler.gson.toJson(-1))
                                            } else {
                                                throw e
                                            }
                                        }
                                    } else {
                                        println("creating buffer on API 26-")
                                        val fdint = ConnectionHandler.create_ashmem(w * h * 4)
                                        if (fdint == -1) {
                                            println("could not create ashmem with NDK")
                                            Util.sendMessage(out, ConnectionHandler.gson.toJson(-1))
                                            return
                                        }
                                        val buff: ByteBuffer? = ConnectionHandler.map_ashmem(fdint, w * h * 4)
                                        if (buff == null) {
                                            println("could not map ashmem with NDK")
                                            ConnectionHandler.destroy_ashmem(fdint)
                                            Util.sendMessage(out, ConnectionHandler.gson.toJson(-1))
                                            return
                                        }
                                        try {
                                            val fdesc = FileDescriptor()
                                            val setInt =  FileDescriptor::class.java.getDeclaredMethod("setInt$", Int::class.java)
                                            setInt(fdesc, fdint)
                                            Util.sendMessageFd(out, ConnectionHandler.gson.toJson(bid), main, fdesc)
                                            buffers[bid] = SharedBuffer(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888), null, buff, fdint)
                                        } catch (e: Exception) {
                                            ConnectionHandler.unmap_ashmem(buff)
                                            ConnectionHandler.destroy_ashmem(fdint)
                                            if (e is NoSuchMethodException || e is IllegalArgumentException || e is IllegalAccessException ||
                                                    e is InstantiationException || e is InvocationTargetException) {
                                                println("reflection exception")
                                                e.printStackTrace()
                                                Util.sendMessage(out, ConnectionHandler.gson.toJson(-1))
                                            } else {
                                                throw e
                                            }
                                        }
                                    }
                                } else {
                                    println("invalid parameters")
                                    Util.sendMessage(out, ConnectionHandler.gson.toJson(-1))
                                }
                                continue
                            }
                            "deleteBuffer" -> {
                                val bid = m.params?.get("bid")?.asInt
                                val buffer = buffers[bid]
                                if (buffer != null) {
                                    buffers.remove(bid)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                        SharedMemory.unmap(buffer.buff)
                                        buffer.shm?.close()
                                    } else {
                                        val fd = buffer.fd
                                        if (fd != null) {
                                            ConnectionHandler.unmap_ashmem(buffer.buff)
                                            ConnectionHandler.destroy_ashmem(fd)
                                        }
                                    }
                                }
                                continue
                            }
                            "blitBuffer" -> {
                                val buffer = buffers[m.params?.get("bid")?.asInt]
                                if (buffer != null) {
                                    buffer.btm.copyPixelsFromBuffer(buffer.buff)
                                    buffer.buff.position(0)
                                }
                                continue
                            }
                            "setBuffer" -> {
                                val aid = m.params?.get("aid")?.asString
                                val a = activities[aid]
                                val id = m.params?.get("id")?.asInt
                                val buffer = buffers[m.params?.get("bid")?.asInt]
                                val o = overlays[aid]
                                if (buffer != null && id != null) {
                                    if (a != null) {
                                        runOnUIThreadActivityStarted(a) {
                                            it.findViewReimplemented<ImageView>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.setImageBitmap(buffer.btm)
                                        }
                                    }
                                    if (o != null) {
                                        runOnUIThreadBlocking {
                                            o.root.findViewReimplemented<ImageView>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.setImageBitmap(buffer.btm)
                                        }
                                    }
                                }
                                continue
                            }
                            "refreshImageView" -> {
                                val aid = m.params?.get("aid")?.asString
                                val a = activities[aid]
                                val id = m.params?.get("id")?.asInt
                                val o = overlays[aid]
                                if (id != null) {
                                    if (a != null) {
                                        runOnUIThreadActivityStarted(a) {
                                            it.findViewReimplemented<ImageView>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.invalidate()
                                        }
                                    }
                                    if (o != null) {
                                        runOnUIThreadBlocking {
                                            o.root.findViewReimplemented<ImageView>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.invalidate()
                                        }
                                    }
                                }
                                continue
                            }
                            "bindWidget" -> {
                                val wid = m.params?.get("wid")?.asInt
                                if (wid != null && !widgets.containsKey(wid)) {
                                    widgets[wid] = WidgetRepresentation(TreeSet(), null, null)
                                }
                                continue
                            }
                            "blitWidget" -> {
                                val wid = m.params?.get("wid")?.asInt
                                val root = widgets[wid]?.root
                                if (wid != null && widgets.containsKey(wid) && root != null) {
                                    println("updated widget")
                                    app.getSystemService(AppWidgetManager::class.java).updateAppWidget(wid, root)
                                }
                                continue
                            }
                            "clearWidget" -> {
                                val wid = m.params?.get("wid")?.asInt
                                val v = widgets[wid]
                                if (v != null) {
                                    v.root = null
                                    v.usedIds.clear()
                                }
                                continue
                            }
                            // Event methods
                            in Regex("send.*Event") -> {
                                val aid = m.params?.get("aid")?.asString
                                val a = activities[aid]
                                val id = m.params?.get("id")?.asInt
                                val o = overlays[aid]
                                val send = m.params?.get("send")?.asBoolean ?: false
                                if (m.method == "sendOverlayTouchEvent" && o != null) {
                                    o.sendTouch = send
                                }
                                if (id != null && aid != null) {
                                    if (m.method == "sendTouchEvent") {
                                        if (a != null) {
                                            runOnUIThreadActivityStarted(a) {
                                                it.findViewReimplemented<View>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.let { Util.setTouchListener(it, aid, send, eventQueue) }
                                            }
                                        }
                                        if (o != null) {
                                            runOnUIThreadBlocking {
                                                o.root.findViewReimplemented<View>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.let { Util.setTouchListener(it, aid, send, eventQueue) }
                                            }
                                        }
                                    }
                                    if (m.method == "sendFocusChangeEvent") {
                                        if (a != null) {
                                            runOnUIThreadActivityStarted(a) {
                                                it.findViewReimplemented<View>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.let { Util.setFocusChangeListener(it, aid, send, eventQueue) }
                                            }
                                        }
                                        if (o != null) {
                                            runOnUIThreadBlocking {
                                                o.root.findViewReimplemented<View>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.let { Util.setFocusChangeListener(it, aid, send, eventQueue) }
                                            }
                                        }
                                    }
                                    if (m.method == "sendLongClickEvent") {
                                        if (a != null) {
                                            runOnUIThreadActivityStarted(a) {
                                                it.findViewReimplemented<View>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.let { Util.setLongClickListener(it, aid, send, eventQueue) }
                                            }
                                        }
                                        if (o != null) {
                                            runOnUIThreadBlocking {
                                                o.root.findViewReimplemented<View>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.let { Util.setLongClickListener(it, aid, send, eventQueue) }
                                            }
                                        }
                                    }
                                    if (m.method == "sendClickEvent") {
                                        if (a != null) {
                                            runOnUIThreadActivityStarted(a) {
                                                it.findViewReimplemented<View>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.let { Util.setClickListener(it, aid, send, eventQueue) }
                                            }
                                        }
                                        if (o != null) {
                                            runOnUIThreadBlocking {
                                                o.root.findViewReimplemented<View>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.let { Util.setClickListener(it, aid, send, eventQueue) }
                                            }
                                        }
                                    }
                                    if (m.method == "sendTextEvent") {
                                        if (a != null) {
                                            runOnUIThreadActivityStarted(a) {
                                                it.findViewReimplemented<TextView>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.let {Util.setTextWatcher(it, aid, send, eventQueue)}
                                            }
                                        }
                                        if (o != null) {
                                            runOnUIThreadBlocking {
                                                o.root.findViewReimplemented<TextView>(id, m.params?.get("recyclerview")?.asInt, m.params?.get("recyclerindex")?.asInt)?.let {Util.setTextWatcher(it, aid, send, eventQueue)}
                                            }
                                        }
                                    }
                                }
                                
                                continue
                            }
                        }
                        eventQueue.offer(ConnectionHandler.INVALID_METHOD)
                    }
                    0 -> {
                        
                    }
                }
            }
        } finally {
            println("cleanup V0")
            for (o in overlays.values) {
                wm.removeView(o.root)
            }
            App.APP?.unregisterReceiver(sysrec)
            App.APP?.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
            for (t in tasks) {
                try {
                    t.finishAndRemoveTask()
                } catch (ignored: Exception) {}
            }
            for (a in activities.values) {
                try {
                    a.a?.finishAndRemoveTask()
                } catch (ignored: Exception) {}
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                for (b in buffers.values) {
                    SharedMemory.unmap(b.buff)
                    b.shm?.close()
                }
            } else {
                for (b in buffers.values) {
                    val fd = b.fd
                    if (fd != null) {
                        ConnectionHandler.unmap_ashmem(b.buff)
                        ConnectionHandler.destroy_ashmem(fd)
                    }
                }
            }
        }
    }
    
    @Suppress("DEPRECATION")
    private fun handleActivity(m: ConnectionHandler.Message, activities: MutableMap<String, ActivityState>, tasks: LinkedList<ActivityManager.AppTask>, wm: WindowManager,
                               overlays: MutableMap<String, Overlay>, out: DataOutputStream, app: Context,eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
        for (t in tasks) {
            try {
                if (t.taskInfo == null) {
                    tasks.remove(t)
                }
            } catch (e: IllegalArgumentException) {
                tasks.remove(t)
            }
        }
        if (m.params?.get("overlay")?.asBoolean == true) {
            if (!Settings.canDrawOverlays(app)) {
                try {
                    val a = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    a.data = Uri.parse(app.packageName)
                    app.startActivity(a)
                } catch (ignored: Exception) {
                    runOnUIThreadBlocking {
                        Toast.makeText(app, R.string.overlay_settings, Toast.LENGTH_LONG).show()
                    }
                }
                Util.sendMessage(out, ConnectionHandler.gson.toJson(arrayOf("-1")))
            } else {
                val aid = Thread.currentThread().id.toString() + "-" + activityID.toString()
                activityID++
                val o = Overlay(app)
                overlays[aid] = o
                try {
                    runOnUIThreadBlocking {
                        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE, 0, PixelFormat.RGBA_8888)
                        params.x = 0
                        params.y = 0
                        params.gravity = Gravity.START or Gravity.TOP
                        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        val scale = ScaleGestureDetector(app, object : ScaleGestureDetector.OnScaleGestureListener {
                            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                                //println(detector?.currentSpan)
                                if (o.sendTouch) {
                                    eventQueue.offer(ConnectionHandler.Event("overlayScale", ConnectionHandler.gson.toJsonTree(detector?.currentSpan)))
                                }
                                return true
                            }

                            override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
                                return true
                            }

                            override fun onScaleEnd(detector: ScaleGestureDetector?) {}
                        })
                        o.root.interceptListener = { e ->
                            //println(e)
                            if (o.sendTouch) {
                                val map = HashMap<String, Any?>()
                                map["x"] = e.rawX
                                map["y"] = e.rawY
                                map["action"] = when (e.action) {
                                    MotionEvent.ACTION_DOWN -> "down"
                                    MotionEvent.ACTION_UP -> "up"
                                    MotionEvent.ACTION_MOVE -> "move"
                                    else -> null
                                }
                                if (map["action"] != null) {
                                    eventQueue.offer(ConnectionHandler.Event("overlayTouch", ConnectionHandler.gson.toJsonTree(map)))
                                }
                            }
                            if (o.root.inside(e)) {
                                scale.onTouchEvent(e)
                                //println("inside")
                                params.flags = 0
                                wm.updateViewLayout(o.root, params)
                            } else {
                                //println("outside")
                                scale.onTouchEvent(e)
                                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                wm.updateViewLayout(o.root, params)
                            }
                        }

                        wm.addView(o.root, params)
                    }
                    Util.sendMessage(out, ConnectionHandler.gson.toJson(aid))
                } catch (e: Exception) {
                    e.printStackTrace()
                    overlays.remove(aid)
                    Util.sendMessage(out, ConnectionHandler.gson.toJson("-1"))
                }
            }
            return
        }
        Util.sendMessage(out, newActivityJSON(tasks, activities, m.params?.get("tid")?.asInt,
                m.params?.get("pip")?.asBoolean
                        ?: false, m.params?.get("dialog")?.asBoolean
                ?: false,
                m.params?.get("lockscreen")?.asBoolean ?: false, m.params?.get("canceloutside")?.asBoolean ?: true))
    }


    @Suppress("DEPRECATION")
    private fun newActivityJSON(tasks: LinkedList<ActivityManager.AppTask>, activities: MutableMap<String, ActivityState>, ptid: Int?, pip: Boolean, dialog: Boolean, lockscreen: Boolean, canceloutside: Boolean): String {
        //println("ptid: $ptid")
        val i = Intent(app, GUIActivity::class.java)
        if (ptid == null) {
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        }
        val aid = Thread.currentThread().id.toString()+"-"+activityID.toString()
        i.data = Uri.parse(aid)
        activityID++
        
        activities[aid] = ActivityState(null)
        i.putExtra("pip", pip)
        when {
            pip -> {
                i.flags = i.flags or Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            dialog -> { // pip overrides dialog
                i.setClass(app, GUIActivityDialog::class.java)
                i.putExtra("canceloutside", canceloutside)
            }
            lockscreen -> { // dialog overrides lockscreen
                i.setClass(app, GUIActivityLockscreen::class.java)
            }
        }
        
        var task: ActivityManager.AppTask? = null
        if (ptid == null) {
            app.startActivity(i)
        } else {
            for (t in tasks) {
                if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            t.taskInfo?.taskId == ptid
                        } else {
                            t.taskInfo?.id == ptid
                        }) {
                    task = t
                    break
                }
            }
            if (task == null) {
                return ConnectionHandler.gson.toJson(arrayOf("-1",-1))
            }
            task.startActivity(app, i, null)
        }
        while (true) {
            if (activities[aid]?.a != null) {
                break
            }
            Thread.sleep(1)
        }
        return if (ptid == null) {
            ConnectionHandler.gson.toJson(arrayOf(aid, activities[aid]?.a?.taskId ?: 0))
        } else {
            ConnectionHandler.gson.toJson(aid)
        }
    }
    companion object {
        fun setViewOverlay(o: Overlay, v: View, parent: Int?, recid: Int?, recindex: Int?) {
            val t = o.theme
            if (v is TextView) {
                if (t != null) {
                    v.setTextColor(t.textColor)
                } else {
                    v.setTextColor((0xffffffff).toInt())
                }
            }
            if (parent == null) {
                if (t != null) {
                    v.setBackgroundColor(t.windowBackground)
                } else {
                    v.setBackgroundColor((0xff000000).toInt())
                }
                val fl = o.root
                println("removing views")
                fl.removeAllViews()
                println("adding view")
                fl.addView(v)
            } else {
                val g = o.root.findViewReimplemented<ViewGroup>(parent, recid, recindex)
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
        
        fun setViewWidget(w: WidgetRepresentation, v: RemoteViews, parent: Int?, id: Int) {
            if (parent == null) {
                if (w.theme != null) {
                    v.setInt(id, "setBackgroundColor", w.theme!!.windowBackground)
                } else {
                    v.setInt(id, "setBackgroundColor", R.color.widget_background)
                }
                w.root = v
            } else {
                w.root?.addView(parent, v)
            }
        }
        
        fun runOnUIThreadActivityStarted(a: ActivityState, r: (activity: GUIActivity) -> Unit) {
            var e: Exception? = null
            var stopped = false
            runBlocking(Dispatchers.Main) {
                launch {
                    val ac = a.a
                    if (a.saved || ac == null) {
                        stopped = true
                        return@launch
                    }
                    try {
                        r(ac)
                    } catch (ex: java.lang.Exception) {
                        e = ex
                    }
                }
            }
            val ex = e
            if (ex != null) {
                throw ex
            }
            if (stopped) {
                println("queueing Runnable")
                a.queued.offer(r)
            }
        }
        
        fun runOnUIThreadActivityStartedBlocking(a: ActivityState, r: (activity: GUIActivity) -> Unit) : Boolean {
            var e: Exception? = null
            var stopped = false
            runBlocking(Dispatchers.Main) {
                launch {
                    val ac = a.a
                    if (a.saved || ac == null) {
                        stopped = true
                        return@launch
                    }
                    try {
                        r(ac)
                    } catch (ex: java.lang.Exception) {
                        e = ex
                    }
                }
            }
            val ex = e
            if (ex != null) {
                throw ex
            }
            return ! stopped
        }
        
        fun generateWidgetViewID(rand: Random, w: WidgetRepresentation): Int {
            return Util.generateViewIDRaw(rand, w.usedIds)
        }
        
        fun generateBufferID(rand: Random, buffers: MutableMap<Int, SharedBuffer>): Int {
            var id = rand.nextInt(Integer.MAX_VALUE)
            while (buffers.contains(id)) {
                id = rand.nextInt(Integer.MAX_VALUE)
            }
            return id
        }
    }
}












