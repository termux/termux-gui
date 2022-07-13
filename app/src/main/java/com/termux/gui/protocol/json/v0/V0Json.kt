package com.termux.gui.protocol.json.v0

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.net.LocalSocket
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.*
import android.widget.TextView
import android.widget.Toast
import com.termux.gui.*
import com.termux.gui.Util.Companion.runOnUIThreadBlocking
import com.termux.gui.protocol.json.v0.HandleActivityAndTask.Companion.handleActivityTaskMessage
import com.termux.gui.protocol.json.v0.HandleView.Companion.handleView
import com.termux.gui.protocol.shared.v0.DataClasses
import com.termux.gui.protocol.shared.v0.V0Shared
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class V0Json(app: Context, private val eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) : V0Shared(app) {
    

    @SuppressLint("ClickableViewAccessibility")
    @Suppress("DEPRECATION")
    fun handleConnection(main: LocalSocket) {
        val inp = DataInputStream(main.inputStream)
        val out = DataOutputStream(main.outputStream)
        val am = app.getSystemService(ActivityManager::class.java)
        val wm = app.getSystemService(WindowManager::class.java)
        withSystemListenersAndCleanup(am, wm) {
            var msgbytes = ByteArray(0)

            // to use regex in the when clause
            operator fun Regex.contains(text: String?): Boolean = if (text == null) false else this.matches(text)
            while (! Thread.currentThread().isInterrupted) {
                val len = inp.readInt()
                // resize the buffer if needed
                if (len > msgbytes.size) {
                    msgbytes = ByteArray(len)
                }
                inp.readFully(msgbytes, 0, len)
                val msg = msgbytes.decodeToString(0, len, true)
                val m = ConnectionHandler.gson.fromJson(msg, ConnectionHandler.Message::class.java)
                //println(m?.method)
                if (m?.method != null) {
                    if (HandleRemote.handleRemoteMessage(m, remoteviews, rand, out, app)) continue
                    if (handleActivityTaskMessage(m, activities, tasks, overlays, app, wm, out)) continue
                    if (handleView(m, activities, overlays, rand, out, app, eventQueue)) continue
                    if (HandleBuffer.handleBuffer(m, activities, overlays, rand, out, buffers, main)) continue
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
                                        it.findViewReimplemented<View>(id)?.let { Util.setTouchListener(it, aid, send, eventQueue) }
                                    }
                                }
                                if (o != null) {
                                    runOnUIThreadBlocking {
                                        o.root.findViewReimplemented<View>(id)?.let { Util.setTouchListener(it, aid, send, eventQueue) }
                                    }
                                }
                            }
                            if (m.method == "sendFocusChangeEvent") {
                                if (a != null) {
                                    runOnUIThreadActivityStarted(a) {
                                        it.findViewReimplemented<View>(id)?.let { Util.setFocusChangeListener(it, aid, send, eventQueue) }
                                    }
                                }
                                if (o != null) {
                                    runOnUIThreadBlocking {
                                        o.root.findViewReimplemented<View>(id)?.let { Util.setFocusChangeListener(it, aid, send, eventQueue) }
                                    }
                                }
                            }
                            if (m.method == "sendLongClickEvent") {
                                if (a != null) {
                                    runOnUIThreadActivityStarted(a) {
                                        it.findViewReimplemented<View>(id)?.let { Util.setLongClickListener(it, aid, send, eventQueue) }
                                    }
                                }
                                if (o != null) {
                                    runOnUIThreadBlocking {
                                        o.root.findViewReimplemented<View>(id)?.let { Util.setLongClickListener(it, aid, send, eventQueue) }
                                    }
                                }
                            }
                            if (m.method == "sendClickEvent") {
                                if (a != null) {
                                    runOnUIThreadActivityStarted(a) {
                                        it.findViewReimplemented<View>(id)?.let { Util.setClickListener(it, aid, send, eventQueue) }
                                    }
                                }
                                if (o != null) {
                                    runOnUIThreadBlocking {
                                        o.root.findViewReimplemented<View>(id)?.let { Util.setClickListener(it, aid, send, eventQueue) }
                                    }
                                }
                            }
                            if (m.method == "sendTextEvent") {
                                if (a != null) {
                                    runOnUIThreadActivityStarted(a) {
                                        it.findViewReimplemented<TextView>(id)?.let { Util.setTextWatcher(it, aid, send, eventQueue) }
                                    }
                                }
                                if (o != null) {
                                    runOnUIThreadBlocking {
                                        o.root.findViewReimplemented<TextView>(id)?.let { Util.setTextWatcher(it, aid, send, eventQueue) }
                                    }
                                }
                            }
                        }

                        continue
                    }
                }
                eventQueue.offer(ConnectionHandler.INVALID_METHOD)
            }
        }
    }
    
    @Suppress("DEPRECATION")
    private fun handleActivity(m: ConnectionHandler.Message, activities: MutableMap<String, DataClasses.ActivityState>, tasks: LinkedList<ActivityManager.AppTask>, wm: WindowManager,
                               overlays: MutableMap<String, DataClasses.Overlay>, out: DataOutputStream, app: Context, eventQueue: LinkedBlockingQueue<ConnectionHandler.Event>) {
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
                val aid = generateActivityID()
                val o = DataClasses.Overlay(app)
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
    private fun newActivityJSON(tasks: LinkedList<ActivityManager.AppTask>, activities: MutableMap<String, DataClasses.ActivityState>, ptid: Int?, pip: Boolean, dialog: Boolean, lockscreen: Boolean, canceloutside: Boolean): String {
        //println("ptid: $ptid")
        val i = Intent(app, GUIActivity::class.java)
        if (ptid == null) {
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        }
        val aid = generateActivityID()
        i.data = Uri.parse(aid)
        
        activities[aid] = DataClasses.ActivityState(null)
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

    override fun onActivityCreated(state: DataClasses.ActivityState) {
        val map = HashMap<String, Any?>()
        map["finishing"] = state.a?.isFinishing
        map["aid"] = state.a?.intent?.dataString
        eventQueue.offer(ConnectionHandler.Event("create", ConnectionHandler.gson.toJsonTree(map)))
    }

    override fun onActivityStarted(state: DataClasses.ActivityState) {
        val map = HashMap<String, Any?>()
        map["finishing"] = state.a?.isFinishing
        map["aid"] = state.a?.intent?.dataString
        eventQueue.offer(ConnectionHandler.Event("start", ConnectionHandler.gson.toJsonTree(map)))
    }

    override fun onActivityResumed(state: DataClasses.ActivityState) {
        val map = HashMap<String, Any?>()
        map["finishing"] = state.a?.isFinishing
        map["aid"] = state.a?.intent?.dataString
        eventQueue.offer(ConnectionHandler.Event("resume", ConnectionHandler.gson.toJsonTree(map)))
    }

    override fun onActivityPaused(state: DataClasses.ActivityState) {
        val map = HashMap<String, Any?>()
        map["finishing"] = state.a?.isFinishing
        map["aid"] = state.a?.intent?.dataString
        eventQueue.offer(ConnectionHandler.Event("pause", ConnectionHandler.gson.toJsonTree(map)))
    }

    override fun onActivityStopped(state: DataClasses.ActivityState) {
        val map = HashMap<String, Any?>()
        map["finishing"] = state.a?.isFinishing
        map["aid"] = state.a?.intent?.dataString
        eventQueue.offer(ConnectionHandler.Event("stop", ConnectionHandler.gson.toJsonTree(map)))
    }

    override fun onActivityDestroyed(state: DataClasses.ActivityState) {
        val map = HashMap<String, Any?>()
        map["finishing"] = state.a?.isFinishing
        map["aid"] = state.a?.intent?.dataString
        eventQueue.offer(ConnectionHandler.Event("destroy", ConnectionHandler.gson.toJsonTree(map)))
    }

    override fun onAirplaneModeChanged(c: Context, i: Intent) {
        eventQueue.offer(ConnectionHandler.Event("airplane", ConnectionHandler.gson.toJsonTree(i.getBooleanExtra("state", false))))
    }

    override fun onLocaleChanged(c: Context, i: Intent) {
        eventQueue.offer(ConnectionHandler.Event("locale", ConnectionHandler.gson.toJsonTree(c.resources.configuration.locales.get(0).language)))
    }

    override fun onScreenOff(c: Context, i: Intent) {
        eventQueue.offer(ConnectionHandler.Event("screen_off",null))
    }

    override fun onScreenOn(c: Context, i: Intent) {
        eventQueue.offer(ConnectionHandler.Event("screen_on",null))
    }

    override fun onTimezoneChanged(c: Context, i: Intent) {
        eventQueue.offer(ConnectionHandler.Event("timezone", ConnectionHandler.gson.toJsonTree(TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT, c.resources.configuration.locales.get(0)))))
    }

    override fun onWidgetButton(rid: Int, id: Int) {
        val map = HashMap<String, Any?>()
        map["aid"] = rid
        map["id"] = id
        eventQueue.offer(ConnectionHandler.Event("click", ConnectionHandler.gson.toJsonTree(map)))
    }

    override fun onConfigurationChanged(a: GUIActivity, newConfig: Configuration) {
        eventQueue.offer(ConnectionHandler.Event("config", a.configToJson(newConfig)))
    }

    override fun onPictureInPictureModeChanged(a: GUIActivity, isInPictureInPictureMode: Boolean) {
        try {
            eventQueue.add(ConnectionHandler.Event("pipchanged", ConnectionHandler.gson.toJsonTree(isInPictureInPictureMode)))
        } catch (ignored: Exception) {}
    }

    override fun onUserLeaveHint(a: GUIActivity) {
        try {
            eventQueue.add(ConnectionHandler.Event("UserLeaveHint", null))
        } catch (ignored: Exception) {}
    }
}












